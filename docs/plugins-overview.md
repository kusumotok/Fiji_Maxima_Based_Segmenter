# Fiji Segmentation Suite - プラグイン概要

## 5つのプラグイン

- Maxima Based Segmenter 系（4本）: `Plugins > Segmentation > Maxima Based Segmenter`
- Spot Quantifier 系（1本）: `Plugins > Segmentation > Spot Quantifier`

### 1. Maxima_Based_Segmenter（フル機能版）

**目的**: 高度なパラメータ調整が可能な2Dセグメンテーション

**対象ユーザー**: 詳細な制御が必要な上級ユーザー

**主な機能**:
- 複数のシードソース選択
  - Find Maxima（デフォルト）
  - Threshold Components
  - ROI Manager
  - Binary Image
  - Manual Selection
- アルゴリズム選択（Watershed / Random Walker）
- Surface選択（Invert Original / Original / Gradient Sobel）
- Connectivity選択（4近傍 / 8近傍）
- 前処理オプション（Gaussian）

**デフォルト設定**:
- Marker Source: Find Maxima
- Tolerance: 2000
- Connectivity: C4
- Method: Watershed
- Surface: Invert Original
- Preview Colors: Seed（赤）、Domain（緑）、BG（青、デフォルトOFF）

**UI配置**:
- メインパネル: BG Threshold, FG Threshold, Tolerance
- Advancedパネル: Marker Source, Method, Surface, Connectivity, Preprocessing

**マクロパラメータ**:
```
bg_threshold=N fg_threshold=N tolerance=N marker_source=SOURCE method=METHOD surface=SURFACE connectivity=CONN
```

---

### 2. Maxima_Based_Segmenter_Simple（シンプル版）

**目的**: 初心者向けの簡易2Dセグメンテーション

**対象ユーザー**: シンプルな操作を求めるユーザー

**主な機能**:
- 2パラメータのみ（BG Threshold, Tolerance）
- 固定設定で最適化済み
- リアルタイムプレビュー
- ROI出力（Manager / ZIP）

**固定設定**:
- Marker Source: Find Maxima
- Tolerance: 2000（デフォルト）
- Connectivity: C4
- Method: Watershed
- Surface: Invert Original
- Preprocessing: なし
- Preview Colors: Seed（赤）、Domain（緑）、BG（青、デフォルトOFF）

**UI配置**:
- BG Threshold スライダー
- Tolerance スライダー
- Preview Mode 選択
- Apply / Add ROI / Save ROI ボタン
- Histogram パネル

**マクロパラメータ**:
```
bg_threshold=N tolerance=N
```

**プログラマティックAPI**:
```java
ImagePlus labelImage = Maxima_Based_Segmenter_Simple_.segment(imp, bgThreshold, tolerance);
```

---

### 3. Maxima_Based_Segmenter_3D（3D版）

**目的**: 3Dスタック画像のセグメンテーション

**対象ユーザー**: 3D画像解析を行うユーザー

**主な機能**:
- MorphoLibJ Extended Maxima による3D局所最大検出
- Marker-Controlled Watershed 3D
- 現在のZ平面のプレビュー表示
- 3D ROI出力（Position/Group属性付き）

**固定設定**:
- Marker Source: Extended Maxima (MorphoLibJ)
- Tolerance: 2000（デフォルト）
- Connectivity: C6
- Method: Marker-Controlled Watershed 3D
- Surface: Invert Original
- Preprocessing: なし
- Preview Colors: 
  - 現在のスライス: Seed（赤）、Domain（緑）
  - 他のスライス: Seedクロス（半透明青色）で全スライスのシード位置を表示

**UI配置**:
- BG Threshold スライダー
- Tolerance スライダー
- Preview Mode 選択
- Apply / Add ROI / Save ROI ボタン
- Histogram パネル（現在のZ平面）

**入力要件**:
- 3Dスタック画像（Z > 1）
- XYZ次元（TやCは想定外）

**ROI出力形式**:
- 各オブジェクトの各Z平面が個別のROI
- Position属性: Z座標（1-based）
- Group属性: オブジェクトID
- 命名規則: `obj-XXX-zYYY`
  - XXX: オブジェクトID（001, 002, ...）
  - YYY: Z座標（001, 002, ...）

**マクロパラメータ**:
```
bg_threshold=N tolerance=N
```

