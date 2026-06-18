package entities;

import static org.junit.Assert.*;

import org.junit.Test;

import algorithms.moga.EGAObjectives;

/**
 * Unit tests for MOCloudChromosome.
 * External dependencies (MutableCloud, MetaTestCase) are passed as null because
 * only the simple energy/time accessors are exercised here.
 */
public class MOCloudChromosomeTest {

    /** Verify that the parametrized constructor correctly sets energy and time. */
    @Test
    public void testConstructorSetsFields() {
        MOCloudChromosome c = new MOCloudChromosome(null, 10.5, 20.3, null);
        assertEquals(10.5, c.getEnergyConsumption(), 1e-9);
        assertEquals(20.3, c.getSimTime(), 1e-9);
    }

    /** Verify that the default constructor creates a non-null object. */
    @Test
    public void testDefaultConstructor() {
        MOCloudChromosome c = new MOCloudChromosome();
        assertNotNull(c);
    }

    /** Verify setter and getter for energyConsumption. */
    @Test
    public void testSetAndGetEnergyConsumption() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setEnergyConsumption(7.77);
        assertEquals(7.77, c.getEnergyConsumption(), 1e-9);
    }

    /** Verify setter and getter for simulation time. */
    @Test
    public void testSetAndGetTime() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setTime(3.14);
        assertEquals(3.14, c.getSimTime(), 1e-9);
    }

    /** Verify that getObjective(eENERGY) returns the energy consumption value. */
    @Test
    public void testGetObjectiveEnergy() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setEnergyConsumption(5.5);
        assertEquals(5.5, c.getObjective(EGAObjectives.eENERGY), 1e-9);
    }

    /** Verify that getObjective(eTIME) returns the simulation time value. */
    @Test
    public void testGetObjectiveTime() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setTime(9.9);
        assertEquals(9.9, c.getObjective(EGAObjectives.eTIME), 1e-9);
    }

    /**
     * Verify that dominates() returns true when this chromosome has strictly higher
     * energy and equal-or-higher time than the other (per the current implementation).
     */
    @Test
    public void testDominatesReturnsTrueWhenDominates() {
        MOCloudChromosome a = new MOCloudChromosome();
        a.setEnergyConsumption(10.0);
        a.setTime(5.0);

        MOCloudChromosome b = new MOCloudChromosome();
        b.setEnergyConsumption(5.0);
        b.setTime(3.0);

        assertTrue(a.dominates(b));
    }

    /** Verify that dominates() returns false when this chromosome does not dominate the other. */
    @Test
    public void testDominatesReturnsFalseWhenNotDominates() {
        MOCloudChromosome a = new MOCloudChromosome();
        a.setEnergyConsumption(3.0);
        a.setTime(2.0);

        MOCloudChromosome b = new MOCloudChromosome();
        b.setEnergyConsumption(5.0);
        b.setTime(4.0);

        assertFalse(a.dominates(b));
    }

    /**
     * Verify that dup() creates a distinct (non-same-reference) object.
     * With null external fields the method prints error messages to stdout,
     * which is the expected behaviour of the current implementation.
     */
    @Test
    public void testDupCreatesACopy() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setEnergyConsumption(5.0);
        c.setTime(3.0);
        MOCloudChromosome dup = c.dup();
        assertNotNull(dup);
        assertNotSame(c, dup);
    }

    /**
     * Verify that toString() uses the format "(id, energy, time)".
     * The expected value is built with the same format spec as the production
     * code so the assertion is locale-independent: {@code %f} renders the
     * decimal separator according to the default locale (e.g. "1.500000" under
     * Locale.US, "1,500000" under a comma-decimal locale such as es_ES), and a
     * hard-coded literal would fail on the latter.
     */
    @Test
    public void testToString() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setId(7);
        c.setEnergyConsumption(1.5);
        c.setTime(2.5);
        String result = c.toString();
        String expected = String.format("(%d, %.6f, %f)", 7, 1.5, 2.5);
        assertEquals(expected, result);
    }

    /** Verify that getId() returns the id that was previously set. */
    @Test
    public void testGetId() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setId(42);
        assertEquals(42, c.getId());
    }
}
