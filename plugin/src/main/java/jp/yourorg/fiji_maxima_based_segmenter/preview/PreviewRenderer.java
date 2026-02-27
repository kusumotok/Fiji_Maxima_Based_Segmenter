package jp.yourorg.fiji_maxima_based_segmenter.preview;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ImageRoi;
import ij.gui.PointRoi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.core.AppearanceSettings;
import jp.yourorg.fiji_maxima_based_segmenter.core.MarkerResult;
import jp.yourorg.fiji_maxima_based_segmenter.core.MarkerResult3D;
import jp.yourorg.fiji_maxima_based_segmenter.core.MarkerSource;
import java.awt.Color;

public class PreviewRenderer {
    public void renderMarkerFill(ImagePlus imp, MarkerResult markers, AppearanceSettings appearance, MarkerSource markerSource) {
        if (markers == null) return;
        int w = markers.width;
        int h = markers.height;
        ColorProcessor cp = new ColorProcessor(w, h);
        int[] pixels = (int[]) cp.getPixels();
        int size = w * h;
        int seedColor = appearance.getSeedColor();
        int domainColor = appearance.getDomainColor();
        int bgColor = appearance.getBgColor();
        boolean showSeed = appearance.isShowSeed();
        boolean showDomain = appearance.isShowDomain();
        boolean showBg = appearance.isShowBg();
        boolean showSeedCentroids = appearance.isShowSeedCentroids();
        
        // When using FIND_MAXIMA, don't show seed mask (FG area) - only show seed centroids as crosses
        boolean hideSeedMask = (markerSource == MarkerSource.FIND_MAXIMA);
        
        for (int i = 0; i < size; i++) {
            if (showBg && markers.bgMask[i]) {
                pixels[i] = bgColor;
            }
            if (showDomain && markers.domainMask[i]) {
                pixels[i] = domainColor;
            }
            if (showSeed && !hideSeedMask && markers.seedMask[i]) {
                pixels[i] = seedColor;
            }
        }
        ImageRoi roi = new ImageRoi(0, 0, cp);
        roi.setZeroTransparent(true);
        roi.setOpacity(appearance.getOpacity());
        Overlay overlay = new Overlay();
        overlay.add(roi);
        if (showSeedCentroids && markers.fgCount > 0) {
            PointRoi points = buildSeedCentroids(markers);
            if (points != null) {
                points.setPointType(PointRoi.CROSSHAIR);
                points.setStrokeColor(new Color(seedColor));
                overlay.add(points);
            }
        }
        imp.setOverlay(overlay);
        imp.updateAndDraw();
    }

