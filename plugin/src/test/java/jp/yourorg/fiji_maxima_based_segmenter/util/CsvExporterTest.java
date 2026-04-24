package jp.yourorg.fiji_maxima_based_segmenter.util;

import jp.yourorg.fiji_maxima_based_segmenter.alg.SpotMeasurement;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CsvExporterTest {
    @Test
    public void writesCurrentMeasurementColumns() throws Exception {
        File out = File.createTempFile("spots", ".csv");
        out.deleteOnExit();

        SpotMeasurement spot = new SpotMeasurement(
            7, 3, 12.5, 22.0, 0.9,
            300.0, 100.0, 180.0,
            1.0, 2.0, 3.0,
            4.0,
            1.0, 2.0, 3.0,
            4.0, 5.0, 6.0);

        CsvExporter.writeCsv(Collections.singletonList(spot), out);

        String csv = new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8);
        String[] lines = csv.trim().split("\\R");
        assertEquals(2, lines.length);
        assertTrue(lines[0].contains("max_feret3d_um"));
        assertTrue(lines[0].contains("max_feret_p1_x_um"));
        assertTrue(lines[0].contains("max_feret_p2_z_um"));
        assertEquals(lines[0].split(",", -1).length, lines[1].split(",", -1).length);
    }
}
