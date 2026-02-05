# Decisions (authoritative)

- Project root: Fiji_Area_Segmentater: YES
- 2D only (no 3D/3D ROI): YES
- Histogram default: current slice: YES
- Default semantics: foreground is HIGH intensity side: YES
- Invert option exists: YES
- Two thresholds:
  - Foreground Threshold = upper
  - Background Threshold = lower
  - Must maintain T_bg <= T_fg with snap behavior: YES
- Connectivity option: 4 / 8 (default 8): YES
- Preview modes (3):
  1) Off (no compute)
  2) Seed preview (single ImageRoi overlay)
  3) ROI boundaries (no text labels): YES
- Preview uses seed fill (and optional seed centroids), not boundary lines: YES
- Unknown "islands" absorption to background: default ON: YES
- DOMAIN is fixed by T_bg: pixels with I<=T_bg are always background, and segmentation runs only inside DOMAIN: YES
- Seed sources are switchable (Threshold components / ROI Manager / Binary / Find Maxima / Manual): YES
- Non-threshold seed sources do NOT depend on T_fg: YES
- Preprocessing (Gaussian) can be enabled:
  - Surface sigma applies to Watershed/RandomWalker surface only (masks unchanged): YES
  - Seed sigma applies only to Find Maxima seed detection: YES
- Random Walker postprocess removes disconnected islands per label (keep largest CC, others -> background 0): YES
- Watershed surface options include Invert Original: YES
- Random Walker beta is user-adjustable in UI (disabled when Watershed is selected): YES
- Segmentation methods:
  - Marker-controlled Watershed
  - Random Walker: YES
- Watershed surface options:
  - Invert Original (default)
  - Original image
  - Sobel gradient (optional): YES
- Random Walker uses original intensity differences; no gradient option: YES
- Apply outputs label image (0 background, 1..N objects): YES
- Add ROI exports each object region as ROI to RoiManager: YES
