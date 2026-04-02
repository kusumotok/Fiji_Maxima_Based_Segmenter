package jp.yourorg.fiji_maxima_based_segmenter;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import jp.yourorg.fiji_maxima_based_segmenter.alg.*;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter3D;
import jp.yourorg.fiji_maxima_based_segmenter.ui.SpotQuantifier3DFrame;
import jp.yourorg.fiji_maxima_based_segmenter.util.CsvExporter;

import java.io.File;
import java.util.List;

/**
 * Spot_Quantifier_3D_ — Fiji plugin entry point.
 *
 * GUI mode:  run("Spot Quantifier 3D")
 * Macro mode: run("Spot Quantifier 3D",
 *               "threshold=300 min_vol=0.1 max_vol=50.0 gaussian_blur=false output=[/path/to/dir]");
 *
 * In macro mode the plugin segments the current image, measures spots, saves
 * [basename]_spots.csv and [basename]_params.txt into the output directory,
 * and adds ROIs to the ROI Manager.
 *
 * Empty value for min_vol or max_vol (e.g. "max_vol=") disables that filter.
 */
public class Spot_Quantifier_3D_ implements PlugIn {

    @Override
    public void run(String arg) {
        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.error("Spot Quantifier 3D", "No image is open.");
            return;
        }
        if (imp.getNSlices() < 2) {
            IJ.error("Spot Quantifier 3D", "This plugin requires a 3D image stack.");
            return;
        }

        String options = Macro.getOptions();
        boolean isMacroMode = (options != null && !options.trim().isEmpty());

        if (!isMacroMode) {
            // GUI mode
            new SpotQuantifier3DFrame(imp).setVisible(true);
        } else {
            // Macro / headless mode: parse args and run non-interactively
            runMacroMode(imp, options);
        }
    }

    private void runMacroMode(ImagePlus imp, String options) {
        try {
            // Parse parameters
            int     threshold = parseInt(options,  "threshold",    500);
            Double  minVol    = parseDoubleOpt(options, "min_vol");
            Double  maxVol    = parseDoubleOpt(options, "max_vol");
            boolean gauss     = parseBool(options, "gaussian_blur", false);
            double  gaussXY   = parseDouble(options, "gauss_xy", 1.0);
            double  gaussZ    = parseDouble(options, "gauss_z",  0.5);
            int     conn      = parseInt(options, "connectivity", 6);
            boolean fillHoles = parseBool(options, "fill_holes", false);
            String  outputDir = parseString(options, "output", null);

            QuantifierParams params = new QuantifierParams(
                threshold, minVol, maxVol, gauss, gaussXY, gaussZ, conn, fillHoles);

            // Voxel calibration
            Calibration cal = imp.getCalibration();
            double vw = cal.pixelWidth;
            double vh = cal.pixelHeight;
            double vd = cal.pixelDepth;
            if (vw <= 0) vw = 1;
            if (vh <= 0) vh = 1;
            if (vd <= 0) vd = 1;

            // Segment
            IJ.showStatus("Spot Quantifier 3D: computing connected components...");
            CcResult3D cc = SpotQuantifier3D.computeCC(imp, params);
            if (cc.voxelCounts.isEmpty()) {
                IJ.log("Spot Quantifier 3D: no spots detected for threshold=" + threshold);
                return;
            }

            double voxelVol = vw * vh * vd;
            java.util.Map<Integer, Integer> statusMap = cc.classifyLabels(params, voxelVol);
            SegmentationResult3D seg = cc.buildFilteredResult(statusMap);

            // Measure
            IJ.showStatus("Spot Quantifier 3D: measuring...");
            List<SpotMeasurement> spots = SpotMeasurer.measure(seg, imp, vw, vh, vd);

            // Output directory
            File dir;
            if (outputDir != null && !outputDir.isEmpty()) {
                dir = new File(outputDir);
            } else {
                String path = imp.getOriginalFileInfo() != null
                    ? imp.getOriginalFileInfo().directory : null;
                dir = (path != null) ? new File(path) : new File(".");
            }
            if (!dir.exists()) dir.mkdirs();

            File csvDir = new File(dir, "csv");
            File roiDir = new File(dir, "roi");
            if (!csvDir.exists()) csvDir.mkdirs();
            if (!roiDir.exists()) roiDir.mkdirs();

            String basename = imp.getShortTitle().replaceAll("\\.tif$", "");

            // Save CSV to csv/ subfolder
            File csvFile = new File(csvDir, basename + "_spots.csv");
            CsvExporter.writeCsv(spots, csvFile);

            // Save params.txt once at output dir root (overwritten each run = same params for whole batch)
            File paramsFile = new File(dir, "params.txt");
            CsvExporter.writeParams(params, paramsFile);

            // Save ROI ZIP to roi/ subfolder (skip if no spots)
            if (!spots.isEmpty()) {
                RoiManager rm = RoiManager.getRoiManager();
                rm.reset();
                new RoiExporter3D().exportToRoiManager(seg.labelImage);
                if (rm.getCount() > 0) {
                    File roiFile = new File(roiDir, basename + "_RoiSet.zip");
                    RoiExporter.saveRoiManagerToZip(roiFile.getAbsolutePath());
                }
            }

            IJ.log("Spot Quantifier 3D: " + spots.size() + " spot(s) → " + csvFile.getAbsolutePath());
            IJ.showStatus("Spot Quantifier 3D: done (" + spots.size() + " spots).");

        } catch (Exception ex) {
            IJ.error("Spot Quantifier 3D", "Error: " + ex.getMessage());
            IJ.handleException(ex);
        }
    }

    // ---- Argument parsing helpers ----

    private static int parseInt(String opts, String key, int def) {
        String v = Macro.getValue(opts, key, null);
        if (v == null || v.trim().isEmpty()) return def;
        try { return Integer.parseInt(v.trim()); }
        catch (NumberFormatException ex) { return def; }
    }

    private static double parseDouble(String opts, String key, double def) {
        String v = Macro.getValue(opts, key, null);
        if (v == null || v.trim().isEmpty()) return def;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException ex) { return def; }
    }

    /** Returns null if the key is absent or its value is empty (= disabled). */
    private static Double parseDoubleOpt(String opts, String key) {
        String v = Macro.getValue(opts, key, null);
        if (v == null || v.trim().isEmpty()) return null;
        try { return Double.parseDouble(v.trim()); }
        catch (NumberFormatException ex) { return null; }
    }

    private static boolean parseBool(String opts, String key, boolean def) {
        String v = Macro.getValue(opts, key, null);
        if (v == null || v.trim().isEmpty()) return def;
        return Boolean.parseBoolean(v.trim());
    }

    private static String parseString(String opts, String key, String def) {
        String v = Macro.getValue(opts, key, null);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }
}
