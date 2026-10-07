---
name: Facturador Agromanguera
description: Legal invoicing workbench that turns a validated bill (Factus/DIAN) into a stamped, audited, collectable ledger entry.
---

<!-- RESOLVED: inherits the Cotizador Agromanguera world ("Deployable Sheet", see ../cotizador/DESIGN.md), extended for Operate-density billing. Tokens confirmed from cotizador-frontend tokens.css; app shell from AppShell.tsx; document template from cotizador templates/documento.*. -->

# Design System: Facturador Agromanguera

## Overview

**Creative North Star: "The Validation Ledger"**

The facturador is the cotizador's operational back-room: the quote was the deployable sheet that persuaded the buyer; now the sale must be *stamped, validated, and logged*. Every invoice surface reads as a ledger entry in a DIAN filing system — CUFE, resolution, status, FAJ error codes, stock deduction, payment balance — inspection marks on a legal record, not dashboard widgets.

The visual world is **inherited from the cotizador's Deployable Sheet** (same identity system, same tokens), tuned to a different mode: **Operate**. Where the cotizador sells a document outward, the facturador is used daily, in volume (50–200 invoices/month), by sellers, admins, and a read-only accountant. Ergonomics follow the density and habits of Colombian POS/invoicing systems (Alegra POS / Siigo POS): quick entry, tabular density, visible state, clear status stamps. The blueprint identity makes the facturador unmistakably Agromanguera; the Operate discipline lets a seller work all day without friction.

**Key Characteristics:**
- Precise, audited, legal, dense — an instrument panel for sales.
- One validation ledger: every invoice is a row inside a state machine (pendiente → validada → rechazada → anulada), never a floating card.
- The warm lock accent is reserved for money, statuses, and decisive actions.
- CUFE, resolution, FAJ codes, NIT appear as mono annotations, reinforcing "this is a legal exhibit".

## Colors

The palette is the cotizador's palette, unchanged except for new functional states for the fiscal lifecycle. Physical scene: the accountant works on desktop in daylight; sellers use mobile in bright field light. Resolved tokens (light):

### Primary
- **Blueprint Ink** `#10243E`: Dominant text, sidebar, structural rules, table headers, the ledger's authoritative voice.

### Secondary
- **Lock Foil** `#C8A44D`: Warm accent for total bands, primary actions, active nav notch, validated/settled marks, the brand mark. Text on light uses **Foil Deep** `#7A5C18` (≥4.5:1 on `#F0E6C8` wash and sheet).

### Tertiary
- **Valley Blue** `#A9D6E5`: Pale annotation color for seams, dashed inspection borders, helper lines, pending states. Text uses **Valley Ink** `#33627A`.

### Neutral
- **Matte Sheet** `#F8F7F0`: Main field and app background; raised facet `#FDFCF7`, tucked facet `#EFEDE2`.
- **Scored Graphite** `#2F3133` strong text/rules; hairlines `#D6D3C6`; fold shadows `#B8B4A4`.

### Dark (app shell only; the sheet stays paper-light)
- **Night** `#0B1420` background, raised `#101C2B`, tucked `#08101A`; ink `#E8E5DA`, soft `#A8B4C2`, rules `#223246`/`#33465E`; foil `#DCB95E`; valley `#7FB2C9`.

### Functional state inks — extended for the fiscal lifecycle (state is color, color is state)
- `validada` = **foil** pair
- `pendiente` = **valley** pair (awaiting DIAN)
- `rechazada` = **denial stamp** `#B3402A` (wash `#F6E5E0`)
- `anulada` = **scored graphite** pair (voided, quiet)
- Settled payment (`ok`) `#1E6B4F` / `no` `#B3402A` / `warn` `#8A6A1E` — inherited from the cotizador
- **CUFE** and **FAJ error codes** always render in IBM Plex Mono; never re-colored by state

**Named Rules**

**The Lock Color Rule (inherited).** The warm accent appears only where the document asks the user to decide, trust, or act: total, primary CTA, validated status, active nav, settled state.

**The Status Stamp Rule.** Invoice state (pendiente/validada/rechazada/anulada) is an instrument stamp, not a decorative badge: mono, capped tracking, rotated 2° when it sits on a document, tonal wash. Status never appears as an unlabeled spotlight.

**The Ledger Row Rule.** In lists, an invoice is one row in a dense report. Columns: número/prefijo, cliente, fecha, total COP, método de pago, due/plazo, estado stamp, acciones. No card-based invoice list — the list is the ledger.

## Typography

Resolved faces: **Archivo** (display and document headings — the engineering voice), **Inter** (app-shell body, Operate workhorse), **IBM Plex Mono** (labels, folios, CUFE, resolution numbers, tags, metadata). Sizes stay operative-dense: shell body 15px/1.55; data tables 13.5px with 1.4 line height; document 14px; labels 9.5–11px mono tracked +0.14–0.24em; display 26px page titles, 22px section leads, the total 32px.

### Hierarchy
- **Display** (Archivo 700): the invoice total band, company identity, page titles (26px).
- **Headline** (Archivo 650): section leads — client, items, terms, fiscal data.
- **Title** (Archivo 600 15px): facet/table group headings in the shell.
- **Body** (Inter 400/500): client details, descriptions, notes.
- **Label** (IBM Plex Mono 600, tracked caps): folios (`FEV-0001`, `COT-0001`), NIT, resolution, CUFE excerpt, tax labels, status stamps, Toggle annotations.

