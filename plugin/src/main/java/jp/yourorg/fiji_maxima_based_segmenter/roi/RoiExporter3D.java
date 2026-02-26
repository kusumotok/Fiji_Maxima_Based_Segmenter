package jp.yourorg.fiji_maxima_based_segmenter.roi;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.plugin.filter.ThresholdToSelection;
import ij.plugin.frame.RoiManager;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.util.TreeSet;

public class RoiExporter3D {

    /**
     * Export 3D segmentation as 2D ROI slices with Position and Group attributes.
     */
    public void exportToRoiManager(ImagePlus labelImage) {
        if (labelImage == null) {
            IJ.error("Add ROI failed", "Label image is missing.");
            return;
        }
        ImageStack stack = labelImage.getStack();
        int w = labelImage.getWidth();
        int h = labelImage.getHeight();
        int d = labelImage.getNSlices();

        // Find all unique labels (use getPixel to handle float/int properly)
        TreeSet<Integer> labels = new TreeSet<>();
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = stack.getProcessor(z);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int v = (int) Math.round(ip.getPixelValue(x, y));
                    if (v > 0) labels.add(v);
                }
            }
        }

        if (labels.isEmpty()) {
            IJ.error("Add ROI failed", "No objects found in label image.");
            return;
        }

        RoiManager rm = RoiManager.getRoiManager();
        for (int label : labels) {
            for (int z = 1; z <= d; z++) {
                ImageProcessor ip = stack.getProcessor(z);
                ByteProcessor bp = new ByteProcessor(w, h);
                byte[] pixels = (byte[]) bp.getPixels();
                boolean hasPixel = false;
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int idx = y * w + x;
                        int v = (int) Math.round(ip.getPixelValue(x, y));
                        if (v == label) {
                            pixels[idx] = (byte) 255;
                            hasPixel = true;
                        }
                    }
                }
                if (!hasPixel) continue;

                bp.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
                ImagePlus mask = new ImagePlus("mask", bp);
                Roi roi = ThresholdToSelection.run(mask);
                if (roi == null) continue;

                roi.setPosition(0, z, 0);  // C=0, Z=z, T=0
                roi.setGroup(label);
                roi.setName(String.format("obj-%03d-z%03d", label, z));
                rm.addRoi(roi);
            }
        }
    }
}
