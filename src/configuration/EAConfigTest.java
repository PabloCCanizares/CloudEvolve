package configuration;

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for the EAConfig singleton.
 * Reflection is used to reset the singleton instance between tests so that
 * each test starts from a clean state.
 */
public class EAConfigTest {

    /**
     * Reset the singleton instance before each test using reflection so that
     * tests remain independent of each other.
     */
    @Before
    public void resetSingleton() throws Exception {
        Field field = EAConfig.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, null);
    }

    /** Verify that getInstance() always returns the same instance. */
    @Test
    public void testSingletonPattern() {
        EAConfig first  = EAConfig.getInstance();
        EAConfig second = EAConfig.getInstance();
        assertSame(first, second);
    }

    /** Verify that the default log level is LogLevel.eNORMAL. */
    @Test
    public void testDefaultLogLevel() {
        EAConfig config = EAConfig.getInstance();
        assertEquals(LogLevel.eNORMAL, config.getLogLevel());
    }

    /** Verify setter and getter for the log level. */
    @Test
    public void testSetAndGetLogLevel() {
        EAConfig config = EAConfig.getInstance();
        config.seteLogLevel(LogLevel.eVERBOSE);
        assertEquals(LogLevel.eVERBOSE, config.getLogLevel());
    }

    /** Verify setter and getter for the total timeout. */
    @Test
    public void testSetAndGetTotalTimeout() {
        EAConfig config = EAConfig.getInstance();
        config.setTotalTimeout(120);
        assertEquals(120, config.getTotalTimeout());
    }

    /** Verify setter and getter for the total iterations. */
    @Test
    public void testSetAndGetTotalIterations() {
        EAConfig config = EAConfig.getInstance();
        config.setTotalIterations(50);
        assertEquals(50, config.getTotalIterations());
    }
}
