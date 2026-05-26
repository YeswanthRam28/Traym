---
name: Kinetic Editorial
colors:
  surface: '#121409'
  surface-dim: '#121409'
  surface-bright: '#383a2c'
  surface-container-lowest: '#0d0f05'
  surface-container-low: '#1b1d10'
  surface-container: '#1f2114'
  surface-container-high: '#292b1e'
  surface-container-highest: '#343628'
  on-surface: '#e3e4d0'
  on-surface-variant: '#c6c9ad'
  inverse-surface: '#e3e4d0'
  inverse-on-surface: '#303224'
  outline: '#909379'
  outline-variant: '#454933'
  surface-tint: '#b6d300'
  primary: '#efffa1'
  on-primary: '#2c3400'
  primary-container: '#c8e800'
  on-primary-container: '#576600'
  inverse-primary: '#566500'
  secondary: '#c7c7be'
  on-secondary: '#30312b'
  secondary-container: '#494a43'
  on-secondary-container: '#b9b9b0'
  tertiary: '#eff8ff'
  on-tertiary: '#003549'
  tertiary-container: '#afe1ff'
  on-tertiary-container: '#32657f'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#d0f118'
  primary-fixed-dim: '#b6d300'
  on-primary-fixed: '#181e00'
  on-primary-fixed-variant: '#404c00'
  secondary-fixed: '#e4e3da'
  secondary-fixed-dim: '#c7c7be'
  on-secondary-fixed: '#1b1c17'
  on-secondary-fixed-variant: '#464741'
  tertiary-fixed: '#c3e8ff'
  tertiary-fixed-dim: '#9bcdea'
  on-tertiary-fixed: '#001e2c'
  on-tertiary-fixed-variant: '#124c65'
  background: '#121409'
  on-background: '#e3e4d0'
  surface-variant: '#343628'
typography:
  display-xl:
    fontFamily: anybody
    fontSize: 80px
    fontWeight: '900'
    lineHeight: 100%
    letterSpacing: -0.02em
  display-lg:
    fontFamily: anybody
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 100%
    letterSpacing: -0.01em
  headline-md:
    fontFamily: anybody
    fontSize: 32px
    fontWeight: '800'
    lineHeight: 110%
  body-lg:
    fontFamily: hankenGrotesk
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 150%
  body-md:
    fontFamily: hankenGrotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 150%
  stat-lg:
    fontFamily: jetbrainsMono
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 100%
    letterSpacing: -0.05em
  stat-sm:
    fontFamily: jetbrainsMono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 100%
  label-caps:
    fontFamily: hankenGrotesk
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 100%
    letterSpacing: 0.1em
spacing:
  base: 4px
  container-margin: 24px
  gutter: 16px
  section-gap: 48px
---

## Brand & Style

This design system is built on the raw energy of performance sports and the uncompromising precision of high-end editorial design. It targets the "data-obsessed athlete" who demands efficiency and clarity above all else. The visual language is aggressive, urgent, and technical.

The aesthetic blends **Brutalism** with **High-Contrast Editorial** layouts. It eschews modern UI conventions like soft shadows or rounded corners in favor of a "machine-shop" precision. The UI should evoke the feeling of a technical manual or a premium sports broadsheet—utilitarian, loud, and authoritative.

## Colors

The palette is anchored by a high-visibility Acid Yellow, used as a functional disruptor against a monolithic near-black background. 

- **Primary (Acid Yellow):** Used for background fills on high-priority sections, CTA buttons, and critical status highlights.
- **Background (Near-Black):** The canvas for all main screens, ensuring maximum contrast for the yellow and white elements.
- **Secondary Text (Off-White):** Used for primary readability to reduce eye strain compared to pure white, maintaining an editorial feel.
- **Surface (Dark Dim Gray):** Provides subtle separation for cards or secondary modules.
- **Muted (Gray):** Reserved for hairline borders, tertiary metadata, and inactive states.

## Typography

The typography strategy uses three distinct voices to create a clear information hierarchy:

1.  **The Voice of Authority (Display):** Uses **Anybody** (extra-wide, heavy weights). Set in ALL CAPS with tight leading. This is for hero numbers, workout titles, and section headers.
2.  **The Voice of Utility (Body):** Uses **Hanken Grotesk**. A clean, neutral grotesque for descriptive text, exercise instructions, and settings.
3.  **The Voice of Data (Accent):** Uses **JetBrains Mono**. This monospaced font provides a technical, precise feel for all numerical data, including set counts, weights, timers, and timestamps.

## Layout & Spacing

This design system utilizes a **fixed-column grid** (12 columns for desktop, 4 columns for mobile) with generous outer margins to create an "airy but aggressive" editorial look. 

- **Alignment:** All elements must align strictly to the grid. Use hard-edged containers to define space rather than padding alone.
- **Rhythm:** Spacing follows a strict 4px baseline.
- **Margins:** Screens should utilize a minimum 24px margin on mobile to push content toward the center, mimicking a magazine layout.
- **Negative Space:** Use intentionally large gaps (48px+) between major sections to emphasize the starkness of the high-contrast color palette.

## Elevation & Depth

Depth is conveyed through **structural layering** rather than lighting effects. 

- **Zero Shadows:** No box-shadows are permitted.
- **Hairline Borders:** Use 0.5px or 1px borders in `#4A4A40` to define the edges of cards or sections against the background.
- **Tonal Stepping:** Use the `#1C1C18` surface color to lift secondary content modules. 
- **Stark Overlays:** High-priority modals or overlays should use the Acid Yellow (`#C8E800`) background with black text to immediately claim the highest level of hierarchy.

## Shapes

The design system is defined by **sharp geometric precision**. 

- **Corners:** All primary containers, cards, and input fields must have a 0px border radius.
- **Exceptions:** A maximum 4px radius is permitted on primary action buttons solely to provide a subtle affordance of "interactivity" vs "structure," though 0px is preferred where possible.
- **Visual Accents:** Incorporate thin diagonal lines and crosshair overlays (0.5px Acid Yellow) over dark photography to reinforce the technical/AI tracking theme.

## Components

### Buttons
- **Primary:** Acid Yellow background, black ALL CAPS bold typography. 0-4px corner radius.
- **Secondary:** Transparent background, 1px Off-white border, Off-white text.
- **Ghost:** Monospaced text with a right-pointing arrow `->`.

### Cards
- **Stat Card:** Deep gray (`#1C1C18`) background, 0.5px muted gray border. Large monospaced numbers.
- **Exercise Card:** Full-bleed dark imagery with a 20% black tint overlay and Acid Yellow crosshairs in corners.

### Inputs & UI Elements
- **Input Fields:** Bottom-border only (2px Off-white). Monospaced input text.
- **Progress Bars:** Flat blocks. The "filled" portion is Acid Yellow; the "unfilled" portion is the muted gray border color. No rounded caps.
- **Icons:** Flat, single-stroke line icons. Stroke weight should be 1.5px to match the technicality of the monospaced font.
- **Crosshairs:** Decorative 0.5px lines that intersect at focal points on images or at the corners of high-data modules.