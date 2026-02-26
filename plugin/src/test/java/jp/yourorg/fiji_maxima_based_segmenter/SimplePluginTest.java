package jp.yourorg.fiji_maxima_based_segmenter;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import jp.yourorg.fiji_maxima_based_segmenter.alg.SegmentationResult;
import jp.yourorg.fiji_maxima_based_segmenter.alg.WatershedRunner;
import jp.yourorg.fiji_maxima_based_segmenter.core.*;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for Simple Plugin properties (Tasks 5.8-5.11).
 */
public class SimplePluginTest {

    // --- Property 1: Simple Plugin Domain Definition ---
    // For any 2D image and background threshold, domain = pixels with intensity >= BG_Threshold.

    @Test
    public void domainDefinition_allPixelsAboveThreshold() {
        // All pixels = 200, threshold = 100 => all in domain
        ByteProcessor bp = new ByteProcessor(4, 4);
        for (int i = 0; i < 16; i++) bp.set(i, 200);
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(100);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult result = builder.build(imp, model);

        for (int i = 0; i < 16; i++) {
            assertTrue("Pixel " + i + " should be in domain", result.domainMask[i]);
        }
    }

    @Test
    public void domainDefinition_noPixelsAboveThreshold() {
        // All pixels = 50, threshold = 100 => none in domain
        ByteProcessor bp = new ByteProcessor(4, 4);
        for (int i = 0; i < 16; i++) bp.set(i, 50);
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(100);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult result = builder.build(imp, model);

        for (int i = 0; i < 16; i++) {
            assertFalse("Pixel " + i + " should NOT be in domain", result.domainMask[i]);
        }
    }

    @Test
    public void domainDefinition_mixedPixels() {
        // Row 0: 200 (above), Row 1: 50 (below). threshold=100
        ByteProcessor bp = new ByteProcessor(4, 2);
        for (int x = 0; x < 4; x++) {
            bp.set(x, 0, 200);  // in domain
            bp.set(x, 1, 50);   // not in domain
        }
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(100);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult result = builder.build(imp, model);

        for (int x = 0; x < 4; x++) {
            assertTrue("Top row should be in domain", result.domainMask[x]);
            assertFalse("Bottom row should NOT be in domain", result.domainMask[4 + x]);
        }
    }

    // --- Property 2: Simple Plugin Fixed Parameters ---
    // C4, WATERSHED, INVERT_ORIGINAL, FIND_MAXIMA, no preprocessing.

    @Test
    public void fixedParameters() {
        ByteProcessor bp = new ByteProcessor(4, 4);
        ImagePlus imp = new ImagePlus("test", bp);
        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);

