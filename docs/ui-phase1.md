# UI Phase 1 — Threshold-like 挙動

## コンポーネント

- Histogram（current slice）
- Foreground Threshold: slider + numeric field
- Background Threshold: slider + numeric field
- Preview Mode radio: Off / Seed preview / ROI boundaries
- Segmentation radio: Watershed / Random Walker
- Surface radio（Watershedのみ）: Invert Original / Original / Gradient(Sobel)
- Random Walker beta: numeric + slider（Random Walkerのみ有効）
- Invert checkbox
- Connectivity radio: 4 / 8
- Buttons: Apply / Add ROI / Reset

## ルール

- 常に `T_bg <= T_fg` を維持する
  - `T_bg > T_fg` になったら `T_bg` を `T_fg` へsnap
  - `T_fg < T_bg` になったら `T_fg` を `T_bg` へsnap
- slider変更は即時にnumericへ反映
- numeric変更は確定時にsliderへ反映
- method切替時
  - Random Walker選択時: Surfaceを無効化、betaを有効化
  - Watershed選択時: Surfaceを有効化、betaを無効化
