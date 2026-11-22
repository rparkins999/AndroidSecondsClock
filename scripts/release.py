#!/usr/bin/env python3
"""Validate or explicitly prepare SecondsClock release metadata."""

from __future__ import annotations

import argparse
import os
import re
import sys
import tempfile
from pathlib import Path


F_DROID_APPLICATION_ID = "uk.co.yahoo.p1rpp.secondsclock"
APPLICATION_ID_PATTERN = re.compile(
    r'^([ \t]*applicationId ")([^\"]+)("[ \t]*)$', re.MULTILINE
)
VERSION_CODE_PATTERN = re.compile(
    r"^([ \t]*versionCode[ \t]+)([0-9]+)([ \t]*)$", re.MULTILINE
)
VERSION_NAME_PATTERN = re.compile(
    r'^([ \t]*versionName ")([^"]+)("[ \t]*)$', re.MULTILINE
)
CHANGELOG_ENTRY_PATTERN = re.compile(
    r"^## (?P<version>\S+) \((?P<code>[0-9]+)\)[ \t]*$", re.MULTILINE
)
README_VERSION_PATTERN = re.compile(
    r"^(This is version )\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?(\.)$",
    re.MULTILINE,
)
SEMVER_PATTERN = re.compile(r"\d+\.\d+\.\d+(?:[-+][0-9A-Za-z.-]+)?")
MAX_CHANGELOG_CHARS = 500


class ReleaseError(Exception):
    """A release metadata operation could not be completed safely."""


def _paths(root: Path) -> tuple[Path, Path, Path, Path]:
    return (
        root / "SecondsClock/build.gradle",
        root / "README.md",
        root / "fastlane/metadata/android/en-US/changelogs",
        root / "CHANGELOG.md",
    )


def _single_match(pattern: re.Pattern[str], text: str, description: str) -> re.Match[str]:
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise ReleaseError(f"Expected exactly one {description}; found {len(matches)}")
    return matches[0]


def _changelog_entry(
    text: str, version_name: str, version_code: int
) -> tuple[re.Match[str], str]:
    """Return a unique release-history heading and its notes."""
    entries = list(CHANGELOG_ENTRY_PATTERN.finditer(text))
    matches = [
        entry
        for entry in entries
        if entry.group("version") == version_name
        and int(entry.group("code")) == version_code
    ]
    if len(matches) != 1:
        raise ReleaseError(
            f"Expected exactly one CHANGELOG.md entry for "
            f"{version_name} ({version_code}); found {len(matches)}"
        )

    entry = matches[0]
    next_entry = next(
        (candidate for candidate in entries if candidate.start() > entry.start()),
        None,
    )
    end = next_entry.start() if next_entry else len(text)
    return entry, text[entry.end() : end].strip()


def _prepend_changelog_entry(
    text: str, version_name: str, version_code: int, notes: str
) -> str:
    """Insert a release at the top of the root changelog, after its introduction."""
    if not text.startswith("# Changelog\n"):
        raise ReleaseError("CHANGELOG.md must start with '# Changelog'")

    heading = f"## {version_name} ({version_code})"
    entries = list(CHANGELOG_ENTRY_PATTERN.finditer(text))
    if any(
        entry.group("version") == version_name
        or int(entry.group("code")) == version_code
        for entry in entries
    ):
        raise ReleaseError(f"Refusing to overwrite existing CHANGELOG.md entry: {heading}")

    insertion_point = entries[0].start() if entries else len(text.rstrip())
    prefix = text[:insertion_point].rstrip()
    suffix = text[insertion_point:].lstrip("\n")
    new_entry = f"{heading}\n\n{notes.strip()}\n\n"
    if suffix:
        return f"{prefix}\n\n{new_entry}{suffix}"
    return f"{prefix}\n\n{new_entry}"


