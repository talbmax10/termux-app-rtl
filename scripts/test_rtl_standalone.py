#!/usr/bin/env python3
import hashlib
import importlib.util
import json
import pathlib
import tempfile
import unittest
import zipfile

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("bootstrap", ROOT / "scripts/package-rtl-bootstrap.py")
bootstrap = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bootstrap)


class StandaloneTest(unittest.TestCase):
    def fixture(self, path, original=False):
        elf = bytearray(64)
        elf[:4] = b"\x7fELF"
        elf[18:20] = (183).to_bytes(2, "little")
        elf.extend(bootstrap.PREFIX.encode())
        if original:
            elf.extend(bootstrap.OLD_PREFIX)
        with zipfile.ZipFile(path, "w") as z:
            z.writestr("bin/bash", elf)
            for name in ("bin/apt", "bin/dpkg", "SYMLINKS.txt", "var/lib/dpkg/status"):
                z.writestr(name, b"")
            z.writestr("etc/apt/sources.list", "deb https://packages.termux.dev/apt/termux-main stable main")
            z.writestr("etc/apt/sources.list.d/extra.sources", "URIs: https://example.invalid")

    def test_verified_bootstrap_disables_original_repositories(self):
        with tempfile.TemporaryDirectory() as tmp:
            source, out = pathlib.Path(tmp) / "source.zip", pathlib.Path(tmp) / "out"
            self.fixture(source)
            bootstrap.package(source, out, "test-revision")
            meta = json.loads((out / "bootstrap-metadata.json").read_text())
            archive = out / "bootstrap-aarch64.zip"
            self.assertEqual(hashlib.sha256(archive.read_bytes()).hexdigest(), meta["sha256"])
            self.assertEqual("com.termux.rtl", meta["applicationId"])
            with zipfile.ZipFile(archive) as z:
                self.assertFalse(any(n.startswith("etc/apt/sources.list.d/") for n in z.namelist()))
                self.assertNotIn(b"https://", z.read("etc/apt/sources.list"))
                self.assertIn(b"exit 1", z.read("bin/pkg"))

    def test_original_runtime_prefix_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            source = pathlib.Path(tmp) / "source.zip"
            self.fixture(source, original=True)
            with self.assertRaisesRegex(ValueError, "Upstream runtime path"):
                bootstrap.package(source, pathlib.Path(tmp) / "out", "test")

    def test_package_identity_is_distinct_but_classes_are_not_renamed(self):
        gradle = (ROOT / "app/build.gradle").read_text()
        self.assertIn('applicationId "com.termux.rtl"', gradle)
        self.assertIn('manifestPlaceholders.TERMUX_PACKAGE_NAME = "com.termux.rtl"', gradle)
        constants = (ROOT / "termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java").read_text()
        self.assertIn('TERMUX_PACKAGE_NAME = "com.termux.rtl"', constants)
        self.assertIn('TERMUX_ACTIVITY_NAME = "com.termux.app.TermuxActivity"', constants)
        self.assertIn('BUILD_CONFIG_CLASS_NAME = "com.termux.BuildConfig"', constants)
        shortcuts = (ROOT / "app/src/main/res/xml/shortcuts.xml").read_text()
        self.assertNotIn('targetPackage="com.termux"', shortcuts)
        self.assertIn('targetClass="com.termux.app.TermuxActivity"', shortcuts)


if __name__ == "__main__":
    unittest.main()
