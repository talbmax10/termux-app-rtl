# Testing Guide

## Automated checks

Every change should pass the existing Termux unit tests and Android build checks before release.

## RTL regression checklist

- Arabic connected glyphs render as connected words.
- RTL runs are visually ordered correctly.
- Mixed Arabic/English remains readable.
- Numbers and punctuation remain in the expected visual position.
- Cursor movement does not corrupt displayed shaping.
- Selection/copy preserves logical text.
- ANSI styling does not unexpectedly break shaping.
- Long lines and wrapping remain aligned to terminal cells.

## Device testing

For release candidates, test on a real Android device in portrait and landscape modes, with both light and dark themes where applicable. Record the Android version, device model, app version and commit SHA with each test report.

## Performance

Continuous terminal output should be tested while scrolling and while updating the cursor. The goal is to avoid unnecessary allocations in the render loop and to preserve smooth interaction on high-refresh-rate devices.