def validate_release_metadata(root: Path) -> list[str]:
    """Return validation problems, or an empty list when release metadata is consistent."""
    build_file, readme_file, changelog_dir, history_file = _paths(root)
    problems: list[str] = []

    if not build_file.is_file():
        return [f"Missing {build_file.relative_to(root)}"]
    if not readme_file.is_file():
        return [f"Missing {readme_file.relative_to(root)}"]
    if not history_file.is_file():
        return [f"Missing {history_file.relative_to(root)}"]

    try:
        build_text = build_file.read_text(encoding="utf-8")
        app_id_match = _single_match(
            APPLICATION_ID_PATTERN, build_text, "literal canonical applicationId"
        )
        code_match = _single_match(
            VERSION_CODE_PATTERN, build_text, "literal versionCode"
        )
        name_match = _single_match(
            VERSION_NAME_PATTERN, build_text, "literal versionName"
        )
        version_code = int(code_match.group(2))
        version_name = name_match.group(2)
    except (OSError, ValueError, ReleaseError) as error:
        return [str(error)]
    if version_code <= 0:
        problems.append("versionCode must be a positive integer")
    if app_id_match.group(2) != F_DROID_APPLICATION_ID:
        problems.append(
            "F-Droid's literal applicationId must remain "
            f"{F_DROID_APPLICATION_ID}"
        )
    if not SEMVER_PATTERN.fullmatch(version_name):
        problems.append(f"versionName is not a valid release version: {version_name}")

    readme_text = readme_file.read_text(encoding="utf-8")
    try:
        readme_match = _single_match(README_VERSION_PATTERN, readme_text, "README version")
    except ReleaseError as error:
        problems.append(str(error))
    else:
        if readme_match.group(0) != f"This is version {version_name}.":
            problems.append(
                f"README version does not match SecondsClock/build.gradle ({version_name})"
            )

    changelog = changelog_dir / f"{version_code}.txt"
    if not changelog.is_file():
        problems.append(f"Missing version-code changelog: {changelog.relative_to(root)}")
    else:
        changelog_text = changelog.read_text(encoding="utf-8").strip()
        if not changelog_text:
            problems.append(f"Changelog is empty: {changelog.relative_to(root)}")
        if len(changelog_text) > MAX_CHANGELOG_CHARS:
            problems.append(
                f"Changelog exceeds {MAX_CHANGELOG_CHARS} characters: "
                f"{changelog.relative_to(root)} ({len(changelog_text)} characters)"
            )
        history_text = history_file.read_text(encoding="utf-8")
        try:
            _, history_entry = _changelog_entry(
                history_text, version_name, version_code
            )
        except ReleaseError as error:
            problems.append(str(error))
        else:
            if history_entry != changelog_text:
                problems.append(
                    "CHANGELOG.md entry does not match "
                    f"{changelog.relative_to(root)}"
                )

    return problems


