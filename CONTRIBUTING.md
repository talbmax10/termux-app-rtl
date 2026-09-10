# Contributing to Termux RTL

Thank you for contributing.

## Scope

This repository focuses on native RTL/BiDi terminal rendering, Arabic/Persian/Urdu/Hebrew cursive shaping, usability and documentation.

## Before submitting changes

1. Keep changes focused and reviewable.
2. Preserve upstream Termux behavior unless the change is intentionally RTL-specific.
3. Run the relevant unit tests and build checks.
4. Add a regression test or reproducible example for rendering bugs when practical.
5. Never commit API keys, tokens, private certificates or other secrets.

## Attribution

The RTL implementation incorporates upstream/community work. Keep copyright notices and attribution intact. When modifying code derived from an upstream pull request, document the source in the commit or relevant documentation.

## Commit style

Prefer conventional, descriptive messages such as:

- `fix: correct mixed RTL/LTR cursor rendering`
- `test: add Arabic shaping regression case`
- `docs: clarify RTL selection behavior`
- `ci: publish arm64 debug artifact`

## Pull requests

Describe the problem, the technical change, how it was tested, and any remaining limitations. Screenshots are encouraged for visual rendering changes.
