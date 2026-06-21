package platform.surrogate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * Unit test for {@link SampleLogger}: the re-training increment must carry the
 * declared feature columns plus the two real labels, with a single header.
 */
public class SampleLoggerTest {

    @Test
    public void writesHeaderOnceAndAppendsRows() throws Exception {
        File csv = File.createTempFile("increment", ".csv");
        csv.delete();
        try {
            List<String> cols = Arrays.asList("host.quantity", "host.mips", "net.latency", "sto.type");
            SampleLogger logger = new SampleLogger(csv.getPath(), cols);

            logger.record(features("512", "1000", "10", "hdd"), 18.77, 2770.10);
            logger.record(features("484", "980", "245", "hdd"), 13.38, 3633.10);

            List<String> lines = Files.readAllLines(csv.toPath(), StandardCharsets.UTF_8);
            assertEquals(3, lines.size()); // header + 2 rows
            assertEquals("host.quantity,host.mips,net.latency,sto.type,energy_kwh,sim_time_sec", lines.get(0));
            assertTrue(lines.get(1).startsWith("512,1000,10,hdd,18.77"));
            assertTrue(lines.get(2).startsWith("484,980,245,hdd,13.38"));
        } finally {
            csv.delete();
        }
    }

    private static Map<String, String> features(String hostQ, String hostMips, String netLat, String stoType) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("host.quantity", hostQ);
        m.put("host.mips", hostMips);
        m.put("net.latency", netLat);
        m.put("sto.type", stoType);
        return m;
    }
}
