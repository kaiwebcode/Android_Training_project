# Project guidance for coding agents

## Bootcamp Workflow — Week 2: Build with AI Agents
- Follow the current day's scope in `docs/CURRENT_STATUS.md` and context in `docs/AI_HANDOFF.md`.
- Read `docs/REQUIREMENTS.md`, `docs/DEVELOPMENT_PLAN.md`, `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, and `docs/API_CONTRACT.md` before implementing feature changes.
- **Day 6 is planning only.** Do not write API or feature code until trainer approval is recorded in `docs/DECISIONS.md` and `docs/CURRENT_STATUS.md`.
- On Day 7, use the approved plan for REST API integration and handle all 4 UI states (`Loading`, `Content/Success`, `Empty`, `Error`).
- On Day 8, follow the 4-step defect debugging framework: **Reproduce → Observe → Isolate → Verify** (`docs/DEBUGGING_LOG.md`).
- On Day 9, ensure ViewModel & Repository unit tests pass, generate debug APK, and conduct diff reviews before opening a PR.

## Project Conventions
- Preserve existing Kotlin Android architecture (MVVM, `StateFlow`, `ViewModel`, `RecyclerView` / `ListAdapter`, XML layouts) unless explicitly approved.
- Keep UI rendering, ViewModel state/event handling, and Repository data operations strictly separated as documented in `docs/ARCHITECTURE.md`.
- Handle network errors gracefully and map API data to immutable domain models.
- Never claim builds, tests, or runtime checks passed unless actually executed; record results in `docs/CURRENT_STATUS.md`.
- Keep credentials and secrets out of prompts, source code, and Git.
