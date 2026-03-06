package configuration;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for the LogLevel enum.
 */
public class LogLevelTest {

    /** Verify that the enum contains the expected four constants. */
    @Test
    public void testValues() {
        LogLevel[] levels = LogLevel.values();
        boolean hasVerbose  = false;
        boolean hasLog      = false;
        boolean hasNormal   = false;
        boolean hasCritical = false;
        for (LogLevel l : levels) {
            if (l == LogLevel.eVERBOSE)  hasVerbose  = true;
            if (l == LogLevel.eLOG)      hasLog      = true;
            if (l == LogLevel.eNORMAL)   hasNormal   = true;
            if (l == LogLevel.eCRITICAL) hasCritical = true;
        }
        assertTrue("eVERBOSE missing",  hasVerbose);
        assertTrue("eLOG missing",      hasLog);
        assertTrue("eNORMAL missing",   hasNormal);
        assertTrue("eCRITICAL missing", hasCritical);
    }

    /** Verify that eVERBOSE has the numeric value 4. */
    @Test
    public void testGetValueVerbose() {
        assertEquals(4, LogLevel.eVERBOSE.getValue());
    }

    /** Verify that eLOG has the numeric value 3. */
    @Test
    public void testGetValueLog() {
        assertEquals(3, LogLevel.eLOG.getValue());
    }

    /** Verify that eNORMAL has the numeric value 2. */
    @Test
    public void testGetValueNormal() {
        assertEquals(2, LogLevel.eNORMAL.getValue());
    }

    /** Verify that eCRITICAL has the numeric value 1. */
    @Test
    public void testGetValueCritical() {
        assertEquals(1, LogLevel.eCRITICAL.getValue());
    }

    /** Verify that the numeric ordering is VERBOSE > LOG > NORMAL > CRITICAL. */
    @Test
    public void testOrdinalOrder() {
        assertTrue(LogLevel.eVERBOSE.getValue()  > LogLevel.eLOG.getValue());
        assertTrue(LogLevel.eLOG.getValue()      > LogLevel.eNORMAL.getValue());
        assertTrue(LogLevel.eNORMAL.getValue()   > LogLevel.eCRITICAL.getValue());
    }
}
