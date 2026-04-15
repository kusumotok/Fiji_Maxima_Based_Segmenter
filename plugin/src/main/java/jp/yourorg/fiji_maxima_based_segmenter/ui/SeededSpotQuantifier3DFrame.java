package jp.yourorg.fiji_maxima_based_segmenter.ui;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageRoi;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.PlugInFrame;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import jp.yourorg.fiji_maxima_based_segmenter.alg.*;
import jp.yourorg.fiji_maxima_based_segmenter.core.ThresholdModel;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter3D;
import jp.yourorg.fiji_maxima_based_segmenter.util.CsvExporter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/**
 * GUI frame for Seeded_Spot_Quantifier_3D_.
 */
public class SeededSpotQuantifier3DFrame extends PlugInFrame {

    static final String[][] PREVIEW_COLOR_OPTIONS = {
        { "Yellow",  "#FFFF00" },
        { "Purple",  "#AA00FF" },
        { "Cyan",    "#00FFFF" },
        { "Magenta", "#FF00FF" },
        { "Red",     "#FF0000" },
        { "Green",   "#00FF00" },
        { "White",   "#FFFFFF" },
    };

    private final ImagePlus imp;
    private final ThresholdModel model;

    private final double vw, vh, vd, voxelVol;

    // --- UI components ---
    private final HistogramPanel histogramPanel;

    private final Checkbox  areaEnabledCheck;
    private final Scrollbar areaThreshBar;
    private final TextField areaThreshField;

    private final Scrollbar seedThreshBar;
    private final TextField seedThreshField;

    private final Checkbox  minVolCheck;
    private final Scrollbar minVolBar;
    private final TextField minVolField;

    private final Checkbox  maxVolCheck;
    private final Scrollbar maxVolBar;
    private final TextField maxVolField;

    private final Choice   connectivityChoice;
    private final Checkbox fillHolesCheck;

    private final CheckboxGroup previewGroup = new CheckboxGroup();
    private final Checkbox previewOff;
    private final Checkbox previewOverlay;
    private final Checkbox previewRoi;

    private final Button applyBtn   = new Button("Apply");
    private final Button saveAllBtn = new Button("Save All");
    private final Button batchBtn   = new Button("Batch\u2026");

    // --- Save section ---
    private final Checkbox  saveSeedRoiCheck   = new Checkbox("Seed ROI",   true);
    private final Checkbox  saveSizeRoiCheck   = new Checkbox("Size ROI",   false);
    private final Checkbox  saveAreaRoiCheck   = new Checkbox("Area ROI",   true);
    private final Checkbox  saveResultRoiCheck = new Checkbox("Result ROI", true);
    private final Checkbox  saveCsvCheck       = new Checkbox("CSV",        true);
    private final Checkbox  saveParamCheck     = new Checkbox("Param",      true);
    private final Checkbox  customFolderCheck  = new Checkbox("Custom folder name:", false);
    private final TextField folderNameField    = new TextField("{name} result", 22);
    private final Button    saveToggleBtn      = new Button("\u25bc Save options");
    private       boolean   saveSectionExpanded = true;
    private       Panel     saveOptionsPanel;
    private       Panel     centerPanel;

    private final Choice    seedColorChoice;
    private final Choice    roiColorChoice;
    private final TextField overlayOpacityField;

    private final Choice zprojChoice;
    private final Button zprojRefreshBtn = new Button("Reload");

    private final Label statusLabel = new Label("", Label.LEFT);

    // --- State ---
    private boolean syncing = false;

    private boolean areaEnabled;
    private int     areaThreshold;
    private int     seedThreshold;
    private boolean minVolEnabled, maxVolEnabled;
    private double  minVolVal, maxVolVal;

    // --- Segmentation cache ---
    private SeededQuantifier3D.SeededResult cachedSeededResult;
    private String                          segCacheKey;

    // --- Z-proj render cache ---
    private int[]      cachedZProjTypeMap;
    private List<Roi>  cachedZProjSeedRois;
    private List<Roi>  cachedZProjAreaRois;

    private double volRangeMin = 0.01;
    private double volRangeMax = 100.0;
    private static final int VOL_SLIDER_STEPS = 1000;

    private final Timer previewTimer = new Timer("ssq3d-preview", true);
    private TimerTask   previewTask;
    private final AtomicInteger previewGen = new AtomicInteger();
    private final Timer zWatchTimer = new Timer("ssq3d-zwatch", true);
    private int lastZ = -1;

    public SeededSpotQuantifier3DFrame(ImagePlus imp) {
        super("Seeded Spot Quantifier 3D");
        this.imp = imp;

        Calibration cal = imp.getCalibration();
        vw       = cal.pixelWidth  > 0 ? cal.pixelWidth  : 1;
        vh       = cal.pixelHeight > 0 ? cal.pixelHeight : 1;
        vd       = cal.pixelDepth  > 0 ? cal.pixelDepth  : 1;
        voxelVol = vw * vh * vd;

        model = ThresholdModel.createFor3DPlugin(imp);

        int imgMin = model.getMinValue();
        int imgMax = model.getMaxValue();
        if (imgMax <= imgMin) imgMax = imgMin + 1;

        areaEnabled   = true;
        areaThreshold = model.getTBg();
        seedThreshold = Math.min(imgMax, areaThreshold * 2);
        model.setTBg(areaThreshold);
        model.setTFg(seedThreshold);

        areaEnabledCheck = new Checkbox("", areaEnabled);
        areaThreshBar    = new Scrollbar(Scrollbar.HORIZONTAL, areaThreshold, 1, imgMin, imgMax + 1);
        areaThreshField  = new TextField(Integer.toString(areaThreshold), 6);

        seedThreshBar   = new Scrollbar(Scrollbar.HORIZONTAL, seedThreshold, 1, imgMin, imgMax + 1);
        seedThreshField = new TextField(Integer.toString(seedThreshold), 6);

        minVolEnabled = true;
        maxVolEnabled = false;
        minVolVal     = 0.1;
        maxVolVal     = 50.0;

        minVolCheck = new Checkbox("", minVolEnabled);
        minVolBar   = makeVolBar(minVolVal);
        minVolField = new TextField(formatVol(minVolVal), 7);

        maxVolCheck = new Checkbox("", maxVolEnabled);
        maxVolBar   = makeVolBar(maxVolVal);
        maxVolBar.setEnabled(maxVolEnabled);
        maxVolField = new TextField(formatVol(maxVolVal), 7);
        maxVolField.setEnabled(maxVolEnabled);

        connectivityChoice = new Choice();
        connectivityChoice.add("6");
        connectivityChoice.add("18");
        connectivityChoice.add("26");
        connectivityChoice.select("6");
        fillHolesCheck = new Checkbox("Fill holes", false);

        previewOff     = new Checkbox("Off",     previewGroup, true);
        previewOverlay = new Checkbox("Overlay", previewGroup, false);
        previewRoi     = new Checkbox("ROI",     previewGroup, false);

        seedColorChoice = new Choice();
        for (String[] entry : PREVIEW_COLOR_OPTIONS) seedColorChoice.add(entry[0]);
        seedColorChoice.select(1); // Purple

        roiColorChoice = new Choice();
        for (String[] entry : PREVIEW_COLOR_OPTIONS) roiColorChoice.add(entry[0]);
        roiColorChoice.select(0); // Yellow

        overlayOpacityField = new TextField("50", 3);

        zprojChoice = new Choice();
        refreshZProjChoiceItems();

        histogramPanel = new HistogramPanel(imp, model, this::onHistogramThresholds);
        histogramPanel.setFgEnabled(true);

        buildLayout();
        wireEvents();
        startZWatch();
        pack();
        placeNearImage();
    }

