# Phase Plan (DoD per phase)

Phase 1: UI skeleton
- Implement the full UI layout and event wiring.
- Enforce snap rule T_bg <= T_fg.
- Enable/disable Surface based on method.
- No marker computation required yet.

Phase 2: Marker generation
- Implement FG/BG/UNKNOWN masks per truth table.
- DOMAIN fixed by T_bg (BG_SIDE exclusion).
- Seed source selection (threshold components / ROI / binary / find maxima / manual).
- Foreground connected-component labeling (4/8) for threshold/binary.
- Unknown islands absorption (default ON).
- No overlays required in this phase.

Phase 3: Preview overlays
- Preview Off: clear overlay + stop computation.
- Seed fill overlay (single ImageRoi).
- Optional seed centroid crosshair overlay.
- ROI boundaries overlay.
- Realtime update on threshold change when preview != Off.

Phase 4: Apply segmentation
- Watershed (marker-controlled), surface: Original/Gradient(Sobel).
- Random Walker with original intensity weights.
- Output label image.

Phase 5: Add ROI
- Export label 1..N to RoiManager.

Phase 6: Performance (optional)
- Debounce + generation cancel for preview.
- Caching for MarkerResult / SegmentationResult without breaking semantics.
