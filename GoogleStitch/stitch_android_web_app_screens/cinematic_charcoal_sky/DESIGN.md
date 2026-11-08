---
name: Cinematic Charcoal Sky
colors:
  surface: '#121316'
  surface-dim: '#121316'
  surface-bright: '#38393c'
  surface-container-lowest: '#0d0e11'
  surface-container-low: '#16181e'
  surface-container: '#1a1c22'
  surface-container-high: '#252830'
  surface-container-highest: '#323742'
  on-surface: '#e3e2e6'
  on-surface-variant: '#bdc8d1'
  inverse-surface: '#e3e2e6'
  inverse-on-surface: '#2f3034'
  outline: '#87929a'
  outline-variant: '#3e484f'
  surface-tint: '#7bd0ff'
  primary: '#8ed5ff'
  on-primary: '#00354a'
  primary-container: '#38bdf8'
  on-primary-container: '#004965'
  inverse-primary: '#00668a'
  secondary: '#ffb3b6'
  on-secondary: '#68001a'
  secondary-container: '#cc003c'
  on-secondary-container: '#ffdcdc'
  tertiary: '#83d7ff'
  on-tertiary: '#003547'
  tertiary-container: '#64bbe3'
  on-tertiary-container: '#004961'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#c4e7ff'
  primary-fixed-dim: '#7bd0ff'
  on-primary-fixed: '#001e2c'
  on-primary-fixed-variant: '#004c69'
  secondary-fixed: '#ffdada'
  secondary-fixed-dim: '#ffb3b6'
  on-secondary-fixed: '#40000c'
  on-secondary-fixed-variant: '#920028'
  tertiary-fixed: '#c0e8ff'
  tertiary-fixed-dim: '#7bd1fa'
  on-tertiary-fixed: '#001e2b'
  on-tertiary-fixed-variant: '#004d66'
  background: '#121316'
  on-background: '#e3e2e6'
  surface-variant: '#343538'
  surface-base: '#121316'
  text-primary: '#f3f4f6'
  text-secondary: '#9ca3af'
  text-tertiary: '#6b7280'
  border-subtle: '#2a2e39'
  status-watching: '#38bdf8'
  status-completed: '#34d399'
  status-pending: '#9ca3af'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 36px
    letterSpacing: -0.015em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: -0.01em
  title-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '600'
    lineHeight: 24px
  title-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Inter
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.04em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  gutter-compact: 0.75rem
  margin: 1rem
  margin-tablet: 1.5rem
  margin-desktop: 2rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
---

## Brand & Style

This design system defines an immersive, native Android (Material 3 / Material You adapted) dark-mode experience tailored for cataloging, reviewing, and discovering films and television series.

### Visual Personality & Mood
- **Atmospheric & Immersive:** Dark cinema auditorium ambience translated into UI surfaces. Interfaces gracefully recede, spotlighting film key art, stills, and episodic backdrops without glare.
- **Harmonious Luminescence:** The transition from warm amber to soft light blue (sky/soft cyan) introduces a calm, ethereal luminescence that complements rich dark charcoal without harsh, abrasive contrast.
- **Editorial Precision:** Strict hierarchical typography, precise badges, and high-visibility status indicators keep complex media metadata structured, legible, and glanceable.

### Target Audience
Cinephiles, series binge-watchers, and collectors seeking a fast, offline-first local tracking tool that feels deeply integrated into modern Android hardware, avoiding clutter and bright glare during evening viewing sessions.

## Colors

The palette leverages a stepped tonal architecture built specifically for dark display conditions, using deep charcoals rather than harsh pure blacks to minimize eye fatigue and eliminate black smear on OLED panels.

### Surface Tones
- **`surface-base` (`#121316`):** Canvas root, backdrop underlays, and system window backgrounds.
- **`surface-container-low` (`#16181E`):** Bottom sheets, secondary structural containers, and search bars.
- **`surface-container` (`#1A1C22`):** Primary card surfaces, list row backgrounds, and dialog backgrounds.
- **`surface-container-high` (`#252830`):** Floating navigation bars, active chips, hovered states, and inset poster wells.
- **`surface-container-highest` (`#323742`):** Unselected state layers, dividers, and disabled elements.