**プログラマティックAPI**:
```java
ImagePlus labelImage3D = Maxima_Based_Segmenter_3D_.segment(imp, bgThreshold, tolerance);
```

---

### 4. Slice_Based_3D_Segmenter（スライスベース3D版）

**目的**: MorphoLibJ 非依存の3Dスタックセグメンテーション

**対象ユーザー**: MorphoLibJ なし環境、またはスライス単位の Watershed で十分なユーザー

**主な機能**:
- 各スライスに2D Watershed（C4、Invert Original、Find Maxima）を適用
- スライス間の重複領域をUnion-Findでマージし3D領域を構築
- BG Threshold と Tolerance の2パラメータ
- 3D ROI出力（Position/Group属性付き）
- MorphoLibJ 非依存

**固定設定**:
- Connectivity: C4（各スライスの2D処理）
- Method: Watershed (Invert Original)
- Marker Source: Find Maxima

**ROI出力形式**:
- 各オブジェクトの各Z平面が個別のROI
- Position属性: Z座標（1-based）、Group属性: オブジェクトID
- 命名規則: `obj-XXX-zYYY`

**マクロパラメータ**:
```
bg_threshold=N tolerance=N
```

---

### 5. Spot_Quantifier_3D_（スポット定量版）

**目的**: 3D共焦点画像における蛍光スポット（中心体・斑点状シグナル等）の定量

**対象ユーザー**: バッチ処理で蛍光スポットの輝度・体積・個数を定量したいユーザー

**主な機能**:
- 固定閾値 → バイナリマスク → 3D Connected Components（MorphoLibJ・32-bit）
- 近傍選択（6 / 18 / 26、デフォルト: 6）
- 穴埋めオプション（Fill holes、スライス毎2D、デフォルト: off）
- 体積フィルタ（min/max vol µm³）
- 測定値: volume_um3 / volume_vox / integrated_intensity / mean_intensity / centroid XYZ
- プレビュー: Overlay（カラー塗りつぶし）/ ROI（輪郭線・ROI Managerを変更しない）
- バッチマクロ対応、出力ディレクトリ指定可能

**パラメータ**:
- Threshold: 固定強度閾値（デフォルト: 500）
- Min vol µm³: 最小体積フィルタ（チェックで有効化）
- Max vol µm³: 最大体積フィルタ（チェックで有効化）
- Gaussian blur: 前処理オプション（XY σ / Z σ）
- Connectivity: 3D連結近傍（6 / 18 / 26、デフォルト: 6）
- Fill holes: バイナリマスク穴埋め（デフォルト: off）

**プレビューモード**:
- Off: プレビュー無効
- Overlay: 黄=valid / 赤=too small / 青=too large（50% 透明度塗りつぶし）
- ROI: 実際に保存されるROIの輪郭線のみ

**ボタン**:
- **Save CSV**: CSVのみ任意の場所に保存
- **Save All**: csv/ + roi/ + params.txt を一括保存（フォルダ構造を自動生成）

**出力ファイル構成（Save All）**:
```
{outputDir}/
├── csv/{basename}_spots.csv
├── roi/{basename}_RoiSet.zip
└── params.txt                 # 条件ごとに1ファイル（バッチ全体で上書き）
```

**CSV カラム**:
```
spot_id, volume_um3, volume_vox, integrated_intensity, mean_intensity,
centroid_x_um, centroid_y_um, centroid_z_um
```

**マクロパラメータ**:
```
threshold=N min_vol=N max_vol=N gaussian_blur=true/false gauss_xy=N gauss_z=N
connectivity=N fill_holes=true/false output=[path]
```

**依存関係**: MorphoLibJ（`BinaryImages.componentsLabeling` 使用）

---

## 共通機能

### Preview Mode

すべてのプラグインで以下のプレビューモードが利用可能：

- **Off**: プレビュー無効、オーバーレイクリア
- **Seed preview**: シード、ドメイン、背景の可視化
  - 2D版: Seed（赤）、Domain（緑）、BG（青、デフォルトOFF）
  - 3D版: 現在のスライス（赤）+ 他のスライスのシードクロス（半透明青色）
- **ROI boundaries**: セグメンテーション結果の境界表示

パネルを閉じると自動的にプレビューオーバーレイがクリアされます。

### ROI出力

すべてのプラグインで以下の出力方法が利用可能：

- **Apply**: ラベル画像を生成（背景=0、前景=1..N）
- **Add ROI**: ROI Manager に追加
- **Save ROI**: ZIP ファイルに保存

