package jp.yourorg.fiji_maxima_based_segmenter.alg;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpotMeasurerTest {
    @Test
    public void measuresMaxFeretEndpointsAndVoxelMetrics() {
        ImageStack labels = new ImageStack(5, 5);
        ImageStack intensities = new ImageStack(5, 5);

        ShortProcessor labelZ1 = new ShortProcessor(5, 5);
        ShortProcessor labelZ2 = new ShortProcessor(5, 5);
        ShortProcessor intensityZ1 = new ShortProcessor(5, 5);
        ShortProcessor intensityZ2 = new ShortProcessor(5, 5);

        labelZ1.set(0, 0, 1);
        intensityZ1.set(0, 0, 100);
        labelZ2.set(3, 4, 1);
        intensityZ2.set(3, 4, 200);

        labels.addSlice(labelZ1);
        labels.addSlice(labelZ2);
        intensities.addSlice(intensityZ1);
        intensities.addSlice(intensityZ2);

        SegmentationResult3D seg = new SegmentationResult3D(new ImagePlus("labels", labels));
        ImagePlus raw = new ImagePlus("raw", intensities);

        List<SpotMeasurement> measurements = SpotMeasurer.measure(seg, raw, 2.0, 3.0, 5.0);

        assertEquals(1, measurements.size());
        SpotMeasurement spot = measurements.get(0);
        assertEquals(1, spot.id);
        assertEquals(2, spot.volumeVox);
        assertEquals(60.0, spot.volumeUm3, 1e-6);
        assertEquals(124.0, spot.surfaceAreaUm2, 1e-6);
        assertEquals(300.0, spot.integratedIntensity, 1e-6);
        assertEquals(150.0, spot.meanIntensity, 1e-6);
        assertEquals(200.0, spot.maxIntensity, 1e-6);
        assertEquals(Math.sqrt(205.0), spot.maxFeret3DUm, 1e-6);
        assertEquals(0.0, spot.maxFeretP1XUm, 1e-6);
        assertEquals(0.0, spot.maxFeretP1YUm, 1e-6);
        assertEquals(0.0, spot.maxFeretP1ZUm, 1e-6);
        assertEquals(6.0, spot.maxFeretP2XUm, 1e-6);
        assertEquals(12.0, spot.maxFeretP2YUm, 1e-6);
        assertEquals(5.0, spot.maxFeretP2ZUm, 1e-6);
    }
}
