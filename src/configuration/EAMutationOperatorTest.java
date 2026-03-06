package configuration;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the EAMutationOperator class.
 */
public class EAMutationOperatorTest {

    /** Verify that the constructor correctly assigns all fields. */
    @Test
    public void testConstructorSetsFields() {
        EAMutationOperator op = new EAMutationOperator(1, 0.25, "TestMutation", true);
        assertEquals(1,             op.getnOperator());
        assertEquals("TestMutation", op.getDescription());
        assertTrue(op.isEnabled());
        assertEquals(0.25, op.getProbability(), 1e-9);
    }

    /** Verify setter and getter for probability. */
    @Test
    public void testSetAndGetProbability() {
        EAMutationOperator op = new EAMutationOperator(2, 0.5, "Op2", true);
        op.setProbability(0.75);
        assertEquals(0.75, op.getProbability(), 1e-9);
    }

    /** Verify that the operator is enabled when constructed with bEnabled=true. */
    @Test
    public void testIsEnabled() {
        EAMutationOperator op = new EAMutationOperator(3, 0.1, "EnabledOp", true);
        assertTrue(op.isEnabled());
    }

    /** Verify that the operator is disabled when constructed with bEnabled=false. */
    @Test
    public void testIsDisabled() {
        EAMutationOperator op = new EAMutationOperator(4, 0.1, "DisabledOp", false);
        assertFalse(op.isEnabled());
    }
}
