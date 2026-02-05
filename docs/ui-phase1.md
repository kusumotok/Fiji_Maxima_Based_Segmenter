# UI Phase 1 — Threshold-like Behavior

## Components
- Histogram (current slice)
- Foreground Threshold: slider + numeric field
- Background Threshold: slider + numeric field
- Preview Mode radio: Off / Marker boundaries / ROI boundaries
- Segmentation radio: Watershed / Random Walker
- Surface radio (Watershed only): Original / Gradient(Sobel)
- Invert checkbox
- Connectivity radio: 4 / 8
- Buttons: Apply / Add ROI / Reset

## Rules
- Keep T_bg <= T_fg at all times:
  - If user sets T_bg > T_fg => snap T_bg to T_fg
  - If user sets T_fg < T_bg => snap T_fg to T_bg
- Slider changes update numeric fields immediately; numeric field changes update sliders on commit.
- When segmentation method switches:
  - If Random Walker: disable Surface option.
  - If Watershed: enable Surface option.
