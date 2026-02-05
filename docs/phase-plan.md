# フェーズ計画（各PhaseのDoD）

Phase 1: UI skeleton

- UIレイアウトとイベント配線を実装
- `T_bg <= T_fg` のsnapを保証
- methodに応じてSurfaceの有効/無効を切替
- marker計算は未実装でも可

Phase 2: Marker generation

- 真理値表どおりに FG/BG/UNKNOWN を実装
- DOMAINを `T_bg` 固定で実装（BG_SIDE除外）
- Seed source切替（threshold / ROI / binary / find maxima / manual）
- threshold/binary の連結成分ラベリング（4/8）
- Unknown islands吸収（既定ON）
- このPhaseではoverlay不要

Phase 3: Preview overlays

- Preview Off: overlay消去 + 計算停止
- Seed fill overlay（単一ImageRoi）
- 任意のseed centroid crosshair overlay
- ROI boundaries overlay
- preview有効時にリアルタイム更新

Phase 4: Apply segmentation

- Watershed（marker-controlled）
  - Surface: Invert Original / Original / Gradient(Sobel)
- Random Walker
  - 強度差重み + beta調整
- ラベル画像出力（背景0, 前景1..N）

Phase 5: Add ROI

- ラベル `1..N` を RoiManager へ出力

Phase 6: Performance（任意）

- previewのdebounce + generation cancel
- MarkerResult / SegmentationResult のキャッシュ（意味を変えない）
