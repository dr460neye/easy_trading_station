"""Prints the CHANGELOG.md section of one version (Keep a Changelog format).
Usage: python tools/changelog_section.py 1.0.0   (exit code 1 if the section is missing or empty)"""
import re
import sys
from pathlib import Path


def section(text: str, version: str) -> str:
    pattern = re.compile(r"^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", re.S | re.M)
    match = pattern.search(text)
    return match.group(1).strip() if match else ""


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2
    changelog = Path(__file__).resolve().parent.parent / "CHANGELOG.md"
    body = section(changelog.read_text(encoding="utf-8"), sys.argv[1])
    if not body:
        print(f"CHANGELOG.md has no (or an empty) section for {sys.argv[1]}", file=sys.stderr)
        return 1
    print(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
