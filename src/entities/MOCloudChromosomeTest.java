package entities;

import static org.junit.Assert.*;

import org.junit.Test;

import algorithms.moga.EGAObjectives;
import dataParser.cloud.ECloudSimulator;
import dataParser.metadata.MetaTestCase;
import mutation.MutableCloud.MutableCloud;

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

    /** Verify the index-based getObjective: 0 -> energy, 1 -> time, other -> -1. */
    @Test
    public void testGetObjectiveByIndex() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setEnergyConsumption(4.0);
        c.setTime(6.0);
        assertEquals(4.0, c.getObjective(0), 1e-9);
        assertEquals(6.0, c.getObjective(1), 1e-9);
        assertEquals(-1.0, c.getObjective(2), 1e-9);
    }

    /** isFitnessValid is true only when both energy and time are strictly positive. */
    @Test
    public void testIsFitnessValid() {
        MOCloudChromosome valid = new MOCloudChromosome();
        valid.setEnergyConsumption(1.0);
        valid.setTime(1.0);
        assertTrue(valid.isFitnessValid());

        MOCloudChromosome invalid = new MOCloudChromosome();
        invalid.setEnergyConsumption(0.0);
        invalid.setTime(5.0);
        assertFalse(invalid.isFitnessValid());
    }

    /** The auxiliary vector is fixed-length 5 and shared by reference. */
    @Test
    public void testGetVector() {
        MOCloudChromosome c = new MOCloudChromosome();
        assertEquals(5, c.getVector().length);
        c.getVector()[0] = 9;
        assertEquals(9, c.getVector()[0]);
    }

    /** With no MutableCloud set, getCloudSystem / getMutableCloudSystem return null. */
    @Test
    public void testCloudSystemAccessorsWhenUnset() {
        MOCloudChromosome c = new MOCloudChromosome();
        assertNull(c.getCloudSystem());
        assertNull(c.getMutableCloudSystem());
    }

    /** setMetaTC / getMetaTC (and its getMetaTestCase alias) round-trip null safely. */
    @Test
    public void testMetaTestCaseAccessors() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setMetaTestCase(null);
        assertNull(c.getMetaTC());
        assertNull(c.getMetaTestCase());
    }

    /** dominates() is also true via the time branch (strictly higher time, equal energy). */
    @Test
    public void testDominatesViaTimeBranch() {
        MOCloudChromosome a = new MOCloudChromosome();
        a.setEnergyConsumption(5.0);
        a.setTime(10.0);
        MOCloudChromosome b = new MOCloudChromosome();
        b.setEnergyConsumption(5.0);
        b.setTime(3.0);
        assertTrue(a.dominates(b));
    }

    /** clone() performs a shallow copy of the fixed-size auxiliary vector. */
    @Test
    public void testCloneCopiesVector() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.getVector()[2] = 13;
        MOCloudChromosome clone = c.clone();
        assertNotSame(c, clone);
        assertEquals(13, clone.getVector()[2]);
    }

    /**
     * With a real MutableCloud and MetaTestCase attached, dup() takes its
     * non-null branches (cloning both) and getCloudSystem() resolves through the
     * mutable cloud rather than returning null.
     */
    @Test
    public void testDupAndCloudSystemWithRealCollaborators() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setMutableCloudSystem(new MutableCloud(ECloudSimulator.eCLOUDSIMSTORAGE));
        c.setMetaTestCase(new MetaTestCase());

        MOCloudChromosome dup = c.dup();
        assertNotNull(dup);
        assertNotSame(c, dup);
        assertNotNull("metadata must be cloned", dup.getMetaTC());
        assertNotNull("mutable cloud must be cloned", dup.getMutableCloudSystem());
        // getCloudSystem() now follows the non-null branch.
        c.getCloudSystem();
    }

    /**
     * The crowding/rank/fitness members are inert stubs on this entity (the
     * algorithms keep that state in MOSolution). Exercise them for completeness.
     */
    @Test
    public void testInertOverridesAreNoOps() {
        MOCloudChromosome c = new MOCloudChromosome();
        c.setDominated(3);
        c.setnCrowdDensity(1.0);
        c.setFitness();
        c.setCrowdingDistance(2.0);
        c.addToCrowdingDistance(1.0);
        c.setRank(4);
        assertEquals(0.0, c.getNumDom(), 1e-9);
        assertEquals(0.0, c.getFitness(), 1e-9);
        assertEquals(0.0, c.getCrowdingDistance(), 1e-9);
        assertEquals(0, c.getRank());
        assertNull(c.getObjectivesIndex());
    }
}
