# Implementation Plan: Maxima-Based Segmenter Suite

## Overview

このプランは、既存のArea_SegmentaterプラグインをMaxima_Based_Segmenterにリネームし、シンプル版2Dプラグイン（Maxima_Based_Segmenter_Simple）と3D版プラグイン（Maxima_Based_Segmenter_3D）を新規作成する実装手順を定義します。

実装戦略：
- リネームタスクを最初に実行（他のタスクの基盤となる）
- Simple版は既存コードを再利用するため、リネーム後に実装
- 3D版は新規実装が多いため、独立して進められる
- テストタスクは実装タスクの後に配置

## Tasks

- [x] 1. プロジェクトリネーム：パッケージとクラス名の変更
  - [x] 1.1 Javaパッケージ名を変更
    - すべてのJavaソースファイルのパッケージ宣言を `jp.yourorg.fiji_area_segmentater` から `jp.yourorg.fiji_maxima_based_segmenter` に変更
    - すべてのimport文を新しいパッケージ名に更新
    - _Requirements: 1.1, 1.8, 1.9_
  
  - [x] 1.2 プラグインクラス名を変更
    - `DualThresholdMarkers_.java` を `Maxima_Based_Segmenter_.java` にリネーム
    - クラス内のコンストラクタ名とウィンドウタイトルを更新
    - _Requirements: 1.2, 1.3_
  
  - [x] 1.3 Maven設定を更新
    - pom.xmlのartifactIdを `fiji-maxima-based-segmenter` に変更
    - finalNameを `Maxima_Based_Segmenter` に設定
    - _Requirements: 1.5, 1.6_
  
  - [x] 1.4 plugins.configファイルを更新
    - プラグインクラス名を新しい名前に更新
    - メニューパスを "Plugins>Segmentation>Maxima_Based_Segmenter" に設定
    - _Requirements: 1.4, 1.10_
  
  - [ ]* 1.5 リネーム後の動作確認テスト
    - プラグインがFijiメニューに正しく表示されることを確認
    - 既存機能が正常に動作することを確認
    - _Requirements: 1.7_

- [x] 2. リネーム版のデフォルト設定とUI改善
  - [x] 2.1 デフォルト値を変更
    - MarkerSourceのデフォルトを `FIND_MAXIMA` に設定
    - Connectivityのデフォルトを `C4` に設定
    - _Requirements: 16.1, 16.2_

  - [x] 2.2 UIレイアウトを調整
    - FindMaxima Toleranceスライダーをメインパネルに移動
    - MarkerSource選択をAdvancedパネルに移動
    - _Requirements: 16.3, 16.4_

  - [x] 2.3 FG_Threshold動的制御を実装
    - MarkerSourceが `THRESHOLD_COMPONENTS` 以外の場合、FG_Thresholdスライダーを無効化
    - MarkerSourceが `THRESHOLD_COMPONENTS` の場合、BG_Threshold <= FG_Threshold制約を適用
    - _Requirements: 16.5, 16.6, 16.7_

  - [x] 2.4 Renamed版のマクロ対応を実装
    - Macro.getOptions()でマクロパラメータを取得
    - runMacroMode()メソッドを実装
    - パラメータ解析: "bg_threshold=N fg_threshold=N tolerance=N marker_source=SOURCE"
    - 非インタラクティブモードでの実行を実装
    - _Requirements: 19.1, 19.4_
  
  - [x] 2.5 Renamed版のSave ROI機能を実装
    - DualThresholdFrameにsaveRoiBtnのイベントハンドラーを追加
    - FileDialogでZIPファイル保存パスを取得
    - RoiExporter.saveRoisToZip()メソッドを実装
    - _Requirements: 20.9_
  
  - [ ]* 2.6 UI改善の動作確認テスト
    - MarkerSource変更時のFG_Threshold有効/無効切り替えを確認
    - デフォルト値が正しく設定されることを確認
    - Save ROIボタンの動作を確認
    - マクロ呼び出しの動作を確認
    - _Requirements: 16.8, 16.9, 16.10, 16.11, 16.12, 16.13, 16.14_

- [x] 3. Checkpoint - リネーム版の完成確認
  - すべてのテストが通過することを確認し、質問があればユーザーに確認してください。

- [x] 4. 共有コアパッケージの抽出
  - [x] 4.1 coreパッケージを作成
    - `jp.yourorg.fiji_maxima_based_segmenter.core` パッケージを作成
    - Enum型（Connectivity, Method, Surface, PreviewMode, OverlapRule, MarkerSource）を移動
    - ThresholdModel, AppearanceSettingsクラスを移動
    - _Requirements: 15.6_
  
  - [x] 4.2 algパッケージを作成
    - `jp.yourorg.fiji_maxima_based_segmenter.alg` パッケージを作成
    - WatershedRunner, RandomWalkerRunner, SegmentationResultクラスを移動
    - _Requirements: 15.1_
  
  - [x] 4.3 previewパッケージを作成
    - `jp.yourorg.fiji_maxima_based_segmenter.preview` パッケージを作成
    - PreviewRendererクラスを移動
    - _Requirements: 15.4_
  
  - [x] 4.4 roiパッケージを作成
    - `jp.yourorg.fiji_maxima_based_segmenter.roi` パッケージを作成
    - RoiExporterクラスを移動
    - _Requirements: 15.3_
  
  - [x] 4.5 MarkerBuilderクラスを抽出
    - seed検出ロジックをMarkerBuilderクラスとして抽出
    - MarkerResultクラスを作成してseed検出結果を格納
    - _Requirements: 15.2_