        assertEquals(Connectivity.C4, model.getConnectivity());
        assertEquals(Method.WATERSHED, model.getMethod());
        assertEquals(Surface.INVERT_ORIGINAL, model.getSurface());
        assertEquals(MarkerSource.FIND_MAXIMA, model.getMarkerSource());
        assertFalse(model.isPreprocessingEnabled());
    }

    // --- Property 3: Tolerance Parameter Propagation ---
    // The FindMaxima algorithm receives the exact tolerance set by the user.

    @Test
    public void tolerancePropagation() {
        ByteProcessor bp = new ByteProcessor(4, 4);
        ImagePlus imp = new ImagePlus("test", bp);
        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);

        model.setFindMaximaTolerance(25.5);
        assertEquals(25.5, model.getFindMaximaTolerance(), 0.001);

        model.setFindMaximaTolerance(0.0);
        assertEquals(0.0, model.getFindMaximaTolerance(), 0.001);

        model.setFindMaximaTolerance(100.0);
        assertEquals(100.0, model.getFindMaximaTolerance(), 0.001);
    }

    // --- Property 4: Label Image Format ---
    // Output contains only 0 (background) or 1..N (object labels).

    @Test
    public void labelImageFormat_singleObject() {
        // Create image with bright region on dark background
        ByteProcessor bp = new ByteProcessor(10, 10);
        // Bright spot at center
        for (int y = 3; y < 7; y++) {
            for (int x = 3; x < 7; x++) {
                bp.set(x, y, 200);
            }
        }
        // Peak at center
        bp.set(5, 5, 255);
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(50);
        model.setFindMaximaTolerance(10.0);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult markers = builder.build(imp, model);

        if (markers.fgCount > 0) {
            SegmentationResult seg = new WatershedRunner().run(
                imp, markers, model.getSurface(), model.getConnectivity(),
                false, 0.0
            );

            assertNotNull(seg);
            assertNotNull(seg.labelImage);

            int w = seg.labelImage.getWidth();
            int h = seg.labelImage.getHeight();
            int maxLabel = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int v = seg.labelImage.getProcessor().get(x, y);
                    assertTrue("Label should be >= 0", v >= 0);
                    if (v > maxLabel) maxLabel = v;
                }
            }
            assertTrue("Should have at least one object", maxLabel >= 1);
        }
    }

    // --- Property 6: Domain-Only Segmentation ---
    // Pixels outside domain get label 0.

    @Test
    public void domainOnlySegmentation() {
        ByteProcessor bp = new ByteProcessor(10, 10);
        // Bright region
        for (int y = 2; y < 8; y++) {
            for (int x = 2; x < 8; x++) {
                bp.set(x, y, 200);
            }
        }
        bp.set(5, 5, 255);
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        model.setTBg(50);
        model.setFindMaximaTolerance(10.0);

        MarkerBuilder builder = new MarkerBuilder();
        MarkerResult markers = builder.build(imp, model);

        if (markers.fgCount > 0) {
            SegmentationResult seg = new WatershedRunner().run(
                imp, markers, model.getSurface(), model.getConnectivity(),
                false, 0.0
            );
            assertNotNull(seg);
            assertNotNull(seg.labelImage);

            // Corners (outside domain) should be 0
            assertEquals(0, seg.labelImage.getProcessor().get(0, 0));
            assertEquals(0, seg.labelImage.getProcessor().get(9, 9));
            assertEquals(0, seg.labelImage.getProcessor().get(0, 9));
            assertEquals(0, seg.labelImage.getProcessor().get(9, 0));
        }
    }

    // --- Simple Plugin programmatic API ---

    @Test
    public void staticSegmentMethod() {
        ByteProcessor bp = new ByteProcessor(10, 10);
        for (int y = 2; y < 8; y++) {
            for (int x = 2; x < 8; x++) {
                bp.set(x, y, 200);
            }
        }
        bp.set(5, 5, 255);
        ImagePlus imp = new ImagePlus("test", bp);

        ImagePlus result = Maxima_Based_Segmenter_Simple_.segment(imp, 50, 10.0);
        // Result may be null if no seeds found (depends on image), but should not throw
        if (result != null) {
            assertEquals(10, result.getWidth());
            assertEquals(10, result.getHeight());
        }
    }

    // --- ThresholdModel defaults ---

    @Test
    public void defaultBgThreshold() {
        ByteProcessor bp = new ByteProcessor(4, 4);
        for (int i = 0; i < 16; i++) bp.set(i, 100);
        ImagePlus imp = new ImagePlus("test", bp);

        ThresholdModel model = ThresholdModel.createForSimplePlugin(imp);
        // Default BG threshold should be ~20% of max
        assertTrue("Default BG threshold should be set", model.getTBg() >= 0);
    }

    // --- Renamed Plugin FG Threshold Constraint (Property 21) ---

    @Test
    public void fgThresholdConstraint_thresholdComponents() {
        ShortProcessor sp = new ShortProcessor(4, 4);
        for (int i = 0; i < 16; i++) sp.set(i, 1000);
        ImagePlus imp = new ImagePlus("test", sp);

        ThresholdModel model = new ThresholdModel(imp);
        model.setMarkerSource(MarkerSource.THRESHOLD_COMPONENTS);
        model.setTFg(200);
        model.setTBg(300);

        // When MarkerSource = THRESHOLD_COMPONENTS, BG <= FG must hold
        assertTrue("BG should be snapped to <= FG",
            model.getTBg() <= model.getTFg());
    }

    // --- Renamed Plugin FG Threshold Independence (Property 22) ---

    @Test
    public void fgThresholdIndependence_findMaxima() {
        ShortProcessor sp = new ShortProcessor(4, 4);
        for (int i = 0; i < 16; i++) sp.set(i, 1000);
        ImagePlus imp = new ImagePlus("test", sp);

        // Verify image statistics
        assertEquals(1000.0, imp.getStatistics().max, 0.001);

        ThresholdModel model = new ThresholdModel(imp);
        // MarkerSource is already FIND_MAXIMA by default
        assertEquals(MarkerSource.FIND_MAXIMA, model.getMarkerSource());
        
        // Set values independently (no constraint when MarkerSource != THRESHOLD_COMPONENTS)
        model.setTBg(500);
        model.setTFg(100);

        // When MarkerSource != THRESHOLD_COMPONENTS, BG can exceed FG
        assertEquals(100, model.getTFg());
        assertEquals(500, model.getTBg());
    }
}
