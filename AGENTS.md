# Agent Rules & Workflows

## Core Programming Rules

> [!IMPORTANT]
> **Code Reuse & Consistency**: Always prioritize reusing existing code and implementation patterns. The new code must maintain the same style, structure, and logic as the existing codebase.
> 
> **Zero Reformatting**: It is strictly forbidden to change the file encoding, indentation (spaces vs. tabs), or perform any general reformatting of existing code or files. Any modification must be surgery-precise.

## Gated Development Workflow

Process: `brainstorming` -> `writing-plans` -> `executing-plans`

1. **Brainstorming**: No code writing until a design is approved.
2. **Planning**: Create a detailed plan in `implementation_plan.md`.
3. **Execution**: Track progress in `task.md`.

## Agent Roles & Guidelines
- **codebase_investigator**: Used for deep architectural analysis and finding patterns in the reactive flows.
- **generalist**: Used for batch operations or complex refactoring of infrastructure adapters.

---

### Context & Navigation
- [GEMINI.md](GEMINI.md)
- [docs/setup.md](docs/setup.md)
- [docs/architecture.md](docs/architecture.md)
