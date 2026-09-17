# Termux RTL — independent ARM64 application

## Installation and compatibility

The standalone APK has Android application ID **`com.termux.rtl`** and launcher
name **Termux RTL**. It can coexist with `com.termux` without uninstalling it.
Data, shared user ID, provider authorities, preferences, shortcuts, permissions and
intent actions are independent. Java/JNI class namespaces deliberately remain
`com.termux.*`: application IDs and class names are different concepts.

This build targets ARM64 and Android 7+. Do **not** uninstall the original Termux.
The earlier `termux-app_rtl-debug_universal.apk` (rtl.1) used `com.termux` and is
**not** a side-by-side build. Use only the new `rtl-standalone-debug_arm64-v8a.apk`
artifact after its workflow succeeds. Debug builds still use the public upstream
test signing key, not a production signing identity.

## Why a different APK ID is not enough

Upstream Termux binaries embed `/data/data/com.termux/files/usr`. Android does not
allow an unrelated application to execute/read another application's private files.
Renaming the APK alone results in a broken environment. This project instead
compiles the bootstrap from pinned package sources for
`/data/data/com.termux.rtl/files/usr`. No binary string rewriting, rooting, shared
UID with the original app, or copying of the user's original files is used.

## Package limitation (important)

The initial standalone build includes the source-built bootstrap utilities. It is
**not compatible with the complete upstream binary package ecosystem**. Official
APT sources are removed and `pkg`/`termux-change-repo` are replaced with an explicit
explanation to prevent accidentally installing incompatible upstream packages.
APT/dpkg remain available for locally supplied packages built with the same new
prefix. Do not add official Termux mirrors. A full custom package repository is a
separate project and has not been provided. Original Termux plugins also target a
different package/permission namespace and cannot be reused without rebuilding.

## Reproducible source selection and build

The `RTL Standalone ARM64 APK` workflow:

1. Tests application identity and bootstrap validation logic.
2. Checks out pinned `termux/termux-packages` revision
   `4f7f86a4159a3f1b90ab839f8cd9be8c301138a5`.
3. Patches the **source configuration assignment** `TERMUX_APP__PACKAGE_NAME`,
   before compiling any dependencies. It does not use `-i`/`-I` prebuilt packages.
4. Runs the upstream source bootstrap builder inside the package-builder Docker
   image for `aarch64`. The first build can take hours; validated bootstrap outputs
   are cached by source revision and preparation script hashes.
5. Verifies that the shell is ARM64, includes the new prefix, and that executables,
   shebang scripts and symlink metadata do not reference the old runtime prefix.
6. Disables incompatible repositories and writes SHA-256/provenance metadata.
7. Runs terminal unit tests and builds the APK; checks its Android package ID,
   native architecture and APK signature before publishing the ARM64 artifact.

The Docker image and external source availability can change; the pinned package
revision is not a guarantee of bit-for-bit reproducibility of the entire toolchain.

To build the APK with an already verified custom bootstrap:

```sh
export TERMUX_RTL_BOOTSTRAP_DIR=/absolute/path/to/rtl-bootstrap
export TERMUX_APP_VERSION_NAME=0.118.0-rtl.2
export TERMUX_APK_VERSION_TAG=rtl-standalone-debug
export TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS=1
./gradlew :terminal-view:testDebugUnitTest :terminal-emulator:testDebugUnitTest :app:assembleDebug
```

Gradle fails closed if the custom archive/metadata are missing or do not match the
application ID, architecture, prefix or checksum. It never falls back to upstream
bootstrap downloads. Generated bootstraps/APKs must not be committed to Git.

## Device acceptance checks (not yet performed)

- Install official Termux and standalone Termux RTL together; both launcher icons
  must open the intended app.
- In RTL: `echo "$PREFIX"; echo "$HOME"; uname -m` must show `com.termux.rtl`
  private paths and `aarch64`; `bash`, `ls`, `printf` and a new session must work.
- Open settings, launcher shortcuts, failsafe, file sharing, and document provider.
- Check Arabic rendering/selection as described in `RTL.md`.
- Confirm original Termux files and sessions are unaffected, including after
  uninstalling **only** the RTL app.
- Confirm `pkg update` explains the custom-package requirement rather than
  contacting official mirrors. Do not treat an APK build as device validation.
