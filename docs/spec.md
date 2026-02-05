# Spec: Area_Segmentater (Fiji / ImageJ1 plugin)

## Goal
A Threshold-like GUI that:
- Uses two thresholds to generate foreground/background markers and unknown range.
- Offers realtime preview (unless Preview=Off).
- Runs marker-controlled watershed or random walker segmentation on Apply.
- Exports object ROIs on Add ROI.

## UI (Threshold-like)
- Histogram panel for current slice.
- Two sliders + numeric fields:
  - Foreground Threshold (upper)
  - Background Threshold (lower)
  - Always enforce T_bg <= T_fg (snap)
- Options:
  - Preview Mode: Off / Marker boundaries / ROI boundaries
  - Segmentation: Watershed / Random Walker
  - Surface (Watershed only): Original / Invert Original / Gradient (Sobel)
  - Random Walker beta: numeric + slider (enabled only when Random Walker is selected)
  - Invert checkbox
- Connectivity: 4 / 8
- Advanced (Appearance):
  - Seed/Domain/BG preview toggles and colors
  - Opacity
  - ShowSeedCross: toggle to show crosshair at seed centroids
- Advanced (Preprocessing):
  - Enable Preprocessing (checkbox)
  - Sigma (surface) slider [0..5]
  - Sigma (seed) slider [0..5] (enabled only when SeedSource=Find Maxima)
- Buttons:
  - Apply (produces label image)
  - Add ROI (exports ROIs for labels 1..N)
  - Reset (restore defaults, clear overlay)

## Marker generation
- Based on docs/masks-and-truth-table.md.
- Foreground markers: connected components on foreground-side threshold mask; each component becomes a distinct marker label.
- Background marker: background-side threshold mask; treated as background label.
- Unknown: the range between thresholds.
- Unknown islands absorption (default ON):
  - Unknown connected components that do NOT touch any foreground marker component are absorbed into background.
 - Find Maxima seeds use an optional Gaussian prefilter (seed sigma) when preprocessing is enabled.

## Preview modes
- Off:
  - Clear overlay and stop any marker/preview computation.
- Marker boundaries:
  - Draw boundary lines for foreground markers and background marker.
- ROI boundaries:
  - Draw boundary lines for each foreground marker component (no text labels).

## Apply
- Always compute at full resolution.
- Method: Watershed
  - Surface: Original / Invert Original / Sobel(Original)
  - Use marker-controlled watershed; output label image with 0 background, 1..N objects.
- Method: Random Walker
  - Use original intensity difference weights; beta is adjustable by UI.
  - Output label image with 0 background, 1..N objects.
  - Postprocess: keep only the largest connected component per label; other islands become background (0).
 - Optional Gaussian preprocessing:
   - Surface sigma applies to the surface used by Watershed/Random Walker only.
   - Masks/seeds/domain are computed without preprocessing.

## Add ROI
- If no current segmentation result exists, run Apply-equivalent compute first.
- Export one ROI per object label (1..N) to RoiManager.
- ROI naming: obj-001, obj-002, ...

## Non-goals
- No 3D support.
- No LoG option.
- No auto-tuning filters by default; advanced options may exist later but default is minimal.
