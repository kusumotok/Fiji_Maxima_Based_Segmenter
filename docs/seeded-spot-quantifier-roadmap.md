# Seeded Spot Quantifier 3D — 今後の実装ロードマップ

最終更新: 2026-04-15

---

## 背景

`Seeded_Spot_Quantifier_3D_` は現在、プラグイン起動時にアクティブだった `ImagePlus` を
コンストラクタで受け取り、その後は固定して動作する。

これにより以下の制限がある：
- 起動後に別の画像に切り替えたい場合、プラグインを閉じて再起動する必要がある
- マルチチャネル画像を開いたとき、チャネルが混在した状態で誤処理される（無言で失敗）
- スライダーの min/max が起動時画像の輝度範囲に固定されるため、別画像では適切でない

---

## Feature 1: 処理ウィンドウセレクター

### やりたいこと

ヒストグラムの上に「Processing image」のプルダウンを追加し、  
現在開いているマルチスライス画像から対象を選べるようにする。

```
[Processing image: (Choice) ▼] [Ch: (Choice) ▼]   ← multi-channel時のみChが表示
───────────────────────────────────────
[Histogram]
...
```

選択を変えると：
- ヒストグラム表示が新画像に切り替わる
- Seed/Area threshold スライダーの範囲が新画像の輝度範囲に更新される
- セグメンテーションキャッシュがクリアされる
- Z-proj の候補リストが新画像のXYに合う2D画像に更新される
- Calibration（µm/voxel）が新画像のものに更新される

### 表示ルール

| ドロップダウン | 表示内容 |
|--------------|---------|
| Processing image | `getNSlices() >= 2` な全 ImagePlus（None含む） |
| Ch（multi-ch時のみ） | 1 〜 `nChannels` のチャネル番号 |
| Z-proj | 処理画像と同じ XY の `getNSlices() == 1` な ImagePlus（None含む） |

### 現在の問題（マルチチャネル画像）

`SeededQuantifier3D.compute()` 内で `blurred.getNSlices()` のみ参照しているため、
2ch × 10z のハイパースタック（実スタック枚数=20）を渡すと：

- z=1..10 でアクセスされるスライスが ch1/ch2 交互になり**チャネルが混在**
- Z方向が半分しか処理されない

対策として、チャネル選択後に単一チャネルを抽出した `ImagePlus` を処理に渡す。

```java
// チャネル抽出ユーティリティ（案）
private static ImagePlus extractChannel(ImagePlus hyper, int ch) {
    // Duplicator で ch のみのスタックを複製して返す
    return new ij.plugin.Duplicator().run(hyper, ch, ch, 1, hyper.getNSlices(), 1, 1);
}
```

### 実装上の変更点

現在 `imp` / `vw` / `vh` / `vd` / `voxelVol` / `model` はすべて `final`。  
以下のように非 final 化し、`changeTarget(ImagePlus rawImp, int channel)` メソッドで一括更新する設計にする。

```java
// 変更前（現状）
private final ImagePlus imp;
private final double vw, vh, vd, voxelVol;
private final ThresholdModel model;
private final HistogramPanel histogramPanel;

// 変更後（案）
private ImagePlus imp;          // 処理対象（単一チャネル抽出済み）
private ImagePlus rawImp;       // 選択中の元画像（ハイパースタックそのもの）
private int       selectedCh;   // 選択中チャネル（単一チャネルなら常に1）
private double    vw, vh, vd, voxelVol;
private ThresholdModel model;
// histogramPanel はレイアウトに add 済みなので、新 imp への切り替えメソッドを HistogramPanel に追加
```

`changeTarget()` の処理手順：
1. `imp` / `rawImp` / `selectedCh` を更新
2. calibration を再計算
3. `ThresholdModel` を再生成
4. `histogramPanel.setImage(imp, model)` で更新（HistogramPanel に新メソッド追加）
5. threshold スライダーの min/max を新画像の輝度範囲に更新
6. セグメンテーションキャッシュをクリア
7. Z-proj choice リストを更新（`refreshZProjChoiceItems()`）
8. プレビューをクリア

### 拡張性: T 軸対応

T（時系列）軸への拡張は別プラグインで対応予定。  
設計上の考慮点：
- 処理単位が「1 つの Z スタック」であることを `SeededQuantifier3D.compute()` の API として維持する
- T 軸のループは呼び出し側（別 class / 別プラグイン）で担当
- `saveOneToDir()` が `ImagePlus` 単位で完結しているため、T 軸プラグインからそのまま呼べる

---

## Feature 2: Threshold スライダー範囲の自動更新

### やりたいこと

処理ウィンドウを切り替えたとき、スライダーの min/max が新画像の輝度範囲に追従する。

### 現状

