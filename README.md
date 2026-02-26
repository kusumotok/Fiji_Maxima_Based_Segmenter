# Maxima-Based Segmenter Suite

Fiji / ImageJ1 向けの領域分割プラグインスイートです。  
Find Maxima ベースのシード検出と Watershed アルゴリズムを使用して、2D/3D 画像のセグメンテーションを行います。

## プラグイン構成

このスイートには3つのプラグインが含まれています：

### 1. Maxima_Based_Segmenter（フル機能版）
- 2D画像用の高度なセグメンテーションツール
- 複数のシードソース（Find Maxima / Threshold Components / ROI Manager / Binary Image / Manual Selection）
- Watershed / Random Walker アルゴリズム選択
- 詳細なパラメータ調整が可能

### 2. Maxima_Based_Segmenter_Simple（シンプル版）
- 2D画像用の簡易セグメンテーションツール
- Find Maxima のみを使用（固定設定）
- BG Threshold と Tolerance の2パラメータのみ
- 初心者向けのシンプルなUI

### 3. Maxima_Based_Segmenter_3D（3D版）
- 3Dスタック画像用のセグメンテーションツール
- MorphoLibJ の Extended Maxima と Marker-Controlled Watershed 3D を使用
- BG Threshold と Tolerance の2パラメータ
- 3D ROI出力（Position/Group属性付き）

## ダウンロード（推奨）

最新版は **GitHub Releases** から取得してください。

- Releases: `https://github.com/kusumotok/Fiji_Area_Segmentater/releases`
- 配布ファイル: `Maxima_Based_Segmenter.jar`

## インストール

1. Releases から `Maxima_Based_Segmenter.jar` をダウンロード
2. `Maxima_Based_Segmenter.jar` を `Fiji/plugins/` にコピー
3. Fiji を再起動

メニュー構成：
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter`（フル機能版）
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_Simple`（シンプル版）
- `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_3D`（3D版）

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
   - Seed preview: シード（赤）、ドメイン（緑）、背景（青、デフォルトOFF）
   - ROI boundaries: セグメンテーション結果の境界
5. **Apply** でラベル画像生成、**Add ROI** で ROI Manager に追加、**Save ROI** で ZIP ファイルに保存
6. パネルを閉じると自動的にプレビューがクリアされます

### Maxima_Based_Segmenter_Simple（シンプル版）

1. Fiji で2D画像を開く
2. `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_Simple` を実行
3. パラメータを調整：
   - **BG Threshold**: 背景閾値
   - **Tolerance**: Find Maxima の tolerance パラメータ（デフォルト: 2000）
4. Preview Mode でリアルタイムプレビュー
   - Seed preview: シード（赤）、ドメイン（緑）、背景（青、デフォルトOFF）
5. **Apply** / **Add ROI** / **Save ROI** で結果を出力
6. パネルを閉じると自動的にプレビューがクリアされます

固定設定：
- Connectivity: 4近傍
- Method: Watershed (Invert Original)
- Marker Source: Find Maxima

### Maxima_Based_Segmenter_3D（3D版）

1. Fiji で3Dスタック画像を開く（Z > 1）
2. `Plugins > Segmentation > Maxima Based Segmenter > Maxima_Based_Segmenter_3D` を実行
3. パラメータを調整：
   - **BG Threshold**: 背景閾値
   - **Tolerance**: Extended Maxima の tolerance パラメータ（デフォルト: 2000）
4. Preview Mode で現在のZ平面のプレビュー表示
   - Seed preview: 
     - 現在のスライス: シード（赤）、ドメイン（緑）
     - 他のスライス: シードクロス（半透明青色）で全スライスのシード位置を表示
   - ROI boundaries: セグメンテーション結果の境界
5. **Apply** / **Add ROI** / **Save ROI** で結果を出力
6. パネルを閉じると自動的にプレビューがクリアされます

固定設定：
- Connectivity: 6近傍
- Method: Marker-Controlled Watershed 3D (Invert Original)
- Marker Source: Extended Maxima (MorphoLibJ)

ROI出力形式：
- 各オブジェクトの各Z平面が個別のROIとして出力
- Position属性: Z座標
- Group属性: オブジェクトID
- 命名規則: `obj-XXX-zYYY`

## マクロ対応

