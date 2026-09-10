# Architecture

## Position in Termux

The RTL work is implemented in the terminal rendering path. Shell programs continue to operate on the original logical character stream; visual reordering is performed for display.

## Rendering pipeline

```text
PTY / shell output
        |
        v
Terminal emulator buffer
        |
        v
Logical text + terminal attributes
        |
        v
Unicode BiDi analysis
        |
        v
Visual RTL/LTR runs
        |
        v
Android native text shaping
        |
        v
Monospace grid fitting
        |
        v
Canvas rendering
```

## Design principles

- Do not rewrite shell output merely to make it look RTL.
- Keep logical ordering available for selection, copy and terminal semantics.
- Perform BiDi processing as close as practical to rendering.
- Reuse layout information to reduce allocations during continuous terminal updates.
- Preserve cell/grid alignment even when a shaped run has different native glyph metrics.

## Upstream attribution

This project is a focused community build based on Termux and incorporates the native RTL/cursive-shaping work from Termux pull request #5179 by `muslim-kh09`.

Upstream project: https://github.com/termux/termux-app

RTL implementation reference: https://github.com/termux/termux-app/pull/5179

Changes in this repository should remain clearly attributable to their original authors and upstream sources.
