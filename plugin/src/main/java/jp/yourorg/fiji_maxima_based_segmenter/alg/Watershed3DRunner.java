package jp.yourorg.fiji_maxima_based_segmenter.alg;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.watershed.Watershed;
import jp.yourorg.fiji_maxima_based_segmenter.core.Connectivity;
import jp.yourorg.fiji_maxima_based_segmenter.core.MarkerResult3D;

public class Watershed3DRunner {

    /**
     * Run 3D marker-controlled watershed using MorphoLibJ.
     * @param imp Input 3D image
     * @param markers 3D seed labels and domain
     * @param connectivity C6 connectivity
     * @return SegmentationResult3D with 3D label stack
     */
    public SegmentationResult3D run(ImagePlus imp, MarkerResult3D markers,
                                     Connectivity connectivity) {
        ImageStack inputStack = imp.getStack();
        ImageStack markerStack = markers.getSeedLabels();
        ImageStack domainStack = markers.getDomainMask();
        int conn3d = connectivity.to3D();

        // Invert intensity for watershed surface (find basins at bright areas)
        ImageStack invertedStack = invertIntensity(inputStack);

        // Run MorphoLibJ marker-controlled watershed
        ImageStack labelStack = Watershed.computeWatershed(
            invertedStack,
            markerStack,
            domainStack,
            conn3d,
            true  // use priority queue
        );

        ImagePlus labelImage = new ImagePlus(
            imp.getShortTitle() + "-labels-3D",
            labelStack
        );

        return new SegmentationResult3D(labelImage);
    }

    private ImageStack invertIntensity(ImageStack stack) {
        int w = stack.getWidth();
        int h = stack.getHeight();
        int d = stack.getSize();

        // Find global min/max
        float globalMin = Float.MAX_VALUE;
        float globalMax = -Float.MAX_VALUE;
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = stack.getProcessor(z);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float v = ip.getf(x, y);
                    if (v < globalMin) globalMin = v;
                    if (v > globalMax) globalMax = v;
                }
            }
        }

        float sum = globalMin + globalMax;
        ImageStack inverted = new ImageStack(w, h);
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = stack.getProcessor(z);
            FloatProcessor fp = new FloatProcessor(w, h);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    fp.setf(x, y, sum - ip.getf(x, y));
                }
            }
            inverted.addSlice(fp);
        }
        return inverted;
    }
}
