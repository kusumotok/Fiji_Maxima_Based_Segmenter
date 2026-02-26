package jp.yourorg.fiji_maxima_based_segmenter.alg;

import ij.ImagePlus;

public class SegmentationResult {
    public final ImagePlus labelImage;
    public SegmentationResult(ImagePlus labelImage) { this.labelImage = labelImage; }
}
