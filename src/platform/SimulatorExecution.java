package platform;

/**
 * Callback surface a {@link SimulatorPlatform} uses to launch the external
 * simulator, decoupling the per-simulator command logic from MT_Handler's
 * process machinery.
 *
 * <p>MT_Handler implements it in production; a spy can stand in for it in tests
 * so the per-simulator dispatch is verified without actually running a
 * simulator subprocess.</p>
 */
public interface SimulatorExecution {

    /** Runs a launch command through the CloudSim-Storage execution path. */
    boolean executeCommand(String command);

    /** Runs a launch command through the SimGrid execution path. */
    boolean executeCommandSimGrid(String command);

    /** Shell timeout prefix prepended to the CloudSim-Storage launch command. */
    String timeoutHeader();

    /** Path to the configured simulator jar. */
    String simulatorPath();
}
