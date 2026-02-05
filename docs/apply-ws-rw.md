# Apply（Watershed / Random Walker）

## 共通

- Applyは常に full resolution で実行する
- 出力ラベルは `0=背景, 1..N=前景`
- DOMAIN外は常に背景0固定

## Watershed

- marker-controlled watershed を実行
- Surface は `Invert Original / Original / Gradient(Sobel)` から選択
- Enable Preprocessing が ON の場合のみ surface に Gaussian を適用

## Random Walker

- 強度差重みで確率を反復更新してラベルを決定
- beta は UI で調整可能
- Enable Preprocessing が ON の場合のみ surface に Gaussian を適用
- 後処理として、各ラベルは最大連結成分のみ保持（飛び地は背景0）
