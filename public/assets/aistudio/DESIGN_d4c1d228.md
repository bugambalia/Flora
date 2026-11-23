# DESIGN.md — Flora, herbario musical

Agent-facing design contract for this repository. Read it before touching `src/App.tsx` or
`src/index.css`. It records decisions that are already implemented, not aspirations.

| | |
|---|---|
| **Product** | Flora — a music app whose catalog is a botanical herbarium |
| **Surface** | App UI (Operate mode). Not marketing, not a landing page |
| **Target platform** | Android native, phone portrait first |
| **Runtime** | React 19 + Vite 8, Tailwind CSS v4 imported but **not used for styling** |
| **Styling model** | One hand-authored stylesheet (`src/index.css`) + inline `<svg>` illustrations |
| **Reference captures** | `docs/screens/*.png` (Playwright, 430×900 CSS px; the shell capture is 1280×900) |

The brief is pinned: editorial botanical-plate aesthetic, flower theme visible and felt,
warm paper + ink, Fraunces / Cormorant Garamond / DM Sans. **The brief wins over generic
"modern app" taste.** Do not drift toward SaaS neutrals, glassmorphism, or neon.

---

## 1. Visual world

The interface behaves like a **printed herbarium**: paper sheets, mounted plates, ink rules,
specimen labels, wax seals and gold-leaf accents. Every layout decision should be defensible
as something a botanical illustrator would do on paper.

Consequences that are already in the code:

- Cards are **mounted plates**: thin ink border, an inner gold hairline, a hard offset shadow
  (`5px 6px 0`) that reads as a sheet resting on a desk — never a blurry drop shadow.
- Corners are **asymmetric** (see §6): one sharp corner + one generous round corner, like a
  plate glued onto a sheet with one lifted corner.
- Illustration is **pigment**, UI is **ink**. Illustrations may use raw hex; components may not.
- Decorative seals (`FL`, `❀`, hanko squares) are always `aria-hidden`.

## 2. Anti-slop rules (hard bans)

These are verified against the detector `scripts/detect.mjs` of the Impeccable skill. Violating
them makes the UI look AI-generated:

1. **No side-tab accent borders** (`border-left: 3px solid …`). One was removed from
   `.editorial-strip`; do not reintroduce it. Use a full border + a seal.
2. **Never animate layout properties** (`width`, `height`, `padding`, `margin`). Progress bars
   use `transform: scaleX()` with `transform-origin: left center`.
3. **No raw hex in JSX** except `collections[].color` (per-collection pigment data) and SVG
   illustration paths.
4. **No gradient text**, no glass blur, no neon glow, no emoji used as an icon.
5. **No unnamed icon buttons** and no placeholder-as-label.
6. **No text below 10px** outside decorative `aria-hidden` labels (EQ scale is 8px and hidden
   from AT; status bar is a prop of the mockup).

Known and accepted detector warnings: `overused-font` for Fraunces (13 hits). It is intentional
— the brief pins an editorial serif voice. Do not "fix" it by switching fonts.

## 3. Tokens

Defined in `src/index.css` `:root`. **Components must consume `var(--token)`.** A token that is
not used anywhere must be deleted, not kept "for later" (`--blush`, `--paper-deep`,
`--vermilion` were removed for this reason).

| Token | Value | Role |
|---|---|---|
| `--ink` | `#173026` | Primary text, 9px phone frame border, deep surfaces |
| `--forest` | `#204235` | Dark surface base (player, eq board) |
| `--forest-soft` | `#264c3d` | Dark surface highlight / avatar gradient start |
| `--forest-deep` | `#1d3c30` | Dark surface shadow / avatar gradient end |
| `--sage` | `#58614a` | Kickers, eyebrows, secondary serif labels on paper |
| `--sage-light` | `#dce2d2` | Active nav pill, quiet fills |
| `--paper` | `#f6efe0` | Screen background, card fill |
| `--terracotta` | `#b8442c` | Primary accent: active chip, numbers, seal, warnings |
| `--cobalt` | `#23479c` | Action color (primary CTA), global focus ring on light |
| `--cobalt-deep` | `#183270` | CTA pressed/hover |
| `--gold` | `#d5a342` | Hairlines, plate frames, borders on paper |
| `--gold-soft` | `#e6c678` | Gold on dark surfaces (liked state, focus ring on dark) |
| `--shell-bg` | `#d2dac8` | Desktop stage behind the phone |
| `--border-ink` | `#274437` | Card/plate outlines |

