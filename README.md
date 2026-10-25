# AI Website Studio — three-agent team

This repository is a website-building workflow, **not LEADATLAS**.

## Team
- **Designer** creates a concrete visual design specification.
- **Developer** writes a responsive, interactive single-file website.
- **QA** reviews the HTML and requests fixes, up to two revisions.

## Run from GitHub
1. In repository **Settings → Secrets and variables → Actions → New repository secret**, add `OPENAI_API_KEY` with an OpenAI API key. **Never paste the key into chat or source code.** API usage is billed separately from ChatGPT.
2. In **Settings → Actions → General**, enable workflow runs. Under **Workflow permissions**, allow **Read and write permissions** and enable **Allow GitHub Actions to create and approve pull requests** if those options are offered.
3. Open **Actions → Generate website with AI team → Run workflow**.
4. Enter a detailed brief (topic, sections, visual style, language, functions) and run.
5. Open the resulting pull request, inspect `index.html`, `DESIGN_SPEC.md` and `QA_REPORT.md`. Merge only after your review.

## How it works
GitHub Actions runs `scripts/generate_site.py` using the OpenAI Responses API. The orchestrator runs Designer → Developer → QA, with at most 3 development attempts. On approval it creates a **pull request**, not a direct deployment. A separate workflow validates structure and JS syntax. Generated HTML uses inline CSS and JS, so it can be published on Netlify after review.

## Limitations
The QA role currently performs **AI code review only**, not browser automation, screenshots or visual comparisons. API calls require a valid funded API key and model access. This workflow is not a continuously running background agent: it starts when you manually trigger GitHub Actions. It cannot be started by simply sending a message to ChatGPT until a separate chat-to-GitHub integration is built. The generated site should be checked in a real browser before deployment.

## Security
Only run this on a trusted repository. Keep API credentials in GitHub Actions Secrets. Do not place real customer data or credentials in the website brief. Review all AI-generated content before publishing.
