---
name: milestone-workflow
description: How to pick, implement and close stories for the Easy Trading Station using GitHub Issues and milestones. Use at the start of every work session and when the user says "weiter".
---

# Milestone workflow

Stories are GitHub issues (`S<milestone>.<n> <title>`), milestones are GitHub milestones (`M1 ...`). Plan and status live there, not in the repo.

1. List the open work: `gh issue list --milestone "<milestone title>" --state open` (`gh milestone` does not exist; list milestones with `gh api repos/dr460neye/easy_trading_station/milestones --jq '.[]|[.title,.open_issues,.closed_issues]|@tsv'`). Open only the issue you work on (`gh issue view <n>`).
2. Take the first open story of the current milestone. Issues labelled `needs-in-game-test` are built but wait for the user; do not re-implement them.
3. Implement the smallest change that meets the acceptance criteria. Reuse `easy-npc-api` and `station-variants`; build with `build-run`.
4. Finish: `gh issue comment <n> --body "<what changed, what is untested>"`. Close with `gh issue close <n>` only when the criteria are met and, for in-game behaviour, the user confirmed. Otherwise add the label `needs-in-game-test` and leave it open. New work: `gh issue create` (templates in `.github/ISSUE_TEMPLATE`). Reference issues in commits (`Refs #12`, `Closes #12`).
5. Report to the user in a few lines: what changed, what is untested, what comes next. Do not start the next milestone unprompted.
6. Keep `CLAUDE.md` and the skills accurate: if a rule, class or command changes, edit it in the same session.

Token rules: filter all build output, read only line ranges you need, no re-research of facts already in the skills, no whole-file rewrites when `Edit` suffices.
