# Requirements Document

## Introduction

このドキュメントは、既存の Area_Segmentater プロジェクトから派生させる3つの機能拡張の要件を定義します：

1. 既存プロジェクトのリネーム（Area_Segmentater → Maxima_Based_Segmenter）
2. シンプル版2Dプラグイン（Maxima_Based_Segmenter_Simple）の実装
3. 3D版プラグイン（Maxima_Based_Segmenter_3D）の実装

これらの機能により、ユーザーは局所最大値ベースのセグメンテーションを2D/3Dの両方で、用途に応じたUIの複雑さで利用できるようになります。

## Glossary

- **Original_Plugin**: 既存の Area_Segmentater プラグイン
- **Renamed_Plugin**: リネーム後の Maxima_Based_Segmenter プラグイン
- **Simple_Plugin**: 新規作成するシンプル版2Dプラグイン（Maxima_Based_Segmenter_Simple）
- **3D_Plugin**: 新規作成する3D版プラグイン（Maxima_Based_Segmenter_3D）
- **Package_Name**: Javaパッケージの名前空間（例: jp.yourorg.fiji_area_segmentater）
- **Plugin_Class**: ImageJ/Fijiプラグインのエントリーポイントクラス
- **UI_Element**: ユーザーインターフェースの構成要素（スライダー、ボタン、チェックボックス等）
- **BG_Threshold**: 背景閾値。この値以下のピクセルは処理対象外（背景）となる
- **FG_Threshold**: 前景閾値。この値以上のピクセルをseed候補とする（Simple版では使用しない）
- **FindMaxima**: ImageJの局所最大値検出アルゴリズム
- **Tolerance**: FindMaximaのパラメータ。この値より大きい強度差がある局所最大のみを検出
- **Seed**: セグメンテーションの開始点となるマーカー
- **Domain**: セグメンテーション処理の対象領域（BG_Threshold以上の領域）
- **Watershed**: マーカー制御型ウォーターシェッドアルゴリズム
- **Invert_Surface**: 元画像の強度を反転してWatershedの表面とする処理
- **Connectivity**: 隣接判定の方法（C4=4近傍、C8=8近傍、C6=6近傍）
- **ROI**: Region of Interest。ImageJの関心領域オブジェクト
- **ROI_Manager**: ImageJの標準ROI管理ツール
- **Label_Image**: ピクセル値がオブジェクトIDを表す画像（0=背景、1..N=オブジェクト）
- **Preview_Mode**: プレビュー表示の種類（Off/Seed preview/ROI boundaries）
- **Overlay**: 元画像上に重ねて表示される視覚化レイヤー
- **Histogram**: 画像の輝度分布を示すグラフ
- **MorphoLibJ**: ImageJ/Fiji用の形態学的画像処理ライブラリ（IJPB-plugins）
- **Extended_Maxima**: MorphoLibJの3D局所最大値検出機能
- **Marker_Controlled_Watershed_3D**: MorphoLibJの3D marker-controlled watershed機能
- **Voxel**: 3次元画像の体積要素（3Dピクセル）
- **Z_Plane**: 3次元画像のZ軸方向の断面
- **Group**: ROI Managerで同一オブジェクトをグループ化する属性
- **Position**: ROI ManagerでZ座標を指定する属性
- **Maven_Artifact**: Mavenビルドシステムで管理されるプロジェクト成果物

## Requirements

### Requirement 1: プロジェクトリネーム

**User Story:** As a developer, I want to rename the existing Area_Segmentater project to Maxima_Based_Segmenter, so that the naming is consistent, the typo is fixed, and the purpose is clearer.

#### Acceptance Criteria

