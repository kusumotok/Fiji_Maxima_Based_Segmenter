package jp.yourorg.fiji_area_segmentater;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import jp.yourorg.fiji_area_segmentater.ui.DualThresholdFrame;

public class DualThresholdMarkers_ implements PlugIn {
    @Override
    public void run(String arg) {
        ImagePlus imp = IJ.getImage();
        if (imp == null) {
            IJ.error("No image", "Open an image first.");
            return;
        }
        new DualThresholdFrame(imp).setVisible(true);
    }
}