Approved on-paper secondary text values (not tokens, validated pairs):
`#5a675c` (collection meta, 5.20:1), `#5b695e` (track meta, 5.06:1), `#5d6a5f` (inactive nav,
4.97:1), `#56635a` on `#ebe1ca` (4.85:1), `#4f5f54` (hero subtitle), `#3f5246` (strip quote).

## 4. Typography

| Family | Loaded in | Used for | Notes |
|---|---|---|---|
| **Cormorant Garamond** italic 700 | `index.html` `<link>` | Screen titles (`h1`, 31px), hero headline (27px), player title (25px) | High-contrast display serif. Italic only. |
| **Fraunces** 500/600 + italic | `index.html` `<link>` | Editorial serif for names and voices: song titles (13.5px), collection names (12px), section headings, quotes, empty state, EQ presets | Never below 11px. |
| **DM Sans** 400–700 | `index.html` `<link>` | All UI text, kickers, labels, numbers | Uppercase kickers get `0.16em–0.22em` tracking. |

Rules:

- Fonts are loaded via `<link rel="preconnect">` + stylesheet in `index.html`. **Never add
  `@import url(fonts.googleapis.com…)` back into CSS** — it blocks first paint.
- Kickers (`.eyebrow`, `.alphabet-header h3`, `.hero-tag`) are 9–10px, uppercase, tracked,
  `--sage` or `--terracotta`.
- Italic is the voice of the editorial serif. Do not italicize DM Sans UI text.
- Numbers in the EQ (`32 … 16K`) and the time row are DM Sans, tabular feel, `aria-hidden`
  when they duplicate information.

## 5. Layout system

### Phone shell

```
.phone  width: min(430px, 100%)
        height: min(900px, calc(100svh - 56px))   /* 56px = .app-shell vertical padding */
        min-height: 680px
        border: 9px solid var(--ink); border-radius: 38px; overflow: hidden
```

The `- 56px` is not arbitrary: `.app-shell` has `padding: 28px`, so any larger value creates a
page scrollbar on desktop. If you change the shell padding, change this calc with it.

`@media (max-width: 560px)`: the phone becomes edge-to-edge (`100svh`, no border, no radius,
`background-botany` hidden). This is the state that represents the real Android device.

### Vertical stack inside the phone

```
status bar (36px, decorative)
└ content (flex: 1, overflow-y: auto, padding: 14px 22px 156px)
└ mini player (absolute, left/right 14px, bottom 69px, height 78px)
└ nav (absolute, bottom 0, height 65px)
└ gesture bar (absolute, bottom 4px, 110×4px, z-index 20)
```

**Bottom inset math:** `156px` bottom padding on `.content` clears the floating player
(69 + 78 = 147) plus breathing room. Any new scrollable screen must keep an equivalent inset or
the last item disappears behind the player.

Overlays (`full-player`, `equalizer-screen`) are `position: absolute; inset: 35px 0 0` — they
start under the status bar and cover the tab bar, matching Android full-screen behaviour. Each
carries `animation: rise 0.35s ease both`.

### Desktop stage

`.app-shell` centers the phone on `--shell-bg` with `.background-botany` (repeating dot grid +
large SVG botanical marks). It is a presentation frame, not a responsive desktop layout: do not
build a wider multi-column UI.

## 6. Corner language

Radii are asymmetric and scale with the element:

| Element size | Pattern | Example |
|---|---|---|
| Thumbnails (46–56px) | `1px 9–13px 1px 1px` / `1px … 13px 1px 1px` | search art, eq art |
| Chips, strips, rows (24–60px) | `2px 10–16px 2px 10–16px` | alphabet button, editorial strip, track row |
| Cards, boards (140–320px) | `3px 13–23px 3px 13–23px` or `3px 22px 3px 3px` | collection card, player, eq board |
| Sheets (hero, cover) | `4px 24–28px 4px 4px` / `4px 18px 4px 18px` | hero card, library cover, endemic card |
| Circular | `50%` | avatar, play button, waveform disc |

Never use a single uniform radius (`border-radius: 12px`) on a plate-like element; that is the
generic-card tell this project avoids.

## 7. Components

### 7.1 Navigation (`nav`)

- 3 tabs, `grid-template-columns: repeat(3, 1fr)`, height 65px, `padding: 7px 26px 12px`.
- Icon sits inside a pill: `span { height: 30px; min-width: 48px; border-radius: 15px }`.
  Active state paints the pill with `--sage-light` (Material 3 style). **No underline** — an
  `::after` bar existed and was removed.
