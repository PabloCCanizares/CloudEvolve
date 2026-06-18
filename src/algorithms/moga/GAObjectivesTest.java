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
     * Pareto convention (minimization): compareTo returns -1 when THIS dominates
     * the other, i.e. this is no worse on every objective and strictly better on
     * at least one ("less than" == "better").
     */
    @Test
    public void testCompareToThisDominates() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 1.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 2.0);
        other.addObjective(EGAObjectives.eTIME, 2.0);

        assertEquals(-1, objectives.compareTo(other));
        // Antisymmetry: the dominated side compares as +1.
        assertEquals(1, other.compareTo(objectives));
    }

    /**
     * Dominance also holds when only one objective is strictly better and the
     * other is equal.
     */
    @Test
    public void testCompareToDominatesOnSingleObjective() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 5.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 1.0); // equal
        other.addObjective(EGAObjectives.eTIME, 8.0);   // worse

        assertEquals(-1, objectives.compareTo(other));
    }

    /** A trade-off pair is mutually non-dominated, so compareTo returns 0 both ways. */
    @Test
    public void testCompareToNonDominatedTradeOff() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 9.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 9.0);
        other.addObjective(EGAObjectives.eTIME, 1.0);

        assertEquals(0, objectives.compareTo(other));
        assertEquals(0, other.compareTo(objectives));
    }

    /** Differences smaller than EPS are treated as ties (non-dominated). */
    @Test
    public void testCompareToTreatsNearEqualAsTie() {
        objectives.addObjective(EGAObjectives.eENERGY, 1.0);
        objectives.addObjective(EGAObjectives.eTIME, 1.0);

        GAObjectives other = new GAObjectives();
        other.addObjective(EGAObjectives.eENERGY, 1.0 + 1e-12);
        other.addObjective(EGAObjectives.eTIME, 1.0);

        assertEquals(0, objectives.compareTo(other));
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
