#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import shutil
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


@dataclass(frozen=True)
class Replacement:
    old: str
    new: str


@dataclass(frozen=True)
class RegexReplacement:
    pattern: str
    repl: str
    flags: int = 0


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
    ".sh",
}

SKIP_DIR_NAMES = {
    ".git",
    ".dart_tool",
    "build",
    "Pods",
    ".symlinks",
    "node_modules",
}


def iter_text_files(repo_root: Path) -> Iterable[Path]:
    for path in repo_root.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIR_NAMES for part in path.parts):
            continue
        if path.suffix.lower() in TEXT_EXTENSIONS:
            yield path


def replace_in_file(
    path: Path,
    replacements: list[Replacement],
    regex_replacements: list[RegexReplacement],
    apply: bool,
) -> int:
    try:
        original = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return 0

    updated = original
    hits = 0
    for rep in replacements:
        count = updated.count(rep.old)
        if count:
            updated = updated.replace(rep.old, rep.new)
            hits += count

    for rep in regex_replacements:
        updated, count = re.subn(rep.pattern, rep.repl, updated, flags=rep.flags)
        hits += count

    if apply and updated != original:
        path.write_text(updated, encoding="utf-8")
    return hits


def move_android_package_dir(repo_root: Path, apply: bool) -> None:
    old_dir = repo_root / "android" / "src" / "main" / "java" / "com" / "pichillilorenzo"
    new_dir = repo_root / "android" / "src" / "main" / "java" / "com" / "microsvc"
    if not old_dir.exists():
        return
    print(f"[dir] move {old_dir} -> {new_dir}")
    if not apply:
        return
    new_dir.parent.mkdir(parents=True, exist_ok=True)
    if new_dir.exists():
        # Merge children to keep idempotent behavior.
        for child in old_dir.iterdir():
            target = new_dir / child.name
            if target.exists():
                continue
            shutil.move(str(child), str(target))
        old_dir.rmdir()
    else:
        shutil.move(str(old_dir), str(new_dir))


def remove_ios_from_pubspec(repo_root: Path, apply: bool) -> None:
    pubspec = repo_root / "pubspec.yaml"
    if not pubspec.exists():
        return
    text = pubspec.read_text(encoding="utf-8")
    updated = re.sub(
        r"\n\s{6}ios:\n\s{8}pluginClass: InAppWebViewFlutterPlugin\n",
        "\n",
        text,
    )
    if updated != text:
        print("[pubspec] remove ios plugin platform entry")
        if apply:
            pubspec.write_text(updated, encoding="utf-8")


def remove_ios_dirs(repo_root: Path, apply: bool) -> None:
    candidates = [
        repo_root / "ios",
        repo_root / "example" / "ios",
    ]
    for d in candidates:
        if d.exists():
            print(f"[rm] {d}")
            if apply:
                shutil.rmtree(d)


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Bulk-refactor package/channel namespace and optional iOS cleanup."
    )
    parser.add_argument(
        "--apply",
        action="store_true",
        help="Apply changes. Without this flag, runs in dry-run mode.",
    )
    parser.add_argument(
        "--remove-ios",
        action="store_true",
        help="Also remove native iOS directories and pubspec iOS plugin platform entry.",
    )
    parser.add_argument(
        "--repo-root",
        default=".",
        help="Repository root path (default: current directory).",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    mode = "APPLY" if args.apply else "DRY-RUN"
    print(f"[mode] {mode}")
    print(f"[repo] {repo_root}")

    replacements = [
        Replacement(
            "com.pichillilorenzo.flutter_inappwebview",
            "com.microsvc.flutter_inappwebview",
        ),
        Replacement(
            "com.pichillilorenzo/flutter_inappwebview",
            "com.microsvc/flutter_inappwebview_v2",
        ),
        Replacement(
            "com.pichillilorenzo/flutter_inappbrowser",
            "com.microsvc/flutter_inappbrowser_v2",
        ),
        Replacement(
            "com.pichillilorenzo/flutter_headless_inappwebview",
            "com.microsvc/flutter_headless_inappwebview_v2",
        ),
        Replacement(
            "com.pichillilorenzo/flutter_chromesafaribrowser",
            "com.microsvc/flutter_chromesafaribrowser_v2",
        ),
        Replacement(
            "flutter_inappwebview.fileprovider",
            "flutter_inappwebview_v2.fileprovider",
        ),
        Replacement("CredentialDatabase.db", "CredentialDatabaseV2.db"),
    ]
    regex_replacements = [
        RegexReplacement(
            r"^name:\s*flutter_inappwebview\s*$",
            "name: flutter_inappwebview_v2",
            re.MULTILINE,
        ),
        RegexReplacement(
            r"^library\s+flutter_inappwebview;\s*$",
            "library flutter_inappwebview_v2;",
            re.MULTILINE,
        ),
        RegexReplacement(
            r"packages/flutter_inappwebview/",
            "packages/flutter_inappwebview_v2/",
        ),
        RegexReplacement(
            r"package:flutter_inappwebview/",
            "package:flutter_inappwebview_v2/",
        ),
        RegexReplacement(
            r"^\s{2}flutter_inappwebview:\s*$",
            "  flutter_inappwebview_v2:",
            re.MULTILINE,
        ),
    ]

    total_hits = 0
    touched_files = 0
    for path in iter_text_files(repo_root):
        hits = replace_in_file(path, replacements, regex_replacements, args.apply)
        if hits > 0:
            touched_files += 1
            total_hits += hits
            print(f"[edit] {path.relative_to(repo_root)} ({hits} hits)")

    print(f"[result] {touched_files} files, {total_hits} replacements")

    move_android_package_dir(repo_root, args.apply)

    if args.remove_ios:
        remove_ios_from_pubspec(repo_root, args.apply)
        remove_ios_dirs(repo_root, args.apply)

    print("[done]")


if __name__ == "__main__":
    main()