- [x] 5. Simple版プラグインの実装
  - [x] 5.1 SimpleSegmenterFrameクラスを作成
    - BG_Thresholdスライダー、Toleranceスライダー、Preview_Mode選択、Apply/Add ROI/Save ROIボタンを配置
    - Histogramパネルを統合
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6_

  - [x] 5.2 ThresholdModelファクトリメソッドを実装
    - `ThresholdModel.createForSimplePlugin()` メソッドを実装
    - 固定パラメータ（C4, WATERSHED, INVERT_ORIGINAL, FIND_MAXIMA）を設定
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5_

  - [x] 5.3 Simple版のセグメンテーション処理を実装
    - MarkerBuilderを使用してseed検出
    - WatershedRunnerを使用してセグメンテーション実行
    - Domain定義（intensity >= BG_Threshold）を実装
    - _Requirements: 3.6, 3.7, 4.1, 4.2, 4.3, 4.4, 4.5, 4.7_

  - [x] 5.4 Simple版のプレビュー機能を実装
    - PreviewRendererを使用してOverlay生成
    - パラメータ変更時のリアルタイム更新を実装
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 5.5 Simple版のROI出力を実装
    - RoiExporterを使用してROI Manager出力
    - ROI命名規則（"obj-001", "obj-002"）を実装
    - Save ROIボタンを追加してZIPファイル出力機能を実装
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 20.1, 20.3, 20.4, 20.5, 20.6, 20.7, 20.8_

  - [x] 5.6 Simple版のマクロ対応を実装
    - マクロパラメータ解析機能を実装（"bg_threshold=N tolerance=N"）
    - 非インタラクティブモードでの実行を実装
    - 静的segmentメソッドを実装してプログラマティックAPIを提供
    - _Requirements: 19.2, 19.4, 19.5, 19.9, 19.11_

  - [x] 5.7 Maxima_Based_Segmenter_Simple_プラグインエントリーポイントを作成
    - PlugInインターフェースを実装
    - 画像存在チェックとエラーハンドリングを実装
    - _Requirements: 2.1-2.13, 3.1-3.7_
  
  - [ ]* 5.8 Simple版のプロパティテストを実装
    - **Property 1: Simple Plugin Domain Definition**
    - **Validates: Requirements 3.7, 4.1**
  
  - [ ]* 5.8 Simple版のプロパティテストを実装
    - **Property 2: Simple Plugin Fixed Parameters**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5**
  
  - [ ]* 5.9 Simple版のプロパティテストを実装
    - **Property 3: Simple Plugin Tolerance Parameter Propagation**
    - **Validates: Requirements 4.2**
  
  - [ ]* 5.10 Simple版のプロパティテストを実装
    - **Property 4: Simple Plugin Label Image Format**
    - **Validates: Requirements 4.4**
  
  - [ ]* 5.11 Simple版のユニットテストを実装
    - UI要素の存在確認テスト
    - デフォルト値検証テスト
    - ROI出力テスト
    - マクロ呼び出しテスト

- [x] 6. Checkpoint - Simple版の完成確認
  - すべてのテストが通過することを確認し、質問があればユーザーに確認してください。

- [x] 7. 3D版プラグインの基盤実装
  - [x] 7.1 MorphoLibJ依存関係を追加
    - pom.xmlにMorphoLibJ（IJPB-plugins）依存関係を追加
    - _Requirements: 13.1, 13.4_

  - [x] 7.2 3D用データモデルを作成
    - MarkerResult3Dクラスを作成（3D seed labels, domain mask, seed count）
    - SegmentationResult3Dクラスを作成（3D label image）
    - _Requirements: 9.5, 12.1, 12.2, 12.3_

  - [x] 7.3 Connectivityに3D対応を追加
    - Connectivity enumにC6を追加
    - `to3D()` メソッドを実装してMorphoLibJ connectivity定数に変換
    - _Requirements: 9.1_

- [x] 8. 3D版のseed検出とセグメンテーション
  - [x] 8.1 MarkerBuilder3Dクラスを実装
    - MorphoLibJ Extended Maximaを使用して3D local maxima検出
    - Domain mask生成（intensity >= BG_Threshold）
    - Connected components labelingでseedラベリング
    - _Requirements: 8.1, 8.2, 8.3, 8.4_

  - [x] 8.2 Watershed3DRunnerクラスを実装
    - MorphoLibJ Marker-Controlled Watershed 3Dを使用
    - Invert_Surface処理を実装
    - C6 connectivityでwatershed実行
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_
  
  - [ ]* 8.3 3D seed検出のプロパティテストを実装
    - **Property 8: 3D Plugin Domain Definition**
    - **Validates: Requirements 8.4**
  
  - [ ]* 8.4 3D seed検出のプロパティテストを実装
    - **Property 9: 3D Seeds Within Domain**
    - **Validates: Requirements 8.3**
  
  - [ ]* 8.5 3Dセグメンテーションのプロパティテストを実装
    - **Property 14: 3D Label Image Format**
    - **Validates: Requirements 9.5, 12.3**

