# Experimental RTL terminal rendering

RTL is enabled by default in the normal terminal buffer. Long-press the terminal,
open **More…** and toggle **RTL terminal text (experimental)** to restore the
upstream renderer. The choice is saved across restarts and font changes.

## Design

- Unicode bidi reorders visual cells, with a left-to-right paragraph base to keep
  shell prompts, commands and trailing blank cells anchored to the terminal grid.
- Android shapes directional text using the surrounding directional run, including
  when ANSI colors, selection or the cursor split that run.
- The emulator, PTY input, history, escape sequences and copied text stay in logical
  order. This is not string reversal and does not change shell commands.
- Combining marks remain attached to their base; surrogate pairs and wide cells
  are not reversed internally. Selection hit testing and handles use the same
  visual/logical mapping as rendering.
- ASCII-only rows keep the original renderer. Alternate-screen programs (for
  example Vim) keep upstream rendering and mouse coordinates intentionally.

## Limits / device verification needed

This is experimental display support, not a bidi-aware shell editor. Cursor and
arrow-key movement are still controlled in logical order by the running program.
A logical selection crossing directional boundaries may highlight disjoint areas.
Bidi is resolved per terminal row, not across soft-wrapped paragraphs. The grid
is not right-aligned. Full-screen applications in the normal buffer may need RTL
disabled from the menu. Font fallback and Arabic ligatures split by cursor/color
boundaries need visual testing on Android; glyphs are fitted to terminal columns.
No device/emulator visual validation has been performed in the coding sandbox.

## Build

Use JDK 17, Android SDK platform 36 and NDK 29.0.14206865. The Gradle wrapper is
pinned to 8.13 for Android Gradle Plugin 8.13.2 (instead of unsupported Gradle 9).

```sh
export TERMUX_SPLIT_APKS_FOR_DEBUG_BUILDS=0
export TERMUX_APP_VERSION_NAME=0.118.0-rtl.1
export TERMUX_APK_VERSION_TAG=rtl-debug
./gradlew :terminal-view:testDebugUnitTest :terminal-emulator:testDebugUnitTest :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/termux-app_rtl-debug_universal.apk`.
The **RTL APK** GitHub Actions workflow runs the tests and publishes this APK plus
a SHA-256 checksum as an artifact.

The APK keeps package ID `com.termux` and uses the upstream **public debug test
key**, not a private production key. It cannot update an installation signed with
a different key. Back up Termux data before uninstalling an existing installation;
uninstalling deletes its data. Plugins must have compatible signatures. Do not
publish this debug build as a privately signed production release.

## Manual Android checks

```sh
printf '%s\n' 'مرحبا بالعالم' 'שלום עולם' 'user: مرحبا 123 /tmp/file' 'رَبَ 😀 界'
printf '\033[31mمرحبا\033[0m بالعالم\n'
printf 'مر\033[32mحب\033[0mا\n'
```

Check connected Arabic letters, readable Hebrew, LTR digits/paths, combining marks,
block/bar/underline cursors, selecting and copying Arabic/mixed lines (including
scrollback), resize/rotation, font size changes, and turning RTL off/on. Test `ls`,
readline editing, `top`, `vim` and `less` for regressions. Copying a complete line
must reproduce the original logical text, not its visual order.
