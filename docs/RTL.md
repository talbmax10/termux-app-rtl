# Native RTL & Arabic Text

Termux RTL adds native bidirectional text handling and cursive shaping for RTL scripts at the terminal rendering layer.

## Supported scripts

- Arabic
- Persian
- Urdu
- Hebrew
- Mixed RTL/LTR text
- Arabic with Latin commands, paths, URLs and numbers

## Rendering goals

1. Preserve logical terminal data.
2. Apply Unicode bidirectional ordering for visual rendering.
3. Use Android's native text engine for cursive joining.
4. Keep terminal cells aligned to the monospace grid.
5. Keep cursor and selection behavior predictable.

## Test matrix

Test at minimum:

- Arabic words in a shell prompt.
- Arabic followed by English.
- English followed by Arabic.
- Arabic + numbers.
- Arabic + punctuation.
- File paths containing Arabic characters.
- Long wrapped Arabic lines.
- Cursor movement through mixed RTL/LTR text.
- Selection and copy of mixed text.
- Command output containing Arabic and ANSI styling.
- Persian and Urdu joining.
- Hebrew RTL ordering.

## Known limitations

Some mixed-direction selections, style boundaries, cursor-adjacent shaping, wide CJK characters and supplementary-plane characters may still expose edge cases. These should be reported with a minimal reproducible terminal example and Android version.

## Reporting a bug

Include:

- Android version and device.
- Termux RTL version/commit.
- Font in use, if non-default.
- Exact command or text that reproduces the issue.
- Screenshot or short recording when visual ordering is relevant.
- Whether the issue occurs with plain text or ANSI styling.
