# Lookabout design values

Everything from the designer's deck "Lookabout-Screen-Review.pdf" (8 pages, 10 October 2026) that a developer needs, so the PDF is not required. Native Android, Material 3.

How to read this file:

- Values without a mark come from the deck's text (page 7 holds the values the developer types in).
- **sampled**: pixel colour read with a script from the PNGs in `screens/`. Antialiased lines and text give a range; the darkest or most common pixel is given.
- **read off**: measured from the PNGs (360 dp screens are 2 px per dp). Good to about 1 dp or 1 sp, not a deck value.
- Page 7 names a file `DEVELOPER-HANDOFF.md` as "the complete specification", and page 8 says SVG and Android vector/adaptive XML resources "are in the pack". Neither was handed over; everything here comes from the deck and its PNGs.

## Name and line

- Name: **Lookabout**
- Line: **A little walk. A fresh look.** (Setup header and deck page 8)
- The cover (`cover.png`) uses its own wording: "HACKTOBERFEST 2026 / TOUCH GRASS", "Lookabout", "Take a walk. Find a fresh way to see.", "3-6 prompts. On-device checks. Offline."

## Colours

From page 7:

| Role | Hex | Used for |
|---|---|---|
| Primary | `#205D75` | Main button fill, text actions (Skip, New list), active progress dots, progress indicator, selected chip border, list number text, "Changed by you" |
| Accepted | `#24623E` | "Accepted" word, its coverage line, check marks in criteria, Summary headline and Accepted chip text |
| Not yet | `#8A4B00` | "Not yet" word, its coverage line, pause icon, Summary Not yet chip text |
| Main text | `#19231F` | Prompts, titles, criteria, counter |
| Background | `#FAFAF7` | Screen background |
| Correction fill | `#DCECF2` | Corrected criterion row (with "Changed by you") |

Other colours read off the screens (all **sampled**):

| Role | Hex | Where |
|---|---|---|
| Accepted circle tint | `#E5F2E8` | 48 dp circle behind the verdict check; Summary "Accepted" chip fill |
| Not yet circle tint | `#FFF0DB` | 48 dp circle behind the verdict pause; Summary "Not yet" chip fill |
| Primary tint | `#DCECF2` | Same value as Correction fill; also the selected segment (Pictures), selected chips (Topic, sky), list number circles, progress indicator track |
| Secondary text | about `#414E47` (antialiased `#414E47` to `#56625B`) | Topic line above the prompt, "You can keep looking around.", "Tap a check to change it.", list meta line, Summary "Your walk" and date, Setup line, "Gemma and you agreed ..." |
| Faint text | about `#78817B` | Summary per-photo line ("75% · 6 s", "Not checked") |
| Cross icon (criterion No) | about `#414E47` | Same grey as secondary text, not red |
| White | `#FFFFFF` | Text and icons on primary buttons; fill of unselected segments, chips and the text field |
| Divider between rows | about `#E2E8E2` | Thin line between criteria rows and list rows |
| Divider under the last row | about `#CFD9D1` | Slightly stronger line under the last criterion and under the last list row |
| Outline (segments, chips, text field) | about `#67766C` to `#7F8B84` | 1 dp outline of unselected segments, chips and the outlined text field |
| Selected chip border | about `#1E5C74` | 2 dp border, close to Primary |
| Inactive progress dots | `#D5DDD6` | Dots for pictures not reached yet |
| Disabled button fill | `#DFE4DF` | "Getting the model ready" |
| Disabled button text | about `#556059` | "Getting the model ready" |
| Error text and icon | about `#9E231C` to `#A12B24` | "Couldn't check this photo.", "Couldn't get the model ready.", error icon |
| Error card fill | `#FCEAE7` | Card around "Couldn't check this photo." |
| Neutral tile and chip fill | `#EFF2EF` | Summary "No photo" tile and "Skipped" chip |
| Icon background | `#205D75` | `icon-combined.png` background, same as Primary |

## Type

Page 7 gives sizes in sp as size / line height. The face in the screens looks like Roboto (the system font); the deck does not name one. Weights are read off.

