# Fiji Segmentation Suite

Fiji / ImageJ1 向けのセグメンテーション・定量プラグイン集です。  
現在の配布 jar には、2D/3D セグメンテーション系 4 本と 3D spot 定量系 2 本が入っています。

## Plugins

`Plugins > Segmentation > Maxima Based Segmenter`

- `Maxima_Based_Segmenter`
- `Maxima_Based_Segmenter_Simple`
- `Maxima_Based_Segmenter_3D`
- `Slice_Based_3D_Segmenter`

`Plugins > Segmentation > Spot Quantifier`

- `Seeded Spot Quantifier 3D`
- `Seeded Spot Quantifier 3D Multi`

## Seeded Spot Quantifier 3D

3D スタック内の spot を、`seed threshold` と `area threshold` を使った seeded segmentation で定量します。

主な機能:

- `Target` / `Ch` 選択
- `Seed threshold` / `Area threshold`
- `Min / Max vol` フィルタ
- `Connectivity`
- `Fill holes`
- `Seed preview` / `ROI boundaries` preview
- `Z-proj` 表示
- `Save`, `Save to...`, `Batch...`

保存オプション:

- `Seed ROI`
- `Size ROI`
- `Area ROI`
- `Result ROI`
- `CSV`
- `Param`

デフォルトで有効:

- `Result ROI`
- `CSV`
- `Param`

保存先:

- 通常の `Save` は元画像と同じフォルダ
- `Save to...` は選択フォルダ
- `Custom folder name` を有効にすると `{name} result` などのパターンを使えます

CSV 出力列:

- `spot_id`
- `volume_um3`
- `volume_vox`
- `surface_area_um2`
- `sphericity`
- `integrated_intensity`
- `mean_intensity`
- `max_intensity`
- `centroid_x_um`
- `centroid_y_um`
- `centroid_z_um`

ROI 出力:

- single-channel 画像では `roi.setPosition(z)`
- multi-channel 画像では `roi.setPosition(channel, z, 1)`

## Seeded Spot Quantifier 3D Multi

複数の 3D 画像に同じ条件を一括適用する GUI 版です。

主な機能:

- `Windows...` パネルで対象画像を複数選択
- 各 3D 画像ごとに `Z-proj` を設定
- 共通 `Ch`
- 共通 threshold / filter / preview / save options
- `Apply`, `Save`, `Save to...`

保存形式と CSV 列は single 版と同じです。

## Macro mode

`Seeded Spot Quantifier 3D` は macro からも呼べます。  
現在の macro 保存経路は GUI と揃っており、同じ save helper を使います。

基本例:

```javascript
run("Seeded Spot Quantifier 3D",
    "channel=1 " +
    "area_threshold=200 seed_threshold=500 " +
    "min_vol=0.1 max_vol=50.0 " +
    "gaussian_blur=false gauss_xy=1.0 gauss_z=0.5 " +
    "connectivity=6 fill_holes=false " +
    "save_result_roi=true save_csv=true save_param=true " +
    "output=[C:/path/to/output]");
```

主な macro option:

- `channel`
- `area_threshold`
- `seed_threshold`
- `area_enabled`
- `min_vol`
- `max_vol`
- `gaussian_blur`
- `gauss_xy`
- `gauss_z`
- `connectivity`
- `fill_holes`
- `save_seed_roi`
- `save_size_roi`
- `save_area_roi`
- `save_result_roi`
- `save_csv`
- `save_param`
- `custom_folder`
- `folder_pattern`
- `output`

macro のデフォルト保存設定:

- `save_result_roi=true`
- `save_csv=true`
- `save_param=true`
- `save_seed_roi=false`
- `save_size_roi=false`
- `save_area_roi=false`
- `custom_folder=false`
- `folder_pattern={name} result`

## Build

```bash
cd plugin
mvn -DskipTests package
```

生成物:

- `plugin/target/Fiji_Segmentation_Suite.jar`

Fiji へ配置:

```bash
cp plugin/target/Fiji_Segmentation_Suite.jar /path/to/Fiji.app/plugins/
```

jar を差し替えたあとは Fiji の再起動が必要です。

## Docs

- `docs/spot-quantifier-3d.md`

## Dependencies

- ImageJ 1.x
- MorphoLibJ / IJPB-plugins

## License

MIT. 詳細は [LICENSE](LICENSE) を参照してください。
