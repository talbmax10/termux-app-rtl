#!/usr/bin/env bash
set -euo pipefail

# Prepare agnostic-apollo/termux-packages for a standalone Termux RTL bootstrap.
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PKGS="${1:-$ROOT/../termux-packages}"
PROPS="$PKGS/scripts/properties.sh"
BOOTSTRAP="$PKGS/scripts/build-bootstraps.sh"

sed -i 's/^TERMUX_APP__PACKAGE_NAME=.*/TERMUX_APP__PACKAGE_NAME="com.termux.rtl"/' "$PROPS"
sed -i 's/^TERMUX_APP_PACKAGE=.*/TERMUX_APP_PACKAGE="com.termux.rtl"/' "$PROPS"

if [ -f "$PKGS/x11-packages/foot/build.sh" ]; then
  sed -i 's/02072b8f0aaf26907b6b02293c875539ce52fc59079344e7cf811ab03394cfa3/b3fa774983abb5f95aecca4557d146091d8666bc65fb310d4d3e38327357ade9/' "$PKGS/x11-packages/foot/build.sh" || true
fi
if [ -f "$PKGS/packages/libunbound/build.sh" ]; then
  sed -i 's/--with-pyunbound/--without-pyunbound/' "$PKGS/packages/libunbound/build.sh" || true
fi
if [ -f "$PKGS/packages/docbook-xml/build.sh" ]; then
  sed -i 's|https://docbook.org/xml|https://archive.docbook.org/xml|g' "$PKGS/packages/docbook-xml/build.sh" || true
fi

python3 - "$BOOTSTRAP" <<'PY'
from pathlib import Path
import re, sys
p = Path(sys.argv[1])
s = p.read_text()
pattern = re.compile(
    r'(?ms)^\s*if \[\[ "\$BOOTSTRAP_ANDROID10_COMPATIBLE" == "false" \]\]; then\n'
    r'(?:(?!^\s*fi\s*$).)*?'
    r'^\s*fi\s*$'
)
match = None
for m in pattern.finditer(s):
    if 'command-not-found' in m.group(0):
        match = m
        break
if match:
    replacement = '''\t\t# Omit command-not-found from the bootstrap because it pulls in heavy\n\t\t# optional dependencies and can create a circular dependency.\n\t\tPACKAGES_LIST+=("proot")'''
    s = s[:match.start()] + replacement + s[match.end():]
s = s.replace('PACKAGES_LIST+=("command-not-found")', '')
p.write_text(s)
PY

if grep -Fq 'PACKAGES_LIST+=("command-not-found")' "$BOOTSTRAP"; then
  echo 'ERROR: command-not-found package entry still present' >&2
  exit 1
fi
bash -n "$BOOTSTRAP"
echo "✓ termux-packages prepared for com.termux.rtl"
