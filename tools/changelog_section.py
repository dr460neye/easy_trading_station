"""Prints the CHANGELOG.md section of one version (Keep a Changelog format).
Usage: python tools/changelog_section.py 1.0.0 [--short] [--link URL]
  --short  lead paragraph plus the top-level bullets (max 8, bold marks removed, long lines cut)
  --link   appended as "Full changelog: URL"
Exit code 1 if the section is missing or empty."""
import re
import sys
from pathlib import Path

MAX_BULLETS = 8
MAX_BULLET_LENGTH = 160
CHANGE_HEADINGS = {"added", "changed", "fixed", "removed", "deprecated", "security"}


def section(text: str, version: str) -> str:
    pattern = re.compile(r"^## \[" + re.escape(version) + r"\][^\n]*\n(.*?)(?=^## \[|\Z)", re.S | re.M)
    match = pattern.search(text)
    return match.group(1).strip() if match else ""


def short(body: str) -> str:
    """Lead paragraph (text before the first ### heading) and the first top-level bullets of the
    Added/Changed/Fixed/Removed/Deprecated/Security parts, wrapped lines joined."""
    lead_lines = []
    bullets = []
    in_lead = True
    counting = False
    for line in body.splitlines():
        if line.startswith("###"):
            in_lead = False
            counting = line[3:].strip().lower() in CHANGE_HEADINGS
            continue
        if in_lead:
            lead_lines.append(line)
        elif counting and line.startswith("- "):
            bullets.append(line[2:].strip())
        elif counting and bullets and line.startswith("  ") and line.strip():
            bullets[-1] += " " + line.strip()
    bullets = [cut(b.replace("**", "")) for b in bullets]
    out = "\n".join(lead_lines).strip()
    shown = [f"- {b}" for b in bullets[:MAX_BULLETS]]
    if shown:
        out += ("\n\n" if out else "") + "\n".join(shown)
    if len(bullets) > MAX_BULLETS:
        out += f"\n- … and {len(bullets) - MAX_BULLETS} more"
    return out.strip()


def cut(text: str) -> str:
    if len(text) <= MAX_BULLET_LENGTH:
        return text
    return text[: MAX_BULLET_LENGTH - 1].rstrip() + "…"


def main() -> int:
    args = sys.argv[1:]
    link = ""
    if "--link" in args:
        i = args.index("--link")
        link = args[i + 1] if i + 1 < len(args) else ""
        del args[i : i + 2]
    use_short = "--short" in args
    args = [a for a in args if a != "--short"]
    if len(args) != 1:
        print(__doc__, file=sys.stderr)
        return 2
    changelog = Path(__file__).resolve().parent.parent / "CHANGELOG.md"
    body = section(changelog.read_text(encoding="utf-8"), args[0])
    if not body:
        print(f"CHANGELOG.md has no (or an empty) section for {args[0]}", file=sys.stderr)
        return 1
    if use_short:
        body = short(body)
    if link:
        body += f"\n\nFull changelog: {link}"
    sys.stdout.reconfigure(encoding="utf-8")
    print(body)
    return 0


if __name__ == "__main__":
    sys.exit(main())
