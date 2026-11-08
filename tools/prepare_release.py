"""Prepares a release locally: sets mod_version, closes the [Unreleased] changelog section and prints
the git commands for the commit. It never pushes. The release itself is started by hand
(GitHub -> Actions -> Release); pre-releases are made automatically from the pushed commits.
Usage: python tools/prepare_release.py 1.1.0"""
import datetime
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEMVER = re.compile(r"^\d+\.\d+\.\d+$")


def prop(text: str, key: str) -> str:
    return re.search(rf"^{key}=(.*)$", text, re.M).group(1).strip()


def main() -> int:
    if len(sys.argv) != 2 or not SEMVER.match(sys.argv[1]):
        print("Usage: prepare_release.py MAJOR.MINOR.PATCH", file=sys.stderr)
        return 2
    version = sys.argv[1]
    props_path = ROOT / "gradle.properties"
    props = props_path.read_text(encoding="utf-8")
    mc = prop(props, "minecraft_version")
    log_path = ROOT / "CHANGELOG.md"
    log = log_path.read_text(encoding="utf-8")
    unreleased = re.search(r"^## \[Unreleased\]\n(.*?)(?=^## \[|\Z)", log, re.S | re.M)
    if not unreleased or not unreleased.group(1).strip():
        print("CHANGELOG.md: the [Unreleased] section is empty, nothing to release", file=sys.stderr)
        return 1
    today = datetime.date.today().isoformat()
    replacement = f"## [Unreleased]\n\n## [{version}] - {today}\n{unreleased.group(1)}"
    log = log[: unreleased.start()] + replacement + log[unreleased.end():]
    log_path.write_text(log, encoding="utf-8")
    props_path.write_text(re.sub(r"^mod_version=.*$", f"mod_version={version}", props, flags=re.M), encoding="utf-8")

    print(f"Prepared {version} for Minecraft {mc}. Review the changes, then:")
    print(f'  git commit -am "chore(release): {version}"')
    print("  git push")
    print(f"and start GitHub -> Actions -> Release with the version {version}.")
    print("Afterwards set mod_version to the next planned version and add a new [Unreleased] entry.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