```java
// コンストラクタ内（一度だけ設定）
int imgMin = model.getMinValue();
int imgMax = model.getMaxValue();
areaThreshBar = new Scrollbar(Scrollbar.HORIZONTAL, areaThreshold, 1, imgMin, imgMax + 1);
seedThreshBar = new Scrollbar(Scrollbar.HORIZONTAL, seedThreshold, 1, imgMin, imgMax + 1);
```

`Scrollbar` は `setMinimum()` / `setMaximum()` をサポートしないため、  
**既存の Scrollbar を削除して新しい Scrollbar に差し替える**か、  
スライダー行全体を再ビルドするアプローチが必要。

### 実装案（スライダー再差し替え）

```java
private void updateThreshSliderRanges(int imgMin, int imgMax) {
    int newAt = Math.max(imgMin, Math.min(imgMax, areaThreshold));
    int newSt = Math.max(imgMin, Math.min(imgMax, seedThreshold));

    // 古い Scrollbar をリスナーごと入れ替え
    replaceScrollbar(areaThreshBar, newAt, imgMin, imgMax);
    replaceScrollbar(seedThreshBar, newSt, imgMin, imgMax);

    areaThreshold = newAt;
    seedThreshold = newSt;
    areaThreshField.setText(Integer.toString(areaThreshold));
    seedThreshField.setText(Integer.toString(seedThreshold));
}
```

あるいはシンプルに threshold 値を範囲外なら clamp する程度でも十分かもしれない。  
→ 実装時にユーザーと確認する。

---

## 実装優先順位

| # | 内容 | 依存 | 優先度 |
|---|------|------|--------|
| 1 | Processing image Choice + Reload ボタン | なし | 高 |
| 2 | changeTarget() による imp 切り替え全体 | 1 | 高 |
| 3 | Threshold スライダー範囲の更新 | 2 | 高（1と同時） |
| 4 | チャネルセレクター（multi-ch 時のみ表示） | 2 | 中 |
| 5 | HistogramPanel に setImage() 追加 | 2 | 高（2と同時） |
| 6 | T 軸プラグインの設計（実装なし） | なし | 低 |

---

## UI レイアウト（変更後のイメージ）

```
┌─ Seeded Spot Quantifier 3D ──────────────────────────┐
│ Processing image: [Series001.tif ▼] [Reload]          │
│ Ch: [1 ▼]                        ← multi-ch時のみ表示 │
│ ┌─ Histogram ──────────────────────────────────────┐  │
│ └──────────────────────────────────────────────────┘  │
│ Seed threshold:                                        │
│   [═══════════════════════════════════] [  500]        │
│ ☑ Min vol µm³ (seed):                                  │
│   [═══════════════════════════════════] [ 0.10]        │
│ ☑ Max vol µm³ (seed):                                  │
│   [═══════════════════════════════════] [50.0 ]        │
│ ☑ Area threshold:                                      │
│   [═══════════════════════════════════] [  200]        │
│ Connectivity: [6▼]  ☐ Fill holes                       │
│ Preview: ○ Off  ● Overlay  ○ ROI                       │
│ Colors: Seed: [Purple▼] Area/ROI: [Yellow▼] Opacity: [50] │
│ Z-proj: [None ══════════════════════════════] [Reload] │
│ ▼ Save options                                         │
│   [Select All] [Deselect All]                          │
│   ☑ Seed ROI ☐ Size ROI ☑ Area ROI ☑ Result ROI ☑ CSV ☑ Param │
│   ☐ Custom folder name: [{name} result     ]           │
│      tokens: {name} {date} {seed} {area}               │
│ Press Apply to update                                  │
│                          [Apply] [Save All] [Batch…]   │
└────────────────────────────────────────────────────────┘
```

---

## 関連ファイル

| ファイル | 変更内容 |
|---------|---------|
| `ui/SeededSpotQuantifier3DFrame.java` | Processing image Choice 追加、changeTarget() 実装 |
| `ui/HistogramPanel.java` | `setImage(ImagePlus, ThresholdModel)` メソッド追加 |
| `Seeded_Spot_Quantifier_3D_.java` | 起動時に imp=null（または最初の候補）で起動するよう変更検討 |
| `alg/SeededQuantifier3D.java` | 変更なし（単一チャネル ImagePlus を受け取る API を維持） |

---

## 備考

- `Seeded_Spot_Quantifier_3D_` エントリポイントの `run(String)` は現在アクティブウィンドウを取得して
  フレームに渡している。Processing image セレクターが実装されると、起動時のアクティブウィンドウは
  「初期選択値」として設定するだけでよくなる。
- マルチチャネル画像のチャネル抽出は `Duplicator` でコピーを作る方式にすることで、
  元画像を汚さず、かつ既存の `computeCCFromBlurred` / `SpotMeasurer` のインターフェイスを変更しない。
- Z-proj の `refreshZProjChoiceItems()` はすでに実装済み。処理画像変更時にも呼ぶだけでよい。
