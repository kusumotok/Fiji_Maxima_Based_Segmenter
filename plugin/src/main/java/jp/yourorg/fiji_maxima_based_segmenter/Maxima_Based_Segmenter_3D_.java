package jp.yourorg.fiji_maxima_based_segmenter;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.plugin.PlugIn;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.alg.Watershed3DRunner;
import jp.yourorg.fiji_maxima_based_segmenter.core.*;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter3D;
import jp.yourorg.fiji_maxima_based_segmenter.ui.Segmenter3DFrame;

public class Maxima_Based_Segmenter_3D_ implements PlugIn {
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
        if (imp.getNSlices() < 2) {
            IJ.error("3D Segmenter", "Image must be a 3D stack (Z > 1).");
            return;
        }
        new Segmenter3DFrame(imp).setVisible(true);
    }

    private void runMacroMode(String options) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.error("No image", "Open an image first.");
            return;
        }
        if (imp.getNSlices() < 2) {
            IJ.error("3D Segmenter", "Image must be a 3D stack (Z > 1).");
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
            new RoiExporter3D().exportToRoiManager(result);
        }
    }

    /**
     * Programmatic API for macro/script usage.
     * @param imp Input 3D image stack
     * @param bgThreshold Background threshold
     * @param tolerance Extended Maxima tolerance
     * @return 3D label image stack
     */
    public static ImagePlus segment(ImagePlus imp, int bgThreshold, double tolerance) {
        if (imp.getNSlices() < 2) {
            IJ.error("3D Segmenter", "Image must be a 3D stack (Z > 1).");
            return null;
        }

        Connectivity connectivity = Connectivity.C6;
        MarkerBuilder3D builder = new MarkerBuilder3D();
        MarkerResult3D markers = builder.build(imp, bgThreshold, tolerance, connectivity);
        if (markers.getSeedCount() == 0) {
            IJ.log("Maxima_Based_Segmenter_3D: No seeds found.");
            return null;
        }

        SegmentationResult3D seg = new Watershed3DRunner().run(imp, markers, connectivity);
        return seg != null ? seg.labelImage : null;
    }
}
