package jp.yourorg.fiji_maxima_based_segmenter;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.plugin.PlugIn;
import jp.yourorg.fiji_maxima_based_segmenter.alg.RandomWalkerRunner;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult;
import jp.yourorg.fiji_maxima_based_segmenter.alg.WatershedRunner;
import jp.yourorg.fiji_maxima_based_segmenter.core.*;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.ui.DualThresholdFrame;

public class Maxima_Based_Segmenter_ implements PlugIn {
    @Override
    public void run(String arg) {
        String macroOptions = Macro.getOptions();
        if (macroOptions != null && !macroOptions.trim().isEmpty()) {
            runMacroMode(macroOptions.trim());
            return;
        }

        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.error("No image", "Open an image first.");
            return;
        }
        new DualThresholdFrame(imp).setVisible(true);
    }

    private void runMacroMode(String options) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.error("No image", "Open an image first.");
            return;
        }

        ThresholdModel model = new ThresholdModel(imp);

        String[] parts = options.split("\\s+");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].toLowerCase();
            String val = kv[1];
            switch (key) {
                case "bg_threshold":
                    try { model.setTBg(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                    break;
                case "fg_threshold":
                    try { model.setTFg(Integer.parseInt(val)); } catch (NumberFormatException ignored) {}
                    break;
                case "tolerance":
                    try { model.setFindMaximaTolerance(Double.parseDouble(val)); } catch (NumberFormatException ignored) {}
                    break;
                case "marker_source":
                    MarkerSource source = parseMarkerSource(val);
                    if (source != null) model.setMarkerSource(source);
                    break;
                case "method":
                    if ("watershed".equalsIgnoreCase(val)) model.setMethod(Method.WATERSHED);
                    else if ("random_walker".equalsIgnoreCase(val)) model.setMethod(Method.RANDOM_WALKER);
                    break;
                case "surface":
                    if ("invert_original".equalsIgnoreCase(val)) model.setSurface(Surface.INVERT_ORIGINAL);
                    else if ("original".equalsIgnoreCase(val)) model.setSurface(Surface.ORIGINAL);
                    else if ("gradient_sobel".equalsIgnoreCase(val)) model.setSurface(Surface.GRADIENT_SOBEL);
                    break;
                case "connectivity":
                    if ("c4".equalsIgnoreCase(val) || "4".equals(val)) model.setConnectivity(Connectivity.C4);
                    else if ("c8".equalsIgnoreCase(val) || "8".equals(val)) model.setConnectivity(Connectivity.C8);
                    break;
            }
        }

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult markers = builder.build(imp, model);
        if (markers.fgCount == 0) {
            IJ.log("Maxima_Based_Segmenter: No seeds found.");
            return;
        }

        SegmentationResult seg;
        if (model.getMethod() == Method.WATERSHED) {
            seg = new WatershedRunner().run(
                imp, markers, model.getSurface(), model.getConnectivity(),
                model.isPreprocessingEnabled(), model.getSigmaSurface()
            );
        } else {
            seg = new RandomWalkerRunner().run(
                imp, markers, model.getConnectivity(),
                model.isPreprocessingEnabled(), model.getSigmaSurface(),
                model.getRandomWalkerBeta()
            );
        }

        if (seg != null && seg.labelImage != null) {
            new RoiExporter().exportToRoiManager(seg.labelImage);
        }
    }

    private MarkerSource parseMarkerSource(String val) {
        String upper = val.toUpperCase();
        for (MarkerSource s : MarkerSource.values()) {
            if (s.name().equals(upper)) return s;
        }
        return null;
    }
}
