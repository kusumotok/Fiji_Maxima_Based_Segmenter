package jp.yourorg.fiji_maxima_based_segmenter.alg;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;

import java.util.HashMap;
import java.util.Map;

/**
 * Core algorithm for 3D spot quantification:
 *   1. [Optional] Gaussian Blur 3D
 *   2. Fixed threshold → binary mask
 *   3. FloodFillComponentsLabeling3D (MorphoLibJ, 26-connectivity)
 *   4. Collect per-label voxel counts
 *
 * Size filtering is NOT applied here; use CcResult3D.classifyLabels() and
 * CcResult3D.buildFilteredResult() to apply size constraints after caching.
 */
public class SpotQuantifier3D {

    /**
     * Run CC labeling and return the full (unfiltered) result.
     *
     * @param imp    Input 3D stack (16-bit)
     * @param params Quantifier parameters
     * @return CcResult3D with full label image and voxel-count map
     */
    public static CcResult3D computeCC(ImagePlus imp, QuantifierParams params) {
        int w = imp.getWidth();
        int h = imp.getHeight();
        int d = imp.getNSlices();

        // --- 1. Optional Gaussian blur on a duplicate ---
        ImagePlus working = imp;
        if (params.gaussianBlur) {
            working = imp.duplicate();
            working.setTitle(imp.getShortTitle() + "-blurred");
            IJ.run(working, "Gaussian Blur 3D...",
                "x=" + params.gaussXY + " y=" + params.gaussXY + " z=" + params.gaussZ);
        }

        // --- 2. Build binary stack: pixels >= threshold → 255, else 0 ---
        ImageStack srcStack    = working.getStack();
        ImageStack binaryStack = new ImageStack(w, h);
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = srcStack.getProcessor(z);
            ByteProcessor bp  = new ByteProcessor(w, h);
            byte[] bpix = (byte[]) bp.getPixels();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (ip.get(x, y) >= params.threshold) {
                        bpix[y * w + x] = (byte) 255;
                    }
                }
            }
            binaryStack.addSlice(bp);
        }

        if (params.gaussianBlur && working != imp) {
            working.close();
        }

        // --- 3. Optional fill holes (per-slice 2D) ---
        ImagePlus binaryImp = new ImagePlus("binary", binaryStack);
        if (params.fillHoles) {
            IJ.run(binaryImp, "Fill Holes", "stack");
        }

        // --- 4. 3D connected-components labeling (32-bit labels) ---
        // 32-bit avoids the 65535 label cap when many noise CCs are present above threshold.
        ImagePlus labelImp  = BinaryImages.componentsLabeling(binaryImp, params.connectivity, 32);
        ImageStack labelStack = labelImp.getStack();

        // --- 5. Count voxels per label ---
        Map<Integer, Long> voxelCounts = new HashMap<>();
        for (int z = 1; z <= d; z++) {
            ImageProcessor ip = labelStack.getProcessor(z);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int label = (int) Math.round(ip.getPixelValue(x, y));
                    if (label > 0) {
                        voxelCounts.merge(label, 1L, Long::sum);
                    }
                }
            }
        }

        ImagePlus labelImage = new ImagePlus(imp.getShortTitle() + "-cc", labelStack);
        return new CcResult3D(labelImage, voxelCounts);
    }
}
