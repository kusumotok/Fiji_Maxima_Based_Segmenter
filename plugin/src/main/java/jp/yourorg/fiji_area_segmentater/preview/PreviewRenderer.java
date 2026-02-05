package jp.yourorg.fiji_area_segmentater.preview;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ImageRoi;
import ij.gui.PointRoi;
import ij.plugin.filter.ThresholdToSelection;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import jp.yourorg.fiji_area_segmentater.alg.SegmentationResult;
import jp.yourorg.fiji_area_segmentater.core.AppearanceSettings;
import jp.yourorg.fiji_area_segmentater.core.MarkerResult;
import java.awt.Color;

public class PreviewRenderer {
    public void renderMarkerFill(ImagePlus imp, MarkerResult markers, AppearanceSettings appearance) {
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
        for (int i = 0; i < size; i++) {
            if (showBg && markers.bgMask[i]) {
                pixels[i] = bgColor;
            }
            if (showDomain && markers.domainMask[i]) {
                pixels[i] = domainColor;
            }
            if (showSeed && markers.seedMask[i]) {
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
        double[] sumX = new double[count + 1];
        double[] sumY = new double[count + 1];
        int[] cnt = new int[count + 1];
        int w = markers.width;
        int h = markers.height;
        int[] labels = markers.fgLabels;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int label = labels[row + x];
                if (label <= 0 || label > count) continue;
                sumX[label] += x;
                sumY[label] += y;
                cnt[label]++;
            }
        }
        float[] xs = new float[count];
        float[] ys = new float[count];
        int k = 0;
        for (int i = 1; i <= count; i++) {
            if (cnt[i] == 0) continue;
            xs[k] = (float) (sumX[i] / cnt[i]);
            ys[k] = (float) (sumY[i] / cnt[i]);
            k++;
        }
        if (k == 0) return null;
        float[] xs2 = new float[k];
        float[] ys2 = new float[k];
        System.arraycopy(xs, 0, xs2, 0, k);
        System.arraycopy(ys, 0, ys2, 0, k);
        return new PointRoi(xs2, ys2, k);
    }

    public void renderSegmentationBoundaries(ImagePlus imp, SegmentationResult result) {
        if (result == null || result.labelImage == null) return;
        ImageProcessor ip = result.labelImage.getProcessor();
        int w = result.labelImage.getWidth();
        int h = result.labelImage.getHeight();
        int size = w * h;
        int maxLabel = 0;
        for (int i = 0; i < size; i++) {
            int v = ip.get(i);
            if (v > maxLabel) maxLabel = v;
        }
        Overlay overlay = new Overlay();
        for (int label = 1; label <= maxLabel; label++) {
            boolean[] mask = new boolean[size];
            for (int i = 0; i < size; i++) {
                if (ip.get(i) == label) mask[i] = true;
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
}
