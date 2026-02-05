# 決定事項（authoritative）

- プロジェクトルートは `Fiji_Area_Segmentater`：YES
- 2Dのみ（3D/3D ROI 非対応）：YES
- ヒストグラム既定は current slice：YES
- 既定セマンティクスは「高輝度側が前景」：YES
- Invertオプションを提供：YES
- 2閾値
  - Foreground Threshold = upper
  - Background Threshold = lower
  - `T_bg <= T_fg` を snap で維持：YES
- Connectivityは `4 / 8`（既定8）：YES
- Preview mode（3種類）
  1) Off（非計算）
  2) Seed preview（単一 ImageRoi）
  3) ROI boundaries（文字なし）：YES
- Previewは境界線ではなく seed fill（+任意のseed centroid）：YES
- Unknown islands の背景吸収（既定ON）：YES
- DOMAIN は `T_bg` で固定（`I<=T_bg` は常に背景、segmentationはDOMAIN内のみ）：YES
- Seed source は切替可能（Threshold components / ROI Manager / Binary / Find Maxima / Manual）：YES
- Threshold以外の Seed source では `T_fg` 非依存：YES
- Preprocessing（Gaussian）を有効化可能
  - surface sigma は Watershed/Random Walker の surface のみに適用：YES
  - seed sigma は Find Maxima seed 検出時のみに適用：YES
- Random Walker 後処理でラベルごとの飛び地を除去（最大連結成分のみ保持）：YES
- Watershed surface に `Invert Original` を含む：YES
- Random Walker beta をUIで調整可能（Watershed選択時は無効化）：YES
- Segmentation method
  - Marker-controlled Watershed
  - Random Walker：YES
- Watershed surface
  - Invert Original（既定）
  - Original
  - Gradient(Sobel)：YES
- Apply はラベル画像 `0=背景, 1..N=前景` を出力：YES
- Add ROI は各ラベル領域を RoiManager へ出力：YES
