package algorithms.moga;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the EGAObjectives enum.
 */
public class EGAObjectivesTest {

    /** Verify that the enum contains eENERGY and eTIME values. */
    @Test
    public void testEnumValues() {
        EGAObjectives energy = EGAObjectives.eENERGY;
        EGAObjectives time   = EGAObjectives.eTIME;
        assertNotNull(energy);
        assertNotNull(time);
        assertEquals("eENERGY", energy.name());
        assertEquals("eTIME",   time.name());
    }

    /** Verify that there are exactly 2 enum constants. */
    @Test
    public void testEnumCount() {
        assertEquals(2, EGAObjectives.values().length);
    }
}