- Inactive label `#5d6a5f` at 10px; active is `--ink` + `font-weight: 700`.
- Semantics: `<nav aria-label="Navegación principal">`, active button carries
  `aria-current="page"`.

### 7.2 Mini player (`.player`)

- `grid-template-columns: 50px 1fr repeat(4, 44px)` → cover, title block, like, prev, play, next.
  Every column is ≥44px: **touch targets are a hard requirement on Android.**
- Background: `linear-gradient(135deg, var(--forest-deep), var(--forest-soft))`, 1px gold border,
  asymmetric radius `3px 19px 3px 19px`.
- Play button 36px inside a 44px cell, gold hairline, `:active { transform: scale(0.92) }`.
- Progress line: 3px `.progress > i` with `transform: scaleX(progress/100)` and
  `transition: transform 0.5s linear`. Container is `aria-hidden` (the full player owns the
  real slider).
- Like button is the **rose/flower glyph**, `filled` when active, `aria-pressed`.

### 7.3 Full player (`.full-player`)

- Layers: dark gradient + terracotta radial glow + `orquideas-acuarela.jpg` texture at 460px,
  plus a 330px `BotanicalMark` at `opacity: .12` in `--gold-soft`.
- `Música | Letras` tabs use `aria-pressed`; separator is a 1px `i[aria-hidden]`.
- Disc: `min(244px, 67vw)` circle; ring is a `repeating-conic-gradient` masked with
  `radial-gradient(transparent 62%, #000 63%)`; `.playing` runs `pulse-ring` (3.4s).
- Panel: `justify-content: center; gap: 44px` — the disc + title block are optically centred,
  controls stay pinned at the bottom. Do not go back to `space-between` (it left a 300px void).

### 7.4 Seek (`input[type=range]` pattern — canonical)

This is the required pattern for **anything scrubbable**:

```jsx
<div className="progress-seek">
  <input type="range" min="0" max="100" value={Math.round(progress)}
         onChange={…} aria-label="Posición de la canción"
         aria-valuetext={`${formatTime(elapsedSeconds)} de ${activeTrack.duration}`} />
  <span className="seek-track" aria-hidden="true">
    <i style={{ transform: `scaleX(${progress / 100})` }} />
    <b style={{ left: `${progress}%` }} />
  </span>
</div>
```

- The real `<input>` is `opacity: 0` and covers the 24px-tall hit area (keyboard + a11y intact).
- Visuals live in the sibling `.seek-track`; focus is forwarded with
  `.progress-seek input:focus-visible ~ .seek-track { box-shadow: 0 0 0 2px var(--gold-soft) }`.
- Fill animates with `transform`, thumb with `left`.

### 7.5 Alphabet bush (`.alphabet-bush`)

- 27 buttons (`Todo` + A–Z), `min-width: 44px; min-height: 44px`, radius `3px 13px 3px 13px`,
  gap 6px → 7 per row at 430px. Uniform grid; do not let widths vary.
- Letters with no content are **`disabled`** and `opacity: .34`. They must never navigate to an
  empty state.
- Active chip: `--terracotta` fill, cream text. `aria-label="Filtrar por X"`.

### 7.6 Track list (`.track-list`)

- Row: `grid-template-columns: 32px 1fr auto`, `padding: 12px 6px`, hairline separator.
- `.track-number` is centred, `--terracotta`, Fraunces 12px. The **current** track shows a
  13px `note` icon instead of its number (it replaces the old `♪` text glyph, which rendered as
  a speck).
- Current row background: `rgba(213, 163, 66, 0.14)`.
- Meta text 10px `#5b695e`; duration in `<time>`.

### 7.7 Plates and cards

- `.collection-card`: paper fill, `3px 18px 3px 3px`, hard shadow `3px 4px 0`, image border
  painted with the collection's own pigment (`style={{ borderColor: item.color }}`).
- `.hero-card`: two columns `46% 54%`, radial terracotta wash + paper gradient, inset gold
  hairline, `box-shadow: 5px 6px 0 #cfd6c3, 0 0 0 1px rgba(213,163,66,.4) inset`.
- `.library-cover`: 196px mounted plate; the `BotanicalArt` there uses `fit="meet"` so the
  portrait plate letterboxes into olive matting instead of being cropped.
- `.editorial-strip`: quote block, full 1px border, `2px 16px 2px 16px`. **No left accent bar.**

### 7.8 Switches, bands, presets (equalizer)

