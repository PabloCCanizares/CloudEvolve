package main_scico.hv;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the package-private {@link Extreme} accumulator and the
 * {@link RefTracker} reference-point store (which persists via a properties file).
 */
public class HVRefTrackerTest {

    private Path store;

    @Before
    public void setUp() throws IOException {
        store = Files.createTempFile("hv-ref", ".properties");
        // Start from a clean slate: RefTracker.load() must early-return on absence.
        Files.deleteIfExists(store);
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(store);
    }

    @Test
    public void testExtremeTracksMaxima() {
        Extreme e = new Extreme();
        assertFalse(e.valid());
        e.observe(5.0, 3.0);
        e.observe(2.0, 9.0);
        e.observe(-1.0, 100.0); // non-positive energy ignored
        e.observe(7.0, 0.0);    // non-positive time ignored
        assertTrue(e.valid());
        assertEquals(5.0, e.maxE, 1e-9);
        assertEquals(9.0, e.maxT, 1e-9);
    }

    @Test
    public void testObserveAccumulatesPerCaseAndGlobal() {
        RefTracker rt = new RefTracker(store);
        rt.observe("caseA", 10.0, 4.0);
        rt.observe("caseB", 3.0, 20.0);

        assertEquals(10.0, rt.getCaseExtreme("caseA").maxE, 1e-9);
        assertEquals(4.0, rt.getCaseExtreme("caseA").maxT, 1e-9);
        // Global maxima span all cases.
        assertEquals(10.0, rt.getGlobalExtreme().maxE, 1e-9);
        assertEquals(20.0, rt.getGlobalExtreme().maxT, 1e-9);
    }

    @Test
    public void testUnknownCaseReturnsEmptyExtreme() {
        RefTracker rt = new RefTracker(store);
        Extreme missing = rt.getCaseExtreme("does-not-exist");
        assertFalse(missing.valid());
    }

    @Test
    public void testSaveThenLoadRoundTrip() {
        RefTracker writer = new RefTracker(store);
        writer.observe("caseA", 10.0, 4.0);
        writer.observe("caseB", 3.0, 20.0);
        writer.save();

        assertTrue("save() must create the store file", Files.exists(store));

        RefTracker reader = new RefTracker(store); // load() reads the persisted maxima
        assertEquals(10.0, reader.getCaseExtreme("caseA").maxE, 1e-9);
        assertEquals(20.0, reader.getCaseExtreme("caseB").maxT, 1e-9);
        assertEquals(10.0, reader.getGlobalExtreme().maxE, 1e-9);
        assertEquals(20.0, reader.getGlobalExtreme().maxT, 1e-9);
    }
}