1. THE Renamed_Plugin SHALL use the package name "jp.yourorg.fiji_maxima_based_segmenter"
2. THE Renamed_Plugin SHALL use the plugin class name "Maxima_Based_Segmenter_"
3. THE Renamed_Plugin SHALL display "Maxima_Based_Segmenter" as the window title
4. THE Renamed_Plugin SHALL appear in Fiji menu as "Plugins > Segmentation > Maxima_Based_Segmenter"
5. THE Maven_Artifact SHALL use the artifact ID "fiji-maxima-based-segmenter"
6. THE Maven_Artifact SHALL produce a JAR file named "Maxima_Based_Segmenter.jar"
7. THE Renamed_Plugin SHALL maintain all existing functionality of the Original_Plugin
8. FOR ALL Java source files, package declarations SHALL be updated to the new package name
9. FOR ALL import statements referencing the old package, imports SHALL be updated to the new package name
10. THE plugins.config file SHALL reference the new plugin class name

### Requirement 2: シンプル版2Dプラグインの基本UI

**User Story:** As a user, I want a simplified 2D plugin with minimal UI elements, so that I can quickly perform maxima-based segmentation without complex configuration.

#### Acceptance Criteria

1. THE Simple_Plugin SHALL display a BG_Threshold slider
2. THE Simple_Plugin SHALL display a FindMaxima Tolerance slider
3. THE Simple_Plugin SHALL display a Preview_Mode selector with options "Off", "Seed preview", and "ROI boundaries"
4. THE Simple_Plugin SHALL display an Apply button
5. THE Simple_Plugin SHALL display an Add ROI button
6. THE Simple_Plugin SHALL display a Histogram of the current image
7. THE Simple_Plugin SHALL NOT display UI_Elements for Connectivity selection
8. THE Simple_Plugin SHALL NOT display UI_Elements for Method selection
9. THE Simple_Plugin SHALL NOT display UI_Elements for Surface selection
10. THE Simple_Plugin SHALL NOT display UI_Elements for Gaussian preprocessing
11. THE Simple_Plugin SHALL NOT display UI_Elements for MarkerSource selection
12. THE Simple_Plugin SHALL NOT display UI_Elements for Invert checkbox
13. THE Simple_Plugin SHALL NOT display UI_Elements for FG_Threshold

### Requirement 3: シンプル版2Dプラグインの固定パラメータ

**User Story:** As a user, I want the Simple_Plugin to use sensible default parameters internally, so that I don't need to configure advanced options.

#### Acceptance Criteria

1. THE Simple_Plugin SHALL use Connectivity C4 for all segmentation operations
2. THE Simple_Plugin SHALL use Watershed as the segmentation method
3. THE Simple_Plugin SHALL use Invert_Surface for the Watershed surface
4. THE Simple_Plugin SHALL NOT apply Gaussian preprocessing to the surface
5. THE Simple_Plugin SHALL use FindMaxima as the Seed source
6. THE Simple_Plugin SHALL derive Seeds exclusively from FindMaxima results
7. THE Simple_Plugin SHALL define Domain as pixels with intensity greater than or equal to BG_Threshold

### Requirement 4: シンプル版2Dプラグインのセグメンテーション動作

**User Story:** As a user, I want the Simple_Plugin to perform maxima-based watershed segmentation, so that I can segment objects based on local intensity peaks.

#### Acceptance Criteria

1. WHEN BG_Threshold is set, THE Simple_Plugin SHALL define Domain as all pixels with intensity >= BG_Threshold
2. WHEN Tolerance is set, THE Simple_Plugin SHALL detect Seeds using FindMaxima with the specified Tolerance
3. WHEN Apply button is clicked, THE Simple_Plugin SHALL execute Marker_Controlled_Watershed with Invert_Surface
4. WHEN Apply button is clicked, THE Simple_Plugin SHALL produce a Label_Image with background=0 and foreground=1..N
5. WHEN multiple Seeds exist in the same connected Domain region, THE Simple_Plugin SHALL subdivide the region using Watershed
6. THE Simple_Plugin SHALL maintain the existing OverlapRule behavior from the Original_Plugin
7. THE Simple_Plugin SHALL apply segmentation only within the Domain region

