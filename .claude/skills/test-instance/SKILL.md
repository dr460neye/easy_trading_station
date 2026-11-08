---
name: test-instance
description: Build, deploy and start the local test instance with the singleplayer world already loaded, then report when the player is in the world. Use when the user says "fertig", "starte das spiel", "deploye und starte" or wants to test in game.
---

# Test in the game (one command)

```powershell
python tools/test_in_game.py                 # build + deploy + start + wait until the player is in the world
python tools/test_in_game.py "<world>"       # other world folder name (default: "world" in tools/local.json)
python tools/test_in_game.py --no-deploy     # only start
```

Paths, JDK and world name come from the git-ignored `tools/local.json` (template: `tools/local.example.json`).

"fertig" from the user means: they are done with the current session. Then run
`python tools/test_in_game.py --close`: it saves and closes the running game the way the window's X does,
waits until it ended, deploys the current build and starts the world again.

Rules:
- The game is **never killed**. With `--close` the script asks the window to close (the world is saved
  as on a normal quit). If the game does not end by itself (dialog, long save, no window) the script
  exits with code 3 and says so; then ask the user to close it. Without `--close` a running game only stops the script (code 3).
  Check without the script: `Get-CimInstance Win32_Process -Filter "Name='javaw.exe'"` and look for the instance folder name in `CommandLine`.
- Use `--close` only when the user said "fertig" (or asked to restart the game). Do not close a game the user is playing for any other reason.
- Loading takes several minutes. Run the command with `run_in_background: true` and read its result when it
  finishes; do not poll in a sleep loop. Exit codes: 0 in the world, 1 failed or the game ended early, 2 timeout, 3 game already running.
- The script prints the number of WARN/ERROR lines of this mod in `latest.log` and the first ten. Mention them in the report.
- The world is opened with `--quickPlaySingleplayer` (+ `--quickPlayPath`) from `tools/launch_test_instance.py`. If the
  world does not open by itself, report that instead of guessing: check `logs/latest.log` and `quickPlay/`
  in the instance folder.
- After starting, tell the user what to test (open issues with label `needs-in-game-test`).
- In-game behaviour stays "not tested in game" until the user confirms it.
