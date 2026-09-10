#!/usr/bin/env bash
set -euo pipefail

PACKAGE_NAME="com.termux.rtl"
APP_NAME="Termux RTL"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - "$ROOT" "$PACKAGE_NAME" "$APP_NAME" <<'PY'
from pathlib import Path
import re, sys
root = Path(sys.argv[1])
pkg = sys.argv[2]
app = sys.argv[3]

# Keep the Java namespace com.termux. Only the Android applicationId and runtime
# data/IPC package are changed; this avoids breaking the upstream source tree.
gradle = root / "app/build.gradle"
s = gradle.read_text()
if 'independentApplicationId' not in s and 'applicationId "com.termux.rtl"' not in s:
    s = s.replace('defaultConfig {\n', f'def independentApplicationId = "{pkg}"\n\n    defaultConfig {{\n        applicationId independentApplicationId\n', 1)
s = s.replace('manifestPlaceholders.TERMUX_PACKAGE_NAME = "com.termux"', f'manifestPlaceholders.TERMUX_PACKAGE_NAME = "{pkg}"')
s = s.replace('manifestPlaceholders.TERMUX_APP_NAME = "Termux RTL"', f'manifestPlaceholders.TERMUX_APP_NAME = "{app}"')
gradle.write_text(s)

# Patch only the authoritative package constant; all dependent paths and IPC
# names are derived from it by TermuxConstants.
constants = root / "termux-shared/src/main/java/com/termux/shared/termux/TermuxConstants.java"
s = constants.read_text()
if '"com.termux.rtl"' in s:
    pass  # already patched, idempotent
else:
    s, n = re.subn(r'(public static final String TERMUX_PACKAGE_NAME = )"com\.termux";', r'\1"com.termux.rtl";', s, count=1)
    if n != 1:
        raise SystemExit("TERMUX_PACKAGE_NAME constant was not found exactly once")
    constants.write_text(s)

for rel in ("app/src/main/res/values/strings.xml", "termux-shared/src/main/res/values/strings.xml"):
    p = root / rel
    s = p.read_text().replace('<!ENTITY TERMUX_PACKAGE_NAME "com.termux">', f'<!ENTITY TERMUX_PACKAGE_NAME "{pkg}">')
    if rel.startswith("termux-shared"):
        s = s.replace('<!ENTITY TERMUX_PREFIX_DIR_PATH "/data/data/com.termux/files/usr">', f'<!ENTITY TERMUX_PREFIX_DIR_PATH "/data/data/{pkg}/files/usr">')
    p.write_text(s)

shortcuts = root / "app/src/main/res/xml/shortcuts.xml"
s = shortcuts.read_text().replace('android:targetPackage="com.termux"', f'android:targetPackage="{pkg}"')
shortcuts.write_text(s)
PY

# The custom bootstrap archives are already built for this package. Prevent the
# normal Gradle downloader from deleting/replacing them with official com.termux zips.
python3 - "$ROOT/app/build.gradle" <<'PY'
from pathlib import Path
import sys
p = Path(sys.argv[1])
s = p.read_text()
needle = '    if (file.exists()) {\n'
replacement = '    if (file.exists()) {\n        return\n'
if replacement not in s:
    if needle not in s:
        raise SystemExit('downloadBootstrap() local-file guard was not found')
    s = s.replace(needle, replacement, 1)
p.write_text(s)
PY

echo "Prepared independent Termux RTL build: $PACKAGE_NAME ($APP_NAME)"
