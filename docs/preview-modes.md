# Previewモード仕様

## Off

- Preview計算を停止する
- 現在のOverlayをクリアする

## Seed preview

- Seed / Domain / BG を overlay 表示する
- 表示は単一の半透明 ImageRoi を使う（重い図形積み上げを避ける）
- ShowSeedCross が ON の場合、seed centroid に crosshair を表示する

## ROI boundaries

- SegmentationResult から境界線を生成して表示する
- 表示対象は Apply と同じラベル結果を使う
- 文字ラベルは表示しない

## 更新ルール

- PreviewがOff以外のときのみ計算する
- 設定変更時は debounce + generation cancel で再計算する