### Requirement 5: シンプル版2Dプラグインのプレビュー機能

**User Story:** As a user, I want to preview the segmentation results in real-time, so that I can adjust parameters before applying the final segmentation.

#### Acceptance Criteria

1. WHEN Preview_Mode is "Off", THE Simple_Plugin SHALL clear all Overlays and stop preview computation
2. WHEN Preview_Mode is "Seed preview", THE Simple_Plugin SHALL display Seeds, Domain, and background regions as an Overlay
3. WHEN Preview_Mode is "ROI boundaries", THE Simple_Plugin SHALL display segmentation boundaries as an Overlay
4. WHEN BG_Threshold or Tolerance changes, THE Simple_Plugin SHALL update the preview in real-time
5. THE Simple_Plugin SHALL render previews using the same visualization style as the Original_Plugin

### Requirement 6: シンプル版2DプラグインのROI出力

**User Story:** As a user, I want to export segmented regions as ROIs, so that I can use them in further ImageJ analysis workflows.

#### Acceptance Criteria

1. WHEN Add ROI button is clicked and no segmentation exists, THE Simple_Plugin SHALL execute Apply operation first
2. WHEN Add ROI button is clicked, THE Simple_Plugin SHALL export each label (1..N) as a separate ROI to ROI_Manager
3. FOR EACH exported ROI, THE Simple_Plugin SHALL assign a name in the format "obj-001", "obj-002", etc.
4. THE Simple_Plugin SHALL use the same ROI export logic as the Original_Plugin
5. THE Simple_Plugin SHALL provide a Save ROI button to export ROIs as a ZIP file
6. WHEN Save ROI button is clicked, THE Simple_Plugin SHALL prompt the user for a file path
7. WHEN a file path is provided, THE Simple_Plugin SHALL save all ROIs to a ZIP file at the specified location

### Requirement 7: 3D版プラグインの基本UI

**User Story:** As a user, I want a 3D plugin with a similar UI to the Simple_Plugin, so that I can perform maxima-based segmentation on 3D image stacks.

#### Acceptance Criteria

1. THE 3D_Plugin SHALL display a BG_Threshold slider
2. THE 3D_Plugin SHALL display a FindMaxima Tolerance slider for 3D Extended_Maxima
3. THE 3D_Plugin SHALL display a Preview_Mode selector with options "Off", "Seed preview", and "ROI boundaries"
4. THE 3D_Plugin SHALL display an Apply button
5. THE 3D_Plugin SHALL display an Add ROI button
6. THE 3D_Plugin SHALL display a Histogram of the current Z_Plane
7. THE 3D_Plugin SHALL accept XYZ 3D image stacks as input
8. THE 3D_Plugin SHALL NOT require T (time) or C (channel) dimensions

### Requirement 8: 3D版プラグインの3D Seed検出

**User Story:** As a user, I want to detect 3D local maxima as seeds, so that I can identify object centers in 3D space.

#### Acceptance Criteria

1. THE 3D_Plugin SHALL use MorphoLibJ Extended_Maxima to detect 3D local maxima
2. WHEN Tolerance is set, THE 3D_Plugin SHALL pass the Tolerance parameter to Extended_Maxima
3. THE 3D_Plugin SHALL identify Seeds as 3D local maxima within the Domain
4. THE 3D_Plugin SHALL define Domain as Voxels with intensity >= BG_Threshold

### Requirement 9: 3D版プラグインの3Dセグメンテーション

**User Story:** As a user, I want to segment 3D regions based on detected seeds, so that I can identify and separate 3D objects.

#### Acceptance Criteria

