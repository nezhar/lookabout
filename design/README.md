# Design hand-over

From the designer's deck "Lookabout-Screen-Review.pdf" (8 pages, 10 October 2026). Start with `values.md`.

- `values.md`: colours, type, layout, screen-by-screen structure and words, icon and cover, where the app departs from the deck, open questions.
- `icon.png`: 512 px transparent adaptive-icon foreground, white subject inside the centre 60%.
- `icon-combined.png`: the foreground on `#205D75` (512 px combined source).
- `cover.png`: 1000 x 420 px DEV cover.
- `screens/`: 412 x 915 dp screens at 824 x 1830 px (`*-412*`) and 360 x 800 dp screens at 720 x 1600 px (`*-360*`).
  - `setup-*`, `list-*`, `summary-*`: before and after the walk (deck pages 3 and 4).
  - `loop-*-next-picture`, `-checking`, `-accepted`, `-not-yet`: the photo loop (pages 1 and 2).
  - `state-360-model-loading`, `-list-writing`, `-check-error`, `-model-error`: waiting and error states (page 5).
  - `correction-360-before`, `-after`, `stress-360-long-prompt`, `-long-criteria`: corrections and long texts (page 6).
  - `icon-mask-circle`, `icon-mask-rounded-square`: the icon under launcher masks (page 8).