    private PointRoi buildSeedCentroids(MarkerResult markers) {
        int count = markers.fgCount;
        if (count <= 0) return null;
        
        // Find actual max label value in the data
        int maxLabel = 0;
        int[] labels = markers.fgLabels;
        for (int label : labels) {
            if (label > maxLabel) maxLabel = label;
        }
        
        if (maxLabel <= 0) return null;
        
        // Use maxLabel for array size to handle sparse label IDs
        double[] sumX = new double[maxLabel + 1];
        double[] sumY = new double[maxLabel + 1];
        int[] cnt = new int[maxLabel + 1];
        int w = markers.width;
        int h = markers.height;
        
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int label = labels[row + x];
                if (label <= 0 || label > maxLabel) continue;
                sumX[label] += x;
                sumY[label] += y;
                cnt[label]++;
            }
        }
        
        // Count non-empty labels
        int actualCount = 0;
        for (int i = 1; i <= maxLabel; i++) {
            if (cnt[i] > 0) actualCount++;
        }
        
        if (actualCount == 0) return null;
        
        float[] xs = new float[actualCount];
        float[] ys = new float[actualCount];
        int k = 0;
        for (int i = 1; i <= maxLabel; i++) {
            if (cnt[i] == 0) continue;
            xs[k] = (float) (sumX[i] / cnt[i]);
            ys[k] = (float) (sumY[i] / cnt[i]);
            k++;
        }
        
        return new PointRoi(xs, ys, actualCount);
    }

    public void renderSegmentationBoundaries(ImagePlus imp, SegmentationResult result) {
        if (result == null || result.labelImage == null) return;
        ImageProcessor ip = result.labelImage.getProcessor();
        int w = result.labelImage.getWidth();
        int h = result.labelImage.getHeight();
        int size = w * h;
        
        // Collect unique labels (handle potential float labels)
        java.util.Set<Integer> uniqueLabels = new java.util.HashSet<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = (int) Math.round(ip.getPixelValue(x, y));
                if (v > 0) uniqueLabels.add(v);
            }
        }
        
        Overlay overlay = new Overlay();
        for (int label : uniqueLabels) {
            boolean[] mask = new boolean[size];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    int v = (int) Math.round(ip.getPixelValue(x, y));
                    if (v == label) mask[idx] = true;
                }
            }
            addMaskOutline(overlay, mask, w, h);
        }
        imp.setOverlay(overlay);
        imp.updateAndDraw();
    }

    private void addMaskOutline(Overlay overlay, boolean[] mask, int w, int h) {
        Roi roi = maskToOutline(mask, w, h);
        if (roi != null) overlay.add(roi);
    }

    private Roi maskToOutline(boolean[] mask, int w, int h) {
        ByteProcessor bp = new ByteProcessor(w, h);
        byte[] pixels = (byte[]) bp.getPixels();
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) pixels[i] = (byte) 255;
        }
        bp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
        ImagePlus tmp = new ImagePlus("mask", bp);
        return ThresholdToSelection.run(tmp);
    }

    /**
     * Render 3D marker preview for the current Z-plane.
     * Shows FG area + seed centroids for current slice, and seed centroids from other slices.
     */
    public void renderMarkerFill3D(ImagePlus imp, MarkerResult3D markers3D,
                                    int zPlane, AppearanceSettings appearance, MarkerSource markerSource) {
        if (markers3D == null) return;
        
        // Render the current slice with FG area
        MarkerResult slice = markers3D.getSlice(zPlane);
        
        int w = slice.width;
        int h = slice.height;
        ColorProcessor cp = new ColorProcessor(w, h);
        int[] pixels = (int[]) cp.getPixels();
        int size = w * h;
        int seedColor = appearance.getSeedColor();
        int domainColor = appearance.getDomainColor();
        int bgColor = appearance.getBgColor();
        boolean showSeed = appearance.isShowSeed();
        boolean showDomain = appearance.isShowDomain();
        boolean showBg = appearance.isShowBg();
        boolean showSeedCentroids = appearance.isShowSeedCentroids();
        
        // 3D Extended Maxima produces areas, so always show seed mask
        for (int i = 0; i < size; i++) {
            if (showBg && slice.bgMask[i]) {
                pixels[i] = bgColor;
            }
            if (showDomain && slice.domainMask[i]) {
                pixels[i] = domainColor;
            }
            if (showSeed && slice.seedMask[i]) {
                pixels[i] = seedColor;
            }
        }
        
        ImageRoi roi = new ImageRoi(0, 0, cp);
        roi.setZeroTransparent(true);
        roi.setOpacity(appearance.getOpacity());
        Overlay overlay = new Overlay();
        overlay.add(roi);
        
        // Add seed centroids from all slices
        if (showSeedCentroids) {
            int nSlices = markers3D.getDepth();
            for (int z = 1; z <= nSlices; z++) {
                MarkerResult sliceZ = markers3D.getSlice(z);
                if (sliceZ.fgCount > 0) {
                    PointRoi points = buildSeedCentroids(sliceZ);
                    if (points != null) {
                        points.setPointType(PointRoi.CROSSHAIR);
                        if (z == zPlane) {
                            // Current slice: full color (red)
                            points.setStrokeColor(new Color(seedColor));
                        } else {
                            // Other slices: blue with transparency
                            points.setStrokeColor(new Color(0, 0, 255, 150));
                        }
                        overlay.add(points);
                    }
                }
            }
        }
        
        imp.setOverlay(overlay);
        imp.updateAndDraw();
    }

    /**
     * Render 3D segmentation boundaries for the current Z-plane.
     */
    public void renderRoiBoundaries3D(ImagePlus imp, SegmentationResult3D result, int zPlane) {
        if (result == null || result.labelImage == null) return;
        ImageProcessor ip = result.labelImage.getStack().getProcessor(zPlane);
        int w = ip.getWidth();
        int h = ip.getHeight();
        int size = w * h;
        
        // Collect unique labels in this slice (handle float labels properly)
        java.util.Set<Integer> uniqueLabels = new java.util.HashSet<>();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int v = (int) Math.round(ip.getPixelValue(x, y));
                if (v > 0) uniqueLabels.add(v);
            }
        }
        
        Overlay overlay = new Overlay();
        for (int label : uniqueLabels) {
            boolean[] mask = new boolean[size];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    int v = (int) Math.round(ip.getPixelValue(x, y));
                    if (v == label) mask[idx] = true;
                }
            }
            addMaskOutline(overlay, mask, w, h);
        }
        imp.setOverlay(overlay);
        imp.updateAndDraw();
    }
}
