package jp.yourorg.fiji_maxima_based_segmenter.ui;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.frame.PlugInFrame;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.alg.Watershed3DRunner;
import jp.yourorg.fiji_maxima_based_segmenter.core.*;
import jp.yourorg.fiji_maxima_based_segmenter.preview.PreviewRenderer;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter3D;

import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class Segmenter3DFrame extends PlugInFrame {
    private final ImagePlus imp;
    private final ThresholdModel model;

    private final Scrollbar bgBar;
    private final TextField bgField;

    private final Scrollbar toleranceBar;
    private final TextField toleranceField;

    private final CheckboxGroup previewGroup = new CheckboxGroup();
    private final Checkbox previewOff;
    private final Checkbox previewMarker;
    private final Checkbox previewRoi;

    private final Button applyBtn = new Button("Apply");
    private final Button addRoiBtn = new Button("Add ROI");
    private final Button saveRoiBtn = new Button("Save ROI");

    private boolean syncing = false;
    private final MarkerBuilder3D markerBuilder3D = new MarkerBuilder3D();
    private final Watershed3DRunner watershed3DRunner = new Watershed3DRunner();
    private final PreviewRenderer previewRenderer = new PreviewRenderer();
    private final RoiExporter3D roiExporter3D = new RoiExporter3D();

    private MarkerResult3D cachedMarkers;
    private SegmentationResult3D cachedSegmentation;
    private String markerCacheKey;
    private String segmentationCacheKey;
    private int lastZPlane = -1;

    private final HistogramPanel histogramPanel;
    private final Timer previewTimer = new Timer("3d-preview", true);
    private TimerTask previewTask;
    private final AtomicInteger previewGen = new AtomicInteger();
    private final Timer zWatchTimer = new Timer("3d-z-watch", true);

    public Segmenter3DFrame(ImagePlus imp) {
        super("Maxima_Based_Segmenter_3D");
        this.imp = imp;
        this.model = ThresholdModel.createFor3DPlugin(imp);

        int min = model.getMinValue();
        int max = model.getMaxValue();
        if (max <= min) max = min + 1;

        bgBar = new Scrollbar(Scrollbar.HORIZONTAL, model.getTBg(), 1, min, max + 1);
        bgField = new TextField(Integer.toString(model.getTBg()), 5);

        toleranceBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 101);
        toleranceField = new TextField(Double.toString(model.getFindMaximaTolerance()), 6);

        previewOff = new Checkbox("Off", previewGroup, model.getPreviewMode() == PreviewMode.OFF);
        previewMarker = new Checkbox("Seed preview", previewGroup, model.getPreviewMode() == PreviewMode.MARKER_BOUNDARIES);
        previewRoi = new Checkbox("ROI boundaries", previewGroup, model.getPreviewMode() == PreviewMode.ROI_BOUNDARIES);

        histogramPanel = new HistogramPanel(imp, model, this::onHistogramThreshold);
        histogramPanel.setFgEnabled(false);

        buildLayout();
        wireEvents();
        startZWatch();
        syncAllFromModel();
        pack();
        placeNearImage();
    }

    private void placeNearImage() {
        Window active = imp.getWindow();
        if (active == null) return;
        Point p;
        try {
            p = active.getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            return;
        }
        int x = p.x + active.getWidth() + 8;
        int y = p.y;
        Rectangle screen = active.getGraphicsConfiguration() != null
            ? active.getGraphicsConfiguration().getBounds()
            : new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        if (x + getWidth() > screen.x + screen.width) x = screen.x + screen.width - getWidth();
        if (x < screen.x) x = screen.x;
        if (y + getHeight() > screen.y + screen.height) y = screen.y + screen.height - getHeight();
        if (y < screen.y) y = screen.y;
        setLocation(x, y);
    }

    private void buildLayout() {
        setLayout(new BorderLayout());

        Panel top = new Panel(new BorderLayout());
        top.add(histogramPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        Panel center = new Panel(new GridLayout(0, 1));

        // BG threshold row
        Panel bgRow = new Panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        bgRow.add(new Label("Background:"), c);
        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        bgBar.setPreferredSize(new Dimension(260, 18));
        bgRow.add(bgBar, c);
        c.gridx = 2;
        c.weightx = 0.0;
        c.fill = GridBagConstraints.NONE;
        bgRow.add(bgField, c);

        // Tolerance row
        Panel tolRow = new Panel(new GridBagLayout());
        GridBagConstraints tc = new GridBagConstraints();
        tc.gridy = 0;
        tc.insets = new Insets(2, 4, 2, 4);
        tc.anchor = GridBagConstraints.WEST;
        tc.gridx = 0;
        tolRow.add(new Label("Tolerance:"), tc);
        tc.gridx = 1;
        tc.weightx = 1.0;
        tc.fill = GridBagConstraints.HORIZONTAL;
        toleranceBar.setPreferredSize(new Dimension(260, 18));
        tolRow.add(toleranceBar, tc);
        tc.gridx = 2;
        tc.weightx = 0.0;
        tc.fill = GridBagConstraints.NONE;
        tolRow.add(toleranceField, tc);

        // Preview row
        Panel previewPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        previewPanel.add(new Label("Preview:"));
        previewPanel.add(previewOff);
        previewPanel.add(previewMarker);
        previewPanel.add(previewRoi);

        center.add(bgRow);
        center.add(tolRow);
        center.add(previewPanel);
        add(center, BorderLayout.CENTER);

        // Buttons
        Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(applyBtn);
        buttons.add(addRoiBtn);
        buttons.add(saveRoiBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void wireEvents() {
        bgBar.addAdjustmentListener(e -> {
            if (syncing) return;
            model.setTBg(bgBar.getValue());
            clearCaches();
            syncAllFromModel();
            onStateChanged();
        });
        bgField.addActionListener(e -> commitBgField());
        bgField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitBgField(); }
        });

        toleranceBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = toleranceBar.getValue();
            model.setFindMaximaTolerance(v);
            toleranceField.setText(Double.toString(v));
            clearCaches();
            onStateChanged();
        });
        toleranceField.addActionListener(e -> commitToleranceField());
        toleranceField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitToleranceField(); }
        });

        ItemListener previewListener = e -> {
            if (syncing) return;
            if (previewGroup.getSelectedCheckbox() == previewOff) model.setPreviewMode(PreviewMode.OFF);
            else if (previewGroup.getSelectedCheckbox() == previewMarker) model.setPreviewMode(PreviewMode.MARKER_BOUNDARIES);
            else model.setPreviewMode(PreviewMode.ROI_BOUNDARIES);
            onStateChanged();
        };
        previewOff.addItemListener(previewListener);
        previewMarker.addItemListener(previewListener);
        previewRoi.addItemListener(previewListener);

        applyBtn.addActionListener(e -> runApply());
        addRoiBtn.addActionListener(e -> runAddRoi());
        saveRoiBtn.addActionListener(e -> runSaveRoi());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                clearOverlay();
                stopZWatch();
                dispose();
            }
        });
    }

    private void startZWatch() {
        lastZPlane = imp.getCurrentSlice();
        zWatchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int z = imp.getCurrentSlice();
                if (z != lastZPlane) {
                    lastZPlane = z;
                    EventQueue.invokeLater(() -> {
                        histogramPanel.repaint();
                        // Z-plane change: update preview immediately without debounce
                        onZPlaneChanged();
                    });
                }
            }
        }, 300, 300);
    }

    private void stopZWatch() {
        zWatchTimer.cancel();
    }

    private void onStateChanged() {
        if (model.getPreviewMode() == PreviewMode.OFF) {
            clearOverlay();
            cancelPreview();
            return;
        }
        schedulePreview(model.getPreviewMode());
    }

    private void onZPlaneChanged() {
        // Z-plane change: update preview immediately without debounce (data is already cached)
        if (model.getPreviewMode() == PreviewMode.OFF) {
            clearOverlay();
            return;
        }
        updatePreviewImmediate(model.getPreviewMode());
    }

    private void updatePreviewImmediate(PreviewMode mode) {
        int zPlane = imp.getCurrentSlice();
        if (mode == PreviewMode.MARKER_BOUNDARIES) {
            if (cachedMarkers != null) {
                previewRenderer.renderMarkerFill3D(imp, cachedMarkers, zPlane, model.getAppearance(), model.getMarkerSource());
            }
        } else if (mode == PreviewMode.ROI_BOUNDARIES) {
            if (cachedSegmentation != null) {
                previewRenderer.renderRoiBoundaries3D(imp, cachedSegmentation, zPlane);
            }
        }
    }

    private void runApply() {
        SegmentationResult3D result = getSegmentationResult();
        if (result == null || result.labelImage == null) return;
        result.labelImage.show();
    }

    private void runAddRoi() {
        SegmentationResult3D result = getSegmentationResult();
        if (result == null || result.labelImage == null) return;
        roiExporter3D.exportToRoiManager(result.labelImage);
    }

    private void runSaveRoi() {
        SegmentationResult3D result = getSegmentationResult();
        if (result == null || result.labelImage == null) {
            IJ.error("Save ROI", "No segmentation result available.");
            return;
        }
        ij.plugin.frame.RoiManager rm = ij.plugin.frame.RoiManager.getInstance();
        if (rm == null || rm.getCount() == 0) {
            roiExporter3D.exportToRoiManager(result.labelImage);
        }
        FileDialog fd = new FileDialog(this, "Save ROIs as ZIP", FileDialog.SAVE);
        fd.setFile("RoiSet.zip");
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String name = fd.getFile();
        if (dir == null || name == null) return;
        RoiExporter.saveRoiManagerToZip(dir + name);
    }

    // --- Segmentation ---

    private synchronized MarkerResult3D getMarkerResult() {
        String key = createCacheKey();
        if (markerCacheKey != null && markerCacheKey.equals(key) && cachedMarkers != null) {
            return cachedMarkers;
        }
        cachedMarkers = markerBuilder3D.build(
            imp, model.getTBg(), model.getFindMaximaTolerance(), model.getConnectivity()
        );
        markerCacheKey = key;
        return cachedMarkers;
    }

    private synchronized SegmentationResult3D getSegmentationResult() {
        String key = createCacheKey();
        if (segmentationCacheKey != null && segmentationCacheKey.equals(key) && cachedSegmentation != null) {
            return cachedSegmentation;
        }
        MarkerResult3D markers = getMarkerResult();
        if (markers.getSeedCount() == 0) return null;
        cachedSegmentation = watershed3DRunner.run(imp, markers, model.getConnectivity());
        segmentationCacheKey = key;
        return cachedSegmentation;
    }

    private String createCacheKey() {
        return imp.getID() + ":" + model.getTBg() + ":" + model.getFindMaximaTolerance();
    }

    private void clearCaches() {
        cachedMarkers = null;
        cachedSegmentation = null;
        markerCacheKey = null;
        segmentationCacheKey = null;
    }

    // --- Preview ---

    private void schedulePreview(PreviewMode mode) {
        if (mode == PreviewMode.OFF) return;
        int gen = previewGen.incrementAndGet();
        cancelPreviewTaskOnly();
        int zPlane = imp.getCurrentSlice();
        previewTask = new TimerTask() {
            @Override
            public void run() {
                if (mode == PreviewMode.MARKER_BOUNDARIES) {
                    MarkerResult3D markers = getMarkerResult();
                    if (previewGen.get() != gen) return;
                    EventQueue.invokeLater(() -> {
                        if (previewGen.get() != gen || model.getPreviewMode() == PreviewMode.OFF) return;
                        previewRenderer.renderMarkerFill3D(imp, markers, zPlane, model.getAppearance(), model.getMarkerSource());
                    });
                } else {
                    SegmentationResult3D result = getSegmentationResult();
                    if (previewGen.get() != gen) return;
                    EventQueue.invokeLater(() -> {
                        if (previewGen.get() != gen || model.getPreviewMode() == PreviewMode.OFF) return;
                        previewRenderer.renderRoiBoundaries3D(imp, result, zPlane);
                    });
                }
            }
        };
        int delay = Math.max(50, model.getPreviewDebounceMs());
        previewTimer.schedule(previewTask, delay);
    }

    private void cancelPreview() {
        previewGen.incrementAndGet();
        cancelPreviewTaskOnly();
    }

    private void cancelPreviewTaskOnly() {
        if (previewTask != null) {
            previewTask.cancel();
            previewTask = null;
        }
    }

    // --- Sync ---

    private void syncAllFromModel() {
        syncing = true;
        try {
            int min = model.getMinValue();
            int max = model.getMaxValue();
            if (max <= min) max = min + 1;
            bgBar.setMinimum(min);
            bgBar.setMaximum(max + 1);
            bgBar.setValue(model.getTBg());
            bgField.setText(Integer.toString(model.getTBg()));
            updateToleranceRange();
            toleranceBar.setValue((int) Math.round(model.getFindMaximaTolerance()));
            toleranceField.setText(Double.toString(model.getFindMaximaTolerance()));
            previewOff.setState(model.getPreviewMode() == PreviewMode.OFF);
            previewMarker.setState(model.getPreviewMode() == PreviewMode.MARKER_BOUNDARIES);
            previewRoi.setState(model.getPreviewMode() == PreviewMode.ROI_BOUNDARIES);
            histogramPanel.repaint();
        } finally {
            syncing = false;
        }
    }

    private void commitBgField() {
        if (syncing) return;
        model.setTBg(parseOrKeep(bgField.getText(), model.getTBg()));
        clearCaches();
        syncAllFromModel();
        onStateChanged();
    }

    private void commitToleranceField() {
        if (syncing) return;
        double v = parseOrKeepDouble(toleranceField.getText(), model.getFindMaximaTolerance());
        v = Math.max(0, v);
        model.setFindMaximaTolerance(v);
        toleranceField.setText(Double.toString(v));
        toleranceBar.setValue((int) Math.round(v));
        clearCaches();
        onStateChanged();
    }

    private void onHistogramThreshold(int tBg, int tFg) {
        if (syncing) return;
        model.setTBg(tBg);
        clearCaches();
        syncAllFromModel();
        onStateChanged();
    }

    private void updateToleranceRange() {
        int range = model.getMaxValue() - model.getMinValue();
        if (range < 1) range = 1;
        toleranceBar.setMinimum(0);
        toleranceBar.setMaximum(range + 1);
    }

    private int parseOrKeep(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception ex) { return fallback; }
    }

    private double parseOrKeepDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception ex) { return fallback; }
    }

    private void clearOverlay() {
        imp.setOverlay((Overlay) null);
        imp.updateAndDraw();
        imp.setRoi((Roi) null);
        Toolbar.getInstance().setTool(Toolbar.RECTANGLE);
    }
}
