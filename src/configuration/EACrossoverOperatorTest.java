package configuration;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link EACrossoverOperator}.
 */
public class EACrossoverOperatorTest {

    @Test
    public void testConstructorSetsProbabilityAndInheritedFields() {
        EACrossoverOperator op = new EACrossoverOperator(2, 0.4, "OnePoint", true);
        assertEquals(0.4, op.getProbability(), 1e-9);
        assertEquals(2, op.getnOperator());
        assertEquals("OnePoint", op.getDescription());
        assertTrue(op.isEnabled());
    }

    @Test
    public void testSetProbability() {
        EACrossoverOperator op = new EACrossoverOperator(1, 0.1, "Op", false);
        op.setProbability(0.85);
        assertEquals(0.85, op.getProbability(), 1e-9);
        assertFalse(op.isEnabled());
    }
}
