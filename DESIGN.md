---
name: Currency Converter Soft Family R6
colors:
  primary: '#207553'
  on-primary: '#ffffff'
  primary-container: '#dfece2'
  on-primary-container: '#1c6247'
  surface: '#f4f1eb'
  surface-container: '#faf9f5'
  on-surface: '#222528'
  on-surface-variant: '#595e60'
  error: '#963c34'
  error-container: '#f5e2dd'
  field: '#eeefea'
  placeholder: '#e5e8df'
  rule: '#d4d8d077'
  glass: '#ffffffa8'
  menu: '#faf9f3fa'
typography:
  display: Bricolage Grotesque
  body: Instrument Sans
rounded:
  sm: 17px
  md: 27px
  lg: 32px
  xl: 39px
  full: 9999px
spacing:
  micro: 4px
  bezel: 7px
  control: 16px
  card: 30px
  section: 80px
---

# Currency Converter design reference

## Overview

You convert an amount and inspect each selected directional rate.
The approved source remains `currency-converter-666f/soft/` in OpenDesign.
Forest green identifies this product within the cream and silver family.
This file defines the shared tokens for design, implementation, and review.
The item design document defines screens and states.

## Colors

Use the named colors above through semantic CSS properties or Tailwind tokens.
Use `primary` for the main action and `on-primary-container` for focus and quiet actions.
Use `error` with explicit error text and `error-container` for blocking notices.
The additional field, placeholder, rule, glass, and menu tokens preserve existing approved materials.

| Material token | Value | Purpose |
| --- | --- | --- |
| `gradient-bezel` | `linear-gradient(145deg, #fff9, #dadcd680)` | Silver outer surface |
| `gradient-core` | `linear-gradient(145deg, #ffffffec, #faf9f5df)` | Cream inner surface |
| `gradient-mesh` | Primary-container at 83% 8%, transparent at 48%. Silver `#e4e6e4a0` at 12% 74%, transparent at 54%. | Approved ambient canvas |
| `gradient-halo` | Primary-container ellipse, transparent at 68% | Conversion form backing |
| `glyph-fill` | `#ffffff57` | Nested CTA circle |
| `quiet-fill` | `#fff9` | Secondary CTA surface |

## Typography

Keep the approved self-hosted fonts and their licenses.
Use tabular numerals for amounts, rates, and dates.
Never round a decimal to fit its container.

| Token | Desktop | Phone |
| --- | --- | --- |
| `display-lg` | `clamp(38px, 5.2vw, 76px)` | 44px |
| `page-title` | `clamp(34px, 4vw, 52px)` | 37px |
| `headline-lg` | `clamp(27px, 3vw, 42px)` | 30px |
| `title` | 22px | 22px |
| `metric` | 54px | 42px |
| `brand` | 21px | 19px |
| `lead` | 18px | 17px |
| `body-md` | 16px / 25px | 16px / 25px |
| `unit`, `label-lg`, `caption`, `fine`, `eyebrow` | 15px, 14px, 13px, 12px, 11px | Same |

Heading weight is 600, line height is 1.07, and tracking is -0.045em.
Title tracking is -0.025em. Metric tracking is -0.05em and line height is 1.1.
Brand tracking is -0.04em. Eyebrow tracking is 0.13em.
Body weight is 400. Labels use weight 600 and line height 20px.

## Layout

Use the existing Tailwind spacing scale with a 4px base unit and half steps.
Use semantic spacing above for surface and section composition.
Card padding becomes 24px below 1024px. Bezel padding becomes 6px below 768px.
The product shell has a 1160px maximum width and desktop padding of 24px 44px 64px.
Phone padding is 16px 18px 35px. Tablet horizontal padding is 28px.
The desktop hero uses columns of 1.02fr and 1fr, with a 60px gap.
The desktop receipt uses columns of 1.45fr and 1fr.
The hero gap stays 60px on tablets and becomes 44px on phones.
Use one column below 768px. Use the menu below 1101px, as the approved prototype does.
Keep the navigation position constant across routes.
Use `measure-reading: 62ch` for prose and `43ch` for hero copy.
Use `target-min: 44px`, `input-min: 49px`, `cta-min: 50px`, and `glyph-size: 38px`.
These target tokens extend the reference to make keyboard and touch requirements explicit.
Use `focus-width: 3px`, `focus-offset: 4px`, and `input-focus: 2px`.
Use `safe-area-inset-*` and `viewport-min: 100dvh` for device boundaries.
Use `overflow-wrap: anywhere` for exact amounts and long rate strings.

## Elevation and depth

| Token | Value |
| --- | --- |
| `shadow-bezel` | `inset 0 1px 0 #fffe, 0 22px 62px -30px #343b3029` |
| `shadow-core` | `inset 0 1px 0 #fff, inset 0 -1px 0 #22252806` |
| `shadow-island` | `inset 0 1px 0 #fff, 0 8px 32px #343b3010` |
| `shadow-cta` | `inset 0 1px 1px #fff9, 0 4px 12px #30362b0c` |
| `shadow-cta-quiet` | `inset 0 1px 0 #fff, 0 2px 10px #27362e08` |
| `shadow-glyph` | `inset 0 1px 1px #fff7` |
| `shadow-field` | `inset 0 2px 4px #30362b07, inset 0 -1px 0 #fff` |
| `shadow-highlight` | `inset 0 1px 0 #fff` |
| `shadow-menu` | `0 20px 40px #27362e1a` |

## Shapes

Use outer radius 39px and inner radius 32px for concentric bezels.
Use phone radii 33px and 27px. Use 17px inputs and full-radius CTAs.
Existing detail radii remain named: `xs: 10px`, `note: 18px`, `node: 20px`, `menu: 25px`, and `orb: 28px`.
Use `ease-spring: cubic-bezier(0.22, 1, 0.36, 1)`.
Use entry duration 850ms, control duration 450ms, and glyph duration 500ms.
Entry moves 20px. Hover glyph movement is 2px. Press scale is 0.97.
Disable transitions, entry movement, skeleton pulsing, and smooth scrolling for reduced motion.

## Components

Reuse `Bezel`, `PageHead`, `SectionHead`, `Notice`, and the existing shell.
Reuse `Button`, `NestedCtaLink`, `Input`, `NativeSelect`, `Label`, and `Skeleton` from the existing component directory.
Use `commit` controls for form submission and `reachesAThumb` for smaller visible controls.
Keep nested CTA glyphs decorative. The complete CTA is one accessible control.
Keep forms labeled and connect each error through `aria-describedby` and `aria-invalid`.
Use named primitive styles for every button and button-like link.

## Do and do not

Keep the approved typography, bezel materials, and restrained spring motion.
Keep exact amounts and requested dates visible with the selected rates.
Do not add uploads, accounts, live quotes, invented deployments, or another color scheme.
Do not treat prototype sample data or local storage as a production service.