| Style | sp | Weight (read off) | Text |
|---|---|---|---|
| Prompt | 57 / 64, 45 / 52, 36 / 44 | Bold | The prompt on Next picture. All supplied screens, including the long prompt at 360 dp (7 lines), use 57 / 64; the deck gives no rule for when 45 / 52 and 36 / 44 apply |
| Verdict | 32 / 40 | Medium | "Accepted", "Not yet"; "Checking" and "Check paused" read off at the same size |
| Coverage | 20 / 24 | Medium | "75%, 3 of 4" under the verdict |
| Criteria | 16 / 20 | Regular | Criterion questions; wraps to two lines (long criteria screen) |
| Buttons | 18 / 22 | Medium | "Shoot", "Retake", "Next picture", "Make my list", "Start walk", "New walk", "Try again", text actions "Skip" and "New list" |
| Caption | 14 / 20 | Regular | "Tap a check to change it.", list meta line, Summary "Your walk" and date |
| Correction | 12 / 18 | Medium | "Changed by you" |

Text without a deck style, sizes **read off** (cap height divided by 0.711), so treat as approximate:

- Screen titles "Lookabout", "Your list", "Making your list": about 28 sp, bold for the first two, medium for "Making your list".
- List topic "Autumn in the city" (List): about 22 sp, medium. Summary title "Autumn in the city": about 24 sp, bold.
- Summary headline "3 of 5 accepted": about 24 sp (360) to 26 sp (412), bold, Accepted colour.
- Setup section labels "Pictures", "Topic", "Look up. What is the sky doing?": about 18 sp, medium.
- Segment numbers: about 18 sp. Chip labels: about 15 sp, medium. Text field value 16 sp, label 12 sp.
- List rows: about 20 sp at 412 dp, about 18 sp at 360 dp, regular, up to two lines. List numbers: about 16 sp, primary.
- Summary row prompt "1. A reflection ...": about 16 sp at 412, about 15 sp at 360, medium. Per-photo line and status chips: about 12 to 13 sp.
- "You can keep looking around.", "Finding five things to notice.", "This may take a little while.", error lines, Setup line, "Gemma and you agreed on ...", counter "2 of 5": about 16 sp, regular.

## Layout

From page 7 (dp):

- Page inset: 20 (left and right on every screen).
- Main button: 56 high, full width inside the inset, fully rounded (pill, read off).
- Secondary action: 48 high, text-only in primary colour ("Skip", "New list"), centred under the main button.
- Minimum target: 48 x 48.
- Action gap: 8 (between main button and secondary action).
- Photo radius: 16.
- Chip radius: 12.

Compact verdict geometry (360 x 800 dp, page 7):

- Photo: y 180, height 144 (full width inside the inset: 320 x 144).
- Criteria: y 336, rows 64 high each; a corrected row adds 16 (80 high).
- Retake: y 648. Skip: y 712. Accepted primary ("Next picture"): y 704.
- "Use actual system insets. These y-values include the reference status and navigation bars." Lay out from the real `WindowInsets`, not from these absolute y-values.

Read off from the screens:

- Top bar on loop screens: counter "2 of 5" at the left, five progress dots at the right; dots 8 dp across, 14 dp centre to centre, active dots Primary for pictures done plus the current one.
- Verdict header: 48 dp tinted circle with the icon, the verdict word to its right, the coverage line below.
- 412 x 915 verdict screens: photo 372 x 200 at y 196; criteria rows 68 high starting at y 412; Retake at y 763, "Next picture" at y 819.
- Checking and check error: photo full width at 4:3 (320 x 240 at 360 dp, 372 x 279 at 412 dp), top at y 104.
- Corrected row and error card: 12 dp corner radius; error card 56 high.
- Criterion row: Yes/No icon centred at about x 40 dp, question text from x 64 dp.
- Setup: segmented control 52 high; chips 48 high with 8 dp gaps; outlined text field 56 high.
- List: rows 88 high, number in a 36 dp tinted circle, divider under each row.
- Summary: square thumbnails 76 dp (360) / 88 dp (412) with 12 dp radius, 12 dp between rows.
- Progress bars (indeterminate, linear): about 4 dp thick; 180 dp wide and centred on Checking, full width on List writing.

## Screen by screen

All screens show the status bar and gesture bar of the reference phone. Screens are drawn at 412 x 915 dp and 360 x 800 dp, same content, same order. The sample walk is "Autumn in the city", Overcast, five pictures.

### Setup (`setup-412.png`, `setup-360.png`)

Top to bottom, scrolls if needed, main button pinned at the bottom:

