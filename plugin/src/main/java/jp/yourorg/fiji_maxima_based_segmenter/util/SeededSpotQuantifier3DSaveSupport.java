package jp.yourorg.fiji_maxima_based_segmenter.util;

import ij.ImagePlus;
import ij.io.FileInfo;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;
import jp.yourorg.fiji_maxima_based_segmenter.alg.CcResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.alg.QuantifierParams;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SeededQuantifier3D;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SpotMeasurement;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SpotMeasurer;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SpotQuantifier3D;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter;
import jp.yourorg.fiji_maxima_based_segmenter.roi.RoiExporter3D;

import java.awt.Color;
import java.awt.Frame;
import java.awt.FileDialog;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class SeededSpotQuantifier3DSaveSupport {
    private SeededSpotQuantifier3DSaveSupport() {}

    public static File resolveOutputDir(Frame parent, ImagePlus target, boolean customFolder, String pattern,
                                        int seedThreshold, int areaThreshold, File explicitBaseDir) {
        String basename = target.getShortTitle().replaceAll("\\.tiff?$", "");
        String folderName = expandFolderTokens(customFolder ? pattern : "{name} result",
            basename, seedThreshold, areaThreshold);

        String dirStr = null;
        if (explicitBaseDir != null) {
            dirStr = explicitBaseDir.getAbsolutePath();
        } else {
            FileInfo fi = target.getOriginalFileInfo();
            dirStr = (fi != null && fi.directory != null && !fi.directory.isEmpty())
                ? fi.directory : null;
        }

        if (dirStr == null) {
            FileDialog fd = new FileDialog(parent, "Choose save location (select any file)", FileDialog.SAVE);
            fd.setFile("placeholder.csv");
            fd.setVisible(true);
            if (fd.getDirectory() == null) return null;
            dirStr = fd.getDirectory();
        }

        return new File(dirStr, folderName);
    }

    public static String expandFolderTokens(String pattern, String name, int seed, int area) {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return pattern
            .replace("{name}", name)
            .replace("{date}", date)
            .replace("{seed}", String.valueOf(seed))
            .replace("{area}", String.valueOf(area));
    }

    public static String saveOneToDir(ImagePlus target, int at, int st, boolean areaEn,
                                      QuantifierParams params, File outDir,
                                      boolean saveSeedRoi, boolean saveSizeRoi,
                                      boolean saveAreaRoi, boolean saveResultRoi,
                                      boolean saveCsv, boolean saveParam,
                                      Color roiColor,
                                      Consumer<String> progress) {
        Calibration cal = target.getCalibration();
        double tw = cal.pixelWidth  > 0 ? cal.pixelWidth  : 1;
        double th = cal.pixelHeight > 0 ? cal.pixelHeight : 1;
        double td = cal.pixelDepth  > 0 ? cal.pixelDepth  : 1;
        double tVoxelVol = tw * th * td;

        reportProgress(progress, "Saving: segmenting...");
        SeededQuantifier3D.SeededResult r = SeededQuantifier3D.compute(
            target, at, st, params, tVoxelVol, areaEn);
        if (r == null) return "no spots detected";

        String basename = target.getShortTitle().replaceAll("\\.tiff?$", "");

        try {
            outDir.mkdirs();

            if (saveCsv) {
                reportProgress(progress, "Saving: writing CSV...");
                List<SpotMeasurement> spots = SpotMeasurer.measure(r.finalSeg, target, tw, th, td);
                CsvExporter.writeCsv(spots, new File(outDir, basename + "_spots.csv"));
            }

            if (saveParam) {
                reportProgress(progress, "Saving: writing params...");
                CsvExporter.writeSeededParams(at, st, params,
                    new File(outDir, basename + "_params.txt"));
            }

            if (saveSeedRoi && r.rawSeedSeg != null) {
                reportProgress(progress, "Saving: writing seed ROI...");
                saveRoiToZip(r.rawSeedSeg, roiColor, new File(outDir, basename + "_seed_roi.zip"));
            }

            if (saveSizeRoi && r.seedSeg != null) {
                reportProgress(progress, "Saving: writing size ROI...");
                saveRoiToZip(r.seedSeg, roiColor, new File(outDir, basename + "_size_roi.zip"));
            }

            if (saveAreaRoi) {
                reportProgress(progress, "Saving: writing area ROI...");
                QuantifierParams noFilter = new QuantifierParams(
                    at, null, null, false, 1.0, 0.5, params.connectivity, params.fillHoles);
                CcResult3D areaCC = SpotQuantifier3D.computeCCFromBlurred(target, at, noFilter);
                Map<Integer, Integer> allValidMap = new HashMap<>();
                areaCC.voxelCounts.keySet().forEach(k -> allValidMap.put(k, CcResult3D.STATUS_VALID));
                SegmentationResult3D areaSeg = areaCC.buildFilteredResult(allValidMap);
                saveRoiToZip(areaSeg, roiColor, new File(outDir, basename + "_area_roi.zip"));
            }

            if (saveResultRoi && r.finalSeg != null) {
                reportProgress(progress, "Saving: writing result ROI...");
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

    private static void reportProgress(Consumer<String> progress, String message) {
        if (progress != null) progress.accept(message);
    }
}
