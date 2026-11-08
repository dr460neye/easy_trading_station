"""Turns curseforge/description.md into curseforge/description.html, which can be pasted into the
source mode of the CurseForge description editor (CurseForge has no Markdown input and no API for the
project page). Only the Markdown that description.md uses is supported: headings, paragraphs, lists,
code blocks, rules, bold, italic, code, links, images.

Usage: python tools/curseforge_description.py"""
import html
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "curseforge" / "description.md"
DST = ROOT / "curseforge" / "description.html"


def inline(text: str) -> str:
    text = html.escape(text, quote=False)
    text = re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", r'<img src="\2" alt="\1">', text)
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', text)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"(?<![*\w])\*([^*\n]+)\*(?![*\w])", r"<em>\1</em>", text)
    return text


def convert(markdown: str) -> str:
    out = []
    lines = markdown.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
        elif line.startswith("```"):
            code = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                code.append(lines[i])
                i += 1
            i += 1
            out.append("<pre><code>" + html.escape("\n".join(code)) + "</code></pre>")
        elif line.startswith("#"):
            level = len(line) - len(line.lstrip("#"))
            out.append(f"<h{level}>{inline(line[level:].strip())}</h{level}>")
            i += 1
        elif line.strip() == "---":
            out.append("<hr>")
            i += 1
        elif re.match(r"^(- |\d+\. )", line):
            ordered = bool(re.match(r"^\d+\. ", line))
            items = []
            while i < len(lines) and (re.match(r"^(- |\d+\. )", lines[i]) or (lines[i].startswith("  ") and lines[i].strip())):
                if re.match(r"^(- |\d+\. )", lines[i]):
                    items.append(re.sub(r"^(- |\d+\. )", "", lines[i]).strip())
                else:
                    items[-1] += " " + lines[i].strip()
                i += 1
            tag = "ol" if ordered else "ul"
            out.append(f"<{tag}>" + "".join(f"<li>{inline(item)}</li>" for item in items) + f"</{tag}>")
        else:
            paragraph = []
            while i < len(lines) and lines[i].strip() and not re.match(r"^(#|```|- |\d+\. |---$)", lines[i]):
                paragraph.append(lines[i].strip())
                i += 1
            text = " ".join(paragraph)
            if re.fullmatch(r"!\[[^\]]*\]\([^)]+\)", text):
                out.append("<p>" + inline(text) + "</p>")
            else:
                out.append("<p>" + inline(text) + "</p>")
    return "\n".join(out) + "\n"


if __name__ == "__main__":
    DST.write_text(convert(SRC.read_text(encoding="utf-8")), encoding="utf-8", newline="\n")
    print(f"wrote {DST.relative_to(ROOT)}")
