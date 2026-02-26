package jp.yourorg.fiji_maxima_based_segmenter;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.plugin.PlugIn;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult;
import jp.yourorg.fiji_maxima_based_segmenter.alg.WatershedRunner;
import jp.yourorg.fiji_maxima_based_segmenter.core.*;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.ui.SimpleSegmenterFrame;

public class Maxima_Based_Segmenter_Simple_ implements PlugIn {
    @Override
    public void run(String arg) {
        // Check for macro parameters
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
        new SimpleSegmenterFrame(imp).setVisible(true);
    }

    private void runMacroMode(String options) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.error("No image", "Open an image first.");
            return;
        }

        int bgThreshold = (int) Math.round(imp.getStatistics().max * 0.2);
        double tolerance = 10.0;

        String[] parts = options.split("\\s+");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].toLowerCase();
            String val = kv[1];
            if (key.equals("bg_threshold")) {
                try { bgThreshold = Integer.parseInt(val); } catch (NumberFormatException ignored) {}
            } else if (key.equals("tolerance")) {
                try { tolerance = Double.parseDouble(val); } catch (NumberFormatException ignored) {}
            }
        }

        ImagePlus result = segment(imp, bgThreshold, tolerance);
        if (result != null) {
            new RoiExporter().exportToRoiManager(result);
        }
    }

    /**
     * Programmatic API for macro/script usage.
     * @param imp Input image
     * @param bgThreshold Background threshold
     * @param tolerance FindMaxima tolerance
     * @return Label image
     */
    public static ImagePlus segment(ImagePlus imp, int bgThreshold, double tolerance) {
        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(bgThreshold);
        model.setFindMaximaTolerance(tolerance);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult markers = builder.build(imp, model);
        if (markers.fgCount == 0) {
            IJ.log("Maxima_Based_Segmenter_Simple: No seeds found.");
            return null;
        }

        SegmentationResult seg = new WatershedRunner().run(
            imp, markers, model.getSurface(), model.getConnectivity(), false, 0.0
        );
        return seg != null ? seg.labelImage : null;
    }
}