1. "Lookabout" (title) and "A little walk. A fresh look." (secondary).
2. "Pictures": segmented control 3 / 4 / 5 / 6. Selected segment: `#DCECF2` fill and a small check before the number.
3. "Topic": filter chips "Autumn colour", "Reflections", "Light and shadow", "Doors and windows", "Small details", "Street life", "Your own". Selected chip: `#DCECF2` fill, 2 dp primary border, check icon, primary text. Chips wrap to the next line.
4. When "Your own" is selected: outlined text field labelled "Your topic" (sample value "Autumn in the city").
5. "Look up. What is the sky doing?": chips "Clear", "Some cloud", "Overcast", "Rain", "Fog", same selected style.
6. Main button "Make my list" (no icon).

The deck's sample selects 5, "Your own" and "Overcast"; which choices start preselected in the app is still open (see the last section).

### List (`list-412.png`, `list-360.png`)

1. "Your list" (title), topic "Autumn in the city", meta line "Overcast · 5 pictures" (caption).
2. Every prompt numbered 1 to n, each with its number in a tinted circle and a divider below. All prompts are visible before leaving ("Reveal every prompt before leaving.").
3. Pinned: main button "Start walk" with a leading arrow icon, then text action "New list".

Sample prompts: "A reflection with someone walking past", "Fallen leaves on something man-made", "A doorway with one strong colour", "Looking straight up through bare branches", "A lit window on a grey street".

### Next picture (`loop-412-next-picture.png`, `loop-360-next-picture.png`, `stress-360-long-prompt.png`)

1. Top bar: "2 of 5" and progress dots.
2. Topic "Autumn in the city" (secondary).
3. The prompt in the Prompt style, left aligned, as large as possible.
4. Pinned: main button "Shoot" with a leading camera icon, then text action "Skip".

Page 1 and 2 headline: "Large prompt, fixed actions, four checks / No scrolling".

### Checking (`loop-412-checking.png`, `loop-360-checking.png`)

1. Top bar.
2. The photo just taken, full width, 4:3, 16 dp radius.
3. "Checking", centred, Verdict size.
4. Indeterminate linear progress bar, centred, Primary on a `#DCECF2` track ("The check indicator is indeterminate.").
5. "You can keep looking around." centred, secondary.

No buttons.

### Accepted (`loop-412-accepted.png`, `loop-360-accepted.png`)

1. Top bar.
2. Verdict header: check icon in a `#E5F2E8` circle, "Accepted" in `#24623E`; coverage "75%, 3 of 4" in `#24623E`.
3. Photo, full width, 16 dp radius (cropped to the compact height).
4. Four criterion rows: check icon (Accepted green) for Yes, cross icon (grey) for No, the question; divider below each row, a stronger one under the last.
5. "Tap a check to change it." (caption).
6. Pinned: main button "Next picture" with a leading arrow icon. No Skip.

Feedback (page 1 and 2): "Accepted: word + check + green, one long buzz."

### Not yet (`loop-412-not-yet.png`, `loop-360-not-yet.png`)

Same structure as Accepted, with:

- Pause icon in a `#FFF0DB` circle, "Not yet" and "25%, 1 of 4" in `#8A4B00`.
- Pinned: main button "Retake" with a leading camera icon, then text action "Skip".

Feedback: "Not yet: word + pause + amber, two short buzzes."

"The walker can inspect and correct each answer before moving on."

### Summary (`summary-412.png`, `summary-360.png`)

1. "Your walk" (caption), topic title "Autumn in the city", date "10 October 2026".
2. Headline: check icon and "3 of 5 accepted" in Accepted green.
3. One row per picture: square thumbnail; "1. <prompt>"; "75% · 6 s" (coverage and check time); a status chip:
   - "Accepted": check icon, `#E5F2E8` fill, green text.
   - "Not yet": pause icon, `#FFF0DB` fill, amber text.
   - "Skipped": skip icon (">|"), `#EFF2EF` fill, grey text. Its thumbnail is a `#EFF2EF` tile with the skip icon and "No photo", and the line reads "Not checked".
4. "Gemma and you agreed on 15 of 16 checks." (secondary, larger than caption).
5. Pinned: main button "New walk" (no icon).

Page 3 and 4: "Keep the contact sheet in one frame for the sample walk." Sample totals: 3 of 5 accepted, agreed on 15 of 16 checks. "Photography and check times are illustrative design samples, not measured model results." Page 8: "The Summary screen itself is the write-up contact sheet."