1. THE 3D_Plugin SHALL use Connectivity C6 (6-neighborhood) to define adjacent Voxels
2. THE 3D_Plugin SHALL group adjacent Voxels within Domain into connected regions
3. WHEN a connected region contains exactly one Seed, THE 3D_Plugin SHALL assign all Voxels in that region to the Seed's label
4. WHEN a connected region contains multiple Seeds, THE 3D_Plugin SHALL subdivide the region using MorphoLibJ Marker_Controlled_Watershed_3D with Invert_Surface
5. WHEN Apply button is clicked, THE 3D_Plugin SHALL produce a 3D Label_Image with background=0 and foreground=1..N
6. THE 3D_Plugin SHALL process the entire 3D volume, not individual Z_Planes

### Requirement 10: 3D版プラグインのプレビュー機能

**User Story:** As a user, I want to preview 3D segmentation results on the current Z plane, so that I can verify parameters before processing the entire volume.

#### Acceptance Criteria

1. WHEN Preview_Mode is "Off", THE 3D_Plugin SHALL clear all Overlays and stop preview computation
2. WHEN Preview_Mode is "Seed preview", THE 3D_Plugin SHALL display Seeds, Domain, and background on the current Z_Plane as an Overlay
3. WHEN Preview_Mode is "ROI boundaries", THE 3D_Plugin SHALL display segmentation boundaries on the current Z_Plane as an Overlay
4. WHEN the user changes the Z_Plane, THE 3D_Plugin SHALL update the Overlay to show the new Z_Plane
5. WHEN BG_Threshold or Tolerance changes, THE 3D_Plugin SHALL update the preview in real-time

### Requirement 11: 3D版プラグインのROI出力

**User Story:** As a user, I want to export 3D segmented regions as 2D ROI slices with position and group information, so that I can use ImageJ's standard ROI_Manager for 3D objects.

#### Acceptance Criteria

1. WHEN Add ROI button is clicked and no segmentation exists, THE 3D_Plugin SHALL execute Apply operation first
2. FOR EACH label (1..N) in the 3D Label_Image, THE 3D_Plugin SHALL create 2D ROIs for each Z_Plane containing that label
3. FOR EACH 2D ROI, THE 3D_Plugin SHALL set the Position attribute to the corresponding Z coordinate
4. FOR EACH 2D ROI belonging to the same 3D object, THE 3D_Plugin SHALL set the Group attribute to the same segment ID
5. THE 3D_Plugin SHALL add all generated ROIs to the ROI_Manager
6. FOR EACH exported ROI, THE 3D_Plugin SHALL assign a name indicating the object ID and Z position

### Requirement 12: 3D版プラグインのラベル画像出力

**User Story:** As a user, I want to export the 3D segmentation result as a label image stack, so that I can use it for quantitative analysis.

#### Acceptance Criteria

1. WHEN Apply button is clicked, THE 3D_Plugin SHALL produce a 3D Label_Image stack
2. THE 3D Label_Image SHALL have the same XYZ dimensions as the input image
3. FOR EACH Voxel in the Label_Image, the value SHALL be 0 for background or 1..N for object labels
4. THE 3D_Plugin SHALL display the Label_Image as a new ImagePlus window

### Requirement 13: MorphoLibJ依存関係

**User Story:** As a developer, I want to declare MorphoLibJ as a Maven dependency, so that the 3D_Plugin can use its 3D morphological operations.

#### Acceptance Criteria

1. THE 3D_Plugin project SHALL include MorphoLibJ (IJPB-plugins) as a Maven dependency
2. THE 3D_Plugin SHALL use MorphoLibJ's Extended_Maxima for 3D local maxima detection
3. THE 3D_Plugin SHALL use MorphoLibJ's Marker_Controlled_Watershed_3D for 3D watershed segmentation
4. THE Maven_Artifact for 3D_Plugin SHALL package with appropriate dependency scope for MorphoLibJ

### Requirement 14: プロジェクト構成

**User Story:** As a developer, I want a clear project structure that separates the renamed plugin, simple 2D plugin, and 3D plugin, so that the codebase is maintainable.

#### Acceptance Criteria

