# AI Agent Guidelines & Documentation Rules

This file defines the conventions for development, commits, and automated documentation updates for the PocketMind project.

## 1. Commit Message Conventions
All commits must follow these prefixes to ensure the automated documentation bot can categorize changes correctly:

- `FEATURE: {description}` - New features or capabilities.
- `BUG: {description}` - Fixes for bugs or build errors.
- `UI: {description}` - Visual changes, layout fixes, or styling updates.
- `PERF: {description}` - Performance optimizations.
- `SEC: {description}` - Security related changes.
- `DOCS: {description}` - Changes to documentation files.
- `REFACTOR: {description}` - Code changes that neither fix a bug nor add a feature.
- `TEST: {description}` - Adding missing tests or correcting existing tests.
- `CHORE: {description}` - Updates to build scripts, dependencies, etc.

## 2. Documentation Updates & AI Agent Behavior
When an AI Agent is working on this project, it must:
1. **Consult Documentation First**: Always read `app/DOCS/PocketMind_PRD.md` for requirements and `app/DOCS/Implementation_Status.md` for current progress before starting new features.
2. **Maintenance**: The AI Agent and GitHub Bot are responsible for maintaining the following files in `app/DOCS/`:
    - **CHANGELOG.md**: Must be updated after every significant change or commit.
        - `FEATURE` maps to `### Added`
        - `BUG`, `UI` map to `### Fixed`
        - `REFACTOR`, `PERF`, `SEC`, `DOCS` map to `### Changed`
    - **Build_Issue_Report.md**: Specifically for `BUG` commits that relate to compilation or runtime environment issues.
    - **Implementation_Status.md**: Updated when a feature mentioned in the PRD is moved to completion.

## 3. GitHub Automation
A GitHub Action is configured to:
1. Parse commit messages on every push to `main`.
2. Automatically append entries to `app/DOCS/CHANGELOG.md`.
3. Commit the documentation changes back to the repository.

---
*Note: This file is the primary source of truth for AI agent behavior in this project.*
