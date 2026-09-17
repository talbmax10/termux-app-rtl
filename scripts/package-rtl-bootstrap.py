#!/usr/bin/env python3
"""Validate source-built ARM64 bootstrap, disable incompatible repos, record provenance.

This does NOT patch executable paths or relocate upstream binaries. They must
already have been compiled against the new prefix by build-bootstraps.sh.
"""
import hashlib
import json
import pathlib
import sys
import zipfile

APPLICATION_ID = "com.termux.rtl"
PREFIX = "/data/data/com.termux.rtl/files/usr"
OLD_PREFIX = b"/data/data/com.termux/files/"


def package(source, destination, revision):
    destination.mkdir(parents=True, exist_ok=True)
    output = destination / "bootstrap-aarch64.zip"
    if source.resolve() == output.resolve():
        raise ValueError("Input and output must be different")
    notice = (
        "Termux RTL: independent ARM64 environment (com.termux.rtl).\n"
        "Original Termux files and packages are NOT shared with this app.\n"
        "Official Termux repositories are incompatible with this prefix and disabled.\n"
        "Additional packages must be built for com.termux.rtl; do not add upstream mirrors.\n"
    )
    disabled_tool = ("#!/system/bin/sh\nprintf '%s\\n' 'Termux RTL needs packages built for com.termux.rtl.' "
                     "'Official Termux repositories cannot be used. See docs/RTL-STANDALONE.md.' >&2\nexit 1\n")
    with zipfile.ZipFile(source) as src:
        names = set(src.namelist())
        required = {"bin/bash", "bin/apt", "bin/dpkg", "SYMLINKS.txt", "var/lib/dpkg/status"}
        if not required <= names:
            raise ValueError(f"Incomplete bootstrap: {required - names}")
        for info in src.infolist():
            name = info.filename
            if name.startswith("/") or ".." in pathlib.PurePosixPath(name).parts:
                raise ValueError(f"Unsafe archive entry: {name}")
            data = src.read(info)
            # Documentation may mention upstream paths. Executables and symlinks must not.
            if (data.startswith(b"\x7fELF") or data.startswith(b"#!") or name == "SYMLINKS.txt") and OLD_PREFIX in data:
                raise ValueError(f"Upstream runtime path still present in {name}")
        bash = src.read("bin/bash")
        if not bash.startswith(b"\x7fELF") or int.from_bytes(bash[18:20], "little") != 183:
            raise ValueError("Bootstrap bash is not an ARM64 ELF")
        if PREFIX.encode() not in bash:
            raise ValueError("Bash does not contain the standalone prefix")
        with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as dst:
            for info in src.infolist():
                name = info.filename
                if name == "etc/apt/sources.list" or name.startswith("etc/apt/sources.list.d/"):
                    continue
                if name in ("bin/pkg", "bin/termux-change-repo", "etc/motd"):
                    continue
                dst.writestr(info, src.read(info))
            for name, content, mode in [
                ("etc/apt/sources.list", "# Only repositories compiled for com.termux.rtl belong here.\n", 0o644),
                ("bin/pkg", disabled_tool, 0o755),
                ("bin/termux-change-repo", disabled_tool, 0o755),
                ("etc/motd", notice, 0o644),
            ]:
                entry = zipfile.ZipInfo(name)
                entry.external_attr = (0o100000 | mode) << 16
                entry.compress_type = zipfile.ZIP_DEFLATED
                dst.writestr(entry, content.encode())
    metadata = {
        "applicationId": APPLICATION_ID,
        "architecture": "aarch64",
        "prefix": PREFIX,
        "packagesRevision": revision,
        "sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
        "officialRepositoriesEnabled": False,
    }
    (destination / "bootstrap-metadata.json").write_text(json.dumps(metadata, indent=2) + "\n")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    package(pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3])