**Named Rules**

**The Legal Exhibit Rule.** Every line that must be legally inspectable (CUFE, resolution, NIT, status, invoice number) appears in the same mono instrument voice — aligned, exact, never decorative.

## Layout

### App shell (inherited, re-tuned for density — from `cotizador-frontend AppShell.tsx`)
- Fixed 240px Blueprint Ink sidebar with foil active-notch nav, mono `.stamp` brand labels.
- Header: left `stamp` label (`Facturador AGM · es-CO`), right the primary foil CTA (e.g. "Nueva factura").
- Main sheet max-width 1200px with `fold-in` entry.
- Role footer already renders `Administrador` / `Contador` / `Vendedor` from auth — the facturador adds the `Contador` role to that footer.

### FacturasPage (the primary surface, Operate)
- **Ledger list** not cards: full-width table (120ch+), sticky header repeated on scroll/sort, hover on rows, right-aligned COP in tabular numbers, `stamp` status column, number/pref in mono, actions (PDF/XML/email, sync, delete if pendiente) inline.
- **Filter strip** above the ledger, mono-labelled: estado, rango de fecha, rango de facturación, cliente, método — plus export/CSV actions aligned right.
- **Two-pane work area** for emission: left facet items + client/fiscal; right facet totals (subtotal, benefits, IVA, total band) + status panel. Inline editing, never a multi-step wizard.
- **Empty states** reuse the folded-facet motif: dashed valley seam, `stamp` label, a primary action to create the first invoice or to configure fiscal data (first-run is the most important coach).
- **Fiscal configuration becomes a checkbox mental model**: every button/role is guided by the 2-col "if fiscal missing → 409 explaining what's missing" hint.

### Docs & PDF (inherited document system from `templates/documento.html`)
- Full-width 900px, 56px padding sheet: cover (brand + folio + status stamp) → meta facet (emisor · valley seam · receptor · fiscal data) → item ledger (7 columns) → summary lines → **total band** (foil, clipped corner) → footer mono.
- The public `/f/:token` is the same identity open to the client: stamps, QR/CUFE, resolution visible. PDF via Playwright/fold for the quote; invoice PDF legally comes from Factus, but the hybrid talonario+DIAN view is ours (decision from test-definicion-mvp).

### Responsive
- Sidebar collapses to a Radix Dialog drawer (280px, as the cotizador).
- The ledger table keeps essential columns (número, cliente, total, estado) on mobile; filters collapse to native select; the footer remains accessible.

## Elevation & Depth

Depth comes from folds, scores, overlaps, and tonal facets, not generic floating shadows. The sheet carries one fold shadow (`0 1px 0 0 ink/4%, 0 24px 48px -24px ink/35%` on the workbench; none in print). App facets use `--shadow-fold` (hairline + 6px soft). Buttons press with a 2px solid under-shadow (`0 2px 0 0`), never blur-only.

**The Fold, Not Float Rule (inherited).** Surfaces do not hover like cards — they seat, align, and lock into place.

## Shapes

Angular and precise. Facet clip (`clip-path: polygon(0 0, 100% 0, 100% calc(100% - 14px), calc(100% - 14px) 100%, 0 100%)`) on brand mark, metric facets, empty states, and the total band. Status stamps: rotated mono rubber stamp (2°), dashed inner border on the invoice plate, dashed valley seams between facets. Corners 2–4px on controls; nothing pill-shaped.

## Motion

One authored moment: `fold-in` (opacity + 6px rise + 2° rotateX, 0.5s expo) on main content entry. Micro-transitions (150ms) on hover/press. Status sync animates as a `stamp` settle, not a spinner. The drawer slides from the left. No parallax, no scattered entrances — this is a work surface.

## Do's and Don'ts

### Do:
- **Do** keep the cotizador identity: same tokens/fonts/sidebar/stamps — the facturador looks like Agromanguera, and fits alongside the cotizador.
- **Do** use the Operate density of Colombian POS systems (Alegra/Siigo) in lists, filters, and data entry, where the cotizador was a persuasive document surface.
- **Do** show the fiscal lifecycle explicitly: `pendiente / validada / rechazada / anulada` always visible as an instrument stamp.
- **Do** put CUFE, resolution, reference_code, and FAJ errors in mono, exact, aligned.
- **Do** make the first run a coach: if fiscal config is missing, the app says "configure the fiscal identity" with one action, never empty dead ends.
- **Do** reserve the foil for money, status, primary actions — and invoice totals (the DIAN-summed value).

### Don't:
- **Don't** reinvent the cotizador's look — the family (world) is the point; splitting into a "white dashboard + cards" identity loses continuity.
- **Don't** let Operate density erase the fold/brand voice: folios, mono annotations, and the total band must stay prominent even in the densest table.
- **Don't** create fake CUFE, resolution, or FAJ codes in demo content — synthetic only, clearly labelled.
- **Don't** make the DIAN legal fields decorative.
- **Don't** use spinners mid-content or generic "nothing here" empty states — coach with a source action or the fold/empty motif.
- **Don't** use modals as a first thought: inline editing first (import for invoices); modals are reserved for factual tasks (column add, destructive confirms).