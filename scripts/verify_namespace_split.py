#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path


TEXT_EXTENSIONS = {
    ".dart",
    ".java",
    ".kt",
    ".xml",
    ".gradle",
    ".kts",
    ".properties",
    ".yaml",
    ".yml",
    ".md",
    ".podspec",
    ".plist",
    ".storyboard",
    ".swift",
    ".m",
    ".mm",
    ".h",
}

SKIP_DIR_NAMES = {
    ".git",
    ".dart_tool",
    "build",
    "Pods",
    ".symlinks",
    "node_modules",
}


def iter_text_files(repo_root: Path):
    for path in repo_root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIR_NAMES for part in path.parts):
            continue
        if path.suffix.lower() in TEXT_EXTENSIONS:
            yield path


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Check namespace split leftovers after package rename."
    )
    parser.add_argument("--repo-root", default=".", help="Repository root path.")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    checks = [
        ("com.pichillilorenzo/flutter_inappwebview", re.compile(r"com\.pichillilorenzo/flutter_inappwebview")),
        ("com.pichillilorenzo/flutter_inappbrowser", re.compile(r"com\.pichillilorenzo/flutter_inappbrowser")),
        ("com.pichillilorenzo/flutter_headless_inappwebview", re.compile(r"com\.pichillilorenzo/flutter_headless_inappwebview")),
        ("com.pichillilorenzo/flutter_chromesafaribrowser", re.compile(r"com\.pichillilorenzo/flutter_chromesafaribrowser")),
        ("com.pichillilorenzo.flutter_inappwebview", re.compile(r"com\.pichillilorenzo\.flutter_inappwebview")),
        ("name: flutter_inappwebview", re.compile(r"^name:\s+flutter_inappwebview\s*$", re.MULTILINE)),
        ("library flutter_inappwebview;", re.compile(r"^library\s+flutter_inappwebview;\s*$", re.MULTILINE)),
        ("packages/flutter_inappwebview/", re.compile(r"packages/flutter_inappwebview/")),
        ("flutter_inappwebview.fileprovider", re.compile(r"flutter_inappwebview\.fileprovider")),
        ("CredentialDatabase.db", re.compile(r"CredentialDatabase\.db")),
    ]

    found = 0
    for path in iter_text_files(repo_root):
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        rel = path.relative_to(repo_root)
        for label, pattern in checks:
            if pattern.search(text):
                found += 1
                print(f"[leftover] {rel}: {label}")

    if found == 0:
        print("[ok] no configured leftovers found")
    else:
        print(f"[fail] found {found} leftovers")
        raise SystemExit(1)


if __name__ == "__main__":
    main()
