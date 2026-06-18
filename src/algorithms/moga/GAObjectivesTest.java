package algorithms.moga;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the GAObjectives class.
 */
public class GAObjectivesTest {

    private GAObjectives objectives;

    @Before
    public void setUp() {
        objectives = new GAObjectives();
    }

    /** Verify that an objective can be added and retrieved. */
    @Test
    public void testAddAndGetObjective() {
        objectives.addObjective(EGAObjectives.eENERGY, 5.0);
        assertEquals(5.0, objectives.getObjective(EGAObjectives.eENERGY), 1e-9);
    }

    /** Verify that getObjective returns -1.0 for keys that have not been registered. */
    @Test
    public void testGetObjectiveReturnsMinusOneWhenMissing() {
        // No objectives added; all keys should return -1.0.
        assertEquals(-1.0, objectives.getObjective(EGAObjectives.eENERGY), 1e-9);
        assertEquals(-1.0, objectives.getObjective(EGAObjectives.eTIME), 1e-9);
    }

    /** Verify that isValid() returns false when at least one objective value is negative. */
    @Test
    public void testIsValidReturnsFalseWhenNegativeValue() {
        objectives.addObjective(EGAObjectives.eENERGY, 3.0);
        objectives.addObjective(EGAObjectives.eTIME, -1.0);
        assertFalse(objectives.isValid());
    }

    /** Verify that isValid() returns true when all objective values are non-negative. */
    @Test
    public void testIsValidReturnsTrueWhenAllPositive() {
        objectives.addObjective(EGAObjectives.eENERGY, 3.0);
        objectives.addObjective(EGAObjectives.eTIME, 2.0);
        assertTrue(objectives.isValid());
    }

    /**
     * Verify that compareTo returns 1 when this dominates other
     * (all objectives of this are strictly less than other's).
     */
    @Test
    public void testCompareToThisDominates() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 1.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 2.0);
        other.addObjective(EGAObjectives.eTIME, 2.0);

        // compareTo returns 1 when at least one objective of this < corresponding in other.
        assertEquals(1, objectives.compareTo(other));
    }

    /** Verify that compareTo returns 0 when all objectives are equal. */
    @Test
    public void testCompareToEqual() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 1.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 1.0);
        other.addObjective(EGAObjectives.eTIME, 1.0);

        assertEquals(0, objectives.compareTo(other));
    }
}
