# Maxima-Based Segmenter Suite - プラグイン概要

## 3つのプラグイン

すべてのプラグインは `Plugins > Segmentation > Maxima Based Segmenter` メニュー下に配置されています。

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
├── core/                                  # 共有データモデル
│   ├── ThresholdModel.java
│   ├── MarkerBuilder.java
│   ├── MarkerBuilder3D.java
│   ├── MarkerResult.java
│   ├── MarkerResult3D.java
│   └── (Enums: Connectivity, Method, Surface, etc.)
├── alg/                                   # セグメンテーションアルゴリズム
│   ├── WatershedRunner.java
│   ├── Watershed3DRunner.java
│   ├── RandomWalkerRunner.java
│   ├── SegmentationResult.java
│   └── SegmentationResult3D.java
├── preview/                               # プレビュー機能
│   └── PreviewRenderer.java
├── roi/                                   # ROI出力
│   ├── RoiExporter.java
│   └── RoiExporter3D.java
└── ui/                                    # UIフレーム
    ├── DualThresholdFrame.java
    ├── SimpleSegmenterFrame.java
    ├── Segmenter3DFrame.java
    └── HistogramPanel.java
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
| 3Dスタック画像 | Maxima_Based_Segmenter_3D |
| マクロで自動化 | Simple版 または 3D版（静的API利用可能） |
| Random Walkerを使いたい | Maxima_Based_Segmenter（フル機能版のみ） |
| Threshold Componentsをシードにしたい | Maxima_Based_Segmenter（フル機能版のみ） |

---

## 実装状況

- ✅ フル機能版: 完全実装（マクロ対応、Save ROI含む）
- ✅ シンプル版: 完全実装（マクロ対応、静的API、Save ROI含む）
- ✅ 3D版: 完全実装（マクロ対応、静的API、Save ROI含む）
- ✅ ビルド設定: 単一JAR（Maxima_Based_Segmenter.jar）に3プラグイン同梱
- ✅ plugins.config: 3プラグインすべて登録済み
