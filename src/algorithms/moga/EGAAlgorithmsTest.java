package algorithms.moga;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the {@link EGAAlgorithms} enum and its {@code toString()} mapping.
 */
public class EGAAlgorithmsTest {

    @Test
    public void testThereAreNineAlgorithms() {
        assertEquals(9, EGAAlgorithms.values().length);
    }

    @Test
    public void testToStringMapping() {
        assertEquals("MOGA", EGAAlgorithms.eMOGA.toString());
        assertEquals("VEGA", EGAAlgorithms.eVEGA.toString());
        assertEquals("VEGA2", EGAAlgorithms.eVEGA2.toString());
        assertEquals("SPEA2", EGAAlgorithms.eSPEA2.toString());
        assertEquals("SPEA3", EGAAlgorithms.eSPEA3.toString());
        assertEquals("PAES", EGAAlgorithms.ePAES.toString());
        assertEquals("PAES2", EGAAlgorithms.ePAES2.toString());
        assertEquals("NSGAII", EGAAlgorithms.eNSGAII.toString());
        assertEquals("NSGAII2", EGAAlgorithms.eNSGAII2.toString());
    }

    @Test
    public void testValueOfRoundTrip() {
        for (EGAAlgorithms a : EGAAlgorithms.values()) {
            assertSame(a, EGAAlgorithms.valueOf(a.name()));
        }
    }
}