    // =========================================================
    // Layout
    // =========================================================

    private void buildLayout() {
        setLayout(new BorderLayout(4, 4));

        Panel top = new Panel(new BorderLayout());
        top.add(histogramPanel, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        centerPanel = new Panel(new GridLayout(0, 1, 2, 2));
        centerPanel.add(makeThreshRow("Seed threshold:", seedThreshBar, seedThreshField));
        centerPanel.add(makeVolRow("Min vol \u00b5m\u00b3 (seed):", minVolCheck, minVolBar, minVolField));
        centerPanel.add(makeVolRow("Max vol \u00b5m\u00b3 (seed):", maxVolCheck, maxVolBar, maxVolField));
        centerPanel.add(makeAreaThreshRow());
        centerPanel.add(makeConnectivityRow());
        centerPanel.add(makePreviewRow());
        centerPanel.add(makeColorsRow());
        centerPanel.add(makeZProjRow());
        centerPanel.add(makeSaveToggleRow());
        saveOptionsPanel = makeSaveOptionsPanel();
        centerPanel.add(saveOptionsPanel);
        centerPanel.add(makeStatusRow());
        add(centerPanel, BorderLayout.CENTER);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT, 4, 4));
        buttons.add(applyBtn);
        buttons.add(saveAllBtn);
        buttons.add(batchBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    /** Two-row row: label on top, slider+field on bottom. */
    private Panel makeThreshRow(String label, Scrollbar bar, TextField field) {
        Panel outer = new Panel(new GridLayout(2, 1, 1, 1));
        Panel labelRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        labelRow.add(new Label(label));
        outer.add(labelRow);
        outer.add(makeSliderFieldRow(bar, field));
        return outer;
    }

    private Panel makeAreaThreshRow() {
        Panel outer = new Panel(new GridLayout(2, 1, 1, 1));
        Panel labelRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        labelRow.add(areaEnabledCheck);
        labelRow.add(new Label("Area threshold:"));
        outer.add(labelRow);
        outer.add(makeSliderFieldRow(areaThreshBar, areaThreshField));
        return outer;
    }

    private Panel makeVolRow(String label, Checkbox check, Scrollbar bar, TextField field) {
        Panel outer = new Panel(new GridLayout(2, 1, 1, 1));
        Panel labelRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        labelRow.add(check);
        labelRow.add(new Label(label));
        outer.add(labelRow);
        outer.add(makeSliderFieldRow(bar, field));
        return outer;
    }

    /** Bottom row of a two-row slider block: slider stretches, field fixed. */
    private Panel makeSliderFieldRow(Scrollbar bar, TextField field) {
        Panel p = new Panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(0, 4, 0, 2);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(bar, c);
        c.gridx = 1; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 2, 0, 4);
        p.add(field, c);
        return p;
    }

    private Panel makeConnectivityRow() {
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.add(new Label("Connectivity:"));
        p.add(connectivityChoice);
        p.add(fillHolesCheck);
        return p;
    }

    /** Z-proj row: label fixed, Choice stretches with window, Reload fixed. */
    private Panel makeZProjRow() {
        Panel p = new Panel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.insets = new Insets(2, 4, 2, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        p.add(new Label("Z-proj:"), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        p.add(zprojChoice, c);
        c.gridx = 2; c.weightx = 0; c.fill = GridBagConstraints.NONE;
        p.add(zprojRefreshBtn, c);
        return p;
    }

    private Panel makeStatusRow() {
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        statusLabel.setForeground(new Color(80, 80, 80));
        p.add(statusLabel);
        return p;
    }

    private Panel makeColorsRow() {
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.add(new Label("Colors:"));
        p.add(new Label("Seed:"));
        p.add(seedColorChoice);
        p.add(new Label("Area/ROI:"));
        p.add(roiColorChoice);
        p.add(new Label("Opacity %:"));
        p.add(overlayOpacityField);
        return p;
    }

    private Panel makePreviewRow() {
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.add(new Label("Preview:"));
        p.add(previewOff);
        p.add(previewOverlay);
        p.add(previewRoi);
        return p;
    }

    private Panel makeSaveToggleRow() {
        Panel p = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        p.add(saveToggleBtn);
        return p;
    }

    private Panel makeSaveOptionsPanel() {
        Panel p = new Panel(new GridLayout(0, 1, 2, 2));

        Panel selectRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        Button selectAllBtn   = new Button("Select All");
        Button deselectAllBtn = new Button("Deselect All");
        selectAllBtn  .addActionListener(e -> setSaveChecks(true));
        deselectAllBtn.addActionListener(e -> setSaveChecks(false));
        selectRow.add(selectAllBtn);
        selectRow.add(deselectAllBtn);
        p.add(selectRow);

        Panel checksRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        checksRow.add(saveSeedRoiCheck);
        checksRow.add(saveSizeRoiCheck);
        checksRow.add(saveAreaRoiCheck);
        checksRow.add(saveResultRoiCheck);
        checksRow.add(saveCsvCheck);
        checksRow.add(saveParamCheck);
        p.add(checksRow);

        Panel folderRow = new Panel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        folderRow.add(customFolderCheck);
        folderRow.add(folderNameField);
        folderRow.add(new Label("tokens: {name} {date} {seed} {area}"));
        p.add(folderRow);

        return p;
    }

    private void setSaveChecks(boolean state) {
        saveSeedRoiCheck  .setState(state);
        saveSizeRoiCheck  .setState(state);
        saveAreaRoiCheck  .setState(state);
        saveResultRoiCheck.setState(state);
        saveCsvCheck      .setState(state);
        saveParamCheck    .setState(state);
    }

    // =========================================================
    // Events
    // =========================================================

    private void wireEvents() {
        areaEnabledCheck.addItemListener(e -> {
            if (syncing) return;
            areaEnabled = areaEnabledCheck.getState();
            areaThreshBar.setEnabled(areaEnabled);
            areaThreshField.setEnabled(areaEnabled);
            onParamsChanged();
        });

        areaThreshBar.addAdjustmentListener(e -> {
            if (syncing) return;
            areaThreshold = areaThreshBar.getValue();
            model.setTBg(areaThreshold);
            syncing = true;
            areaThreshField.setText(Integer.toString(areaThreshold));
            histogramPanel.repaint();
            syncing = false;
            onParamsChanged();
        });
        areaThreshField.addActionListener(e -> commitAreaThreshField());
        areaThreshField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitAreaThreshField(); }
        });

