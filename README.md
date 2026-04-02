# Fiji Segmentation Suite

[![GitHub release](https://img.shields.io/github/v/release/kusumotok/Fiji_Segmentation_Suite)](https://github.com/kusumotok/Fiji_Segmentation_Suite/releases)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Fiji / ImageJ1 向けの領域分割・スポット定量プラグインスイートです。  
2D/3D 共焦点顕微鏡画像のセグメンテーションおよび蛍光スポット定量に対応します。

## プラグイン構成

### Maxima Based Segmenter（4本）
`Plugins > Segmentation > Maxima Based Segmenter`

Find Maxima ベースのシード検出と Watershed アルゴリズムによる領域分割プラグイン群です。

| プラグイン | 対象 | 概要 |
|---|---|---|
| **Maxima_Based_Segmenter** | 2D | フル機能版。複数シードソース・Watershed/Random Walker |
| **Maxima_Based_Segmenter_Simple** | 2D | シンプル版。BG Threshold + Tolerance の2パラメータ |
| **Maxima_Based_Segmenter_3D** | 3D | MorphoLibJ Extended Maxima + Watershed 3D |
| **Slice_Based_3D_Segmenter** | 3D | スライス毎2D Watershed → スライス間マージ（MorphoLibJ不要） |

### Spot Quantifier（1本）
`Plugins > Segmentation > Spot Quantifier`

固定閾値ベースの3Dスポット定量プラグインです。Maxima不使用・アルゴリズムが独立しているため別メニューに配置しています。

| プラグイン | 概要 |
|---|---|
| **Spot Quantifier 3D** | 閾値 → バイナリ → 3D CC ラベリング → 体積・強度測定・CSV出力 |

## ダウンロード（推奨）

最新版は **GitHub Releases** から取得してください。

- Releases: `https://github.com/kusumotok/Fiji_Segmentation_Suite/releases`
- 配布ファイル: `Fiji_Segmentation_Suite.jar`

## インストール

1. Releases から `Fiji_Segmentation_Suite.jar` をダウンロード
2. `Fiji_Segmentation_Suite.jar` を `Fiji.app/plugins/` にコピー
3. Fiji を再起動

メニュー構成：
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter`
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_Simple`
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_3D`
- `Plugins > Segmentation > Maxima Based Segmenter > Slice_Based_3D_Segmenter`
- `Plugins > Segmentation > Spot Quantifier > Spot Quantifier 3D`

## 使用方法

### Maxima_Based_Segmenter（フル機能版）

1. Fiji で2D画像を開く
2. `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter` を実行
3. パラメータを調整：
   - **BG Threshold**: 背景閾値（この値以上がドメイン）
   - **FG Threshold**: 前景閾値（Threshold Components モード時のみ有効）
   - **Tolerance**: Find Maxima の tolerance パラメータ（デフォルト: 2000）
   - **Marker Source**: シード検出方法（デフォルト: Find Maxima）
   - **Method**: Watershed / Random Walker
   - **Connectivity**: 4近傍 / 8近傍
4. Preview Mode でリアルタイムプレビュー
5. **Apply** でラベル画像生成、**Add ROI** で ROI Manager に追加、**Save ROI** で ZIP 保存

### Maxima_Based_Segmenter_Simple（シンプル版）

1. Fiji で2D画像を開く
2. `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_Simple` を実行
3. **BG Threshold** / **Tolerance** を調整
4. **Apply** / **Add ROI** / **Save ROI** で結果を出力

固定設定: Connectivity C4 / Watershed / Invert Original / Find Maxima

### Maxima_Based_Segmenter_3D（3D版）

1. Fiji で3Dスタック画像を開く（Z > 1）
2. `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_3D` を実行
3. **BG Threshold** / **Tolerance** を調整
4. **Apply** / **Add ROI** / **Save ROI** で結果を出力

固定設定: MorphoLibJ Extended Maxima + Marker-Controlled Watershed 3D / 6近傍

ROI出力形式: `obj-XXX-zYYY`（Position=Z座標、Group=オブジェクトID）

### Slice_Based_3D_Segmenter（スライスベース3D版）

1. Fiji で3Dスタック画像を開く（Z > 1）
2. `Plugins > Segmentation > Maxima Based Segmenter > Slice_Based_3D_Segmenter` を実行
3. **BG Threshold** / **Tolerance** を調整
4. **Apply** / **Add ROI** / **Save ROI** で結果を出力

固定設定: 各スライスに2D Watershed（C4/Invert Original）→ スライス間重複マージ / MorphoLibJ不要

### Spot Quantifier 3D（スポット定量版）

1. Fiji で3Dスタック画像を開く（Z > 1、キャリブレーション設定済みであること）
2. `Plugins > Segmentation > Spot Quantifier > Spot Quantifier 3D` を実行
3. パラメータを調整：
   - **Threshold**: 固定強度閾値
   - **Min / Max vol µm³**: 体積フィルタ（チェックで有効化、対数スライダー）
   - **Gaussian blur**: 前処理（XY σ・Z σ）
   - **Connectivity**: 3D連結近傍（6 / 18 / 26、デフォルト: 6）
   - **Fill holes**: バイナリマスク穴埋め（スライス毎2D、デフォルト: off）
4. Preview Mode でリアルタイムプレビュー
   - **Overlay**: 黄=valid / 赤=too small / 青=too large
   - **ROI**: 保存対象ROIの輪郭線（ROI Managerは変更しない）
5. ボタンで出力：
   - **Save CSV**: CSVのみ保存
   - **Save All**: CSV + params.txt + ROI ZIP を一括保存
   - **Batch…**: フォルダ内の全TIFFに現在の設定を適用して一括処理

出力ファイル構成（Save All / Batch）：
```
{outputDir}/
├── csv/{basename}_spots.csv     # スポット測定値（1行1スポット）
├── roi/{basename}_RoiSet.zip    # ROI Manager 用 ROI セット
└── params.txt                   # 解析パラメータ（バッチ全体で共通・上書き）
```

CSV カラム：`spot_id, volume_um3, volume_vox, integrated_intensity, mean_intensity, centroid_x_um, centroid_y_um, centroid_z_um`

## マクロ対応

### Maxima_Based_Segmenter

```javascript
run("Maxima_Based_Segmenter", "bg_threshold=50 tolerance=10");
run("Maxima_Based_Segmenter", "bg_threshold=50 fg_threshold=100 tolerance=10 marker_source=FIND_MAXIMA method=watershed surface=invert_original connectivity=c4");
```

### Maxima_Based_Segmenter_Simple

```javascript
run("Maxima_Based_Segmenter_Simple", "bg_threshold=50 tolerance=10");
```

### Maxima_Based_Segmenter_3D

```javascript
run("Maxima_Based_Segmenter_3D", "bg_threshold=50 tolerance=10");
```

### Slice_Based_3D_Segmenter

```javascript
run("Slice_Based_3D_Segmenter", "bg_threshold=50 tolerance=10");
```

### Spot Quantifier 3D

```javascript
run("Spot Quantifier 3D",
    "threshold=300 min_vol=0.1 max_vol=200.0 gaussian_blur=false " +
    "connectivity=6 fill_holes=false output=[C:/path/to/results]");
```

パラメータ：
- `threshold=N`: 固定強度閾値（デフォルト: 500）
- `min_vol=N`: 最小体積 µm³（空白で無効）
- `max_vol=N`: 最大体積 µm³（空白で無効）
- `gaussian_blur=true/false`（デフォルト: false）
- `gauss_xy=N`（デフォルト: 1.0）、`gauss_z=N`（デフォルト: 0.5）
- `connectivity=N`: 6 / 18 / 26（デフォルト: 6）
- `fill_holes=true/false`（デフォルト: false）
- `output=[path]`: 出力ディレクトリ

## プログラマティックAPI

```java
// Simple版
ImagePlus labelImage = Maxima_Based_Segmenter_Simple_.segment(imp, bgThreshold, tolerance);

// 3D版
ImagePlus labelImage3D = Maxima_Based_Segmenter_3D_.segment(imp, bgThreshold, tolerance);
```

## アルゴリズム概要

### Maxima Based Segmenter 系

- **Watershed**: Seed 付き優先度伝播で `DOMAIN` 内を分割。Surface は Invert Original / Original / Gradient(Sobel) から選択
- **Random Walker**（フル機能版のみ）: 近傍重み `w = exp(-beta * (Ii - Ij)^2)` で確率を反復更新
- **3D版**: MorphoLibJ Extended Maxima で3D局所最大を検出し、Marker-Controlled Watershed 3D でセグメンテーション

### Spot Quantifier 3D

- 固定閾値でバイナリマスクを生成（オプション: Gaussian blur、Fill holes）
- MorphoLibJ `componentsLabeling` で3D CC ラベリング（6/18/26近傍、32-bit）
- ボクセル単位の体積・強度測定（O(W×H×D) 単一スキャン）

## 開発者向け（ソースからビルド）

```bash
cd plugin
mvn clean package
```

生成物:

- `plugin/target/Fiji_Segmentation_Suite.jar`

インストール:

```bash
cp plugin/target/Fiji_Segmentation_Suite.jar /path/to/Fiji.app/plugins/
```

## ドキュメント

- プラグイン概要: `docs/plugins-overview.md`
- Spot Quantifier 3D 詳細: `docs/spot-quantifier-3d.md`

## 依存関係

- ImageJ 1.x (Public Domain)
- MorphoLibJ (IJPB-plugins) — Maxima_Based_Segmenter_3D・Spot Quantifier 3D で使用 (LGPL-3.0)

## ライセンス

このソフトウェアはMITライセンスの下で配布されています。詳細は[LICENSE](LICENSE)ファイルを参照してください。

サードパーティライブラリのライセンス情報については[NOTICE.txt](NOTICE.txt)を参照してください。

## Note

このプロジェクトはバイブコーディング（vibe coding）で継続的に改善しています。  
運用前には、Releaseノートと仕様ドキュメントの差分確認を推奨します。