### Brand & Interactive Accents
- **Primary Accent (`#38BDF8`):** Soft sky blue providing non-jarring contrast against deep charcoal. Used for primary calls-to-action, rating star highlights, active tab states, and progress indicators.
- **Secondary Accent (`#E11D48`):** Deep cinema crimson reserved for favorites, critical watch toggles, and destructive alerts.
- **Tertiary Accent (`#7DD3FC`):** Soft light cyan highlighting secondary active badges, metadata chips, and subtle focus halos.

### Text & Contrast
- **`text-primary` (`#F3F4F6`):** Primary movie and episode titles, section headers, and high-emphasis data.
- **`text-secondary` (`#9CA3AF`):** Release years, runtime, directors, genres, and unselected tab labels.
- **`text-tertiary` (`#6B7280`):** Metadata captions, timestamps, and input placeholders.
- **`border-subtle` (`#2A2E39`):** Ghost borders for card elevation separation.

## Typography

The typography leverages `Inter` for exceptional legibility across varying display densities, dense media statistics, and condensed poster badge overlays. 

- **Display & Headlines:** Used for hero titles, seasonal arc summaries, and detail screen backdrops. High weight (700–800) anchors user focus even when placed alongside saturated poster art.
- **Titles:** Applied to item titles in grid layouts, drawer section heads, and app bar headers.
- **Body:** Calibrated line heights support sustained reading of film synopses and user reviews.
- **Labels:** Strictly uppercase for tags and compact meta chips (`label-sm`, tracking `0.04em`) to establish visual hierarchy against standard mixed-case body copy.

## Layout & Spacing

The layout is built upon an 8-point base grid system with 4-point sub-steps for dense metadata arrangements.

### Grid & Form Factors
- **Mobile (Width &lt; 600dp):** Fluid container structure utilizing a standard 4-column layout. Posters use a 2-column or 3-column asymmetric vertical poster grid (`2:3` aspect ratio). Screen horizontal margins are fixed at `1rem` (`16dp`).
- **Tablet / Foldables (600dp - 840dp):** Expands to an 8-column layout with `1.5rem` margins. Adapts into a side-navigation rail, presenting poster catalogs in 4 to 5 columns.
- **Desktop / Large Displays (&gt; 840dp):** 12-column layout with a fixed content boundary maxing out at `1280dp` centered, with a persistent expanded side drawer.

### Rhythmic Rules
- **Component Padding:** Standard padding for internal card components is `space-md` (`16dp`); dense rows and badges use `space-xs` (`4dp`) vertically and `space-sm` (`8dp`) horizontally.
- **List Separations:** Vertical gap between feed sections is `space-lg` (`24dp`); gaps between internal section elements are `space-sm` (`8dp`).

## Elevation & Depth

To maintain visual cohesion across dark displays, elevation is communicated via chromatic surface brightening (Material 3 tonal layering) combined with subtle ambient shadows and ultra-thin ghost outlines.

### Surface Hierarchy
1. **Base Floor (0dp):** `#121316` (Underlying workspace and full-screen backgrounds).
2. **Resting Cards (1dp):** `#1A1C22` overlaid with a `1px` continuous border in `#2A2E39`.
3. **Elevated & Interactive Surfaces (2dp - 4dp):** `#252830` with an ambient shadow (`box-shadow: 0 4px 12px rgba(0, 0, 0, 0.45)`).
4. **App Bars & Modals (6dp - 8dp):** Dynamic surface tinting `#1A1C22` featuring backdrop blur (`backdrop-filter: blur(16px)` at 85% opacity) to preserve context beneath floating overlays.

### Shadow Characteristics
Shadows avoid cool grey or tinted spreads. Cast shadows use pitch-black cores with soft, charcoal-tinted diffusion: `rgba(0, 0, 0, 0.5)` for elevation tiers up to 6dp, accented with subtle border reflections to clearly demarcate overlapping poster layers.