        seedThreshBar.addAdjustmentListener(e -> {
            if (syncing) return;
            seedThreshold = seedThreshBar.getValue();
            model.setTFg(seedThreshold);
            syncing = true;
            seedThreshField.setText(Integer.toString(seedThreshold));
            histogramPanel.repaint();
            syncing = false;
            onParamsChanged();
        });
        seedThreshField.addActionListener(e -> commitSeedThreshField());
        seedThreshField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitSeedThreshField(); }
        });

        minVolCheck.addItemListener(e -> {
            if (syncing) return;
            minVolEnabled = minVolCheck.getState();
            minVolBar.setEnabled(minVolEnabled);
            minVolField.setEnabled(minVolEnabled);
            onParamsChanged();
        });
        minVolBar.addAdjustmentListener(e -> {
            if (syncing) return;
            minVolVal = sliderToVol(minVolBar.getValue());
            syncing = true;
            minVolField.setText(formatVol(minVolVal));
            syncing = false;
            onParamsChanged();
        });
        minVolField.addActionListener(e -> commitMinVolField());
        minVolField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitMinVolField(); }
        });

        maxVolCheck.addItemListener(e -> {
            if (syncing) return;
            maxVolEnabled = maxVolCheck.getState();
            maxVolBar.setEnabled(maxVolEnabled);
            maxVolField.setEnabled(maxVolEnabled);
            onParamsChanged();
        });
        maxVolBar.addAdjustmentListener(e -> {
            if (syncing) return;
            maxVolVal = sliderToVol(maxVolBar.getValue());
            syncing = true;
            maxVolField.setText(formatVol(maxVolVal));
            syncing = false;
            onParamsChanged();
        });
        maxVolField.addActionListener(e -> commitMaxVolField());
        maxVolField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitMaxVolField(); }
        });

        connectivityChoice.addItemListener(e -> onParamsChanged());
        fillHolesCheck    .addItemListener(e -> onParamsChanged());

        ItemListener previewListener = e -> onPreviewModeChanged();
        previewOff    .addItemListener(previewListener);
        previewOverlay.addItemListener(previewListener);
        previewRoi    .addItemListener(previewListener);

        seedColorChoice    .addItemListener(e -> onColorChanged());
        roiColorChoice     .addItemListener(e -> onColorChanged());
        overlayOpacityField.addActionListener(e -> onColorChanged());
        overlayOpacityField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { onColorChanged(); }
        });

        zprojChoice    .addItemListener(e -> onColorChanged());
        zprojRefreshBtn.addActionListener(e -> refreshZProjChoiceItems());

        saveToggleBtn.addActionListener(e -> toggleSaveSection());

        applyBtn  .addActionListener(e -> runApply());
        saveAllBtn.addActionListener(e -> runSaveAll());
        batchBtn  .addActionListener(e -> runBatch());

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                clearOverlay();
                zWatchTimer.cancel();
                previewTimer.cancel();
                dispose();
            }
        });
    }

    private void toggleSaveSection() {
        saveSectionExpanded = !saveSectionExpanded;
        if (saveSectionExpanded) {
            // Insert before the last component (status row)
            centerPanel.add(saveOptionsPanel, centerPanel.getComponentCount() - 1);
            saveToggleBtn.setLabel("\u25bc Save options");
        } else {
            centerPanel.remove(saveOptionsPanel);
            saveToggleBtn.setLabel("\u25b6 Save options");
        }
        centerPanel.validate();
        pack();
    }

    // =========================================================
    // Histogram callback
    // =========================================================

    private void onHistogramThresholds(int tBg, int tFg) {
        if (syncing) return;
        areaThreshold = tBg;
        seedThreshold = tFg;
        model.setTBg(areaThreshold);
        model.setTFg(seedThreshold);
        syncing = true;
        areaThreshBar.setValue(areaThreshold);
        areaThreshField.setText(Integer.toString(areaThreshold));
        seedThreshBar.setValue(seedThreshold);
        seedThreshField.setText(Integer.toString(seedThreshold));
        syncing = false;
        onParamsChanged();
    }

    // =========================================================
    // State changes
    // =========================================================

    private void onParamsChanged() {
        cachedSeededResult  = null;
        segCacheKey         = null;
        cachedZProjTypeMap  = null;
        cachedZProjSeedRois = null;
        cachedZProjAreaRois = null;
        setModified();
    }

    private void onPreviewModeChanged() {
        if (previewOff.getState()) {
            clearOverlay();
            cancelPreview();
            statusLabel.setText("");
            IJ.showStatus("");
        } else {
            if (cachedSeededResult != null) {
                updatePreviewForZChange();
            } else {
                setModified();
            }
        }
    }

    private void setModified() {
        statusLabel.setText("Press Apply to update");
        IJ.showStatus("");
    }

    private void onColorChanged() {
        if (previewOff.getState() || cachedSeededResult == null) return;
        int zPlane = imp.getCurrentSlice();
        if (previewRoi.getState())
            renderRoiOverlay(cachedSeededResult.seedSeg, cachedSeededResult.finalSeg,
                             areaEnabled, zPlane);
        else
            renderOverlay(cachedSeededResult.seedSeg, cachedSeededResult.finalSeg, areaEnabled, zPlane);
    }

    // =========================================================
    // Commit helpers
    // =========================================================

    private void commitAreaThreshField() {
        if (syncing) return;
        int v = parseIntOr(areaThreshField.getText(), areaThreshold);
        v = Math.max(0, v);
        areaThreshold = v;
        model.setTBg(areaThreshold);
        syncing = true;
        areaThreshBar.setValue(areaThreshold);
        areaThreshField.setText(Integer.toString(areaThreshold));
        histogramPanel.repaint();
        syncing = false;
        onParamsChanged();
    }

    private void commitSeedThreshField() {
        if (syncing) return;
        int v = parseIntOr(seedThreshField.getText(), seedThreshold);
        v = Math.max(0, v);
        seedThreshold = v;
        model.setTFg(seedThreshold);
        syncing = true;
        seedThreshBar.setValue(seedThreshold);
        seedThreshField.setText(Integer.toString(seedThreshold));
        histogramPanel.repaint();
        syncing = false;
        onParamsChanged();
    }

    private void commitMinVolField() {
        if (syncing) return;
        double v = parseDoubleOr(minVolField.getText(), minVolVal);
        v = Math.max(0, v);
        minVolVal = v;
        syncing = true;
        minVolField.setText(formatVol(minVolVal));
        minVolBar.setValue(volToSlider(minVolVal));
        syncing = false;
        onParamsChanged();
    }

    private void commitMaxVolField() {
        if (syncing) return;
        double v = parseDoubleOr(maxVolField.getText(), maxVolVal);
        v = Math.max(0, v);
        maxVolVal = v;
        syncing = true;
        maxVolField.setText(formatVol(maxVolVal));
        maxVolBar.setValue(volToSlider(maxVolVal));
        syncing = false;
        onParamsChanged();
    }

    // =========================================================
    // Preview
    // =========================================================

    private void updatePreviewForZChange() {
        if (previewOff.getState() || cachedSeededResult == null) return;
        int zPlane = imp.getCurrentSlice();
        if (previewRoi.getState())
            renderRoiOverlay(cachedSeededResult.seedSeg, cachedSeededResult.finalSeg,
                             areaEnabled, zPlane);
        else
            renderOverlay(cachedSeededResult.seedSeg, cachedSeededResult.finalSeg, areaEnabled, zPlane);
    }

    private void cancelPreview() {
        previewGen.incrementAndGet();
        cancelPreviewTask();
    }

    private void cancelPreviewTask() {
        if (previewTask != null) {
            previewTask.cancel();
            previewTask = null;
        }
    }

    private void renderOverlay(SegmentationResult3D seedSeg, SegmentationResult3D finalSeg,
                                boolean areaEn, int zPlane) {
        if (finalSeg == null || finalSeg.labelImage == null) return;
        int nSlices = finalSeg.labelImage.getNSlices();
        if (zPlane < 1 || zPlane > nSlices) return;

        ImageProcessor finalIp = finalSeg.labelImage.getStack().getProcessor(zPlane);
        int w = finalIp.getWidth(), h = finalIp.getHeight();

        ImageProcessor seedIp = (areaEn && seedSeg != null && seedSeg.labelImage != null
                                  && zPlane <= seedSeg.labelImage.getNSlices())
                                ? seedSeg.labelImage.getStack().getProcessor(zPlane)
                                : null;

        int seedRgb = toRgbSolid(selectedSeedPreviewColor());
        int areaRgb = toRgbSolid(selectedRoiColor());
        double opacity = getOverlayOpacity();

        ColorProcessor cp = new ColorProcessor(w, h);
        int[] pixels = (int[]) cp.getPixels();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean isSeed = seedIp != null
                    && (int) Math.round(seedIp.getPixelValue(x, y)) > 0;
                int finalLabel = (int) Math.round(finalIp.getPixelValue(x, y));
                if (isSeed) {
                    pixels[y * w + x] = seedRgb;
                } else if (finalLabel > 0) {
                    pixels[y * w + x] = areaRgb;
                }
            }
        }

        ImageRoi iroi = new ImageRoi(0, 0, cp);
        iroi.setZeroTransparent(true);
        iroi.setOpacity(opacity);
        Overlay overlay = new Overlay();
        overlay.add(iroi);
        imp.setOverlay(overlay);
        imp.updateAndDraw();

        ImagePlus zp = getZProjImp();
        if (zp != null) renderOverlayOnZProj(seedSeg, finalSeg, areaEn, zp);
    }

    private static int toRgbSolid(Color c) {
        return (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    private double getOverlayOpacity() {
        try {
            int v = Integer.parseInt(overlayOpacityField.getText().trim());
            return Math.max(0, Math.min(100, v)) / 100.0;
        } catch (NumberFormatException ex) {
            return 0.5;
        }
    }

    private void renderRoiOverlay(SegmentationResult3D seedSeg,
                                   SegmentationResult3D finalSeg,
                                   boolean areaEnabled, int zPlane) {
        if (finalSeg == null || finalSeg.labelImage == null) return;
        int nSlices = finalSeg.labelImage.getNSlices();
        if (zPlane < 1 || zPlane > nSlices) return;

        Overlay overlay = new Overlay();

        if (areaEnabled && seedSeg != null && seedSeg.labelImage != null
                && zPlane <= seedSeg.labelImage.getNSlices()) {
            addLabelOutlines(seedSeg.labelImage.getStack().getProcessor(zPlane),
                             selectedSeedPreviewColor(), overlay);
        }

        addLabelOutlines(finalSeg.labelImage.getStack().getProcessor(zPlane),
                         selectedRoiColor(), overlay);

        imp.setOverlay(overlay);
        imp.updateAndDraw();

        ImagePlus zp = getZProjImp();
        if (zp != null) renderRoiOverlayOnZProj(seedSeg, finalSeg, areaEnabled, zp);
    }

    private static void addLabelOutlines(ImageProcessor labelIp, Color color, Overlay overlay) {
        int w = labelIp.getWidth(), h = labelIp.getHeight();

        HashMap<Integer, int[]> bboxMap = new HashMap<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = (int) Math.round(labelIp.getPixelValue(x, y));
                if (v <= 0) continue;
                int[] bb = bboxMap.get(v);
                if (bb == null) {
                    bboxMap.put(v, new int[]{x, y, x, y});
                } else {
                    if (x < bb[0]) bb[0] = x;
                    if (y < bb[1]) bb[1] = y;
                    if (x > bb[2]) bb[2] = x;
                    if (y > bb[3]) bb[3] = y;
                }
            }
        }

        for (Map.Entry<Integer, int[]> entry : bboxMap.entrySet()) {
            int label = entry.getKey();
            int[] bb = entry.getValue();
            int x0 = Math.max(0, bb[0] - 1), y0 = Math.max(0, bb[1] - 1);
            int x1 = Math.min(w - 1, bb[2] + 1), y1 = Math.min(h - 1, bb[3] + 1);
            int bw = x1 - x0 + 1, bh = y1 - y0 + 1;

            ByteProcessor bp = new ByteProcessor(bw, bh);
            byte[] pixels = (byte[]) bp.getPixels();
            for (int y = y0; y <= y1; y++)
                for (int x = x0; x <= x1; x++)
                    if ((int) Math.round(labelIp.getPixelValue(x, y)) == label)
                        pixels[(y - y0) * bw + (x - x0)] = (byte) 255;

            bp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(new ImagePlus("", bp));
            if (roi == null) continue;
            roi.setLocation(roi.getXBase() + x0, roi.getYBase() + y0);
            roi.setStrokeColor(color);
            overlay.add(roi);
        }
    }

    // =========================================================
    // Z-projection overlay rendering
    // =========================================================

    private ImagePlus getZProjImp() {
        String title = zprojChoice.getSelectedItem();
        if (title == null || "None".equals(title)) return null;
        ImagePlus zp = WindowManager.getImage(title);
        if (zp == null || zp.getProcessor() == null) return null;
        if (zp.getWidth() != imp.getWidth() || zp.getHeight() != imp.getHeight()) return null;
        return zp;
    }

    private void refreshZProjChoiceItems() {
        String current = zprojChoice.getSelectedItem();
        zprojChoice.removeAll();
        zprojChoice.add("None");
        int[] ids = WindowManager.getIDList();
        if (ids != null) {
            for (int id : ids) {
                ImagePlus zp = WindowManager.getImage(id);
                if (zp == null || zp == imp) continue;
                if (zp.getNSlices() != 1) continue;
                if (zp.getWidth() != imp.getWidth() || zp.getHeight() != imp.getHeight()) continue;
                zprojChoice.add(zp.getTitle());
            }
        }
        if (current != null) zprojChoice.select(current);
    }

    private void renderOverlayOnZProj(SegmentationResult3D seedSeg, SegmentationResult3D finalSeg,
                                       boolean areaEn, ImagePlus zp) {
        if (finalSeg == null || finalSeg.labelImage == null) return;
        int w = finalSeg.labelImage.getWidth();
        int h = finalSeg.labelImage.getHeight();

        if (cachedZProjTypeMap == null) {
            int d = finalSeg.labelImage.getNSlices();
            boolean hasSeed = areaEn && seedSeg != null && seedSeg.labelImage != null;
            cachedZProjTypeMap = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int type = 0;
                    for (int z = 1; z <= d; z++) {
                        if (hasSeed && z <= seedSeg.labelImage.getNSlices()
                                && (int) Math.round(seedSeg.labelImage.getStack()
                                        .getProcessor(z).getPixelValue(x, y)) > 0) {
                            type = 1;
                            break;
                        }
                        if ((int) Math.round(finalSeg.labelImage.getStack()
                                .getProcessor(z).getPixelValue(x, y)) > 0) {
                            type = 2;
                        }
                    }
                    cachedZProjTypeMap[y * w + x] = type;
                }
            }
        }

        int seedRgb = toRgbSolid(selectedSeedPreviewColor());
        int areaRgb = toRgbSolid(selectedRoiColor());
        ColorProcessor cp = new ColorProcessor(w, h);
        int[] pixels = (int[]) cp.getPixels();
        for (int i = 0; i < cachedZProjTypeMap.length; i++) {
            if      (cachedZProjTypeMap[i] == 1) pixels[i] = seedRgb;
            else if (cachedZProjTypeMap[i] == 2) pixels[i] = areaRgb;
        }

        ImageRoi iroi = new ImageRoi(0, 0, cp);
        iroi.setZeroTransparent(true);
        iroi.setOpacity(getOverlayOpacity());
        Overlay overlay = new Overlay();
        overlay.add(iroi);
        zp.setOverlay(overlay);
        zp.updateAndDraw();
    }

    private void renderRoiOverlayOnZProj(SegmentationResult3D seedSeg, SegmentationResult3D finalSeg,
                                          boolean areaEn, ImagePlus zp) {
        if (finalSeg == null || finalSeg.labelImage == null) return;

        if (cachedZProjAreaRois == null) {
            cachedZProjAreaRois = buildLabelUnionRois(finalSeg.labelImage);
            cachedZProjSeedRois = (areaEn && seedSeg != null && seedSeg.labelImage != null)
                                  ? buildLabelUnionRois(seedSeg.labelImage) : null;
        }

        Overlay overlay = new Overlay();
        if (cachedZProjSeedRois != null) {
            Color sc = selectedSeedPreviewColor();
            for (Roi r : cachedZProjSeedRois) { r.setStrokeColor(sc); overlay.add(r); }
        }
        Color ac = selectedRoiColor();
        for (Roi r : cachedZProjAreaRois) { r.setStrokeColor(ac); overlay.add(r); }
        zp.setOverlay(overlay);
        zp.updateAndDraw();
    }

    private static List<Roi> buildLabelUnionRois(ImagePlus labelImp) {
        int w = labelImp.getWidth(), h = labelImp.getHeight(), d = labelImp.getNSlices();
        TreeSet<Integer> labels = new TreeSet<>();
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = labelImp.getStack().getProcessor(z);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int v = (int) Math.round(ip.getPixelValue(x, y));
                    if (v > 0) labels.add(v);
                }
        }
        List<Roi> rois = new ArrayList<>();
        for (int label : labels) {
            ByteProcessor bp = new ByteProcessor(w, h);
            byte[] bpix = (byte[]) bp.getPixels();
            for (int z = 1; z <= d; z++) {
                ImageProcessor ip = labelImp.getStack().getProcessor(z);
                for (int y = 0; y < h; y++)
                    for (int x = 0; x < w; x++)
                        if ((int) Math.round(ip.getPixelValue(x, y)) == label)
                            bpix[y * w + x] = (byte) 255;
            }
            bp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            Roi roi = ThresholdToSelection.run(new ImagePlus("", bp));
            if (roi != null) rois.add(roi);
        }
        return rois;
    }

    private void clearOverlay() {
        imp.setOverlay((Overlay) null);
        imp.updateAndDraw();
        ImagePlus zp = getZProjImp();
        if (zp != null) { zp.setOverlay((Overlay) null); zp.updateAndDraw(); }
    }

    // =========================================================
    // Segmentation cache
    // =========================================================

    private synchronized SeededQuantifier3D.SeededResult getOrComputeSeeded(
            QuantifierParams params, int at, int st, boolean areaEn) {
        String key = segKey(params, at, st, areaEn);
        if (segCacheKey != null && segCacheKey.equals(key) && cachedSeededResult != null) {
            return cachedSeededResult;
        }
        try {
            cachedSeededResult = SeededQuantifier3D.compute(imp, at, st, params, voxelVol, areaEn);
            segCacheKey        = key;
            return cachedSeededResult;
        } catch (Exception ex) {
            IJ.log("Seeded Spot Quantifier 3D preview error: " + ex.getMessage());
            return null;
        }
    }

    private String segKey(QuantifierParams p, int at, int st, boolean areaEn) {
        return at + ":" + st + ":" + areaEn + ":" + p.connectivity + ":" + p.fillHoles
             + ":" + p.minVolUm3 + ":" + p.maxVolUm3;
    }

    // =========================================================
    // Button actions
    // =========================================================

    private void runApply() {
        if (previewOff.getState()) return;
        statusLabel.setText("Computing...");
        IJ.showStatus("Seeded Spot Quantifier 3D: computing...");
        boolean roiMode = previewRoi.getState();
        int gen = previewGen.incrementAndGet();
        cancelPreviewTask();
        int zPlane = imp.getCurrentSlice();
        QuantifierParams params = buildParams();
        int at = areaThreshold;
        int st = seedThreshold;
        boolean areaEn = areaEnabled;
        previewTask = new TimerTask() {
            @Override public void run() {
                SeededQuantifier3D.SeededResult r = getOrComputeSeeded(params, at, st, areaEn);
                if (r == null || previewGen.get() != gen) {
                    EventQueue.invokeLater(() -> statusLabel.setText("No spots found."));
                    return;
                }
                int nSpots = (int) Math.round(r.finalSeg.labelImage.getStatistics().max);
                String msg = nSpots + " spot" + (nSpots != 1 ? "s" : "");
                IJ.showStatus("Seeded Spot Quantifier 3D: " + msg);
                EventQueue.invokeLater(() -> {
                    if (previewGen.get() != gen) return;
                    if (roiMode) renderRoiOverlay(r.seedSeg, r.finalSeg, areaEn, zPlane);
                    else         renderOverlay(r.seedSeg, r.finalSeg, areaEn, zPlane);
                    statusLabel.setText(msg);
                });
            }
        };
        previewTimer.schedule(previewTask, 0);
    }

    // =========================================================
    // Save
    // =========================================================

    private void runSaveAll() {
        File outDir = resolveOutputDir(imp);
        if (outDir == null) return;

        if (outDir.exists()) {
            int choice = JOptionPane.showOptionDialog(
                this,
                "Folder already exists:\n" + outDir.getAbsolutePath() + "\nOverwrite?",
                "Folder Exists",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Overwrite", "Cancel"},
                "Cancel");
            if (choice != 0) return;
        }

        QuantifierParams params = buildParams();
        String err = saveOneToDir(imp, areaThreshold, seedThreshold, areaEnabled, params, outDir,
            saveSeedRoiCheck.getState(), saveSizeRoiCheck.getState(),
            saveAreaRoiCheck.getState(), saveResultRoiCheck.getState(),
            saveCsvCheck.getState(), saveParamCheck.getState(),
            selectedRoiColor());
        if (err == null) {
            IJ.showMessage("Saved", "Saved to:\n" + outDir.getAbsolutePath());
        } else {
            IJ.error("Seeded Spot Quantifier 3D", "Save failed: " + err);
        }
    }

    private void runBatch() {
        new BatchDialog().setVisible(true);
    }

    /** Resolve output directory: <imageFileDir>/<folderName>. Falls back to FileDialog. */
    private File resolveOutputDir(ImagePlus target) {
        String basename = target.getShortTitle().replaceAll("\\.tiff?$", "");
        String pattern  = customFolderCheck.getState() ? folderNameField.getText() : "{name} result";
        String folderName = expandFolderTokens(pattern, basename, seedThreshold, areaThreshold);

        FileInfo fi = target.getOriginalFileInfo();
        String dirStr = (fi != null && fi.directory != null && !fi.directory.isEmpty())
            ? fi.directory : null;

        if (dirStr == null) {
            FileDialog fd = new FileDialog(this, "Choose save location (select any file)", FileDialog.SAVE);
            fd.setFile("placeholder.csv");
            fd.setVisible(true);
            if (fd.getDirectory() == null) return null;
            dirStr = fd.getDirectory();
        }

        return new File(dirStr, folderName);
    }

    private static String expandFolderTokens(String pattern, String name, int seed, int area) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return pattern
            .replace("{name}", name)
            .replace("{date}", date)
            .replace("{seed}", String.valueOf(seed))
            .replace("{area}", String.valueOf(area));
    }

    /**
     * Process one image and save selected file types under outDir (flat structure).
     * Returns null on success, error message on failure.
     */
    private static String saveOneToDir(ImagePlus target, int at, int st, boolean areaEn,
                                        QuantifierParams params, File outDir,
                                        boolean saveSeedRoi, boolean saveSizeRoi,
                                        boolean saveAreaRoi, boolean saveResultRoi,
                                        boolean saveCsv, boolean saveParam,
                                        Color roiColor) {
        Calibration cal = target.getCalibration();
        double tw = cal.pixelWidth  > 0 ? cal.pixelWidth  : 1;
        double th = cal.pixelHeight > 0 ? cal.pixelHeight : 1;
        double td = cal.pixelDepth  > 0 ? cal.pixelDepth  : 1;
        double tVoxelVol = tw * th * td;

        SeededQuantifier3D.SeededResult r = SeededQuantifier3D.compute(
            target, at, st, params, tVoxelVol, areaEn);
        if (r == null) return "no spots detected";

        String basename = target.getShortTitle().replaceAll("\\.tiff?$", "");

        try {
            outDir.mkdirs();

            if (saveCsv) {
                List<SpotMeasurement> spots = SpotMeasurer.measure(r.finalSeg, target, tw, th, td);
                CsvExporter.writeCsv(spots, new File(outDir, basename + "_spots.csv"));
            }

            if (saveParam) {
                CsvExporter.writeSeededParams(at, st, params,
                    new File(outDir, basename + "_params.txt"));
            }

            if (saveSeedRoi && r.rawSeedSeg != null) {
                saveRoiToZip(r.rawSeedSeg, roiColor, new File(outDir, basename + "_seed_roi.zip"));
            }

            if (saveSizeRoi && r.seedSeg != null) {
                saveRoiToZip(r.seedSeg, roiColor, new File(outDir, basename + "_size_roi.zip"));
            }

            if (saveAreaRoi) {
                QuantifierParams noFilter = new QuantifierParams(
                    at, null, null, false, 1.0, 0.5, params.connectivity, params.fillHoles);
                CcResult3D areaCC = SpotQuantifier3D.computeCCFromBlurred(target, at, noFilter);
                Map<Integer, Integer> allValidMap = new HashMap<>();
                areaCC.voxelCounts.keySet().forEach(k -> allValidMap.put(k, CcResult3D.STATUS_VALID));
                SegmentationResult3D areaSeg = areaCC.buildFilteredResult(allValidMap);
                saveRoiToZip(areaSeg, roiColor, new File(outDir, basename + "_area_roi.zip"));
            }

            if (saveResultRoi && r.finalSeg != null) {
                saveRoiToZip(r.finalSeg, roiColor, new File(outDir, basename + "_result_roi.zip"));
            }

            return null;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    private static void saveRoiToZip(SegmentationResult3D seg, Color roiColor, File zipFile) {
        if (seg == null || seg.labelImage == null) return;
        RoiManager rm = RoiManager.getRoiManager();
        rm.reset();
        new RoiExporter3D().exportToRoiManager(seg.labelImage, roiColor);
        if (rm.getCount() > 0) {
            RoiExporter.saveRoiManagerToZip(zipFile.getAbsolutePath());
        }
        rm.reset();
    }

    // =========================================================
    // Batch Dialog
    // =========================================================

    private class BatchDialog extends JDialog {

        private final JCheckBox bSaveSeedRoi;
        private final JCheckBox bSaveSizeRoi;
        private final JCheckBox bSaveAreaRoi;
        private final JCheckBox bSaveResultRoi;
        private final JCheckBox bSaveCsv;
        private final JCheckBox bSaveParam;
        private final JCheckBox bCustomFolder;
        private final JTextField bFolderName;

        private final JTextField pathField  = new JTextField(40);
        private final JPanel     fileListPanel = new JPanel();
        private final List<JCheckBox> fileChecks = new ArrayList<>();
        private final JLabel     countLabel    = new JLabel("0 file(s) found");
        private final JProgressBar progressBar = new JProgressBar();
        private final JLabel     dlgStatus    = new JLabel(" ");
        private       JButton    runBtn;

        BatchDialog() {
            super(SeededSpotQuantifier3DFrame.this, "Batch Processing", false);

            bSaveSeedRoi   = new JCheckBox("Seed ROI",   saveSeedRoiCheck  .getState());
            bSaveSizeRoi   = new JCheckBox("Size ROI",   saveSizeRoiCheck  .getState());
            bSaveAreaRoi   = new JCheckBox("Area ROI",   saveAreaRoiCheck  .getState());
            bSaveResultRoi = new JCheckBox("Result ROI", saveResultRoiCheck.getState());
            bSaveCsv       = new JCheckBox("CSV",        saveCsvCheck      .getState());
            bSaveParam     = new JCheckBox("Param",      saveParamCheck    .getState());
            bCustomFolder  = new JCheckBox("Custom folder name:", customFolderCheck.getState());
            bFolderName    = new JTextField(folderNameField.getText(), 22);

            buildBatchLayout();
        }

        private void buildBatchLayout() {
            setLayout(new BorderLayout(6, 6));
            JPanel main = new JPanel(new BorderLayout(4, 4));
            main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            // Input folder row
            JPanel inputRow = new JPanel(new BorderLayout(4, 0));
            inputRow.add(new JLabel("Input folder:"), BorderLayout.WEST);
            inputRow.add(pathField, BorderLayout.CENTER);
            JButton browseBtn = new JButton("Browse");
            browseBtn.addActionListener(e -> browsePath());
            inputRow.add(browseBtn, BorderLayout.EAST);
            main.add(inputRow, BorderLayout.NORTH);

            // File list
            fileListPanel.setLayout(new BoxLayout(fileListPanel, BoxLayout.Y_AXIS));
            JScrollPane scroll = new JScrollPane(fileListPanel);
            scroll.setPreferredSize(new Dimension(520, 180));
            main.add(scroll, BorderLayout.CENTER);

            // Bottom section
            JPanel bottom = new JPanel(new BorderLayout(4, 4));

            JPanel listCtrl = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            JButton selAll   = new JButton("Select All");
            JButton deselAll = new JButton("Deselect All");
            selAll  .addActionListener(e -> setAllFileChecks(true));
            deselAll.addActionListener(e -> setAllFileChecks(false));
            listCtrl.add(selAll);
            listCtrl.add(deselAll);
            listCtrl.add(countLabel);
            bottom.add(listCtrl, BorderLayout.NORTH);

            bottom.add(buildBatchSavePanel(), BorderLayout.CENTER);

            // Progress + buttons
            JPanel ctrlRow = new JPanel(new BorderLayout(4, 4));
            progressBar.setStringPainted(true);
            progressBar.setVisible(false);
            ctrlRow.add(progressBar, BorderLayout.CENTER);

            JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.addActionListener(e -> dispose());
            runBtn = new JButton("Run");
            runBtn.addActionListener(e -> runBatchProcess());
            btnRow.add(dlgStatus);
            btnRow.add(cancelBtn);
            btnRow.add(runBtn);
            ctrlRow.add(btnRow, BorderLayout.SOUTH);
            bottom.add(ctrlRow, BorderLayout.SOUTH);

            main.add(bottom, BorderLayout.SOUTH);
            add(main);

            pathField.addActionListener(e -> scanFiles());
            pathField.addFocusListener(new FocusAdapter() {
                @Override public void focusLost(FocusEvent e) { scanFiles(); }
            });

            pack();
            setLocationRelativeTo(SeededSpotQuantifier3DFrame.this);
        }

        private JPanel buildBatchSavePanel() {
            JPanel p = new JPanel(new GridLayout(0, 1, 2, 2));
            p.setBorder(BorderFactory.createTitledBorder("Save options"));

            JPanel selRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            JButton selAll   = new JButton("Select All");
            JButton deselAll = new JButton("Deselect All");
            selAll  .addActionListener(e -> setBatchSaveChecks(true));
            deselAll.addActionListener(e -> setBatchSaveChecks(false));
            selRow.add(selAll);
            selRow.add(deselAll);
            p.add(selRow);

            JPanel checksRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            checksRow.add(bSaveSeedRoi);
            checksRow.add(bSaveSizeRoi);
            checksRow.add(bSaveAreaRoi);
            checksRow.add(bSaveResultRoi);
            checksRow.add(bSaveCsv);
            checksRow.add(bSaveParam);
            p.add(checksRow);

            JPanel folderRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            folderRow.add(bCustomFolder);
            folderRow.add(bFolderName);
            folderRow.add(new JLabel("tokens: {name} {date} {seed} {area}"));
            p.add(folderRow);

            return p;
        }

        private void setBatchSaveChecks(boolean state) {
            bSaveSeedRoi  .setSelected(state);
            bSaveSizeRoi  .setSelected(state);
            bSaveAreaRoi  .setSelected(state);
            bSaveResultRoi.setSelected(state);
            bSaveCsv      .setSelected(state);
            bSaveParam    .setSelected(state);
        }

        private void setAllFileChecks(boolean state) {
            for (JCheckBox cb : fileChecks) cb.setSelected(state);
        }

        private void browsePath() {
            FileDialog fd = new FileDialog(SeededSpotQuantifier3DFrame.this,
                "Select input folder (select any file inside it)", FileDialog.LOAD);
            fd.setVisible(true);
            if (fd.getDirectory() != null) {
                pathField.setText(fd.getDirectory());
                scanFiles();
            }
        }

        private void scanFiles() {
            String pathStr = pathField.getText().trim();
            if (pathStr.isEmpty()) return;
            File dir = new File(pathStr);
            if (!dir.isDirectory()) return;

            fileChecks.clear();
            fileListPanel.removeAll();

            List<File> tiffs = new ArrayList<>();
            try {
                Files.walk(dir.toPath())
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".tif") || n.endsWith(".tiff");
                    })
                    .sorted()
                    .forEach(p -> tiffs.add(p.toFile()));
            } catch (Exception ex) {
                IJ.log("Batch scan error: " + ex.getMessage());
            }

            for (File f : tiffs) {
                JCheckBox cb = new JCheckBox(f.getAbsolutePath(), true);
                fileChecks.add(cb);
                fileListPanel.add(cb);
            }

            countLabel.setText(tiffs.size() + " file(s) found");
            fileListPanel.revalidate();
            fileListPanel.repaint();
            pack();
        }

        private void runBatchProcess() {
            List<File> selected = new ArrayList<>();
            for (JCheckBox cb : fileChecks) {
                if (cb.isSelected()) selected.add(new File(cb.getText()));
            }
            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No files selected.");
                return;
            }

            runBtn.setEnabled(false);
            progressBar.setVisible(true);
            progressBar.setMaximum(selected.size());
            progressBar.setValue(0);

            QuantifierParams params   = buildParams();
            int     at        = areaThreshold;
            int     st        = seedThreshold;
            boolean areaEn    = areaEnabled;
            Color   roiColor  = selectedRoiColor();
            boolean seedRoi   = bSaveSeedRoi  .isSelected();
            boolean sizeRoi   = bSaveSizeRoi  .isSelected();
            boolean areaRoi   = bSaveAreaRoi  .isSelected();
            boolean resultRoi = bSaveResultRoi.isSelected();
            boolean csv       = bSaveCsv      .isSelected();
            boolean param     = bSaveParam    .isSelected();
            boolean customF   = bCustomFolder .isSelected();
            String  folderPat = bFolderName   .getText();

            // 0 = ask, 1 = overwrite all, 2 = skip all
            int[] overwriteState = {0};

            new SwingWorker<int[], Void>() {
                @Override
                protected int[] doInBackground() {
                    int ok = 0, skipped = 0;
                    for (int i = 0; i < selected.size(); i++) {
                        File f = selected.get(i);
                        final int fi = i;
                        EventQueue.invokeLater(() -> {
                            progressBar.setValue(fi);
                            dlgStatus.setText("Processing " + (fi + 1) + "/" + selected.size()
                                + ": " + f.getName());
                        });

                        ImagePlus target = IJ.openImage(f.getAbsolutePath());
                        if (target == null)          { skipped++; IJ.log("Batch SKIP (cannot open): " + f.getName()); continue; }
                        if (target.getNSlices() < 2) { target.close(); skipped++; IJ.log("Batch SKIP (not 3D): " + f.getName()); continue; }

                        String basename = target.getShortTitle().replaceAll("\\.tiff?$", "");
                        String pattern  = customF ? folderPat : "{name} result";
                        String folder   = expandFolderTokens(pattern, basename, st, at);
                        File   outDir   = new File(f.getParent(), folder);

                        if (outDir.exists()) {
                            if (overwriteState[0] == 2) {
                                target.close(); skipped++;
                                IJ.log("Batch SKIP (folder exists, skip all): " + f.getName());
                                continue;
                            } else if (overwriteState[0] == 0) {
                                int[] response = {-1};
                                try {
                                    EventQueue.invokeAndWait(() -> {
                                        Object[] opts = {"Overwrite", "Skip", "Overwrite All", "Skip All"};
                                        response[0] = JOptionPane.showOptionDialog(
                                            BatchDialog.this,
                                            "Folder already exists:\n" + outDir.getAbsolutePath(),
                                            "Folder Exists",
                                            JOptionPane.DEFAULT_OPTION,
                                            JOptionPane.QUESTION_MESSAGE,
                                            null, opts, opts[0]);
                                    });
                                } catch (Exception ex) { response[0] = 1; }

                                if (response[0] == 1) { target.close(); skipped++; continue; }      // Skip
                                if (response[0] == 2) { overwriteState[0] = 1; }                    // Overwrite All
                                if (response[0] == 3) { overwriteState[0] = 2; target.close(); skipped++; continue; } // Skip All
                                // 0 = Overwrite once — fall through
                            }
                            // overwriteState[0] == 1 — overwrite all, fall through
                        }

                        String err = saveOneToDir(target, at, st, areaEn, params, outDir,
                            seedRoi, sizeRoi, areaRoi, resultRoi, csv, param, roiColor);
                        target.close();
                        if (err != null) {
                            IJ.log("Batch SKIP (" + err + "): " + f.getName());
                            skipped++;
                        } else {
                            ok++;
                        }
                    }
                    return new int[]{ok, skipped};
                }

                @Override
                protected void done() {
                    IJ.showStatus("");
                    try {
                        int[] counts = get();
                        progressBar.setValue(selected.size());
                        dlgStatus.setText(counts[0] + " processed, " + counts[1] + " skipped.");
                    } catch (Exception ex) {
                        IJ.error("Batch", "Batch failed: " + ex.getMessage());
                    }
                    runBtn.setEnabled(true);
                }
            }.execute();
        }
    }

    // =========================================================
    // Helpers
    // =========================================================

    private QuantifierParams buildParams() {
        Double minVol = minVolEnabled ? minVolVal : null;
        Double maxVol = maxVolEnabled ? maxVolVal : null;
        int conn = Integer.parseInt(connectivityChoice.getSelectedItem());
        boolean fillH = fillHolesCheck.getState();
        return new QuantifierParams(areaThreshold, minVol, maxVol,
            false, 1.0, 0.5, conn, fillH);
    }

    private Color selectedSeedPreviewColor() {
        return Color.decode(PREVIEW_COLOR_OPTIONS[seedColorChoice.getSelectedIndex()][1]);
    }

    private Color selectedRoiColor() {
        return Color.decode(PREVIEW_COLOR_OPTIONS[roiColorChoice.getSelectedIndex()][1]);
    }

    private int volToSlider(double vol) {
        if (volRangeMax <= volRangeMin || volRangeMin <= 0) return 0;
        vol = Math.max(volRangeMin, Math.min(volRangeMax, vol));
        double ratio = Math.log(vol / volRangeMin) / Math.log(volRangeMax / volRangeMin);
        return (int) Math.round(ratio * VOL_SLIDER_STEPS);
    }

    private double sliderToVol(int pos) {
        if (volRangeMax <= volRangeMin || volRangeMin <= 0) return volRangeMin;
        double ratio = pos / (double) VOL_SLIDER_STEPS;
        return volRangeMin * Math.pow(volRangeMax / volRangeMin, ratio);
    }

    private Scrollbar makeVolBar(double initVal) {
        int initPos = volToSlider(initVal);
        return new Scrollbar(Scrollbar.HORIZONTAL, initPos, 1, 0, VOL_SLIDER_STEPS + 1);
    }

    private static String formatVol(double v) {
        if (v >= 10) return String.format("%.1f", v);
        if (v >= 1)  return String.format("%.2f", v);
        return String.format("%.4f", v);
    }

    private static int parseIntOr(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception ex) { return fallback; }
    }

    private static double parseDoubleOr(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); } catch (Exception ex) { return fallback; }
    }

    // =========================================================
    // Z-watch
    // =========================================================

    private void startZWatch() {
        lastZ = imp.getCurrentSlice();
        zWatchTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                int z = imp.getCurrentSlice();
                if (z != lastZ) {
                    lastZ = z;
                    EventQueue.invokeLater(() -> updatePreviewForZChange());
                }
            }
        }, 300, 300);
    }

    private void placeNearImage() {
        Window active = imp.getWindow();
        if (active == null) return;
        Point p;
        try { p = active.getLocationOnScreen(); }
        catch (IllegalComponentStateException ex) { return; }
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
}
