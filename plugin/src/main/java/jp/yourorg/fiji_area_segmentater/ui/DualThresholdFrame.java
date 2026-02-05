package jp.yourorg.fiji_area_segmentater.ui;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.frame.PlugInFrame;
import ij.WindowManager;
import jp.yourorg.fiji_area_segmentater.alg.RandomWalkerRunner;
import jp.yourorg.fiji_area_segmentater.alg.SegmentationResult;
import jp.yourorg.fiji_area_segmentater.alg.WatershedRunner;
import jp.yourorg.fiji_area_segmentater.core.*;
import jp.yourorg.fiji_area_segmentater.preview.PreviewRenderer;
import jp.yourorg.fiji_area_segmentater.roi.RoiExporter;
import jp.yourorg.fiji_area_segmentater.util.IJLog;

import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class DualThresholdFrame extends PlugInFrame {
    private ImagePlus imp;
    private final ThresholdModel model;

    // UI components (AWT)
    private final Scrollbar fgBar;
    private final Scrollbar bgBar;
    private final TextField fgField;
    private final TextField bgField;
    private final Label fgLabel = new Label("Foreground (upper):");
    private final Label bgLabel = new Label("Background (lower):");

    private final Checkbox invertCb;

    private final CheckboxGroup previewGroup = new CheckboxGroup();
    private final Checkbox previewOff;
    private final Checkbox previewMarker;
    private final Checkbox previewRoi;

    private final CheckboxGroup methodGroup = new CheckboxGroup();
    private final Checkbox methodWs;
    private final Checkbox methodRw;

    private final CheckboxGroup surfaceGroup = new CheckboxGroup();
    private final Checkbox surfaceOriginal;
    private final Checkbox surfaceInvert;
    private final Checkbox surfaceGradient;

    private final CheckboxGroup connGroup = new CheckboxGroup();
    private final Checkbox conn4;
    private final Checkbox conn8;

    private final Button applyBtn = new Button("Apply");
    private final Button addRoiBtn = new Button("Add ROI");
    private final Button resetBtn = new Button("Reset");
    private boolean syncing = false;
    private final MarkerBuilder markerBuilder = new MarkerBuilder();
    private final PreviewRenderer previewRenderer = new PreviewRenderer();
    private SegmentationResult lastSegmentation;
    private MarkerResult cachedMarkers;
    private MarkerKey markerKey;
    private SegmentationResult cachedSegmentation;
    private SegmentationKey segmentationKey;
    private final HistogramPanel histogramPanel;
    private final Timer previewTimer = new Timer("dt-preview", true);
    private TimerTask previewTask;
    private final AtomicInteger previewGen = new AtomicInteger();
    private final Timer imageWatchTimer = new Timer("dt-image-watch", true);
    private TimerTask imageWatchTask;
    private static final int IMAGE_WATCH_MS = 300;
    private int lastImageCount = -1;

    private final Checkbox advancedToggle = new Checkbox("Advanced");
    private final Panel advancedPanel = new Panel(new GridLayout(0, 1));
    private final Checkbox absorbUnknownCb = new Checkbox("Absorb UNKNOWN islands");
    private final Choice overlapChoice = new Choice();
    private final TextField debounceField = new TextField("", 4);
    private final Choice markerSourceChoice = new Choice();
    private final Choice binaryChoice = new Choice();
    private int[] binaryChoiceIds = new int[0];
    private final TextField findMaximaField = new TextField("", 6);
    private final Scrollbar findMaximaBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 101);
    private final TextField rwBetaField = new TextField("", 10);
    private final Scrollbar rwBetaBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 10001);
    private final Checkbox preprocessingCb = new Checkbox("Enable Preprocessing");
    private final TextField sigmaSurfaceField = new TextField("", 4);
    private final Scrollbar sigmaSurfaceBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 501);
    private final TextField sigmaSeedField = new TextField("", 4);
    private final Scrollbar sigmaSeedBar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, 501);
    private final Button addSeedBtn = new Button("Add Seed from Selection");
    private final Button clearSeedsBtn = new Button("Clear Manual Seeds");
    private final Checkbox showSeedCb = new Checkbox("ShowSeed");
    private final Checkbox showSeedCrossCb = new Checkbox("ShowSeedCross");
    private final Checkbox showDomainCb = new Checkbox("ShowDomain");
    private final Checkbox showBgCb = new Checkbox("ShowBG");
    private final TextField seedColorField = new TextField("", 6);
    private final TextField domainColorField = new TextField("", 6);
    private final TextField bgColorField = new TextField("", 6);
    private final TextField opacityField = new TextField("", 4);
    private final Scrollbar opacityBar = new Scrollbar(Scrollbar.HORIZONTAL, 35, 1, 0, 101);
    private final TextField seedMinAreaField = new TextField("", 6);
    private final TextField seedMaxAreaField = new TextField("", 6);

    public DualThresholdFrame(ImagePlus imp) {
        super("Area_Segmentater");
        this.imp = imp;
        this.model = new ThresholdModel(imp);

        int min = model.getMinValue();
        int max = model.getMaxValue();
        if (max <= min) max = min + 1;
        fgBar = new Scrollbar(Scrollbar.HORIZONTAL, model.getTFg(), 1, min, max + 1);
        bgBar = new Scrollbar(Scrollbar.HORIZONTAL, model.getTBg(), 1, min, max + 1);
        fgField = new TextField(Integer.toString(model.getTFg()), 5);
        bgField = new TextField(Integer.toString(model.getTBg()), 5);

        invertCb = new Checkbox("Invert (foreground on low intensity)", model.isInvert());

        previewOff = new Checkbox("Off", previewGroup, model.getPreviewMode() == PreviewMode.OFF);
        previewMarker = new Checkbox("Seed preview", previewGroup, model.getPreviewMode() == PreviewMode.MARKER_BOUNDARIES);
        previewRoi = new Checkbox("ROI boundaries", previewGroup, model.getPreviewMode() == PreviewMode.ROI_BOUNDARIES);

        methodWs = new Checkbox("Watershed", methodGroup, model.getMethod() == Method.WATERSHED);
        methodRw = new Checkbox("Random Walker", methodGroup, model.getMethod() == Method.RANDOM_WALKER);

        surfaceOriginal = new Checkbox("Original", surfaceGroup, model.getSurface() == Surface.ORIGINAL);
        surfaceInvert = new Checkbox("Invert Original", surfaceGroup, model.getSurface() == Surface.INVERT_ORIGINAL);
        surfaceGradient = new Checkbox("Gradient (Sobel)", surfaceGroup, model.getSurface() == Surface.GRADIENT_SOBEL);

        conn4 = new Checkbox("4-connected", connGroup, model.getConnectivity() == Connectivity.C4);
        conn8 = new Checkbox("8-connected", connGroup, model.getConnectivity() == Connectivity.C8);

        histogramPanel = new HistogramPanel(imp, model, this::onHistogramThresholds);
        buildLayout();
        wireEvents();
        updateSurfaceEnabled();
        startImageWatch();
        syncAllFromModel();
        pack();
        placeNearActiveImageWindow();
    }

    private void placeNearActiveImageWindow() {
        Window active = imp != null ? imp.getWindow() : null;
        if (active == null) return;
        Point p;
        try {
            p = active.getLocationOnScreen();
        } catch (IllegalComponentStateException ex) {
            return;
        }
        int margin = 8;
        int x = p.x + active.getWidth() + margin;
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

        Panel sliders = new Panel(new GridLayout(2, 1));

        Panel fgRow = new Panel(new GridBagLayout());
        GridBagConstraints fgC = new GridBagConstraints();
        fgC.gridy = 0;
        fgC.insets = new Insets(2, 4, 2, 4);
        fgC.anchor = GridBagConstraints.WEST;
        fgC.gridx = 0;
        fgRow.add(fgLabel, fgC);
        fgC.gridx = 1;
        fgC.weightx = 1.0;
        fgC.fill = GridBagConstraints.HORIZONTAL;
        fgBar.setPreferredSize(new Dimension(260, 18));
        fgRow.add(fgBar, fgC);
        fgC.gridx = 2;
        fgC.weightx = 0.0;
        fgC.fill = GridBagConstraints.NONE;
        fgRow.add(fgField, fgC);

        Panel bgRow = new Panel(new GridBagLayout());
        GridBagConstraints bgC = new GridBagConstraints();
        bgC.gridy = 0;
        bgC.insets = new Insets(2, 4, 2, 4);
        bgC.anchor = GridBagConstraints.WEST;
        bgC.gridx = 0;
        bgRow.add(bgLabel, bgC);
        bgC.gridx = 1;
        bgC.weightx = 1.0;
        bgC.fill = GridBagConstraints.HORIZONTAL;
        bgBar.setPreferredSize(new Dimension(260, 18));
        bgRow.add(bgBar, bgC);
        bgC.gridx = 2;
        bgC.weightx = 0.0;
        bgC.fill = GridBagConstraints.NONE;
        bgRow.add(bgField, bgC);

        sliders.add(fgRow);
        sliders.add(bgRow);

        Panel options = new Panel(new GridLayout(0, 1));
        options.add(invertCb);

        Panel previewPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        previewPanel.add(new Label("Preview:"));
        previewPanel.add(previewOff);
        previewPanel.add(previewMarker);
        previewPanel.add(previewRoi);

        Panel methodPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        methodPanel.add(new Label("Segmentation:"));
        methodPanel.add(methodWs);
        methodPanel.add(methodRw);

        Panel surfacePanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        surfacePanel.add(new Label("Surface (WS):"));
        surfacePanel.add(surfaceInvert);
        surfacePanel.add(surfaceOriginal);
        surfacePanel.add(surfaceGradient);

        rwBetaBar.setPreferredSize(new Dimension(220, 18));
        Panel rwBetaPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        rwBetaPanel.add(new Label("RW beta:"));
        rwBetaPanel.add(rwBetaField);
        rwBetaPanel.add(rwBetaBar);

        Panel connPanel = new Panel(new FlowLayout(FlowLayout.LEFT));
        connPanel.add(new Label("Connectivity:"));
        connPanel.add(conn4);
        connPanel.add(conn8);

        options.add(previewPanel);
        options.add(methodPanel);
        options.add(surfacePanel);
        options.add(rwBetaPanel);
        options.add(connPanel);
        options.add(advancedToggle);

        buildAdvancedPanel();

        Panel optionsWrap = new Panel(new BorderLayout());
        optionsWrap.add(options, BorderLayout.NORTH);
        optionsWrap.add(advancedPanel, BorderLayout.CENTER);

        Panel center = new Panel(new BorderLayout());
        center.add(sliders, BorderLayout.NORTH);
        center.add(optionsWrap, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        Panel buttons = new Panel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(applyBtn);
        buttons.add(addRoiBtn);
        buttons.add(resetBtn);
        add(buttons, BorderLayout.SOUTH);
    }

    private void buildAdvancedPanel() {
        advancedPanel.setVisible(false);
        overlapChoice.add("First wins");
        overlapChoice.add("Last wins");
        markerSourceChoice.add("Threshold Components");
        markerSourceChoice.add("ROI Manager");
        markerSourceChoice.add("Binary Image");
        markerSourceChoice.add("Find Maxima");
        markerSourceChoice.add("Manual Selection");

        Panel absorbRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        absorbRow.add(absorbUnknownCb);

        Panel overlapRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        overlapRow.add(new Label("ROI overlap:"));
        overlapRow.add(overlapChoice);

        Panel debounceRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        debounceRow.add(new Label("Preview debounce (ms):"));
        debounceRow.add(debounceField);

        Panel sourceRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        sourceRow.add(new Label("Seed source:"));
        sourceRow.add(markerSourceChoice);

        Panel binaryRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        binaryRow.add(new Label("Binary image:"));
        binaryRow.add(binaryChoice);

        findMaximaBar.setPreferredSize(new Dimension(220, 18));
        opacityBar.setPreferredSize(new Dimension(220, 18));
        sigmaSurfaceBar.setPreferredSize(new Dimension(220, 18));
        sigmaSeedBar.setPreferredSize(new Dimension(220, 18));

        Panel maximaRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        maximaRow.add(new Label("Find Maxima tol:"));
        maximaRow.add(findMaximaField);
        maximaRow.add(findMaximaBar);

        Panel preprocessRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        preprocessRow.add(preprocessingCb);

        Panel sigmaSurfaceRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        sigmaSurfaceRow.add(new Label("Sigma (surface):"));
        sigmaSurfaceRow.add(sigmaSurfaceField);
        sigmaSurfaceRow.add(sigmaSurfaceBar);

        Panel sigmaSeedRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        sigmaSeedRow.add(new Label("Sigma (seed):"));
        sigmaSeedRow.add(sigmaSeedField);
        sigmaSeedRow.add(sigmaSeedBar);

        Panel manualRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        manualRow.add(addSeedBtn);
        manualRow.add(clearSeedsBtn);

        Panel seedRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        seedRow.add(new Label("Seed"));
        seedRow.add(seedColorField);
        seedRow.add(showSeedCb);
        seedRow.add(showSeedCrossCb);

        Panel domainRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        domainRow.add(new Label("Domain"));
        domainRow.add(domainColorField);
        domainRow.add(showDomainCb);

        Panel bgRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        bgRow.add(new Label("BG"));
        bgRow.add(bgColorField);
        bgRow.add(showBgCb);

        Panel opacityRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        opacityRow.add(new Label("Opacity"));
        opacityRow.add(opacityField);
        opacityRow.add(opacityBar);

        Panel areaRow = new Panel(new FlowLayout(FlowLayout.LEFT));
        areaRow.add(new Label("Seed area min/max:"));
        areaRow.add(seedMinAreaField);
        areaRow.add(seedMaxAreaField);

        advancedPanel.add(absorbRow);
        advancedPanel.add(overlapRow);
        advancedPanel.add(debounceRow);
        advancedPanel.add(sourceRow);
        advancedPanel.add(binaryRow);
        advancedPanel.add(maximaRow);
        advancedPanel.add(preprocessRow);
        advancedPanel.add(sigmaSurfaceRow);
        advancedPanel.add(sigmaSeedRow);
        advancedPanel.add(manualRow);
        advancedPanel.add(seedRow);
        advancedPanel.add(domainRow);
        advancedPanel.add(bgRow);
        advancedPanel.add(opacityRow);
        advancedPanel.add(areaRow);
    }

    private void wireEvents() {
        // sliders -> model
        fgBar.addAdjustmentListener(e -> {
            if (syncing) return;
            model.setTFg(fgBar.getValue());
            syncAllFromModel();
            onStateChanged();
        });
        bgBar.addAdjustmentListener(e -> {
            if (syncing) return;
            model.setTBg(bgBar.getValue());
            syncAllFromModel();
            onStateChanged();
        });

        // numeric fields -> model (on Enter or focus lost)
        fgField.addActionListener(e -> commitFgField());
        fgField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitFgField(); }
        });
        bgField.addActionListener(e -> commitBgField());
        bgField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitBgField(); }
        });

        invertCb.addItemListener(e -> { if (syncing) return; model.setInvert(invertCb.getState()); onStateChanged(); });

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

        ItemListener methodListener = e -> {
            if (syncing) return;
            if (methodGroup.getSelectedCheckbox() == methodWs) model.setMethod(Method.WATERSHED);
            else model.setMethod(Method.RANDOM_WALKER);
            updateSurfaceEnabled();
            onStateChanged();
        };
        methodWs.addItemListener(methodListener);
        methodRw.addItemListener(methodListener);

        ItemListener surfaceListener = e -> {
            if (syncing) return;
            if (surfaceGroup.getSelectedCheckbox() == surfaceOriginal) model.setSurface(Surface.ORIGINAL);
            else if (surfaceGroup.getSelectedCheckbox() == surfaceInvert) model.setSurface(Surface.INVERT_ORIGINAL);
            else model.setSurface(Surface.GRADIENT_SOBEL);
            onStateChanged();
        };
        surfaceOriginal.addItemListener(surfaceListener);
        surfaceInvert.addItemListener(surfaceListener);
        surfaceGradient.addItemListener(surfaceListener);

        rwBetaField.addActionListener(e -> commitRwBetaField());
        rwBetaField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitRwBetaField(); }
        });
        rwBetaBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = rwBetaFromSlider(rwBetaBar.getValue());
            model.setRandomWalkerBeta(v);
            rwBetaField.setText(Double.toString(v));
            clearCaches();
            onStateChanged();
        });

        ItemListener connListener = e -> {
            if (syncing) return;
            if (connGroup.getSelectedCheckbox() == conn4) model.setConnectivity(Connectivity.C4);
            else model.setConnectivity(Connectivity.C8);
            onStateChanged();
        };
        conn4.addItemListener(connListener);
        conn8.addItemListener(connListener);

        applyBtn.addActionListener(e -> model.requestApply());
        addRoiBtn.addActionListener(e -> model.requestAddRoi());
        resetBtn.addActionListener(e -> {
            model.resetDefaults();
            syncAllFromModel();
            clearOverlay();
        });

        advancedToggle.addItemListener(e -> {
            advancedPanel.setVisible(advancedToggle.getState());
            pack();
        });

        absorbUnknownCb.addItemListener(e -> {
            if (syncing) return;
            model.setAbsorbUnknown(absorbUnknownCb.getState());
            clearCaches();
            onStateChanged();
        });

        overlapChoice.addItemListener(e -> {
            if (syncing) return;
            model.setOverlapRule(overlapChoice.getSelectedIndex() == 0 ? OverlapRule.FIRST_WINS : OverlapRule.LAST_WINS);
            clearCaches();
            onStateChanged();
        });

        debounceField.addActionListener(e -> commitDebounceField());
        debounceField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitDebounceField(); }
        });

        markerSourceChoice.addItemListener(e -> {
            if (syncing) return;
            model.setMarkerSource(markerSourceFromIndex(markerSourceChoice.getSelectedIndex()));
            if (model.getMarkerSource() == MarkerSource.BINARY_IMAGE && model.getBinarySourceId() <= 0
                && binaryChoiceIds.length > 0) {
                int idx = binaryChoice.getSelectedIndex();
                if (idx >= 0 && idx < binaryChoiceIds.length) model.setBinarySourceId(binaryChoiceIds[idx]);
            }
            clearCaches();
            syncAllFromModel();
            onStateChanged();
        });

        binaryChoice.addItemListener(e -> {
            if (syncing) return;
            int idx = binaryChoice.getSelectedIndex();
            if (idx >= 0 && idx < binaryChoiceIds.length) {
                model.setBinarySourceId(binaryChoiceIds[idx]);
            }
            clearCaches();
            onStateChanged();
        });

        findMaximaField.addActionListener(e -> commitFindMaximaField());
        findMaximaField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitFindMaximaField(); }
        });
        findMaximaBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = findMaximaFromSlider(findMaximaBar.getValue());
            model.setFindMaximaTolerance(v);
            findMaximaField.setText(Double.toString(v));
            clearCaches();
            onStateChanged();
        });

        preprocessingCb.addItemListener(e -> {
            if (syncing) return;
            model.setPreprocessingEnabled(preprocessingCb.getState());
            updatePreprocessEnabled();
            clearCaches();
            onStateChanged();
        });
        sigmaSurfaceField.addActionListener(e -> commitSigmaSurfaceField());
        sigmaSurfaceField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitSigmaSurfaceField(); }
        });
        sigmaSurfaceBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = sigmaFromSlider(sigmaSurfaceBar.getValue());
            model.setSigmaSurface(v);
            sigmaSurfaceField.setText(Double.toString(v));
            clearCaches();
            onStateChanged();
        });
        sigmaSeedField.addActionListener(e -> commitSigmaSeedField());
        sigmaSeedField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitSigmaSeedField(); }
        });
        sigmaSeedBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = sigmaFromSlider(sigmaSeedBar.getValue());
            model.setSigmaSeed(v);
            sigmaSeedField.setText(Double.toString(v));
            clearCaches();
            onStateChanged();
        });

        seedColorField.addActionListener(e -> commitAppearanceFields());
        seedColorField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitAppearanceFields(); }
        });
        domainColorField.addActionListener(e -> commitAppearanceFields());
        domainColorField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitAppearanceFields(); }
        });
        bgColorField.addActionListener(e -> commitAppearanceFields());
        bgColorField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitAppearanceFields(); }
        });
        opacityField.addActionListener(e -> commitAppearanceFields());
        opacityField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitAppearanceFields(); }
        });
        opacityBar.addAdjustmentListener(e -> {
            if (syncing) return;
            double v = opacityBar.getValue() / 100.0;
            model.getAppearance().setOpacity(v);
            opacityField.setText(Double.toString(v));
            onStateChanged();
        });
        showSeedCb.addItemListener(e -> {
            if (syncing) return;
            model.getAppearance().setShowSeed(showSeedCb.getState());
            onStateChanged();
        });
        showSeedCrossCb.addItemListener(e -> {
            if (syncing) return;
            model.getAppearance().setShowSeedCentroids(showSeedCrossCb.getState());
            onStateChanged();
        });
        showDomainCb.addItemListener(e -> {
            if (syncing) return;
            model.getAppearance().setShowDomain(showDomainCb.getState());
            onStateChanged();
        });
        showBgCb.addItemListener(e -> {
            if (syncing) return;
            model.getAppearance().setShowBg(showBgCb.getState());
            onStateChanged();
        });

        addSeedBtn.addActionListener(e -> {
            Roi roi = imp != null ? imp.getRoi() : null;
            if (roi == null) {
                model.showNotImplemented("No selection ROI found.");
                return;
            }
            model.addManualSeed(roi);
            clearCaches();
            onStateChanged();
        });
        clearSeedsBtn.addActionListener(e -> {
            model.clearManualSeeds();
            clearCaches();
            onStateChanged();
        });

        seedMinAreaField.addActionListener(e -> commitSeedAreaFields());
        seedMinAreaField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitSeedAreaFields(); }
        });
        seedMaxAreaField.addActionListener(e -> commitSeedAreaFields());
        seedMaxAreaField.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) { commitSeedAreaFields(); }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                stopImageWatch();
                dispose();
            }
        });

        // Hook model events to avoid future rewiring (Phase3+)
        model.setUiCallback(this::onModelAction);
    }

    private void onStateChanged() {
        if (model.getPreviewMode() == PreviewMode.OFF) {
            clearOverlay();
            cancelPreview();
            return;
        }
        schedulePreview(model.getPreviewMode());
    }

    private void onModelAction(ThresholdModel.Action action) {
        // Phase4/5 will implement Apply/Add ROI here.
        switch (action) {
            case APPLY:
                runApply();
                break;
            case ADD_ROI:
                runAddRoi();
                break;
        }
    }

    private void runApply() {
        SegmentationResult result = computeSegmentation();
        if (result == null || result.labelImage == null) return;
        lastSegmentation = result;
        result.labelImage.show();
    }

    private void runAddRoi() {
        SegmentationResult result = getSegmentationResult();
        if (result == null || result.labelImage == null) return;
        lastSegmentation = result;
        new RoiExporter().exportToRoiManager(result.labelImage);
    }

    private SegmentationResult computeSegmentation() {
        SegmentationResult cached = getSegmentationResult();
        if (cached != null) return cached;
        return null;
    }

    private void startImageWatch() {
        updateBinaryChoice();
        imageWatchTask = new TimerTask() {
            @Override
            public void run() {
                int[] ids = WindowManager.getIDList();
                int count = (ids == null) ? 0 : ids.length;
                if (count != lastImageCount) {
                    EventQueue.invokeLater(() -> updateBinaryChoice());
                }
                ImagePlus current = WindowManager.getCurrentImage();
                if (current == null || current == imp) return;
                EventQueue.invokeLater(() -> onImageChanged(imp, current));
            }
        };
        imageWatchTimer.scheduleAtFixedRate(imageWatchTask, IMAGE_WATCH_MS, IMAGE_WATCH_MS);
    }

    private void stopImageWatch() {
        if (imageWatchTask != null) {
            imageWatchTask.cancel();
            imageWatchTask = null;
        }
    }

    private void onImageChanged(ImagePlus oldImp, ImagePlus newImp) {
        if (newImp == null || newImp == oldImp) return;
        if (oldImp != null) {
            oldImp.setOverlay((Overlay) null);
            oldImp.setRoi((Roi) null);
        }
        this.imp = newImp;
        model.setImage(newImp);
        histogramPanel.setImage(newImp);
        updateFindMaximaRange();
        updateBinaryChoice();
        clearCaches();
        model.resetManualSeeds();
        syncAllFromModel();
        onStateChanged();
    }

    private void schedulePreview(PreviewMode mode) {
        if (mode == PreviewMode.OFF) return;
        int gen = previewGen.incrementAndGet();
        cancelPreviewTaskOnly();
        previewTask = new TimerTask() {
            @Override
            public void run() {
                if (mode == PreviewMode.MARKER_BOUNDARIES) {
                    renderMarkerPreview(gen);
                } else {
                    renderSegmentationPreview(gen);
                }
            }
        };
        int delay = Math.max(50, model.getPreviewDebounceMs());
        previewTimer.schedule(previewTask, delay);
    }

    private void renderMarkerPreview(int generation) {
        MarkerResult markers = getMarkerResult();
        if (previewGen.get() != generation) return;
        EventQueue.invokeLater(() -> {
            if (!isPreviewGenerationValid(generation)) return;
            previewRenderer.renderMarkerFill(imp, markers, model.getAppearance());
        });
    }

    private void renderSegmentationPreview(int generation) {
        SegmentationResult result = getSegmentationResult();
        if (previewGen.get() != generation) return;
        EventQueue.invokeLater(() -> {
            if (!isPreviewGenerationValid(generation)) return;
            previewRenderer.renderSegmentationBoundaries(imp, result);
        });
    }

    private boolean isPreviewGenerationValid(int generation) {
        return previewGen.get() == generation && model.getPreviewMode() != PreviewMode.OFF;
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

    private synchronized MarkerResult getMarkerResult() {
        MarkerKey key = createMarkerKey();
        if (markerKey != null && markerKey.equals(key) && cachedMarkers != null) {
            return cachedMarkers;
        }
        cachedMarkers = markerBuilder.build(imp, model);
        markerKey = key;
        return cachedMarkers;
    }

    private synchronized SegmentationResult getSegmentationResult() {
        SegmentationKey key = createSegmentationKey();
        if (segmentationKey != null && segmentationKey.equals(key) && cachedSegmentation != null) {
            return cachedSegmentation;
        }
        MarkerResult markers = markerBuilder.build(imp, model);
        if (markers.fgCount == 0 && isAreaSeedSource(model.getMarkerSource())) {
            IJLog.warn("Seed is empty after filtering.");
            return null;
        }
        cachedSegmentation = runSegmentation(markers);
        segmentationKey = key;
        return cachedSegmentation;
    }

    private MarkerKey createMarkerKey() {
        return new MarkerKey(
            imp.getID(),
            model.getTFg(),
            model.getTBg(),
            model.isInvert(),
            model.getConnectivity(),
            model.isAbsorbUnknown(),
            model.getMarkerSource(),
            model.getOverlapRule(),
            model.getBinarySourceId(),
            model.getFindMaximaTolerance(),
            model.isPreprocessingEnabled(),
            model.getSigmaSeed(),
            model.getSeedMinArea(),
            model.getSeedMaxArea(),
            model.getManualSeedVersion(),
            imp.getCurrentSlice()
        );
    }

    private SegmentationKey createSegmentationKey() {
        return new SegmentationKey(
            imp.getID(),
            model.getTFg(),
            model.getTBg(),
            model.isInvert(),
            model.getConnectivity(),
            model.getMethod(),
            model.getSurface(),
            model.isAbsorbUnknown(),
            model.getMarkerSource(),
            model.getOverlapRule(),
            model.getBinarySourceId(),
            model.getFindMaximaTolerance(),
            model.isPreprocessingEnabled(),
            model.getSigmaSurface(),
            model.getSigmaSeed(),
            model.getRandomWalkerBeta(),
            model.getSeedMinArea(),
            model.getSeedMaxArea(),
            model.getManualSeedVersion(),
            imp.getCurrentSlice()
        );
    }

    private SegmentationResult runSegmentation(MarkerResult markers) {
        if (model.getMethod() == Method.WATERSHED) {
            return new WatershedRunner().run(
                imp, markers, model.getSurface(), model.getConnectivity(),
                model.isPreprocessingEnabled(), model.getSigmaSurface()
            );
        }
        return new RandomWalkerRunner().run(
            imp, markers, model.getConnectivity(),
            model.isPreprocessingEnabled(), model.getSigmaSurface(), model.getRandomWalkerBeta()
        );
    }

    private boolean isAreaSeedSource(MarkerSource source) {
        return source == MarkerSource.THRESHOLD_COMPONENTS
            || source == MarkerSource.ROI_MANAGER
            || source == MarkerSource.BINARY_IMAGE;
    }

    private void onHistogramThresholds(int tBg, int tFg) {
        if (syncing) return;
        int curBg = model.getTBg();
        int curFg = model.getTFg();
        if (tBg != curBg && tFg == curFg) {
            model.setTBg(tBg);
            model.setTFg(curFg);
        } else if (tFg != curFg && tBg == curBg) {
            model.setTFg(tFg);
            model.setTBg(curBg);
        } else {
            model.setTBg(tBg);
            model.setTFg(tFg);
        }
        syncAllFromModel();
        onStateChanged();
    }

    private void commitDebounceField() {
        if (syncing) return;
        int v = parseOrKeep(debounceField.getText(), model.getPreviewDebounceMs());
        v = Math.max(50, Math.min(2000, v));
        model.setPreviewDebounceMs(v);
        debounceField.setText(Integer.toString(v));
    }

    private void commitFindMaximaField() {
        if (syncing) return;
        double v = parseOrKeepDouble(findMaximaField.getText(), model.getFindMaximaTolerance());
        if (v < 0) v = 0;
        model.setFindMaximaTolerance(v);
        findMaximaField.setText(Double.toString(v));
        findMaximaBar.setValue(findMaximaToSlider(v));
        clearCaches();
        onStateChanged();
    }

    private void commitRwBetaField() {
        if (syncing) return;
        double v = parseOrKeepDouble(rwBetaField.getText(), model.getRandomWalkerBeta());
        v = clampRwBeta(v);
        model.setRandomWalkerBeta(v);
        rwBetaField.setText(Double.toString(v));
        rwBetaBar.setValue(rwBetaToSlider(v));
        clearCaches();
        onStateChanged();
    }

    private void commitSigmaSurfaceField() {
        if (syncing) return;
        double v = parseOrKeepDouble(sigmaSurfaceField.getText(), model.getSigmaSurface());
        v = clampSigma(v);
        model.setSigmaSurface(v);
        sigmaSurfaceField.setText(Double.toString(v));
        sigmaSurfaceBar.setValue(sigmaToSlider(v));
        clearCaches();
        onStateChanged();
    }

    private void commitSigmaSeedField() {
        if (syncing) return;
        double v = parseOrKeepDouble(sigmaSeedField.getText(), model.getSigmaSeed());
        v = clampSigma(v);
        model.setSigmaSeed(v);
        sigmaSeedField.setText(Double.toString(v));
        sigmaSeedBar.setValue(sigmaToSlider(v));
        clearCaches();
        onStateChanged();
    }

    private void commitAppearanceFields() {
        if (syncing) return;
        AppearanceSettings app = model.getAppearance();
        app.setSeedColor(parseColor(seedColorField.getText(), app.getSeedColor()));
        app.setDomainColor(parseColor(domainColorField.getText(), app.getDomainColor()));
        app.setBgColor(parseColor(bgColorField.getText(), app.getBgColor()));
        double opacity = parseOrKeepDouble(opacityField.getText(), app.getOpacity());
        if (opacity > 1.0) opacity = opacity / 100.0;
        opacity = Math.max(0.0, Math.min(1.0, opacity));
        app.setOpacity(opacity);
        seedColorField.setText(formatColor(app.getSeedColor()));
        domainColorField.setText(formatColor(app.getDomainColor()));
        bgColorField.setText(formatColor(app.getBgColor()));
        opacityField.setText(Double.toString(app.getOpacity()));
        opacityBar.setValue((int) Math.round(app.getOpacity() * 100.0));
        onStateChanged();
    }

    private void commitSeedAreaFields() {
        if (syncing) return;
        int minArea = parseOrKeep(seedMinAreaField.getText(), model.getSeedMinArea());
        int maxArea = parseOrKeep(seedMaxAreaField.getText(), model.getSeedMaxArea());
        if (minArea < 0) minArea = 0;
        if (maxArea < 0) maxArea = 0;
        model.setSeedMinArea(minArea);
        model.setSeedMaxArea(maxArea);
        seedMinAreaField.setText(Integer.toString(minArea));
        seedMaxAreaField.setText(Integer.toString(maxArea));
        clearCaches();
        onStateChanged();
    }

    private MarkerSource markerSourceFromIndex(int idx) {
        switch (idx) {
            case 1: return MarkerSource.ROI_MANAGER;
            case 2: return MarkerSource.BINARY_IMAGE;
            case 3: return MarkerSource.FIND_MAXIMA;
            case 4: return MarkerSource.MANUAL_SELECTION;
            default: return MarkerSource.THRESHOLD_COMPONENTS;
        }
    }

    private int markerSourceToIndex(MarkerSource source) {
        if (source == MarkerSource.ROI_MANAGER) return 1;
        if (source == MarkerSource.BINARY_IMAGE) return 2;
        if (source == MarkerSource.FIND_MAXIMA) return 3;
        if (source == MarkerSource.MANUAL_SELECTION) return 4;
        return 0;
    }

    private void clearCaches() {
        cachedMarkers = null;
        markerKey = null;
        cachedSegmentation = null;
        segmentationKey = null;
        lastSegmentation = null;
    }

    private void updateBinaryChoice() {
        int[] ids = WindowManager.getIDList();
        if (ids == null) ids = new int[0];
        if (lastImageCount == ids.length && binaryChoiceIds.length == ids.length) return;
        binaryChoice.removeAll();
        binaryChoiceIds = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            ImagePlus img = WindowManager.getImage(ids[i]);
            String title = (img != null) ? img.getTitle() : ("Image " + ids[i]);
            binaryChoice.add(title);
            binaryChoiceIds[i] = ids[i];
        }
        lastImageCount = ids.length;
        int sel = 0;
        for (int i = 0; i < binaryChoiceIds.length; i++) {
            if (binaryChoiceIds[i] == model.getBinarySourceId()) { sel = i; break; }
        }
        if (binaryChoice.getItemCount() > 0) binaryChoice.select(sel);
    }

    private void updateFindMaximaRange() {
        int min = model.getMinValue();
        int max = model.getMaxValue();
        int range = Math.max(1, max - min);
        findMaximaBar.setMinimum(0);
        findMaximaBar.setMaximum(range + 1);
    }

    private int findMaximaToSlider(double v) {
        int min = model.getMinValue();
        int max = model.getMaxValue();
        int range = Math.max(1, max - min);
        int iv = (int) Math.round(v);
        if (iv < 0) iv = 0;
        if (iv > range) iv = range;
        return iv;
    }

    private double findMaximaFromSlider(int v) {
        int min = model.getMinValue();
        int max = model.getMaxValue();
        int range = Math.max(1, max - min);
        if (v < 0) v = 0;
        if (v > range) v = range;
        return (double) v;
    }

    private int sigmaToSlider(double v) {
        double clamp = clampSigma(v);
        return (int) Math.round(clamp * 100.0);
    }

    private double sigmaFromSlider(int v) {
        int clamp = Math.max(0, Math.min(500, v));
        return clamp / 100.0;
    }

    private double clampSigma(double v) {
        if (v < 0.0) return 0.0;
        if (v > 5.0) return 5.0;
        return v;
    }

    private int rwBetaToSlider(double v) {
        return (int) Math.round(clampRwBeta(v) * 100000.0);
    }

    private double rwBetaFromSlider(int v) {
        int clamp = Math.max(0, Math.min(10000, v));
        return clamp / 100000.0;
    }

    private double clampRwBeta(double v) {
        if (v < 0.0) return 0.0;
        if (v > 0.1) return 0.1;
        return v;
    }

    private static class MarkerKey {
        private final int tFg;
        private final int tBg;
        private final boolean invert;
        private final Connectivity connectivity;
        private final boolean absorbUnknown;
        private final MarkerSource markerSource;
        private final OverlapRule overlapRule;
        private final int binarySourceId;
        private final double findMaximaTolerance;
        private final boolean preprocessingEnabled;
        private final double sigmaSeed;
        private final int seedMinArea;
        private final int seedMaxArea;
        private final int manualSeedVersion;
        private final int imageId;
        private final int slice;

        MarkerKey(int imageId, int tFg, int tBg, boolean invert, Connectivity connectivity,
                  boolean absorbUnknown, MarkerSource markerSource, OverlapRule overlapRule, int binarySourceId,
                  double findMaximaTolerance, boolean preprocessingEnabled, double sigmaSeed,
                  int seedMinArea, int seedMaxArea, int manualSeedVersion, int slice) {
            this.imageId = imageId;
            this.tFg = tFg;
            this.tBg = tBg;
            this.invert = invert;
            this.connectivity = connectivity;
            this.absorbUnknown = absorbUnknown;
            this.markerSource = markerSource;
            this.overlapRule = overlapRule;
            this.binarySourceId = binarySourceId;
            this.findMaximaTolerance = findMaximaTolerance;
            this.preprocessingEnabled = preprocessingEnabled;
            this.sigmaSeed = sigmaSeed;
            this.seedMinArea = seedMinArea;
            this.seedMaxArea = seedMaxArea;
            this.manualSeedVersion = manualSeedVersion;
            this.slice = slice;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof MarkerKey)) return false;
            MarkerKey other = (MarkerKey) obj;
            return tFg == other.tFg
                && tBg == other.tBg
                && invert == other.invert
                && connectivity == other.connectivity
                && absorbUnknown == other.absorbUnknown
                && markerSource == other.markerSource
                && overlapRule == other.overlapRule
                && binarySourceId == other.binarySourceId
                && Double.compare(findMaximaTolerance, other.findMaximaTolerance) == 0
                && preprocessingEnabled == other.preprocessingEnabled
                && Double.compare(sigmaSeed, other.sigmaSeed) == 0
                && seedMinArea == other.seedMinArea
                && seedMaxArea == other.seedMaxArea
                && manualSeedVersion == other.manualSeedVersion
                && slice == other.slice
                && imageId == other.imageId;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(imageId);
            result = 31 * result + Integer.hashCode(tFg);
            result = 31 * result + Integer.hashCode(tBg);
            result = 31 * result + Boolean.hashCode(invert);
            result = 31 * result + (connectivity != null ? connectivity.hashCode() : 0);
            result = 31 * result + Boolean.hashCode(absorbUnknown);
            result = 31 * result + (markerSource != null ? markerSource.hashCode() : 0);
            result = 31 * result + (overlapRule != null ? overlapRule.hashCode() : 0);
            result = 31 * result + Integer.hashCode(binarySourceId);
            long tmp = Double.doubleToLongBits(findMaximaTolerance);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            result = 31 * result + Boolean.hashCode(preprocessingEnabled);
            tmp = Double.doubleToLongBits(sigmaSeed);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            result = 31 * result + Integer.hashCode(seedMinArea);
            result = 31 * result + Integer.hashCode(seedMaxArea);
            result = 31 * result + Integer.hashCode(manualSeedVersion);
            result = 31 * result + Integer.hashCode(slice);
            return result;
        }
    }

    private static class SegmentationKey {
        private final int tFg;
        private final int tBg;
        private final boolean invert;
        private final Connectivity connectivity;
        private final Method method;
        private final Surface surface;
        private final boolean absorbUnknown;
        private final MarkerSource markerSource;
        private final OverlapRule overlapRule;
        private final int binarySourceId;
        private final double findMaximaTolerance;
        private final boolean preprocessingEnabled;
        private final double sigmaSurface;
        private final double sigmaSeed;
        private final double randomWalkerBeta;
        private final int seedMinArea;
        private final int seedMaxArea;
        private final int manualSeedVersion;
        private final int imageId;
        private final int slice;

        SegmentationKey(int imageId, int tFg, int tBg, boolean invert, Connectivity connectivity,
                        Method method, Surface surface, boolean absorbUnknown, MarkerSource markerSource,
                        OverlapRule overlapRule,
                        int binarySourceId, double findMaximaTolerance, boolean preprocessingEnabled,
                        double sigmaSurface, double sigmaSeed, double randomWalkerBeta, int seedMinArea, int seedMaxArea,
                        int manualSeedVersion, int slice) {
            this.imageId = imageId;
            this.tFg = tFg;
            this.tBg = tBg;
            this.invert = invert;
            this.connectivity = connectivity;
            this.method = method;
            this.surface = surface;
            this.absorbUnknown = absorbUnknown;
            this.markerSource = markerSource;
            this.overlapRule = overlapRule;
            this.binarySourceId = binarySourceId;
            this.findMaximaTolerance = findMaximaTolerance;
            this.preprocessingEnabled = preprocessingEnabled;
            this.sigmaSurface = sigmaSurface;
            this.sigmaSeed = sigmaSeed;
            this.randomWalkerBeta = randomWalkerBeta;
            this.seedMinArea = seedMinArea;
            this.seedMaxArea = seedMaxArea;
            this.manualSeedVersion = manualSeedVersion;
            this.slice = slice;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof SegmentationKey)) return false;
            SegmentationKey other = (SegmentationKey) obj;
            return tFg == other.tFg
                && tBg == other.tBg
                && invert == other.invert
                && connectivity == other.connectivity
                && method == other.method
                && surface == other.surface
                && absorbUnknown == other.absorbUnknown
                && markerSource == other.markerSource
                && overlapRule == other.overlapRule
                && binarySourceId == other.binarySourceId
                && Double.compare(findMaximaTolerance, other.findMaximaTolerance) == 0
                && preprocessingEnabled == other.preprocessingEnabled
                && Double.compare(sigmaSurface, other.sigmaSurface) == 0
                && Double.compare(sigmaSeed, other.sigmaSeed) == 0
                && Double.compare(randomWalkerBeta, other.randomWalkerBeta) == 0
                && seedMinArea == other.seedMinArea
                && seedMaxArea == other.seedMaxArea
                && manualSeedVersion == other.manualSeedVersion
                && slice == other.slice
                && imageId == other.imageId;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(imageId);
            result = 31 * result + Integer.hashCode(tFg);
            result = 31 * result + Integer.hashCode(tBg);
            result = 31 * result + Boolean.hashCode(invert);
            result = 31 * result + (connectivity != null ? connectivity.hashCode() : 0);
            result = 31 * result + (method != null ? method.hashCode() : 0);
            result = 31 * result + (surface != null ? surface.hashCode() : 0);
            result = 31 * result + Boolean.hashCode(absorbUnknown);
            result = 31 * result + (markerSource != null ? markerSource.hashCode() : 0);
            result = 31 * result + (overlapRule != null ? overlapRule.hashCode() : 0);
            result = 31 * result + Integer.hashCode(binarySourceId);
            long tmp = Double.doubleToLongBits(findMaximaTolerance);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            result = 31 * result + Boolean.hashCode(preprocessingEnabled);
            tmp = Double.doubleToLongBits(sigmaSurface);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(sigmaSeed);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            tmp = Double.doubleToLongBits(randomWalkerBeta);
            result = 31 * result + (int) (tmp ^ (tmp >>> 32));
            result = 31 * result + Integer.hashCode(seedMinArea);
            result = 31 * result + Integer.hashCode(seedMaxArea);
            result = 31 * result + Integer.hashCode(manualSeedVersion);
            result = 31 * result + Integer.hashCode(slice);
            return result;
        }
    }
    private void updateSurfaceEnabled() {
        boolean ws = model.getMethod() == Method.WATERSHED;
        surfaceOriginal.setEnabled(ws);
        surfaceInvert.setEnabled(ws);
        surfaceGradient.setEnabled(ws);
        boolean rw = model.getMethod() == Method.RANDOM_WALKER;
        rwBetaField.setEnabled(rw);
        rwBetaBar.setEnabled(rw);
    }

    private void updatePreprocessEnabled() {
        boolean enabled = model.isPreprocessingEnabled();
        sigmaSurfaceField.setEnabled(enabled);
        sigmaSurfaceBar.setEnabled(enabled);
        boolean seedEnabled = enabled && model.getMarkerSource() == MarkerSource.FIND_MAXIMA;
        sigmaSeedField.setEnabled(seedEnabled);
        sigmaSeedBar.setEnabled(seedEnabled);
    }

    private void syncFieldsFromModel() {
        fgField.setText(Integer.toString(model.getTFg()));
        bgField.setText(Integer.toString(model.getTBg()));
    }

    private void syncBarsFromModel() {
        int min = model.getMinValue();
        int max = model.getMaxValue();
        if (max <= min) max = min + 1;
        fgBar.setMinimum(min);
        fgBar.setMaximum(max + 1);
        bgBar.setMinimum(min);
        bgBar.setMaximum(max + 1);
        fgBar.setValue(model.getTFg());
        bgBar.setValue(model.getTBg());
    }

    private void syncAllFromModel() {
        syncing = true;
        try {
            syncBarsFromModel();
            syncFieldsFromModel();
            invertCb.setState(model.isInvert());
            previewOff.setState(model.getPreviewMode() == PreviewMode.OFF);
            previewMarker.setState(model.getPreviewMode() == PreviewMode.MARKER_BOUNDARIES);
            previewRoi.setState(model.getPreviewMode() == PreviewMode.ROI_BOUNDARIES);
            methodWs.setState(model.getMethod() == Method.WATERSHED);
            methodRw.setState(model.getMethod() == Method.RANDOM_WALKER);
            surfaceOriginal.setState(model.getSurface() == Surface.ORIGINAL);
            surfaceInvert.setState(model.getSurface() == Surface.INVERT_ORIGINAL);
            surfaceGradient.setState(model.getSurface() == Surface.GRADIENT_SOBEL);
            rwBetaField.setText(Double.toString(model.getRandomWalkerBeta()));
            rwBetaBar.setValue(rwBetaToSlider(model.getRandomWalkerBeta()));
            conn4.setState(model.getConnectivity() == Connectivity.C4);
            conn8.setState(model.getConnectivity() == Connectivity.C8);
            updateSurfaceEnabled();
            histogramPanel.repaint();
            absorbUnknownCb.setState(model.isAbsorbUnknown());
            overlapChoice.select(model.getOverlapRule() == OverlapRule.FIRST_WINS ? 0 : 1);
            debounceField.setText(Integer.toString(model.getPreviewDebounceMs()));
            markerSourceChoice.select(markerSourceToIndex(model.getMarkerSource()));
            updateBinaryChoice();
            findMaximaField.setText(Double.toString(model.getFindMaximaTolerance()));
            preprocessingCb.setState(model.isPreprocessingEnabled());
            sigmaSurfaceField.setText(Double.toString(model.getSigmaSurface()));
            sigmaSeedField.setText(Double.toString(model.getSigmaSeed()));
            AppearanceSettings app = model.getAppearance();
            seedColorField.setText(formatColor(app.getSeedColor()));
            domainColorField.setText(formatColor(app.getDomainColor()));
            bgColorField.setText(formatColor(app.getBgColor()));
            opacityField.setText(Double.toString(app.getOpacity()));
            opacityBar.setValue((int) Math.round(app.getOpacity() * 100.0));
            showSeedCb.setState(app.isShowSeed());
            showSeedCrossCb.setState(app.isShowSeedCentroids());
            showDomainCb.setState(app.isShowDomain());
            showBgCb.setState(app.isShowBg());
            updateFindMaximaRange();
            findMaximaBar.setValue(findMaximaToSlider(model.getFindMaximaTolerance()));
            sigmaSurfaceBar.setValue(sigmaToSlider(model.getSigmaSurface()));
            sigmaSeedBar.setValue(sigmaToSlider(model.getSigmaSeed()));
            seedMinAreaField.setText(Integer.toString(model.getSeedMinArea()));
            seedMaxAreaField.setText(Integer.toString(model.getSeedMaxArea()));
            boolean fgEnabled = model.getMarkerSource() == MarkerSource.THRESHOLD_COMPONENTS;
            fgBar.setEnabled(fgEnabled);
            fgField.setEnabled(fgEnabled);
            fgLabel.setEnabled(fgEnabled);
            fgLabel.setText(fgEnabled ? "Foreground (upper):" : "Foreground (upper) [unused]:");
            histogramPanel.setFgEnabled(fgEnabled);
            updatePreprocessEnabled();
        } finally {
            syncing = false;
        }
    }

    private void commitFgField() {
        if (syncing) return;
        model.setTFg(parseOrKeep(fgField.getText(), model.getTFg()));
        syncAllFromModel();
        onStateChanged();
    }

    private void commitBgField() {
        if (syncing) return;
        model.setTBg(parseOrKeep(bgField.getText(), model.getTBg()));
        syncAllFromModel();
        onStateChanged();
    }

    private int parseOrKeep(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); }
        catch (Exception ex) { return fallback; }
    }

    private double parseOrKeepDouble(String s, double fallback) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception ex) { return fallback; }
    }

    private int parseColor(String s, int fallback) {
        String t = s.trim();
        if (t.startsWith("#")) t = t.substring(1);
        try { return Integer.parseInt(t, 16); }
        catch (Exception ex) { return fallback; }
    }

    private String formatColor(int rgb) {
        return String.format("%06X", (rgb & 0xFFFFFF));
    }

    private void clearOverlay() {
        imp.setOverlay((Overlay) null);
        imp.updateAndDraw();
        // Match ImageJ feel: also clear any selection
        imp.setRoi((Roi) null);
        Toolbar.getInstance().setTool(Toolbar.RECTANGLE);
    }
}
