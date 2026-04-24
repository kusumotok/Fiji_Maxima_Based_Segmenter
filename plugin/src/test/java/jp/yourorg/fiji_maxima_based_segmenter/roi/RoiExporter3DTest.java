package jp.yourorg.fiji_maxima_based_segmenter.roi;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.ShortProcessor;
import org.junit.Test;

import java.awt.Color;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RoiExporter3DTest {
    @Test
    public void groupsRoisByObjectAndPreservesHyperstackPosition() {
        ImageStack labels = new ImageStack(6, 6);
        ShortProcessor z1 = new ShortProcessor(6, 6);
        ShortProcessor z2 = new ShortProcessor(6, 6);

        fillRect(z1, 1, 1, 3, 3, 1);
        fillRect(z2, 2, 2, 4, 4, 1);
        fillRect(z2, 4, 4, 5, 5, 2);
        labels.addSlice(z1);
        labels.addSlice(z2);

        ImagePlus labelImage = new ImagePlus("labels", labels);
        ImagePlus source = makeHyperStack(6, 6, 3, 2);

        RoiExporter3D exporter = new RoiExporter3D();
        Map<Integer, List<Roi>> grouped = exporter.exportToRoiListsByLabel(
            labelImage, Color.RED, source, 2);

        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get(1).size());
        assertEquals(1, grouped.get(2).size());

        for (Roi roi : grouped.get(1)) {
            assertEquals(2, roi.getCPosition());
            assertEquals(1, roi.getTPosition());
            assertTrue(roi.getZPosition() == 1 || roi.getZPosition() == 2);
        }
        Roi label2 = grouped.get(2).get(0);
        assertEquals(2, label2.getCPosition());
        assertEquals(2, label2.getZPosition());
        assertEquals(1, label2.getTPosition());
    }

    private static void fillRect(ShortProcessor ip, int x0, int y0, int x1, int y1, int label) {
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                ip.set(x, y, label);
            }
        }
    }

    private static ImagePlus makeHyperStack(int w, int h, int channels, int slices) {
        ImageStack stack = new ImageStack(w, h);
        for (int z = 1; z <= slices; z++) {
            for (int c = 1; c <= channels; c++) {
                stack.addSlice(new ShortProcessor(w, h));
            }
        }
        ImagePlus imp = new ImagePlus("source", stack);
        imp.setDimensions(channels, slices, 1);
        imp.setOpenAsHyperStack(true);
        return imp;
    }
}
