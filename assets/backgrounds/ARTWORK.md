# Quiz Arena background artwork

Generation mode: built-in `image_gen.imagegen` tool (no CLI/API fallback).

These are original raster artworks generated for this project. They contain no UI text; Vietnamese headings and controls are rendered by Swing.

## Login background

- File: `auth-arena-v1.png`
- Intended composition: left-weighted trophy/quiz orbit; calm upper-left space for headline; quiet right half for the login card.
- Final prompt:

```text
Use case: stylized-concept. Asset type: full-bleed background illustration for a polished desktop quiz competition game's login screen. Create an original premium stylized 3D quiz arena, wide 16:9 landscape composition. Deep midnight navy atmosphere with vivid cobalt, cyan and violet rim lighting, restrained warm golden highlights. Main sculptural glossy gold-and-glass trophy on a luminous circular plinth, surrounded by a few floating abstract rounded quiz-answer tiles and orbital light arcs suggesting knowledge and friendly competition, on the LEFT-CENTER and LOWER-LEFT around x=25% of image width, y=65% of image height. The upper-left quarter must be calm dark negative space for a Vietnamese interface headline rendered later in code. The entire RIGHT 45% must be very subdued low-detail dark navy negative space, because an opaque white login form card will cover it. Rich cinematic lighting, soft bloom, tasteful depth of field, crisp clean high-end playful game art, beautiful material reflections, professional composition and legible silhouette, no visual clutter. All important objects stay in the left 50%, no people or characters. No text, no letters, no digits, no logos, no brand marks, no watermarks. Generate an actual raster artwork asset, not an image of an interface or a mockup.
```

## Lobby hero background

- File: `lobby-arena-v1.png`
- Intended composition: right-weighted trophy/quiz stage; calm left half for hero text and controls.
- Final prompt:

```text
Use case: stylized-concept. Asset type: cinematic background image for the hero banner of an original premium desktop quiz competition game's lobby. Create a wide 16:9 landscape illustration in the same polished stylized 3D art direction as a modern knowledge-competition arena: deep midnight navy base, cobalt blue and cyan light arcs, a few violet accents and tasteful warm gold highlights. A luminous elegant golden trophy and a cluster of floating glossy abstract rounded quiz answer tiles rise over a circular futuristic stage, concentrated in the RIGHT HALF around x=77% of image width, y=55% of image height. The LEFT 55% must be clean, calm low-detail deep navy gradient negative space reserved for interface headline, subtitle and a primary action button, rendered later in code. Keep main trophy silhouette in the middle-right vertical zone so it remains visible when center-cropped into a wide shallow UI banner. Cinematic premium game lobby mood, soft bloom and reflections, rich depth, restrained particles and tasteful abstract orbit trails, clean uncluttered composition. Beautiful high-end original raster artwork, no interface mockup. No people, no characters, no text, no letters, no digits, no logos, no brand marks, no watermark.
```

Both outputs were visually inspected and copied unchanged into this project from the built-in tool's generated-images directory. The application handles cover-cropping and overlays in code.

