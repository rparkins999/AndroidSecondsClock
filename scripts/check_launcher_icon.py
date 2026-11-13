#!/usr/bin/env python3
"""Check the manifest-selected launcher image in both assembled APKs."""

from __future__ import annotations

import io
import os
import re
import shutil
import subprocess
import sys
import zipfile
from pathlib import Path

try:
    from PIL import Image
except ImportError as error:
    raise SystemExit(
        "Pillow is required for the launcher icon APK check "
        "(CI installs the python3-pil package)"
    ) from error


ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "SecondsClock"
GENERATED_ICON = APP / "build/generated/launcherIconRes/drawable/ic_launcher.png"
FALLBACK_ICON = APP / "src/main/res/drawable/ic_launcher.png"


def find_aapt() -> str:
    """Find aapt from the installed Android SDK build-tools."""
    candidates: list[Path] = []
    for variable in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        sdk_root = os.environ.get(variable)
        if sdk_root:
            candidates.extend(Path(sdk_root).glob("build-tools/*/aapt"))

    if candidates:
        def version_key(path: Path) -> tuple[int, ...]:
            return tuple(int(part) for part in re.findall(r"\d+", path.parent.name))

        return str(max(candidates, key=version_key))

    aapt = shutil.which("aapt")
    if aapt:
        return aapt
    raise RuntimeError("aapt was not found; install Android SDK build-tools")


def decoded_pixels(source: Path | bytes) -> tuple[tuple[int, int], bytes]:
    image_source = io.BytesIO(source) if isinstance(source, bytes) else source
    with Image.open(image_source) as image:
        rgba = image.convert("RGBA")
        return rgba.size, rgba.tobytes()


def selected_icon_path(aapt: str, apk: Path) -> str:
    output = subprocess.check_output(
        [aapt, "dump", "badging", str(apk)], text=True
    )
    match = re.search(r"^application:.*?\bicon='([^']+)'", output, re.MULTILINE)
    if not match:
        raise AssertionError(f"Could not find the manifest launcher icon in {apk}")
    return match.group(1)


def main() -> None:
    for path in (GENERATED_ICON, FALLBACK_ICON):
        if not path.is_file():
            raise AssertionError(f"Required launcher image is missing: {path}")

    generated_size, generated_pixels = decoded_pixels(GENERATED_ICON)
    fallback_size, fallback_pixels = decoded_pixels(FALLBACK_ICON)
    if (generated_size, generated_pixels) == (fallback_size, fallback_pixels):
        raise AssertionError(
            "Generated launcher image matches the static fallback; "
            "the timestamped rasterization did not occur"
        )

    aapt = find_aapt()
    for variant in ("debug", "release"):
        apk_directory = APP / f"build/outputs/apk/{variant}"
        apks = sorted(apk_directory.glob(f"SecondsClock-{variant}*.apk"))
        if len(apks) != 1:
            raise AssertionError(
                f"Expected one assembled {variant} APK in {apk_directory}, found {apks}"
            )
        apk = apks[0]

        icon_path = selected_icon_path(aapt, apk)
        with zipfile.ZipFile(apk) as archive:
            try:
                packaged_icon = archive.read(icon_path)
            except KeyError as error:
                raise AssertionError(
                    f"{variant} APK does not contain its selected launcher resource "
                    f"{icon_path}"
                ) from error

        packaged_size, packaged_pixels = decoded_pixels(packaged_icon)
        if (packaged_size, packaged_pixels) != (generated_size, generated_pixels):
            raise AssertionError(
                f"{variant} APK's selected launcher resource {icon_path} does not "
                "match the generated timestamped icon pixels"
            )
        print(f"{variant}: {icon_path} matches the generated launcher icon")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, OSError, subprocess.CalledProcessError, RuntimeError) as error:
        print(f"Launcher icon APK check failed: {error}", file=sys.stderr)
        sys.exit(1)