- Header switch: `.switch` 37×21, `role="switch"`, `aria-checked`, `aria-label="Ecualizador activo"`.
- 10 bands, rotated `-90deg` range inputs (`accent-color: var(--gold-soft)`), each with
  `aria-label={`Banda de ${freq} hertz`}`; the printed frequency is `aria-hidden`.
- `.eq-board` 315px, `linear-gradient(155deg, var(--forest), var(--ink))`, gold border, hard
  shadow. Disabled state: `opacity: .42; pointer-events: none`.
- Presets: 2×2 grid, `min-height: 54px`, active = terracotta fill.

### 7.9 Empty state

Dashed plate (`1px dashed rgba(39,68,55,.3)`), centred `FlowerGlyph` 26px at `.75` opacity,
then italic Fraunces copy. Never a bare line of text.

### 7.10 Toast

`role="status"`, absolute, gold fill, ink text, `2px 10px 2px 10px`. Appears on share, no
animation beyond opacity/transform.

### 7.11 Gesture bar

Android affordance: 110×4px pill, `bottom: 4px`, `z-index: 20`, `pointer-events: none`.
`.light` variant (`rgba(255,250,240,.5)`) is used on dark overlay screens; the nav reserves 12px
of bottom padding so labels never collide with it.

## 8. Motion

| Name | Use | Spec |
|---|---|---|
| `reveal` | list items on screen mount | `0.35s ease both`, translateY + opacity |
| `rise` | full-screen overlays | `0.35s ease both` |
| `pulse-ring` | playing disc | `3.4s ease-in-out infinite`, scale 1.05 + 6deg |
| UI transitions | colour, border, transform | `0.15–0.3s` |

- Animate only `transform`, `opacity`, `color`, `background-color`, `box-shadow`.
- `:active` feedback on tactile controls: `scale(0.92–0.95)`.
- `@media (prefers-reduced-motion: reduce)` already kills `reveal`/`rise`/`pulse-ring`, collapses
  transitions to `0.01ms`, and removes the `:active` scales. Keep this block intact when adding
  animations; prefer state change (colour/fill) over movement.

## 9. Accessibility contract

**Contrast — validated pairs (WCAG AA 4.5:1 for text).**

| Foreground | Background | Ratio |
|---|---|---|
| `#173026` ink | `#f6efe0` paper | 12.33 |
| `#58614a` sage | `#f6efe0` paper | 5.69 |
| `#5a675c` meta | `#f6efe0` | 5.20 |
| `#5b695e` meta | `#f6efe0` | 5.06 |
| `#5d6a5f` nav idle | `#f6efe0` | 4.97 |
| `#56635a` | `#ebe1ca` | 4.85 |
| `#b8442c` terracotta | `#f6efe0` | 4.70 |
| `#23479c` cobalt | `#f6efe0` | 7.48 |
| `#e6c678` gold-soft | `#204235` | 6.72 |
| `#c2d1c5` | `#204235` | 6.99 |
| `#c6d5cb` lyric idle | `#102c25` / over the terracotta glow | 9.78 / 5.40 |
| `#fffaf0` lyric active | `#102c25` | 14.33 |

Rule: any new text colour must be checked against **its actual background**, including the
gradient/glow behind it. If a surface is translucent, test the worst-case stop.

**Semantics**

- Every control has an accessible name (visible text, `aria-label`, or a real `<label>`).
- State is exposed, not just painted: `aria-pressed` (like, player tabs), `aria-checked`
  (`role="switch"`), `aria-current="page"` (nav).
- Decorative nodes: `aria-hidden="true"` (status bar, seals, glyphs, progress fill, EQ scale,
  frequency labels, separators).
- Illustrations are `role="img"` + a descriptive Spanish `aria-label`.
- Scrubbable values are real `<input type="range">` with `aria-valuetext` in human units.

**Focus**

```css
:focus-visible { outline: 2px solid var(--cobalt); outline-offset: 2px; }
.full-player :focus-visible, .equalizer-screen :focus-visible, nav :focus-visible {
  outline-color: var(--gold-soft);
}
```

Never remove the outline without an equivalent. On dark surfaces the ring must be gold, on paper
cobalt.

**Targets** — minimum 44×44 CSS px for any tappable control (nav pills 48×30 inside a 65px bar,
alphabet buttons 44×44, player cells 44×44, header buttons ≥44px with padding).

## 10. Copy

- Language: **Spanish (Colombia)**, botanical register. "Lámina", "herbario", "ecosistema",
  "nota de herbario", "floración".
