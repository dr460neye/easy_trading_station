"""One command for an in-game test: builds and deploys the current jar into the local test instance,
starts the instance and loads the singleplayer world straight away, then waits until the player is
in the world. Paths come from tools/local.json.

Usage: python tools/test_in_game.py [world_folder_name] [--no-deploy] [--close] [--timeout SECONDS]

--close  if the game is running, close it the way the window's X does (the world is saved, as when
         you quit yourself), wait until it has ended and carry on. Without --close a running game
         stops the script. The game is never killed: if it does not end by itself the script stops
         with exit code 3.

Exit codes: 0 player is in the world, 1 start failed or the game ended early, 2 timeout,
3 the game is running and was not closed."""
import json
import os
import re
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.abspath(__file__))
PYTHON = sys.executable


def arg_value(name, default):
    if name in sys.argv:
        i = sys.argv.index(name)
        return sys.argv[i + 1]
    return default


def game_running(instance_name):
    """True if a javaw process was started from the test instance."""
    script = (
        "Get-CimInstance Win32_Process -Filter \"Name='javaw.exe'\" | "
        "Where-Object { $_.CommandLine -like '*" + instance_name.replace("'", "''") + "*' } | "
        "ForEach-Object { $_.ProcessId }"
    )
    out = subprocess.run(
        ["powershell", "-NoProfile", "-Command", script], capture_output=True, text=True
    ).stdout.strip()
    return bool(out)


def close_running(instance_name, wait=180):
    """Asks the running game to close like the window's X button (saves the world). Never kills."""
    script = (
        "Get-CimInstance Win32_Process -Filter \"Name='javaw.exe'\" | "
        "Where-Object { $_.CommandLine -like '*" + instance_name.replace("'", "''") + "*' } | "
        "ForEach-Object { $p = Get-Process -Id $_.ProcessId -ErrorAction SilentlyContinue; "
        "if ($p -and $p.MainWindowHandle -ne 0) { [void]$p.CloseMainWindow(); 'asked' } else { 'nowindow' } }"
    )
    out = subprocess.run(
        ["powershell", "-NoProfile", "-Command", script], capture_output=True, text=True
    ).stdout.split()
    if "nowindow" in out and "asked" not in out:
        print("The game has no window I can close. Close it yourself (X or Save and Quit).")
        return False
    print("Asked the game to close and save, waiting ...", flush=True)
    end = time.time() + wait
    while time.time() < end:
        if not game_running(instance_name):
            print("The game has ended.")
            return True
        time.sleep(3)
    print(f"The game is still running after {wait} s (a dialog or a long save?). Not killed.")
    return False


def main():
    try:
        config = json.load(open(os.path.join(ROOT, "local.json"), encoding="utf-8"))
    except FileNotFoundError:
        sys.exit("Missing tools/local.json - copy tools/local.example.json and adapt it")
    instance = config["testInstance"]
    positional = [a for a in sys.argv[1:] if not a.startswith("--") and a != arg_value("--timeout", None)]
    world = positional[0] if positional else config.get("world", "New World")
    timeout = int(arg_value("--timeout", "600"))

    if game_running(os.path.basename(instance)):
        if "--close" not in sys.argv:
            print("The game is running. Close it first, or use --close to save and close it.")
            return 3
        if not close_running(os.path.basename(instance)):
            return 3

    if "--no-deploy" not in sys.argv:
        print("Building and deploying ...", flush=True)
        deploy = subprocess.run(
            ["powershell", "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
             os.path.join(ROOT, "deploy-to-test-instance.ps1")],
            capture_output=True, text=True)
        lines = (deploy.stdout + deploy.stderr).splitlines()
        for line in lines:
            if re.search(r"BUILD|Deployed|error|FAILED", line):
                print("  " + line.strip())
        if deploy.returncode != 0:
            print("Deploy failed.")
            return 1

    started = time.time()
    launch = subprocess.run(
        [PYTHON, os.path.join(ROOT, "launch_test_instance.py"), world], capture_output=True, text=True)
    print(launch.stdout.strip() or launch.stderr.strip())
    if launch.returncode != 0:
        return 1

    log = os.path.join(instance, "logs", "latest.log")
    print(f"Waiting for the player to join '{world}' (up to {timeout} s) ...", flush=True)
    joined = re.compile(r"logged in with entity id")
    last_report = 0
    while time.time() - started < timeout:
        time.sleep(5)
        text = ""
        if os.path.exists(log) and os.path.getmtime(log) >= started - 5:
            text = open(log, encoding="utf-8", errors="replace").read()
        if joined.search(text):
            print(f"Player is in the world after {int(time.time() - started)} s.")
            problems = [l for l in text.splitlines()
                        if "easy_npc_trading_station" in l and re.search(r"\b(WARN|ERROR)\b", l)]
            print(f"Warnings/errors from this mod in latest.log: {len(problems)}")
            for line in problems[:10]:
                print("  " + line[:200])
            return 0
        if time.time() - started > 60 and not game_running(os.path.basename(instance)):
            print("The game ended before the world loaded. See logs/latest.log and crash-reports.")
            return 1
        if time.time() - last_report > 60:
            last_report = time.time()
            print(f"  ... {int(time.time() - started)} s", flush=True)
    print("Timeout: the player did not join in time (the game may still be loading).")
    return 2


sys.exit(main())
