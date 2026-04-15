package jp.yourorg.fiji_maxima_based_segmenter;

import ij.plugin.PlugIn;
import jp.yourorg.fiji_maxima_based_segmenter.ui.SeededSpotQuantifier3DMultiFrame;

public class Seeded_Spot_Quantifier_3D_Multi_ implements PlugIn {
    @Override
    public void run(String arg) {
        new SeededSpotQuantifier3DMultiFrame().setVisible(true);
    }
}
