# Verify Manual

## 確認項目

1) Apply出力のラベル値
- 出力画像の画素値が {0, 1..N} のみであること

2) ROI boundary と Apply の一致
- ROI boundaries表示の境界がApply出力のエリア分割と一致すること

3) Seedプレビュー
- SeedSource切替に応じてSeed表示が更新されること
- DOMAIN表示が任意で切り替えられること

4) 画像切替追従
- アクティブ画像切替でUI/ヒストグラム/閾値/プレビューが崩れないこと

5) Marker Fill Appearance
- Seed色/Domain色とopacity変更がプレビューに反映されること

6) ヒストグラム吸着
- ヒストグラムクリックで近い閾値が吸着し、プレビューが更新されること

7) Seedソース切替
- Threshold/ROI/Binary/FindMaxima/Manualで結果が変化すること
- Threshold以外ではT_fg変更で結果が変わらないこと

8) プレビュー固まり対策
- 矢印連打・ドラッグ後に停止すると更新されること

9) DOMAINとT_bg
- T_bg変更でDOMAINが変わり、結果が変わること
- DOMAIN外seedは無視され警告が出ること

10) Preprocessing
- Enable PreprocessingのON/OFFでSigmaスライダーが有効/無効になること
- SeedSource=Find MaximaのときのみSigma(Seed)が有効になること
- Sigma(surface)変更でWatershed/Random Walkerの結果が変わること

11) Random Walker island removal
- 同一ラベルの飛び地が背景(0)に落ちること