1. THE Renamed_Plugin SHALL reside in the existing project directory with updated package names
2. THE Simple_Plugin SHALL reside in the same repository as the Renamed_Plugin
3. THE Simple_Plugin SHALL reuse core algorithm classes from the Renamed_Plugin where possible
4. THE 3D_Plugin SHALL be implemented as a separate module or package within the same repository
5. THE project SHALL maintain a single Maven multi-module structure OR separate Maven projects with shared dependencies
6. THE project README SHALL document all three plugins and their purposes

### Requirement 15: コード再利用

**User Story:** As a developer, I want to maximize code reuse between the plugins, so that maintenance effort is minimized.

#### Acceptance Criteria

1. THE Simple_Plugin SHALL reuse the Watershed algorithm implementation from the Renamed_Plugin
2. THE Simple_Plugin SHALL reuse the FindMaxima integration from the Renamed_Plugin
3. THE Simple_Plugin SHALL reuse the ROI export logic from the Renamed_Plugin
4. THE Simple_Plugin SHALL reuse the preview rendering logic from the Renamed_Plugin
5. THE 3D_Plugin SHALL implement new 3D-specific algorithms while following similar architectural patterns
6. FOR ALL shared utility classes, code SHALL be placed in a common package accessible to all plugins

### Requirement 16: リネーム版のデフォルト設定とUI改善

**User Story:** As a user, I want the Renamed_Plugin to have sensible defaults for maxima-based segmentation and improved UI behavior, so that the plugin name matches its primary use case.

#### Acceptance Criteria

1. THE Renamed_Plugin SHALL use MarkerSource FIND_MAXIMA as the default
2. THE Renamed_Plugin SHALL use Connectivity C4 as the default
3. THE Renamed_Plugin SHALL display FindMaxima Tolerance in the main UI area (not in Advanced panel)
4. THE Renamed_Plugin SHALL move MarkerSource selection to the Advanced panel
5. WHEN MarkerSource is not THRESHOLD_COMPONENTS, THE Renamed_Plugin SHALL disable (gray out) the FG_Threshold UI_Element
6. WHEN FG_Threshold is disabled, THE Renamed_Plugin SHALL allow BG_Threshold to be adjusted independently without constraint from FG_Threshold
7. WHEN MarkerSource is THRESHOLD_COMPONENTS, THE Renamed_Plugin SHALL enforce the constraint BG_Threshold <= FG_Threshold
8. THE Renamed_Plugin SHALL maintain Method WATERSHED and Surface INVERT_ORIGINAL as defaults
9. THE Renamed_Plugin SHALL support all MarkerSource options from the Original_Plugin
10. THE Renamed_Plugin SHALL support both Watershed and Random Walker methods
11. THE Renamed_Plugin SHALL support all Surface options (Invert Original, Original, Gradient Sobel)
12. THE Renamed_Plugin SHALL support both C4 and C8 Connectivity
13. THE Renamed_Plugin SHALL support Gaussian preprocessing options
14. THE Renamed_Plugin SHALL support all Preview_Mode options
15. THE Renamed_Plugin SHALL produce identical segmentation results to the Original_Plugin given the same parameters and MarkerSource

### Requirement 17: ビルドとパッケージング

**User Story:** As a developer, I want to build all plugins with a single Maven command, so that the build process is simple and consistent.

#### Acceptance Criteria

1. THE project SHALL provide a Maven build configuration that compiles all plugins
2. WHEN "mvn package" is executed, THE build SHALL produce separate JAR files for each plugin
3. THE Renamed_Plugin JAR SHALL be named "Maxima_Based_Segmenter.jar"
4. THE Simple_Plugin JAR SHALL be named "Maxima_Based_Segmenter_Simple.jar"
5. THE 3D_Plugin JAR SHALL be named "Maxima_Based_Segmenter_3D.jar"
6. FOR EACH JAR file, the plugins.config SHALL be correctly configured for Fiji plugin discovery

### Requirement 19: マクロ対応とプログラマティックAPI