すべてのプラグインはマクロから呼び出し可能です。

### Maxima_Based_Segmenter（フル機能版）

```javascript
// 基本的な使用例
run("Maxima_Based_Segmenter", "bg_threshold=50 tolerance=10");

// すべてのパラメータを指定
run("Maxima_Based_Segmenter", "bg_threshold=50 fg_threshold=100 tolerance=10 marker_source=FIND_MAXIMA method=watershed surface=invert_original connectivity=c4");
```

パラメータ：
- `bg_threshold=N`: 背景閾値
- `fg_threshold=N`: 前景閾値
- `tolerance=N`: Find Maxima tolerance
- `marker_source=SOURCE`: FIND_MAXIMA / THRESHOLD_COMPONENTS / ROI_MANAGER / BINARY_IMAGE / MANUAL_SELECTION
- `method=METHOD`: watershed / random_walker
- `surface=SURFACE`: invert_original / original / gradient_sobel
- `connectivity=CONN`: c4 / c8 / 4 / 8

### Maxima_Based_Segmenter_Simple（シンプル版）

```javascript
// 基本的な使用例
run("Maxima_Based_Segmenter_Simple", "bg_threshold=50 tolerance=10");
```

パラメータ：
- `bg_threshold=N`: 背景閾値
- `tolerance=N`: Find Maxima tolerance

### Maxima_Based_Segmenter_3D（3D版）

```javascript
// 基本的な使用例
run("Maxima_Based_Segmenter_3D", "bg_threshold=50 tolerance=10");
```

パラメータ：
- `bg_threshold=N`: 背景閾値
- `tolerance=N`: Extended Maxima tolerance

## プログラマティックAPI

Simple版と3D版は静的メソッドを提供しています。

```java
// Simple版
ImagePlus labelImage = Maxima_Based_Segmenter_Simple_.segment(imp, bgThreshold, tolerance);

// 3D版
ImagePlus labelImage3D = Maxima_Based_Segmenter_3D_.segment(imp, bgThreshold, tolerance);
```

## 主な機能

- Preview
  - `Off`: 計算停止 + Overlay クリア
  - `Seed preview`: Seed（赤） / DOMAIN（緑） / BG（青、デフォルトOFF）の可視化
    - 3D版: 全スライスのシード位置を表示（現在のスライス: 赤、他のスライス: 半透明青色）
  - `ROI boundaries`: 処理後ラベル境界の表示
- 出力
  - **Apply**: ラベル画像（背景=0、前景=1..N）
  - **Add ROI**: ラベルごとに ROI Manager へ追加
  - **Save ROI**: ROI を ZIP ファイルに保存
- パネルを閉じると自動的にプレビューオーバーレイがクリアされます


## アルゴリズム概要

### Watershed

- Seed 付き優先度伝播で `DOMAIN` 内を分割
- Surface は Invert Original / Original / Gradient(Sobel) から選択
- Connectivity（4/8 for 2D, 6 for 3D）に従って伝播

### Random Walker（フル機能版のみ）

- 近傍重み `w = exp(-beta * (Ii - Ij)^2)` で確率を反復更新
- 最大確率ラベルを採用
- 後処理で各ラベルの最大連結成分を保持し、飛び地は背景0へ

### 3D Segmentation

- MorphoLibJ の Extended Maxima で3D局所最大を検出
- Marker-Controlled Watershed 3D でボクセル単位のセグメンテーション
- 6近傍接続性を使用

## 開発者向け（ソースからビルド）

```bash
cd plugin
mvn clean package
```

生成物:

- `plugin/target/Maxima_Based_Segmenter.jar`

インストール:

```bash
cp plugin/target/Maxima_Based_Segmenter.jar /path/to/Fiji.app/plugins/
```

## ドキュメント

- 仕様: `docs/spec.md`
- 決定事項: `docs/decisions.md`
- 手動検証: `docs/verify-manual.md`
- Spec: `.kiro/specs/maxima-based-segmenter-suite/`

## 依存関係

- ImageJ 1.x
- MorphoLibJ (IJPB-plugins) - 3D版で使用

## Note

このプロジェクトはバイブコーディング（vibe coding）で継続的に改善しています。  
運用前には、Releaseノートと仕様ドキュメントの差分確認を推奨します。