- [x] 9. 3D版のプレビューとROI出力
  - [x] 9.1 PreviewRendererに3D対応を追加
    - `renderMarkerFill3D()` メソッドを実装（現在のZ planeのみ表示）
    - `renderRoiBoundaries3D()` メソッドを実装
    - Z plane変更時の更新処理を実装
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 9.2 RoiExporter3Dクラスを実装
    - 3D label imageから2D ROI slicesを抽出
    - Position属性（Z座標）を設定
    - Group属性（object ID）を設定
    - ROI命名規則（"obj-XXX-zYYY"）を実装
    - Save ROIボタンを追加してZIPファイル出力機能を実装
    - _Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.6, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7, 20.8_
  
  - [ ]* 9.3 3D ROI出力のプロパティテストを実装
    - **Property 16: 3D ROI Slice Generation**
    - **Validates: Requirements 11.2, 11.3**
  
  - [ ]* 9.4 3D ROI出力のプロパティテストを実装
    - **Property 17: 3D ROI Grouping**
    - **Validates: Requirements 11.4**

- [x] 10. 3D版のUIとプラグインエントリーポイント
  - [x] 10.1 Segmenter3DFrameクラスを作成
    - BG_Thresholdスライダー、Toleranceスライダー、Preview_Mode選択、Apply/Add ROI/Save ROIボタンを配置
    - Histogramパネルを統合（現在のZ planeの輝度分布）
    - Z plane変更リスナーを実装
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

  - [x] 10.2 ThresholdModelファクトリメソッドを実装
    - `ThresholdModel.createFor3DPlugin()` メソッドを実装
    - 固定パラメータ（C6, WATERSHED, INVERT_ORIGINAL, FIND_MAXIMA）を設定
    - _Requirements: 9.1, 9.2_

  - [x] 10.3 Maxima_Based_Segmenter_3D_プラグインエントリーポイントを作成
    - PlugInインターフェースを実装
    - 3D stack検証（Z > 1）を実装
    - マクロパラメータ解析機能を実装
    - 静的segmentメソッドを実装してプログラマティックAPIを提供
    - エラーハンドリングを実装
    - _Requirements: 7.7, 7.8, 19.3, 19.4, 19.6, 19.10, 19.11_
  
  - [ ]* 10.4 3D版のユニットテストを実装
    - 3D stack検証テスト
    - UI要素の存在確認テスト
    - Z plane変更時のプレビュー更新テスト
    - マクロ呼び出しテスト

- [x] 11. Checkpoint - 3D版の完成確認
  - すべてのテストが通過することを確認し、質問があればユーザーに確認してください。

- [x] 12. ビルド設定とパッケージング
  - [x] 12.1 plugins.configに全3プラグインを設定
    - 3つのプラグインすべてがplugins.configに登録済み
    - _Requirements: 14.1, 14.2, 14.3, 14.4, 14.5_

  - [x] 12.2 JARファイル生成を確認
    - 単一JAR（Maxima_Based_Segmenter.jar）に3プラグインを同梱
    - `mvn clean package` でビルド成功を確認
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_

- [x] 13. ドキュメント更新
  - [x] 13.1 READMEを更新
    - 3つのプラグインの目的と機能を説明
    - インストール手順を記載
    - 使用例を追加
    - マクロ使用例を追加（Simple版と3D版）
    - _Requirements: 14.6, 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 19.12_
  
  - [x] 13.2 仕様ドキュメントを更新
    - docs/ディレクトリに各プラグインの仕様を追加
    - _Requirements: 18.7_

- [x] 14. 最終統合テストとバリデーション
  - [ ]* 14.1 リネーム版の後方互換性テストを実装
    - **Property 23: Renamed Plugin Backward Compatibility**
    - **Validates: Requirements 16.15**
  
  - [ ]* 14.2 統合テストを実行
    - 3つのプラグインすべてがFijiで正常に動作することを確認
    - サンプル画像でエンドツーエンドテストを実行

- [ ] 15. Final Checkpoint - プロジェクト完成確認
  - すべてのテストが通過し、3つのプラグインが正常に動作することを確認してください。質問があればユーザーに確認してください。

## Notes

- `*` マークのタスクはオプションであり、MVP実装ではスキップ可能です
- 各タスクは特定の要件を参照しており、トレーサビリティを確保しています
- Checkpointタスクは段階的な検証を保証します
- プロパティテストは設計ドキュメントの正確性プロパティを検証します
- ユニットテストは特定の例とエッジケースを検証します
