<!-- SPECKIT START -->
For additional context about technologies to be used, project structure,
shell commands, and other important information, read:

- `specs/hill-commerce-mvp/plan.md` for the current canonical implementation plan
- `specs/hill-commerce-mvp/spec.md` for the current canonical product baseline
- `.specify/memory/constitution.md` for project-level rules

If future feature-specific specs exist, prefer the most specific `specs/<feature>/spec.md`
and `specs/<feature>/plan.md` over the MVP baseline for that scope.

## Spec Kit / Constitution Workflow

`.specify/memory/constitution.md` is the project-level governance source. This file
defines execution rules that must follow, not override, that constitution.

- Start medium- or high-complexity work by reading `.specify/memory/constitution.md`,
  then the most specific `specs/<feature>/spec.md`, then `plan.md`, then `tasks.md`
- Default new work to `specs/*`, not `docs/superpowers/specs/*` or
  `docs/superpowers/plans/*`
- If work spans multiple features, identify one primary feature spec and treat others
  as secondary references
- Keep `README.md` as developer entry documentation; only extract constraints that
  affect architecture or implementation decisions into canonical specs
- Before claiming behavior-changing work or spec migration is complete, run
  verification appropriate to that claim and ensure the cited canonical spec still
  matches the result
<!-- SPECKIT END -->
