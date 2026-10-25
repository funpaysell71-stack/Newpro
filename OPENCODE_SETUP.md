# OpenCode agent setup

The ready-made OpenCode GitHub Action is configured in `.github/workflows/opencode.yml`.

1. Add the repository secret `OPENROUTER_API_KEY` (already configured if you followed setup).
2. In **Settings → Actions → General**, grant workflows **Read and write permissions**.
3. In **Issues**, create a new issue describing the website to build.
4. Add a comment beginning with `/oc` (or `/opencode`) and describing the task.
5. Open **Actions → OpenCode website agent** to inspect the run. Review the resulting pull request before merging.

Only comments from OWNER, MEMBER, or COLLABORATOR can start this workflow.
The agent uses OpenRouter's free router `openrouter/free` through the OpenCode model identifier `openrouter/openrouter/free`. Free capacity, model compatibility and output quality are not guaranteed.
The earlier custom `Generate website with AI team` workflow remains in the repository for now but is not the recommended route.
