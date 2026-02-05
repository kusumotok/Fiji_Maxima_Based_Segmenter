package jp.yourorg.fiji_area_segmentater.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

public class RoiExporter {
    public void exportToRoiManager(ImagePlus labelImage) {
        if (labelImage == null) {
            IJ.error("Add ROI failed", "Label image is missing.");
            return;
        }
        ImageProcessor ip = labelImage.getProcessor();
        int w = labelImage.getWidth();
        int h = labelImage.getHeight();
        int size = w * h;
        int maxLabel = 0;
        for (int i = 0; i < size; i++) {
            int v = ip.get(i);
            if (v > maxLabel) maxLabel = v;
        }
        if (maxLabel <= 0) {
            IJ.error("Add ROI failed", "No objects found in label image.");
            return;
        }

        RoiManager rm = RoiManager.getRoiManager();
        for (int label = 1; label <= maxLabel; label++) {
            ByteProcessor bp = new ByteProcessor(w, h);
            byte[] pixels = (byte[]) bp.getPixels();
            for (int i = 0; i < size; i++) {
                if (ip.get(i) == label) pixels[i] = (byte) 255;
            }
            bp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
            ImagePlus mask = new ImagePlus("mask", bp);
            Roi roi = ThresholdToSelection.run(mask);
            if (roi == null) continue;
            roi.setName(String.format("obj-%03d", label));
            rm.addRoi(roi);
        }
    }
}
