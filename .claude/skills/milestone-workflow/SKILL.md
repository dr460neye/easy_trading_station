---
name: milestone-workflow
description: How to pick, implement and close stories for the Easy Trading Station using docs/ROADMAP.md and docs/milestones. Use at the start of every work session and when the user says "weiter".
---

# Milestone workflow

1. Read `docs/ROADMAP.md`; open only the current milestone file. Do not read other milestones.
2. Take the first story with status `open` (S5.1 spike may be pulled forward if M2 release loses trades).
3. Implement the smallest change that meets the acceptance criteria. Reuse `easy-npc-api` and `station-variants`; build with `build-run`.
4. Update the story status (`done`, or `done, needs in-game check`) in the milestone file and the ROADMAP table. Add new stories to the milestone file when the user asks (e.g. S6.10).
5. Report to the user in a few lines: what changed, what is untested, what comes next. Do not start the next milestone unprompted.
6. Keep `CLAUDE.md` and the skills accurate: if a rule, class or command changes, edit it in the same session.

Token rules: filter all build output, read only line ranges you need, no re-research of facts already in the skills, no whole-file rewrites when `Edit` suffices.
