# マスクと真理値表（必須）

定義:

- `I` = 画素輝度
- `T_fg` = Foreground Threshold（upper）
- `T_bg` = Background Threshold（lower）
- 不変条件: `T_bg <= T_fg`（UI側でsnap維持）

Connectivity は `4` または `8`（既定8）

## 通常時（invert = false）

前景側マスク（threshold seed候補）:

- `FG_SIDE = (I >= T_fg)`

背景側マスク（DOMAIN除外）:

- `BG_SIDE = (I <= T_bg)`

未知範囲マスク:

- `UNKNOWN = (T_bg < I) AND (I < T_fg)`

## Invert時（invert = true）

前景側マスク（threshold seed候補）:

- `FG_SIDE = (I <= T_fg)`

背景側マスク（DOMAIN除外）:

- `BG_SIDE = (I >= T_bg)`

未知範囲マスク:

- `UNKNOWN = (T_fg < I) AND (I < T_bg)`

## DOMAIN（segmentation対象）

- `DOMAIN = NOT(BG_SIDE)`  
  （invert=false では `I > T_bg`、invert=true では `I < T_bg`）
- DOMAINは `T_bg` のみで決まり、Seed sourceには依存しない
- DOMAIN外は常に背景ラベル0固定
- segmentationはDOMAIN内のみで行う

## 前景Seed（labels）

- Seed source は切替可能（threshold components / ROI / binary image / find maxima / manual）
- Threshold components は `FG_SIDE` の連結成分を使う
- DOMAIN外のseedは無視する

## Unknown islands 吸収（既定ON）

- UNKNOWN 上で連結成分を計算する
- UNKNOWN成分が FG marker 成分に接していない場合（接触判定は選択Connectivity）、
  背景へ吸収する
  - `BG_SIDE <- BG_SIDE OR UNKNOWN_CC`
  - `UNKNOWN <- UNKNOWN - UNKNOWN_CC`

補足:

- `BG_SIDE` は DOMAIN除外と吸収の管理用
- `BG_SIDE` は segmentation の競合seedとしては使わない
