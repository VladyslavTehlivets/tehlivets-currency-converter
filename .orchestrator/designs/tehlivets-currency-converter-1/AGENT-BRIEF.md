# Currency Converter · Agent design brief

This brief describes the experience you must create.

## Your reader

A person who needs to follow how a dated conversion produces its result.

## The main action

Convert an amount and understand each selected rate.

## The visual direction

Forest green on cream and silver. The amount and the conversion path carry the design.

Use the shared silver and cream materials, double bezels, nested action controls, generous squircles, and spring motion.
Keep the product’s own accent and working layout.
Keep technical handoff notes outside product screens.

## What the experience must do

- Provide an amount, source currency, target currency, and conversion date.
- Keep dated rates editable and directional. A rate does not automatically supply its reverse.
- Select the available directional rate nearest to the requested date.
- Pass a non-base currency pair through PLN. Show both steps and their selected rates.
- Keep decimal multiplication exact. Do not replace precise amounts with floating-point approximations.
- Stop when a required rate is missing. Tell you which next action makes a result possible.
- Keep example rates clearly separate from current market quotes. Do not invent a rate provider.
- Treat this as a proposed web worksheet for the existing conversion library. Do not claim a verified web deployment.

## Quality requirements

- Keep the layouts usable on a phone, tablet, and desktop.
- Keep the main action clear on every screen.
- Label inputs. Explain errors beside the input that needs attention.
- Support keyboard access, visible focus, and reduced motion.
- Keep loading, empty, error, and confirmation states distinct.
- Do not invent testimonials, customer counts, prices, integrations, or guarantees.
- Use the connected screens and the screen guide as the design reference.

## Approved upload refinement

Adopt the refined upload controls wherever this product already accepts files.
Use the product’s color, nested surfaces, file preview, remove action, and clear states.
Use real transfer progress, error recovery, and server validation in the application.
Keep the approved typography. Defer further font work.
Read UPLOAD-REFINEMENT.md with this brief.
