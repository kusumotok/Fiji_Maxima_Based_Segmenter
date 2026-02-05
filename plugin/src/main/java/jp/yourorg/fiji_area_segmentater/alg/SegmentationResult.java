package jp.yourorg.fiji_area_segmentater.alg;

import ij.ImagePlus;

public class SegmentationResult {
    public final ImagePlus labelImage;
    public SegmentationResult(ImagePlus labelImage) { this.labelImage = labelImage; }
}
