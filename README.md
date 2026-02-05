# Area_Segmentater

Fiji / ImageJ1 向けの 2D 領域分割プラグインです。  
2閾値と複数の Seed ソースを使って、Watershed または Random Walker でラベル画像を生成します。

## 主な機能

- 2閾値 UI（`T_bg <= T_fg` を常に維持）
- Preview
  - `Off`: 計算停止 + Overlay クリア
  - `Seed preview`: Seed/Domain/BG のオーバーレイ表示
  - `ROI boundaries`: 処理後ラベル境界の表示
- Segmentation
  - Watershed (`Surface`: Invert Original / Original / Gradient(Sobel))
  - Random Walker（`beta` 調整可）
- Seed ソース切替
  - Threshold Components / ROI Manager / Binary Image / Find Maxima / Manual Selection
- 前処理（任意）
  - Surface 用 Gaussian (`sigma surface`)
  - Find Maxima 用 Gaussian (`sigma seed`)
- Apply: ラベル画像出力（背景=0、前景=1..N）
- Add ROI: ラベル 1..N を `obj-001` 形式で RoiManager に出力

## ビルド

標準的な Maven 手順でビルドできます。

```bash
mvn -f plugin/pom.xml package
```

生成物:

- `plugin/target/Area_Segmentater.jar`

補助として `scripts/build.sh` / `scripts/verify.sh` も利用できます（任意）。

## インストール

`plugin/target/Area_Segmentater.jar` を `Fiji/plugins/` にコピーしてください。

```bash
cp plugin/target/Area_Segmentater.jar /path/to/Fiji/plugins/
```

その後 Fiji を再起動します。

## GitHub Releases 配布（推奨）

利用者向け配布は GitHub Releases を推奨します。

1. `mvn -f plugin/pom.xml package` で jar を生成
2. Git タグを作成（例: `v0.1.0`）
3. GitHub Release を作成
4. `plugin/target/Area_Segmentater.jar` をアセットとして添付

## 起動

1. Fiji を起動
2. 任意の画像を開く
3. `Plugins > Area_Segmentater` を実行

## 処理アルゴリズム（概要）

### 1. マスク生成

- `FG_SIDE`: 前景側閾値
- `BG_SIDE`: 背景側閾値
- `UNKNOWN`: 2閾値の間
- `DOMAIN`: `BG_SIDE` の外側（分割対象）

`DOMAIN` 外は常に背景 0 として固定されます。

### 2. Seed 生成

選択した Seed ソースから前景 Seed（1..N）を生成します。

- Threshold Components: `FG_SIDE` の連結成分
- ROI Manager: ROI ごとに Seed
- Binary Image: 非ゼロ領域の連結成分
- Find Maxima: 極大点（必要なら事前に Gaussian）
- Manual Selection: 選択 ROI を追加 Seed 化

`DOMAIN` 外の Seed は無視されます（警告ログ）。

### 3. Segmentation

#### Watershed

- Surface は以下から選択:
  - Invert Original（既定）
  - Original
  - Gradient(Sobel)
- 必要時のみ Surface に Gaussian を適用（マスクや Seed には非適用）
- Marker 制御で `DOMAIN` 内を 1..N に割り当て
- 実装の計算手順（概要）:
  1. Seed画素を優先度付きキューへ投入（優先度=Surface値）
  2. キューから最小優先度画素を取り出し、未確定近傍へ同一ラベルを伝播
  3. 伝播先の優先度は `max(現在優先度, 伝播先Surface)` として登録
  4. `DOMAIN` 内が埋まるまで繰り返し、未確定は最終的に背景0へ
  - 近傍は Connectivity（4/8）に従います

#### Random Walker

- 画素強度差に基づく重みで `DOMAIN` 内を Seed ラベルへ割り当て
- `beta` は UI で調整可能
- 必要時のみ Surface（強度画像）に Gaussian を適用
- 後処理として、各ラベルで最大連結成分のみ保持し、飛び地は背景 0 に戻す
- 実装の計算手順（概要）:
  1. Seed画素は確率1.0で固定（自ラベルのみ1、それ以外0）
  2. 非Seed画素は近傍との重み `w = exp(-beta * (Ii - Ij)^2)` で反復更新
  3. 各ラベル確率を正規化し、収束（または反復上限）まで更新
  4. 最終的に最大確率ラベルを採用し、`DOMAIN` 外は背景0固定
  - 近傍は Connectivity（4/8）に従います

### 4. 出力

- Apply: ラベル画像（背景=0、前景=1..N）
- Add ROI: ラベルごとに ROI を抽出して RoiManager へ追加

## 既知の前提

- 2D 専用（3D 非対応）
- Preview Off 時は計算しません
- 処理はアクティブ画像に追従します

## 開発メモ

- 主要コード: `plugin/src/main/java/jp/yourorg/fiji_area_segmentater/`
- 手動確認項目: `docs/verify-manual.md`
- 仕様: `docs/spec.md`, `docs/decisions.md`

## 公開しないファイル

以下はローカル開発補助のため、通常はリポジトリ公開対象にしません。

- `Fiji/`（ローカル実行環境）
- `codex/`, `skills/`, `.codex/`, `agents.md`（エージェント運用用ファイル）

## 注記

- 本プロジェクトは **バイブコーディング（vibe coding）** で反復的に実装・調整しています。
- そのため、厳密運用前には `docs/spec.md` と実装差分を確認し、必要に応じて追加検証してください。
