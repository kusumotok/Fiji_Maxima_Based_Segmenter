# Area_Segmentater

Area_Segmentater は、Fiji / ImageJ1 向けの 2D 領域分割プラグインです。  
2つの閾値と複数の Seed ソースを使い、Watershed / Random Walker でラベル画像を生成します。

## ダウンロード（推奨）

最新版は **GitHub Releases** から取得してください。

- Releases: `https://github.com/kusumotok/Fiji_Area_Segmentater/releases`
- 配布ファイル: `Area_Segmentater.jar`

## インストール

1. Releases から `Area_Segmentater.jar` をダウンロード
2. `Area_Segmentater.jar` を `Fiji/plugins/` にコピー
3. Fiji を再起動

## 起動

1. Fiji で画像を開く
2. `Plugins > Area_Segmentater` を実行

## 主な機能

- 2閾値 UI（`T_bg <= T_fg` を維持）
- Preview
  - `Off`: 計算停止 + Overlay クリア
  - `Seed preview`: Seed / DOMAIN / BG の可視化
  - `ROI boundaries`: 処理後ラベル境界の表示
- Segmentation
  - Watershed（Surface: Invert Original / Original / Gradient(Sobel)）
  - Random Walker（`beta` 調整可）
- Seed ソース
  - Threshold Components / ROI Manager / Binary Image / Find Maxima / Manual Selection
- 前処理（任意）
  - Gaussian (surface)
  - Gaussian (seed for Find Maxima)
- 出力
  - Apply: ラベル画像（背景=0、前景=1..N）
  - Add ROI: ラベルごとに RoiManager へ追加

## アルゴリズム概要

### Watershed

- Seed 付き優先度伝播で `DOMAIN` 内を分割
- Surface は Invert Original / Original / Gradient(Sobel) から選択
- Connectivity（4/8）に従って伝播

### Random Walker

- 近傍重み `w = exp(-beta * (Ii - Ij)^2)` で確率を反復更新
- 最大確率ラベルを採用
- 後処理で各ラベルの最大連結成分を保持し、飛び地は背景0へ

## 開発者向け（ソースからビルド）

```bash
mvn -f plugin/pom.xml package
```

生成物:

- `plugin/target/Area_Segmentater.jar`

## ドキュメント

- 仕様: `docs/spec.md`
- 決定事項: `docs/decisions.md`
- 手動検証: `docs/verify-manual.md`

## Note

このプロジェクトはバイブコーディング（vibe coding）で継続的に改善しています。  
運用前には、Releaseノートと `docs/spec.md` の差分確認を推奨します。
