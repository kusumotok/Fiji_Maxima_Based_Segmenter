# 仕様: Area_Segmentater (Fiji / ImageJ1 プラグイン)

## 目的

Threshold 風 UI で以下を実現する。

- 2閾値からマスクと Seed を生成する
- Preview をリアルタイム更新する（Preview=Off を除く）
- Apply で marker-controlled Watershed / Random Walker を実行する
- Add ROI でオブジェクト ROI を出力する

## UI（Threshold-like）

- ヒストグラム（current slice）
- 2本のスライダー + 数値入力
  - Foreground Threshold（上限）
  - Background Threshold（下限）
  - 常に `T_bg <= T_fg` を維持（snap）
- オプション
  - Preview Mode: `Off / Seed preview / ROI boundaries`
  - Segmentation: `Watershed / Random Walker`
  - Surface（Watershed時のみ有効）: `Invert Original / Original / Gradient (Sobel)`
  - Random Walker beta: 数値入力 + スライダー（Random Walker時のみ有効）
  - Invert checkbox
  - Connectivity: `4 / 8`
- Advanced（Appearance）
  - Seed / Domain / BG の表示ON/OFFと色
  - Opacity
  - ShowSeedCross（seed centroid crosshair）
- Advanced（Preprocessing）
  - Enable Preprocessing
  - Sigma (surface) [0..5]
  - Sigma (seed) [0..5]（SeedSource=Find Maxima のときのみ有効）
- ボタン
  - Apply（ラベル画像を生成）
  - Add ROI（ラベル1..NをROI出力）
  - Reset（既定値に戻す + Overlay消去）

## マーカー生成

- `docs/masks-and-truth-table.md` に従う
- Foreground markers: 前景側マスクの連結成分を `1..N` ラベル化
- Background sideはDOMAIN除外に使用し、競合seedには使わない
- Unknown islands absorption（既定ON）
  - 前景成分に接していない UNKNOWN 成分を背景側へ吸収
- Find Maxima seed では、Enable Preprocessing時のみ seed用 Gaussian を適用可能

## Preview仕様

- `Off`
  - Overlayを消去し、preview計算を停止
- `Seed preview`
  - Seed/Domain/BG を単一 ImageRoi で表示
  - 必要に応じて seed centroid crosshair を重畳
- `ROI boundaries`
  - segmentation後ラベルの境界を表示（文字ラベルは表示しない）

## Apply仕様

- 常に full resolution で計算する
- Watershed
  - Surface: `Invert Original / Original / Sobel(Original)`
  - marker-controlled watershed で `0=背景, 1..N=前景`
- Random Walker
  - 強度差重みを使用
  - beta はUIで調整可能
  - 出力は `0=背景, 1..N=前景`
  - 後処理でラベルごとに最大連結成分のみ保持し、飛び地は背景0へ戻す
- Optional Gaussian preprocessing
  - surface sigma は Watershed/Random Walker の surface にのみ適用
  - masks / seeds / domain は前処理なしで計算

## Add ROI仕様

- 既存 segmentation 結果が無い場合は Apply 相当を先に実行
- ラベル `1..N` を1ROIずつ RoiManager へ出力
- ROI名は `obj-001, obj-002, ...`

## 非対象

- 3D 非対応
- LoG 非対応
- 自動チューニングは既定で提供しない