## Shapes

The design system incorporates rounded, organic geometries balancing Android's fluid Material shape language with cinematic softness.

- **Posters & Media Cards:** Set to `rounded-lg` / `rounded-xl` (`16dp` / `1rem`), providing a modern, tactile silhouette that prevents poster corners from feeling sharp.
- **Status & Type Badges:** Set to soft rounding (`6dp` - `8dp`) for tight alignment against media thumbnails.
- **Buttons, Score Chips & Text Fields:** Fully rounded or pill-shaped for high interactive affordance and thumb-friendly touch targets.
- **Bottom Sheets & Modal Dialogs:** Rounded top edges using `28dp` radius, consistent with Material 3 specs.

## Components

All user-facing copy and states are structured in Spanish.

### Buttons
- **Primary (`Filled`):** Background `#38BDF8`, text `#0F172A`, font weight 600. Used for definitive actions like "Agregar a la biblioteca" or "Marcar como visto". Focus state displays a subtle sky blue ring with 2px offset.
- **Secondary (`Tonal`):** Background `#252830`, text `#F3F4F6`, border `1px solid #2A2E39`. Used for filter toggles and auxiliary actions like "Ver tráiler". Hover or press transitions border to `rgba(56, 189, 248, 0.3)`.
- **Destructive / Accent (`Cinema Action`):** Background `#E11D48`, text `#FFFFFF`. Used for favorites and removals ("Eliminar de la lista").

### Media Status & Type Badges
Badges are compact, using `label-sm` font attributes and 8dp inner horizontal padding.
- **Status: 'Pendiente'**: Background `rgba(156, 163, 175, 0.12)`, text `#9CA3AF`, border `1px solid rgba(156, 163, 175, 0.25)`.
- **Status: 'Viendo'**: Background `rgba(56, 189, 248, 0.12)`, text `#38BDF8`, border `1px solid rgba(56, 189, 248, 0.35)`.
- **Status: 'Visto'**: Background `rgba(52, 211, 153, 0.12)`, text `#34D399`, border `1px solid rgba(52, 211, 153, 0.35)`.
- **Media Type: 'Película' / 'Serie'**: Semi-transparent dark overlay (`rgba(18, 19, 22, 0.85)`), text `#F3F4F6`, positioned over the top corner of the poster.

### Score Chips
Pill-shaped indicator displaying rating values (e.g., "★ 8.5 / 10").
- Background: Surface `#1A1C22` with a `1px` accent border (`#38BDF8` at 35% opacity).
- Icon: Star glyph tinted with `#38BDF8`.
- Typography: Label medium (`12px`, 600 weight) with `#F3F4F6` value and `#9CA3AF` scale delimiter.

### Cards (Media Card)
- **Structure:** 2:3 aspect ratio poster image enclosed in rounded shell.
- **Elevation:** Surface `#1A1C22` with 1px border `#2A2E39`.
- **Overlay Details:** Floating score chip anchored top-right; media type badge top-left. Bottom scrim (gradient fade from transparent to `#121316`) housing title (`title-sm`), release year, and status tag.

### Lists & Episode Rows
- Background: `#1A1C22` on resting state, active highlight `#252830`.
- Includes trailing checkbox/action button for instant checkmarking of completed episodes, glowing `#38BDF8` upon completion.
- Dividers are inset with color `#252830` and thickness of `1px`.

### Input Fields & Search
- Container: Surface `#16181E` with full-pill radius or rounded-xl.
- Focused State: Border transitions smoothly to `#38BDF8` with zero shadow spread.
- Icons: Leading search icon and trailing clear button tinted with `#9CA3AF`.
- Placeholder text: "Buscar películas, series, directores..." in `#6B7280`.

### Navigation Bar (Bottom Navigation)
- Height: `80dp`, Surface `#1A1C22` with top subtle border `1px solid #252830`.
- Active Indicator: Pill highlight (`#252830`) surrounding the icon with primary active color `#38BDF8`.
- Labels (`label-md`): "Inicio", "Buscar", "Mi Lista", "Estadísticas".