**User Story:** As a macro developer, I want to call the segmentation plugins programmatically from ImageJ macros, so that I can automate batch processing workflows.

#### Acceptance Criteria

1. THE Renamed_Plugin SHALL support ImageJ macro invocation via `run("Maxima_Based_Segmenter", "parameters")`
2. THE Simple_Plugin SHALL support ImageJ macro invocation via `run("Maxima_Based_Segmenter_Simple", "parameters")`
3. THE 3D_Plugin SHALL support ImageJ macro invocation via `run("Maxima_Based_Segmenter_3D", "parameters")`
4. FOR ALL plugins, macro parameters SHALL be specified as space-separated key=value pairs
5. THE Simple_Plugin macro interface SHALL accept parameters: `bg_threshold=N tolerance=N preview=MODE`
6. THE 3D_Plugin macro interface SHALL accept parameters: `bg_threshold=N tolerance=N preview=MODE`
7. WHEN a plugin is invoked from a macro, THE plugin SHALL run in non-interactive mode (no UI display)
8. WHEN a plugin is invoked from a macro, THE plugin SHALL automatically execute segmentation and export results
9. THE Simple_Plugin SHALL provide a static method for programmatic invocation: `MaximaBasedSegmenterSimple.segment(ImagePlus, int bgThreshold, double tolerance)`
10. THE 3D_Plugin SHALL provide a static method for programmatic invocation: `MaximaBasedSegmenter3D.segment(ImagePlus, int bgThreshold, double tolerance)`
11. FOR ALL static segmentation methods, the return value SHALL be the label ImagePlus
12. THE plugins SHALL document macro usage examples in the README

**User Story:** As a user, I want updated documentation that describes all three plugins, so that I can understand how to use each one.

#### Acceptance Criteria

1. THE project README SHALL describe the purpose and features of the Renamed_Plugin
2. THE project README SHALL describe the purpose and features of the Simple_Plugin
3. THE project README SHALL describe the purpose and features of the 3D_Plugin
4. THE project README SHALL provide installation instructions for all three plugins
5. THE project README SHALL provide usage examples for all three plugins
6. THE project SHALL include updated specification documents in the docs/ directory for each plugin


### Requirement 18: ドキュメント更新

**User Story:** As a user, I want updated documentation that describes all three plugins, so that I can understand how to use each one.

#### Acceptance Criteria

1. THE project README SHALL describe the purpose and features of the Renamed_Plugin
2. THE project README SHALL describe the purpose and features of the Simple_Plugin
3. THE project README SHALL describe the purpose and features of the 3D_Plugin
4. THE project README SHALL provide installation instructions for all three plugins
5. THE project README SHALL provide usage examples for all three plugins
6. THE project README SHALL provide macro usage examples for Simple_Plugin and 3D_Plugin
7. THE project SHALL include updated specification documents in the docs/ directory for each plugin

### Requirement 20: ROI ZIP出力機能

**User Story:** As a user, I want to save ROIs as a ZIP file, so that I can easily share or archive segmentation results.

#### Acceptance Criteria

1. THE Simple_Plugin SHALL display a Save ROI button in the UI
2. THE 3D_Plugin SHALL display a Save ROI button in the UI
3. WHEN Save ROI button is clicked and no segmentation exists, THE plugin SHALL execute Apply operation first
4. WHEN Save ROI button is clicked, THE plugin SHALL prompt the user for a file path using a file chooser dialog
5. WHEN a file path is provided, THE plugin SHALL save all ROIs from ROI_Manager to a ZIP file at the specified location
6. THE ZIP file SHALL use ImageJ's standard ROI ZIP format (compatible with ROI_Manager's "Save" function)
7. IF the file path does not end with ".zip", THE plugin SHALL automatically append ".zip" extension
8. IF the save operation fails, THE plugin SHALL display an error message to the user
9. THE Renamed_Plugin SHALL also provide a Save ROI button with the same functionality
