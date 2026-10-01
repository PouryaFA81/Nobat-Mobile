# Nobat Mobile — design tokens (One UI lean)

Sibling of server Nobat: same **Yaru orange** brand, **dark-first** surfaces, soft squircles — not Material sharp cards, not server neumorphism (no dual-tone glow).

Use in Compose `Color.kt` / `Theme.kt`. RTL (`LayoutDirection.Rtl`) for Persian UI.

## Brand
| Token | Hex | Role |
|-------|-----|------|
| `brand` | `#E95420` | Primary actions, FAB, links, calendar accents |
| `brandHover` | `#FF7A4D` | Pressed / highlight on brand |
| `brandMuted` | `#3D2A22` | Soft brand wash on dark surfaces |
| `onBrand` | `#1A120F` | Text/icons on solid brand buttons |

## Dark (default)
| Token | Hex | Role |
|-------|-----|------|
| `bg` | `#1C1B1A` | App scaffold / window |
| `surface` | `#2B2927` | Cards, bars, sheets (matches icon field) |
| `surfaceRaised` | `#353230` | Elevated chips / selected day |
| `ink` | `#F2F0ED` | Primary text |
| `muted` | `#A8A29E` | Secondary text, hints |
| `hairline` | `#000000` @ 35% | Dividers, menu borders |
| `overlay` | `#000000` @ 45% | Scrim behind sheets |

## Light (optional toggle later)
| Token | Hex | Role |
|-------|-----|------|
| `bg` | `#F5F2EF` | Scaffold |
| `surface` | `#FFFFFF` | Cards |
| `surfaceRaised` | `#ECE8E4` | Nested / selected |
| `ink` | `#2C2C2C` | Primary text |
| `muted` | `#6E6A66` | Secondary |
| `hairline` | `#2C2C2C` @ 12% | Dividers |

## Semantic
| Token | Hex | Role |
|-------|-----|------|
| `danger` | `#E35D6A` | Cancel / delete |
| `success` | `#3DDC97` | Arrived / done (sparingly) |
| `friday` | `#E35D6A` | Jalali Friday labels (calendar) |

## Shape (One UI)
| Token | Value | Use |
|-------|-------|-----|
| `radiusXs` | 8.dp | Chips, text fields |
| `radiusSm` | 12.dp | Buttons, list rows |
| `radiusMd` | 16.dp | Cards |
| `radiusLg` | 24.dp | Bottom sheets, dialogs |
| `radiusPill` | 50% | FAB, switches |
| Icon squircle | ~36% corner | Launcher only (already in assets) |

Avoid Material 3 default 4.dp / 28.dp “expressive” extremes — stay in the 12–24.dp band.

## Elevation
Prefer **soft ambient shadow** (black @ 20–40%, blur 8–16) or tonal surface step — **not** neumorphic dual light/dark glow (that’s server web).

Menus/sheets: `surface` + `hairline` + soft drop (same rule as Nobat web A′).

## Type
- UI: **Vazirmatn** (or system Persian-capable) for fa; **Sans** for Latin chrome (`Nobat`, version).
- Title: 22.sp / SemiBold  
- Body: 16.sp / Regular  
- Caption: 13.sp / Medium, `muted`  
- Keep line length comfortable in RTL; no all-caps Persian.

## Touch
- Min target **48.dp**
- Primary FAB: `brand` / `onBrand`, bottom-end in LTR → bottom-**start** visual in RTL (Compose RTL mirrors)

## Do / don’t
- Do: dark charcoal + orange calendar family; thick simple glyphs  
- Don’t: Google blue primary; thin Material outlined icons as brand; Farsi letters in the launcher mark  
