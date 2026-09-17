#!/usr/bin/env python3
"""Configure a clean, pinned termux-packages checkout BEFORE compiling any binaries."""
import pathlib
import sys

root = pathlib.Path(sys.argv[1])
props = root / "scripts/properties.sh"
text = props.read_text()
old = 'TERMUX_APP__PACKAGE_NAME="com.termux"'
if text.count(old) != 1:
    raise SystemExit("Unexpected packages source: app package assignment not unique")
props.write_text(text.replace(old, 'TERMUX_APP__PACKAGE_NAME="com.termux.rtl"'))
print("Configured package compiler for /data/data/com.termux.rtl/files/usr")