### Waiting states (page 5, 360 x 800)

- Model loading (`state-360-model-loading.png`): the Setup screen with the main button disabled and reading "Getting the model ready" (fill `#DFE4DF`, grey text). Inputs stay usable.
- List writing (`state-360-list-writing.png`): List header ("Your list", topic, meta), then "Making your list", "Finding five things to notice." (the count is written as a word), a full-width indeterminate linear progress bar, "This may take a little while." No buttons.
- "Both waiting indicators use standard Material 3 motion."

### Error states (page 5, 360 x 800)

"One error pattern: a short line and Try again. Retry the failed operation; keep completed walk results." "Preserve inputs and the current photo when an operation needs to be retried."

- Check error (`state-360-check-error.png`): top bar, the photo (as on Checking), "Check paused", an error card (`#FCEAE7`, 12 dp radius) with an error icon (exclamation in a circle outline) and "Couldn't check this photo." in red, main button "Try again". The deck shows only "Try again"; the app adds "Skip" (see departure 1).
- Model error (`state-360-model-error.png`): the Setup screen with "Couldn't get the model ready." in red just above the main button, which reads "Try again".

### Corrections (page 6, `correction-360-before.png`, `correction-360-after.png`)

- "Tap the complete criterion row": the whole row is the target and flips Yes and No.
- "All coverage values are multiples of 25%" (four criteria).
- Sample: picture 4 of 5, "Looking straight up through bare branches". Before: 25%, Not yet, "Is the sky visible?" No. After the tap: the row turns `#DCECF2` with 12 dp radius, shows a check and "Changed by you" (Correction style, primary) under the question, the row grows by 16 dp, the verdict becomes Accepted, 50%, 2 of 4, and the action becomes "Next picture".
- "The photo stays in place; the verdict and action update."
- Page 3 and 4: "A blue row tint and 'Changed by you' mark corrections."

### Long texts (page 6)

- Long prompt (`stress-360-long-prompt.png`): "Something small and overlooked, shot so close it fills the whole frame" in 7 lines of 57 / 64 at 360 dp; Shoot and Skip stay in place.
- Long criteria (`stress-360-long-criteria.png`): four criteria of two lines ("Does the subject fill at least half of the frame?") still fit in the 64 dp rows.
- "Stress screens test layout only. They do not add extra prompts to the five-picture walk."

## Icon and cover

- `icon.png`: 512 px transparent foreground, white subject (viewfinder corners around a leaf). The subject sits inside the centre 60% (its bounds are about 127 to 384 px, roughly the centre 50%).
- `icon-combined.png`: the same foreground on `#205D75`, the 512 px combined source.
- `screens/icon-mask-circle.png`, `screens/icon-mask-rounded-square.png`: the deck's preview of the combined icon under a circular and a rounded-square mask.
- Adaptive icon: foreground layer from `icon.png`, background layer a solid `#205D75`. "The icon subject stays inside the centre 60%", so launcher masks never cut it. The deck mentions "Separate transparent foreground and solid background included" and SVG and vector/adaptive XML resources; only the PNGs were handed over, so the vector foreground has to be redrawn from `icon.png`.
- `cover.png`: DEV cover, "exactly 1000 x 420 px". Two phones: Next picture ("Fallen leaves on something man-made") and Accepted (the sample puddle photo). See departure 4.

## Where the app departs from the deck

These are decided; build them this way.

1. **Failed check offers "Try again" and "Skip".** The deck shows only "Try again". Place them like Retake and Skip on Not yet: main button "Try again", text action "Skip" under it with the 8 dp gap.
2. **Not yet at 360 x 800 dp: the photo shrinks when rows grow.** The deck uses fixed positions with room for one corrected row. In the app the photo starts at 144 dp high and shrinks, down to 80 dp, when criteria rows grow (corrections, two-line criteria), so Retake and Skip stay in place.
3. **List and Summary: rows scroll under the pinned buttons when six pictures don't fit.** The deck shows five on one screen. The header and rows scroll; "Start walk" / "New list" and "New walk" stay pinned.
4. **Cover image: real screenshots from a walk replace the sample photo.** Keep the cover's layout and text; the phones show real app screenshots.

## Open with the designer

- Which Setup choices are preselected. Until answered the app starts with 5 pictures, the first topic ("Autumn colour") and no sky selected.
- A yes or a redraw for each of the four departures above.
