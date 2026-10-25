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
    instruction = ("Build a complete responsive single-file index.html using embedded CSS "
                   "and vanilla JS. Output ONLY the HTML, no markdown. All interactions "
                   "must work without external APIs or keys. Do not include fake testimonials, "
                   "invented facts, fake contact details or misleading claims. "
                   "Do not use remote JavaScript. Use accessible semantic HTML.")
    prompt = "USER REQUEST:\n" + brief + "\nDESIGN SPEC:\n" + design
    feedback = ""
    for attempt in range(3):
        html = extract_html(ask(developer + "\n" + instruction, prompt + feedback))
        review = ask(
            qa + "\nReturn JSON ONLY: {\"approved\": boolean, \"issues\": [string]}. "
            "Review the provided HTML as code only; never claim browser testing.",
            "USER REQUEST:\n" + brief + "\nDESIGN SPEC:\n" + design
            + "\nHTML:\n" + html[:90000],
        )
        try:
            match = re.search(r"\{[\s\S]*\}", review)
            verdict = json.loads(match.group(0)) if match else {}
        except (ValueError, AttributeError):
            verdict = {"approved": False, "issues": ["QA response was not valid JSON"]}
        if verdict.get("approved") is True:
            (ROOT / "index.html").write_text(html, encoding="utf-8")
            (ROOT / "QA_REPORT.md").write_text(
                "# QA review\n\nApproved by model-based code review. "
                "Browser/visual tests were NOT run.\n\n"
                + "\n".join("- " + str(i) for i in verdict.get("issues", [])) + "\n",
                encoding="utf-8")
            print("Generated index.html; QA code review approved")
            return
        feedback = "\nQA ISSUES TO FIX:\n" + json.dumps(
            verdict.get("issues", ["QA did not approve"]), ensure_ascii=False)
    (ROOT / "QA_REPORT.md").write_text(
        "# QA review\n\nRejected after three attempts. No website published.\n"
        + feedback, encoding="utf-8")
    raise RuntimeError("QA did not approve after three attempts")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("Generation failed: " + str(exc), file=sys.stderr)
        sys.exit(1)    choices = result.get("choices", [])
    content = choices[0].get("message", {}).get("content", "") if choices else ""
    if isinstance(content, list):
        answer = "\n".join(part.get("text", "") for part in content if isinstance(part, dict)).strip()
    else:
        answer = str(content or "").strip()
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
    instruction = ("Build a complete responsive single-file index.html using embedded CSS "
                   "and vanilla JS. Output ONLY the HTML, no markdown. All interactions "
                   "must work without external APIs or keys. Do not include fake testimonials, "
                   "invented facts, fake contact details or misleading claims. "
                   "Do not use remote JavaScript. Use accessible semantic HTML.")
    prompt = "USER REQUEST:\n" + brief + "\nDESIGN SPEC:\n" + design
    feedback = ""
    for attempt in range(3):
        html = extract_html(ask(developer + "\n" + instruction, prompt + feedback))
        review = ask(
            qa + "\nReturn JSON ONLY: {\"approved\": boolean, \"issues\": [string]}. "
            "Review the provided HTML as code only; never claim browser testing.",
            "USER REQUEST:\n" + brief + "\nDESIGN SPEC:\n" + design
            + "\nHTML:\n" + html[:90000],
        )
        try:
            match = re.search(r"\{[\s\S]*\}", review)
            verdict = json.loads(match.group(0)) if match else {}
        except (ValueError, AttributeError):
            verdict = {"approved": False, "issues": ["QA response was not valid JSON"]}
        if verdict.get("approved") is True:
            (ROOT / "index.html").write_text(html, encoding="utf-8")
            (ROOT / "QA_REPORT.md").write_text(
                "# QA review\n\nApproved by model-based code review. "
                "Browser/visual tests were NOT run.\n\n"
                + "\n".join("- " + str(i) for i in verdict.get("issues", [])) + "\n",
                encoding="utf-8")
            print("Generated index.html; QA code review approved")
            return
        feedback = "\nQA ISSUES TO FIX:\n" + json.dumps(
            verdict.get("issues", ["QA did not approve"]), ensure_ascii=False)
    (ROOT / "QA_REPORT.md").write_text(
        "# QA review\n\nRejected after three attempts. No website published.\n"
        + feedback, encoding="utf-8")
    raise RuntimeError("QA did not approve after three attempts")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print("Generation failed: " + str(exc), file=sys.stderr)
        sys.exit(1)
