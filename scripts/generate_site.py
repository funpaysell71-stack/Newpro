#!/usr/bin/env python3
"""Three-role website generation pipeline. Uses only Python standard library."""
import json
import os
import pathlib
import re
import sys
import urllib.error
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
MODEL = os.environ.get("OPENROUTER_MODEL", "openrouter/free")
API_KEY = os.environ.get("OPENROUTER_API_KEY")
MAX_CHARS = 12000


def ask(role, prompt):
    if not API_KEY:
        raise RuntimeError("OPENROUTER_API_KEY is not set")
    payload = json.dumps({
        "model": MODEL,
        "messages": [{"role": "system", "content": role}, {"role": "user", "content": prompt}],
        "max_tokens": 9000,
    }).encode("utf-8")
    req = urllib.request.Request(
        "https://openrouter.ai/api/v1/chat/completions",
        data=payload,
        headers={"Authorization": "Bearer " + API_KEY,
                 "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=180) as response:
            result = json.load(response)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", "replace")[:500]
        raise RuntimeError("OpenRouter API HTTP " + str(exc.code) + ": " + body) from None
    choices = result.get("choices") or []
    answer = (choices[0].get("message", {}).get("content") or "").strip() if choices else ""
    if not answer:
        raise RuntimeError("Model returned no text; check model access and token limits")
    return answer


def extract_html(answer):
    match = re.search(r"```(?:html)?\s*([\s\S]*?)```", answer, re.I)
    html = match.group(1).strip() if match else answer.strip()
    if "<!doctype html" not in html.lower() or "</html>" not in html.lower():
        raise ValueError("Developer did not return a complete HTML document")
    if re.search(r"<script[^>]+src=[\"']https?://", html, re.I):
        raise ValueError("Remote JavaScript is not permitted")
    if len(html) > 400000:
        raise ValueError("Generated HTML exceeds size limit")
    return html


def main():
    brief = os.environ.get("SITE_BRIEF", "").strip()
    if not brief or len(brief) > MAX_CHARS:
        raise ValueError("SITE_BRIEF must be 1–12000 characters")
    designer = (ROOT / ".github/agents/designer.md").read_text(encoding="utf-8")
    developer = (ROOT / ".github/agents/developer.md").read_text(encoding="utf-8")
    qa = (ROOT / ".github/agents/qa.md").read_text(encoding="utf-8")
    design = ask(designer + "\nReturn a specific, practical design brief.", brief)
    (ROOT / "DESIGN_SPEC.md").write_text(design + "\n", encoding="utf-8")
    base = "USER REQUEST:\\n" + brief + "\\nDESIGN SPEC:\\n" + design[:5000]
    parts = [
        ("HTML", "Return ONLY HTML markup for the page BODY, including a header, main sections and footer. No <html>, <head>, <style> or <script>. Keep under 9000 characters. Use class names consistently; use buttons for actions. No markdown."),
        ("CSS", "Return ONLY plain CSS for the website. No <style> tags, no markdown. Premium visual design, mobile responsive, animations, readable typography. Target the HTML below. Keep under 12000 characters."),
        ("JS", "Return ONLY vanilla JavaScript for functional interactions in the HTML below. No <script> tags, no markdown. Keep under 4500 characters. Do not use external APIs.")
    ]
    outputs = {}
    for name, instruction in parts:
        print("Generating " + name, flush=True)
        context = base
        if name != "HTML":
            context += "\\nHTML BODY:\\n" + outputs["HTML"]
        raw = ask(developer + "\\n" + instruction, context)
        raw = re.sub(r"^\\s*```(?:html|css|javascript|js)?\\s*|\\s*```\\s*$", "", raw.strip(), flags=re.I).strip()
        if not raw:
            raise RuntimeError(name + " generation returned empty content")
        outputs[name] = raw
    body = outputs["HTML"]
    if "<body" in body.lower():
        match = re.search(r"<body[^>]*>([\\s\\S]*?)</body>", body, re.I)
        if match:
            body = match.group(1)
    if not re.search(r"<main\\b", body, re.I):
        raise RuntimeError("Developer did not provide a main page section")
    css = outputs["CSS"].replace("</style", "<\\/style")
    js = outputs["JS"].replace("</script", "<\\/script")
    html = ("<!DOCTYPE html>\\n<html lang='ru'>\\n<head>\\n"
            "<meta charset='utf-8'>\\n<meta name='viewport' content='width=device-width, initial-scale=1'>\\n"
            "<title>Website</title>\\n<style>\\n" + css + "\\n</style>\\n</head>\\n"
            "<body>\\n" + body + "\\n<script>\\n" + js + "\\n</script>\\n</body>\\n</html>")
    html = extract_html(html)
    (ROOT / "index.html").write_text(html, encoding="utf-8")
    review = ask(qa + "\nReturn ONLY compact JSON with keys approved (boolean) and issues (array of strings). "
                 "Review source code, not browser behavior. Focus on critical broken behavior.",
                 "REQUEST:\\n" + brief[:1200] + "\\nHTML:\\n" + html[:18000])
    match = re.search(r"\\{[\\s\\S]*\\}", review)
    try:
        verdict = json.loads(match.group(0)) if match else {}
    except ValueError:
        verdict = {"approved": False, "issues": ["QA response was not valid JSON"]}
    issues = verdict.get("issues", [])
    (ROOT / "QA_REPORT.md").write_text(
        "# QA review\\n\\nAI code review only; browser tests were not run.\\n\\n"
        + "Approved: " + str(verdict.get("approved") is True) + "\\n\\n"
        + "\\n".join("- " + str(issue) for issue in issues) + "\\n", encoding="utf-8")
    if verdict.get("approved") is not True:
        raise RuntimeError("QA rejected the generated site: " + str(issues)[:300])
    print("Generated HTML, CSS and JavaScript separately; QA approved", flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("Generation failed: " + str(exc), file=sys.stderr)
        sys.exit(1)