- Screen names are thematic, not generic: `Floración` (home), `Explorar` (search),
  `Colección` (library), `Ecualizador`.
- Labels are sentence case; kickers and badges are uppercase with tracking.
- No lorem ipsum, no English strings in UI, no emoji.
- Empty states explain the situation and never blame the user.

## 11. Code map

| File | Responsibility |
|---|---|
| `src/main.tsx` | React root, StrictMode, imports `index.css` |
| `src/App.tsx` | Everything: icons (`Icon`, `memo`), illustration (`BotanicalArt`, `FlowerGlyph`, `BotanicalMark`, all `memo`), data (`collections`, `tracks`, `frequencies`), state, all screens |
| `src/index.css` | Tokens, base layer, every component rule, media queries, reduced motion |
| `index.html` | `lang="es"`, viewport `viewport-fit=cover`, `theme-color`, font `<link>`s |
| `public/botanical/*.jpg` | Scene textures (`dosel-bosque`, `orquideas-acuarela`, `endemic-colombia`) |
| `docs/screens/*.png` | Reference captures (see §13) |

`App.tsx` structure: `Icon` (~line 18) → `BotanicalArt` (~105) → `FlowerGlyph` /
`BotanicalMark` → data (~412) → `App` (~500) → screens `Floración` / `Explorar` / `Colección`
→ mini player → nav → overlays `full-player` and `equalizer-screen` → gesture bar.

## 12. How to extend

**Add a collection** — append to `collections` with `name`, `detail`, `art` (`heliconia` |
`orchid` | `guayacan` | `lotus`), `color` (pigment for the card border), `plant`. Home shows the
first 3; Explorar lists all and derives the alphabet from the first letter.

**Add a track** — append to `tracks` with `title`, `artist`, `duration`, `seconds`, `collection`
(index into `collections`).

**Add an icon** — extend the `IconName` union and the `paths` record inside `Icon`. 24×24 grid,
1.75 stroke, round caps, no fills unless `filled` is requested. Any icon-only button needs an
`aria-label`.

**Add a screen** — new `screen` state value, render it as `position: absolute; inset: 35px 0 0`
with `animation: rise 0.35s ease both`, and make sure `light`/dark gesture-bar variants and the
focus-ring colour are set. Return path is always a `Volver`/`Cerrar` button with `aria-label`.

**Add a scrubber** — copy §7.4. Never a `<button>` that reads `clientX`.

## 13. Verification loop

```bash
npx vite build                     # must stay clean
node ~/.opencode/skills/impeccable/scripts/detect.mjs --json src/App.tsx src/index.css
```

Expected detector output: only `overused-font` for Fraunces. Any `side-tab` or
`layout-transition` finding is a regression.

Build note: `src/index.css` opens with `@source not "../DESIGN.md"` and `@source not "../docs/"`
right after the Tailwind import. Tailwind v4 scans the whole project and will happily generate
utilities out of this document's prose (`.uppercase`, `.italic`, `.grid`…), inflating the
bundle by ~3 kB. **Keep those directives** and add one whenever a new prose file enters the repo.

Expected bundle after a clean build: ~29 kB CSS / ~215 kB JS (uncompressed).

Visual check (dev server on `:8443`), screenshots at 430×900:

| Capture | What it proves |
|---|---|
| `docs/screens/01-floracion.png` | Hero plate, ecosystem row, editorial note, nav pill, mini player |
| `docs/screens/02-explorar.png` | Alphabet bush grid, disabled letters, result rows |
| `docs/screens/03-explorar-busqueda.png` | Query state, `Limpiar` action, single result |
| `docs/screens/04-explorar-vacio.png` | Empty state plate |
| `docs/screens/05-coleccion.png` | Mounted cover with `fit="meet"`, track list, current-track note icon |
| `docs/screens/06-reproductor-musica.png` | Disc, centred panel, seek, control row |
| `docs/screens/07-reproductor-letras.png` | Lyrics hierarchy (active vs idle) |
| `docs/screens/08-ecualizador.png` | Switch, band sliders, presets, header |
| `docs/screens/09-shell-desktop.png` | Desktop stage: centred phone, no page scrollbar |

Checklist before declaring UI work done:

1. No element wider than the phone; `document.scrollWidth === clientWidth` on desktop.
2. Every interactive element has a name and ≥44px hit area.
3. Text colour contrast measured against the real background.
4. Nothing animates `width`/`height`.
5. Reduced-motion path still communicates state.
6. Content bottom padding still clears the floating player.
7. `npx vite build` clean, detector shows no new anti-pattern.
