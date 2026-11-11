import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import release
from release import ReleaseError, prepare_release, validate_release_metadata


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        (self.root / "SecondsClock").mkdir()
        (self.root / "fastlane/metadata/android/en-US/changelogs").mkdir(parents=True)
        (self.root / "SecondsClock/build.gradle").write_text(
            'def VERSIONCODE = 86\ndef VERSIONNAME = "2.6.0"\n', encoding="utf-8"
        )
        (self.root / "README.md").write_text("# Test\n\nThis is version 2.6.0.\n", encoding="utf-8")
        (self.root / "fastlane/metadata/android/en-US/changelogs/86.txt").write_text(
            "- Existing release notes.\n", encoding="utf-8"
        )

    def tearDown(self):
        self.temp_dir.cleanup()

    def test_valid_metadata_passes(self):
        self.assertEqual([], validate_release_metadata(self.root))

    def test_version_mismatch_and_missing_changelog_are_reported(self):
        readme = self.root / "README.md"
        readme.write_text("# Test\n\nThis is version 2.5.0.\n", encoding="utf-8")
        (self.root / "fastlane/metadata/android/en-US/changelogs/86.txt").unlink()

        problems = validate_release_metadata(self.root)

        self.assertTrue(any("README version does not match" in item for item in problems))
        self.assertTrue(any("Missing version-code changelog" in item for item in problems))

    def test_prepare_updates_version_and_adds_code_keyed_changelog(self):
        prepare_release(self.root, 87, "2.7.0", "- New feature.\n- Bug fix.")

        self.assertIn("def VERSIONCODE = 87", (self.root / "SecondsClock/build.gradle").read_text())
        self.assertIn('def VERSIONNAME = "2.7.0"', (self.root / "SecondsClock/build.gradle").read_text())
        self.assertIn("This is version 2.7.0.", (self.root / "README.md").read_text())
        self.assertEqual(
            "- New feature.\n- Bug fix.\n",
            (self.root / "fastlane/metadata/android/en-US/changelogs/87.txt").read_text(),
        )
        self.assertEqual([], validate_release_metadata(self.root))

    def test_prepare_refuses_old_version_code(self):
        with self.assertRaisesRegex(ReleaseError, "must be greater"):
            prepare_release(self.root, 86, "2.7.0", "- New release.")

    def test_prepare_rejects_invalid_version_name(self):
        with self.assertRaisesRegex(ReleaseError, "Version name"):
            prepare_release(self.root, 87, "next-release", "- New release.")

    def test_prepare_does_not_overwrite_an_existing_changelog(self):
        (self.root / "fastlane/metadata/android/en-US/changelogs/87.txt").write_text(
            "Keep me.\n", encoding="utf-8"
        )

        with self.assertRaisesRegex(ReleaseError, "Refusing to overwrite"):
            prepare_release(self.root, 87, "2.7.0", "- New release.")

    def test_prepare_rejects_changelog_over_500_characters(self):
        with self.assertRaisesRegex(ReleaseError, "500 characters"):
            prepare_release(self.root, 87, "2.7.0", "x" * 501)

    def test_prepare_accepts_a_500_character_changelog(self):
        prepare_release(self.root, 87, "2.7.0", "x" * 500)

        self.assertEqual(
            500,
            len(
                (self.root / "fastlane/metadata/android/en-US/changelogs/87.txt")
                .read_text(encoding="utf-8")
                .strip()
            ),
        )

    def test_check_reports_changelog_over_500_characters(self):
        changelog = self.root / "fastlane/metadata/android/en-US/changelogs/86.txt"
        changelog.write_text("x" * 501, encoding="utf-8")

        problems = validate_release_metadata(self.root)

        self.assertTrue(any("exceeds 500 characters" in item for item in problems))

    def test_prepare_rolls_back_if_an_atomic_replace_fails(self):
        build_file = self.root / "SecondsClock/build.gradle"
        readme_file = self.root / "README.md"
        old_build = build_file.read_text(encoding="utf-8")
        old_readme = readme_file.read_text(encoding="utf-8")
        real_replace = release.os.replace
        failed = False

        def fail_once(source, destination):
            nonlocal failed
            if Path(destination) == readme_file and not failed:
                failed = True
                raise OSError("simulated replace failure")
            return real_replace(source, destination)

        with patch("release.os.replace", side_effect=fail_once):
            with self.assertRaisesRegex(OSError, "simulated replace failure"):
                prepare_release(self.root, 87, "2.7.0", "- New release.")

        self.assertEqual(old_build, build_file.read_text(encoding="utf-8"))
        self.assertEqual(old_readme, readme_file.read_text(encoding="utf-8"))
        self.assertFalse(
            (self.root / "fastlane/metadata/android/en-US/changelogs/87.txt").exists()
        )
        self.assertEqual([], list(self.root.rglob("*.tmp")))


if __name__ == "__main__":
    unittest.main()
