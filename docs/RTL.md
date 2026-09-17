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

## Current product: local SSH client

For side-by-side installation **using the original Termux files and tools**, use
`:rtl-client:assembleDebug` and the **RTL Local SSH APK** workflow. No custom
bootstrap is required. See [the Arabic setup/security guide](RTL-SSH-AR.md).

The earlier native standalone build in [RTL-STANDALONE.md](RTL-STANDALONE.md)
creates a separate environment and cannot access the original app's private files;
it is not the selected product. Its source-bootstrap workflow is now manual-only.

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