### マクロ対応

すべてのプラグインがマクロから呼び出し可能で、非インタラクティブモードで実行できます。

---

## アーキテクチャ

### パッケージ構成

```
jp.yourorg.fiji_maxima_based_segmenter/
├── Maxima_Based_Segmenter_.java          # フル機能版エントリーポイント
├── Maxima_Based_Segmenter_Simple_.java   # シンプル版エントリーポイント
├── Maxima_Based_Segmenter_3D_.java       # 3D版エントリーポイント
├── Spot_Quantifier_3D_.java              # スポット定量版エントリーポイント
├── core/                                  # 共有データモデル
│   ├── ThresholdModel.java
│   ├── MarkerBuilder.java
│   ├── MarkerBuilder3D.java
│   ├── MarkerResult.java
│   ├── MarkerResult3D.java
│   └── (Enums: Connectivity, Method, Surface, etc.)
├── alg/                                   # セグメンテーションアルゴリズム（共通）
│   ├── WatershedRunner.java
│   ├── Watershed3DRunner.java
│   ├── RandomWalkerRunner.java
│   ├── SegmentationResult.java
│   └── SegmentationResult3D.java
├── alg/                                   # スポット定量版アルゴリズム
│   ├── SpotQuantifier3D.java             # CC計算（MorphoLibJ 26近傍・32-bit）
│   ├── CcResult3D.java                   # CC結果・分類・フィルタ
│   ├── SegmentationResult3D.java         # フィルタ済みラベル画像
│   ├── SpotMeasurer.java                 # O(W×H×D)単一スキャン測定
│   ├── SpotMeasurement.java              # スポット測定値データクラス
│   └── QuantifierParams.java             # パラメータデータクラス
├── preview/                               # プレビュー機能
│   └── PreviewRenderer.java
├── roi/                                   # ROI出力
│   ├── RoiExporter.java
│   └── RoiExporter3D.java
├── ui/                                    # UIフレーム
│   ├── DualThresholdFrame.java
│   ├── SimpleSegmenterFrame.java
│   ├── Segmenter3DFrame.java
│   ├── SpotQuantifier3DFrame.java        # スポット定量版GUI
│   └── HistogramPanel.java
└── util/
    └── CsvExporter.java                  # CSV・params.txt 出力
```

### 依存関係

- **ImageJ 1.x**: すべてのプラグイン
- **MorphoLibJ (IJPB-plugins)**: 3D版のみ
  - Extended Maxima 3D
  - Marker-Controlled Watershed 3D
  - Connectivity 定数

---

## 選択ガイド

### どのプラグインを使うべきか？

| 状況 | 推奨プラグイン |
|------|---------------|
| 2D画像、シンプルな操作 | Maxima_Based_Segmenter_Simple |
| 2D画像、詳細な制御が必要 | Maxima_Based_Segmenter |
| 3Dスタック画像（形状分割、MorphoLibJあり） | Maxima_Based_Segmenter_3D |
| 3Dスタック画像（形状分割、MorphoLibJ不要） | Slice_Based_3D_Segmenter |
| 3Dスタック画像（スポット定量・輝度・体積測定） | Spot_Quantifier_3D_ |
| バッチ処理・CSV出力が必要 | Spot_Quantifier_3D_ |
| 近傍数や穴埋めを制御したい | Spot_Quantifier_3D_ |
| マクロで自動化 | Simple版 / Slice_Based / 3D版 / Spot_Quantifier_3D_ |
| Random Walkerを使いたい | Maxima_Based_Segmenter（フル機能版のみ） |
| Threshold Componentsをシードにしたい | Maxima_Based_Segmenter（フル機能版のみ） |

---

## 実装状況

- ✅ フル機能版: 完全実装（マクロ対応、Save ROI含む）
- ✅ シンプル版: 完全実装（マクロ対応、静的API、Save ROI含む）
- ✅ 3D版: 完全実装（マクロ対応、静的API、Save ROI含む）
- ✅ スライスベース3D版: 完全実装（マクロ対応、ROI出力、MorphoLibJ非依存）
- ✅ スポット定量版: 完全実装（マクロ対応、connectivity/fill_holes、Save CSV/Save All、3プレビューモード）
- ✅ ビルド設定: 単一JAR（Maxima_Based_Segmenter.jar）に5プラグイン同梱
- ✅ plugins.config: 5プラグインすべて登録済み
