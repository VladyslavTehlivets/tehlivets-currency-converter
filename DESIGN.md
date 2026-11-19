---
name: Currency Converter Soft Family
colors:
  primary: '#207553'
  on-primary: '#ffffff'
  primary-container: '#dfece2'
  on-primary-container: '#1c6247'
  surface: '#f4f1eb'
  on-surface: '#222528'
  surface-container: '#faf9f5'
  on-surface-variant: '#595e60'
  error: '#963c34'
  error-container: '#f5e2dd'
typography:
  display-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 76px
    fontWeight: '600'
    lineHeight: 81px
    letterSpacing: -0.045em
  headline-lg:
    fontFamily: Bricolage Grotesque
    fontSize: 42px
    fontWeight: '600'
    lineHeight: 45px
    letterSpacing: -0.045em
  body-md:
    fontFamily: Instrument Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 25px
  label-lg:
    fontFamily: Instrument Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
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

# Currency Converter · Soft Family · Revision 6

You can identify this product through its own color and its own working layout.
You can identify the family through warm cream, silver bezels, soft light, and tactile controls.

## Visual direction

- Use a silver and cream canvas with a quiet ambient color mesh.
- Place each major work surface inside two concentric soft bezels.
- Keep a white inner highlight and a wide, lightly tinted shadow.
- Use generous squircle radii. Keep inner and outer curves concentric.
- Give the main action a nested circular trailing control.
- Use Bricolage Grotesque for headings and Instrument Sans for reading.
- Keep dashboards in sans-serif type. Use tabular numbers for amounts and timers.
- Use the product accent for intent, selection, and progress.
- Use restrained spring motion. Stop movement when you request reduced motion.
- Keep phone layouts in one column. Keep every control reachable without horizontal page scrolling.

## Product palette

| Role | Color |
| --- | --- |
| Canvas | #f4f1eb |
| Work surface | #faf9f5 |
| Text | #222528 |
| Secondary text | #595e60 |
| Product accent | #207553 |
| Accent text | #ffffff |
| Quiet accent | #dfece2 |
| Strong accent | #1c6247 |

## Content and behavior

Use the screen copy as the proposed product copy.
Keep the product purpose visible before showing features.
Give each screen one clear main action.
Keep errors beside the input that needs attention.
Keep empty states useful. Tell you how to begin.
Keep loading states shaped like the content they replace.
Keep confirmations clear about the completed action.
Keep private links and recipient surprises private.
Do not add testimonials, customer counts, prices, or promises without evidence.

## Reference

This revision follows Leonxlnx Soft Skill and Taste Skill sections 4–8.
It also uses the local Taste, UI, Stitch, marketing, and SaaS branding guidance.
The Stitch exploration contributes the nested workspace materials.
The original personal prototype remains the family reference.
The final dashboards exclude Stitch’s unverified soundscape, account plan, and serif type.

## File uploads

Use the shared upload surface with a silver outer bezel and a quiet product-tinted inner surface.
Use the existing inner radius, card spacing, accent, and focus tokens.
Place a designed file action inside the surface. Keep the native picker out of the visual layout.
Show the supported types and size limit before selection.
Show the filename, size, preview, remove action, and upload state after selection.
Keep errors next to the field. Keep a retry action for interrupted transfers.
Keep a keyboard path through the chooser and the remove action.
Use actual progress from the service in production.
Keep server validation for types, size, and access.
Do not add upload features to products that do not support them.
Keep the approved typography unchanged.

## Provider door group

Added by tehlivets-currency-converter-4 for the sign-in door (portfolio ADR-050, design `project-orchestrator-8`).

Reason: Google fixes the colours and font of its sign-in button. Google forbids any change to its mark. The product palette cannot paint that button. These values are therefore one named group. Only the provider door primitive (`web/src/components/ui/provider-door.tsx`) can use them. No other screen element can use them.

| Token | Value | Source |
| --- | --- | --- |
| `provider-door` | #FFFFFF | Google light fill |
| `provider-door-edge` | #747775 | Google light stroke, 1px inside |
| `provider-door-ink` | #1F1F1F | Google light font colour |
| `provider-door-pressed` | #F2F2F2 | Google neutral fill for hover and press |
| `font-provider-door` | "Google Sans" Medium 500, then `font-body` | Google button font, SIL OFL |

Rules:

- Write the values exactly as Google publishes them. A reviewer compares them character by character.
- The product has a light theme only. The dark Google values do not apply.
- The Google G mark is a file in `web/public/brand/`. Never recolour, redraw, or stretch it.
- Self-host one Latin-subset Google Sans Medium woff2. Preload it on `/sign-in` only.
- Measured contrast: ink on fill 16.5:1, ink on pressed fill 14.7:1, edge on the work surface 4.3:1 and on the canvas 4.0:1.
