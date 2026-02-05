# Masks & Truth Table (MUST FOLLOW)

Let:
- I = pixel intensity
- T_fg = Foreground Threshold (upper)
- T_bg = Background Threshold (lower)
Invariant: T_bg <= T_fg (snap UI to maintain)

Connectivity: 4 or 8 (user option, default 8)

## Default (invert = false): foreground is HIGH intensity side
Foreground-side mask (threshold-based seed candidate):
- FG_SIDE = (I >= T_fg)

Background-side mask (domain exclusion):
- BG_SIDE = (I <= T_bg)

Unknown range mask:
- UNKNOWN = (T_bg < I) AND (I < T_fg)

## Inverted (invert = true): foreground is LOW intensity side
Foreground-side mask (threshold-based seed candidate):
- FG_SIDE = (I <= T_fg)

Background-side mask (domain exclusion):
- BG_SIDE = (I >= T_bg)

Unknown range mask:
- UNKNOWN = (T_fg < I) AND (I < T_bg)

## DOMAIN (segmentation domain)
- DOMAIN = NOT(BG_SIDE)  (i.e., I > T_bg when invert=false, I < T_bg when invert=true)
- DOMAIN is fixed by T_bg and is independent of seed source.
- DOMAIN outside is always background (label 0), and segmentation runs only inside DOMAIN.

## Foreground seeds (labels)
- Seed source is selectable (threshold components / ROI Manager / binary image / find maxima / manual).
- Threshold components use connected components on FG_SIDE.
- Seeds outside DOMAIN are ignored.

## Unknown islands absorption (default ON)
- Compute connected components on UNKNOWN.
- If an UNKNOWN CC does NOT touch any FG marker CC (touch uses selected connectivity),
  then absorb it into background:
  - BG_SIDE <- BG_SIDE OR UNKNOWN_CC
  - UNKNOWN <- UNKNOWN minus UNKNOWN_CC

Notes:
- BG_SIDE is used only for DOMAIN exclusion and absorption bookkeeping.
- BG_SIDE is not a competing seed label in segmentation.
