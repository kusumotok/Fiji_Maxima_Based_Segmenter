# ROI Export

## Add ROI の仕様

- SegmentationResult が無い場合は Apply 相当計算を先に実行する
- ラベル `1..N` を1つずつROIへ変換して RoiManager に追加する
- ROI名は `obj-001`, `obj-002`, ... の連番形式

## 想定動作

- 背景ラベル `0` はROI化しない
- segmentation結果に一致した領域がROIとして出力される
