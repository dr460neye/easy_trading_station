"""Starts the dev clone of FTB Evolution 2 directly (offline, no CurseForge login) and opens a
singleplayer world. Usage: python tools/launch_test_instance.py [world_folder_name]"""
import json, os, subprocess, sys, uuid

HOME = os.path.expanduser("~")
CF = os.path.join(HOME, "curseforge", "minecraft")
INST = os.path.join(CF, "Instances", "FTB Evolution 2 - Dev EASYNPC catcher")
INSTALL = os.path.join(CF, "Install")
VERSIONS = os.path.join(INSTALL, "versions")
LIBS = os.path.join(INSTALL, "libraries")
world = sys.argv[1] if len(sys.argv) > 1 else "New World"

def load(v):
    return json.load(open(os.path.join(VERSIONS, v, v + ".json"), encoding="utf-8"))

loader, vanilla = load("neoforge-26.1.2.126"), load("26.1.2")
# only the player's public profile name/uuid is read, never the token
acc = json.load(open(os.path.join(INSTALL, "launcher_accounts.json"), encoding="utf-8"))
profile = next(iter(acc["accounts"].values()))["minecraftProfile"]
name, uid = profile["name"], str(uuid.UUID(profile["id"]))

def allowed(rules, features):
    if not rules:
        return True
    ok = False
    for r in rules:
        match = True
        if "os" in r and r["os"].get("name") not in (None, "windows"):
            match = False
        if "os" in r and r["os"].get("arch") == "x86":
            match = False
        if "os" in r and "versionRange" in r["os"] and "max" in r["os"]["versionRange"]:
            match = False
        for f, val in r.get("features", {}).items():
            if features.get(f, False) != val:
                match = False
        if match:
            ok = r["action"] == "allow"
    return ok

cp, seen = [], set()
for lib in vanilla["libraries"] + loader["libraries"]:
    if not allowed(lib.get("rules"), {}):
        continue
    art = lib.get("downloads", {}).get("artifact")
    if not art:
        continue
    path = os.path.join(LIBS, art["path"].replace("/", os.sep))
    # neoforge json overrides vanilla versions: keep the last one per group:artifact(:classifier)
    parts = lib["name"].split(":")
    key = ":".join(parts[:2] + parts[3:])
    if key in seen:
        cp = [p for p in cp if p[0] != key]
    seen.add(key)
    cp.append((key, path))
cp_paths = [p for _, p in cp] + [os.path.join(VERSIONS, "26.1.2", "26.1.2.jar")]
missing = [p for p in cp_paths if not os.path.exists(p)]
if missing:
    print("missing libraries:", missing[:5]); sys.exit(1)

natives = os.path.join(INST, "natives_dev")
os.makedirs(natives, exist_ok=True)
vars_ = {
    "natives_directory": natives, "launcher_name": "dev-launch", "launcher_version": "1",
    "classpath": os.pathsep.join(cp_paths), "library_directory": LIBS, "classpath_separator": os.pathsep,
    "auth_player_name": name, "version_name": "neoforge-26.1.2.126", "game_directory": INST,
    "assets_root": os.path.join(INSTALL, "assets"), "assets_index_name": vanilla["assetIndex"]["id"],
    "auth_uuid": uid, "auth_access_token": "0", "clientid": "", "auth_xuid": "0",
    "version_type": "release", "quickPlaySingleplayer": world,
}
features = {"is_quick_play_singleplayer": True}

def expand(args):
    out = []
    for a in args:
        if isinstance(a, dict):
            if not allowed(a.get("rules"), features):
                continue
            v = a["value"]
            out += expand(v if isinstance(v, list) else [v])
        else:
            for k, val in vars_.items():
                a = a.replace("${" + k + "}", val)
            out.append(a)
    return out

jvm = expand(vanilla["arguments"]["jvm"] + loader["arguments"].get("jvm", []))
game = expand(vanilla["arguments"]["game"] + loader["arguments"].get("game", []))
user_jvm = ["-Xms2G", "-Xmx8G", "-XX:+UseZGC", "-XX:+ZGenerational"] if False else ["-Xms2G", "-Xmx8G", "-XX:+UseZGC"]
java = os.path.join(INSTALL, "java", "java-runtime-epsilon", "bin", "javaw.exe")
cmd = [java] + user_jvm + jvm + [loader["mainClass"]] + game
log = open(os.path.join(INST, "logs", "dev-launch.out.txt"), "w")
subprocess.Popen(cmd, cwd=INST, stdout=log, stderr=subprocess.STDOUT,
                 creationflags=0x00000008 | 0x00000200)  # DETACHED_PROCESS | NEW_PROCESS_GROUP
print("started as", name, "- world:", world)
