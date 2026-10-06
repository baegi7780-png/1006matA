# Project workflow preferences

- Preserve existing functionality and UI unless the user explicitly requests changes. Disclose required changes to existing behavior and offer choices before implementing them.
- The user requests automatic commits and pushes after completing and verifying future requested work. Include only task-owned changes, not unrelated pre-existing edits.
- Never publish credentials, private configuration, production database dumps, build outputs, caches, or private user uploads. Generated demo assets and scoped seed SQL are allowed.
- Current intended Android repository: https://github.com/baegi7780-png/1006matA.git. Verify the destination before pushing. Do not overwrite newer remote work or force-push.
- Report verification failures honestly. Do not silently change existing behavior to make a check pass. Database updates are not deployed by git push.