def prepare_release(root: Path, version_code: int, version_name: str, changelog: str) -> None:
    """Update release metadata and notes atomically; never stage files."""
    if version_code <= 0:
        raise ReleaseError("Version code must be a positive integer")
    if not SEMVER_PATTERN.fullmatch(version_name):
        raise ReleaseError("Version name must look like MAJOR.MINOR.PATCH (optionally suffixed)")
    changelog = changelog.strip()
    if not changelog:
        raise ReleaseError("Changelog must not be empty")
    if len(changelog) > MAX_CHANGELOG_CHARS:
        raise ReleaseError(
            f"Changelog must be {MAX_CHANGELOG_CHARS} characters or fewer; "
            f"got {len(changelog)}"
        )

    build_file, readme_file, changelog_dir, history_file = _paths(root)
    if not build_file.is_file() or not readme_file.is_file() or not history_file.is_file():
        raise ReleaseError("Run this command from a SecondsClock repository")

    build_text = build_file.read_text(encoding="utf-8")
    app_id_match = _single_match(
        APPLICATION_ID_PATTERN, build_text, "literal canonical applicationId"
    )
    if app_id_match.group(2) != F_DROID_APPLICATION_ID:
        raise ReleaseError(
            "F-Droid's literal applicationId must remain "
            f"{F_DROID_APPLICATION_ID}"
        )
    code_match = _single_match(
        VERSION_CODE_PATTERN, build_text, "literal versionCode"
    )
    _single_match(VERSION_NAME_PATTERN, build_text, "literal versionName")
    current_code = int(code_match.group(2))
    if version_code <= current_code:
        raise ReleaseError(
            f"Version code {version_code} must be greater than the current code {current_code}"
        )

    readme_text = readme_file.read_text(encoding="utf-8")
    _single_match(README_VERSION_PATTERN, readme_text, "README version")
    history_text = history_file.read_text(encoding="utf-8")
    updated_history = _prepend_changelog_entry(
        history_text, version_name, version_code, changelog
    )
    changelog_path = changelog_dir / f"{version_code}.txt"
    if changelog_path.exists():
        raise ReleaseError(f"Refusing to overwrite existing changelog: {changelog_path}")

    updated_build = VERSION_CODE_PATTERN.sub(
        lambda match: f"{match.group(1)}{version_code}{match.group(3)}",
        build_text,
        count=1,
    )
    updated_build = VERSION_NAME_PATTERN.sub(
        lambda match: f'{match.group(1)}{version_name}{match.group(3)}',
        updated_build,
        count=1,
    )
    updated_readme = README_VERSION_PATTERN.sub(
        lambda match: f"{match.group(1)}{version_name}{match.group(2)}",
        readme_text,
        count=1,
    )

    changelog_dir.mkdir(parents=True, exist_ok=True)
    new_contents = {
        build_file: updated_build,
        readme_file: updated_readme,
        changelog_path: changelog + "\n",
        history_file: updated_history,
    }
    old_contents = {
        build_file: build_text,
        readme_file: readme_text,
        history_file: history_text,
    }
    staged: dict[Path, Path] = {}
    backups: dict[Path, Path] = {}
    replaced: list[Path] = []
    try:
        for path, content in new_contents.items():
            staged[path] = _stage_text(path, content)
        for path, content in old_contents.items():
            backups[path] = _stage_text(path, content)

        if changelog_path.exists():
            raise ReleaseError(f"Refusing to overwrite existing changelog: {changelog_path}")

        for path in new_contents:
            if path == changelog_path and path.exists():
                raise ReleaseError(f"Refusing to overwrite existing changelog: {path}")
            os.replace(staged[path], path)
            replaced.append(path)
    except Exception as error:
        rollback_errors = []
        for path in reversed(replaced):
            try:
                if path == changelog_path:
                    path.unlink(missing_ok=True)
                else:
                    os.replace(backups[path], path)
            except OSError as rollback_error:
                rollback_errors.append(f"{path}: {rollback_error}")
        if rollback_errors:
            raise ReleaseError(
                f"Release metadata update failed ({error}); rollback also failed for "
                + ", ".join(rollback_errors)
            ) from error
        raise
    finally:
        for temp_path in (*staged.values(), *backups.values()):
            temp_path.unlink(missing_ok=True)

    print(f"Prepared version {version_name} (code {version_code})")
    print(f"Updated {build_file.relative_to(root)}")
    print(f"Updated {readme_file.relative_to(root)}")
    print(f"Updated {history_file.relative_to(root)}")
    print(f"Created {changelog_path.relative_to(root)}")
    print("Review the changes and stage them manually when ready.")


def _stage_text(path: Path, content: str) -> Path:
    """Write a sibling temp file so final replacements are atomic per metadata file."""
    temp = tempfile.NamedTemporaryFile(
        mode="w",
        encoding="utf-8",
        newline="",
        prefix=f".{path.name}.",
        suffix=".tmp",
        dir=path.parent,
        delete=False,
    )
    temp_path = Path(temp.name)
    try:
        with temp:
            temp.write(content)
            temp.flush()
            os.fsync(temp.fileno())
        os.chmod(temp_path, 0o644)
    except Exception:
        temp_path.unlink(missing_ok=True)
        raise
    return temp_path


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser(
        "check", help="check app version, root changelog, and F-Droid notes consistency"
    )

    prepare = commands.add_parser("prepare", help="explicitly update release metadata")
    prepare.add_argument("--version-code", type=int, required=True)
    prepare.add_argument("--version-name", required=True)
    changelog_source = prepare.add_mutually_exclusive_group(required=True)
    changelog_source.add_argument("--changelog", help="changelog text (may include newlines)")
    changelog_source.add_argument("--changelog-file", type=Path, help="read changelog text from a file")

    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    try:
        if args.command == "check":
            problems = validate_release_metadata(root)
            if problems:
                for problem in problems:
                    print(f"ERROR: {problem}", file=sys.stderr)
                return 1
            print("Release version and changelog metadata are consistent.")
            return 0

        changelog = args.changelog
        if args.changelog_file is not None:
            changelog = args.changelog_file.read_text(encoding="utf-8")
        prepare_release(root, args.version_code, args.version_name, changelog)
        return 0
    except (OSError, ReleaseError) as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
