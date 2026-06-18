package configuration;

import static org.junit.Assert.*;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link EAProbabilityModule}.
 *
 * <p>Only the branches that do not depend on the {@code EAController} singleton
 * (and therefore on a configured simulator platform) are exercised here. The
 * {@code bAlwaysMutate == true} path of {@code calculateMutation()} reads
 * {@code EAController.getInstance().getPlaftormInfo()} and belongs to the
 * environment-coupled set excluded from unit testing.</p>
 */
public class EAProbabilityModuleTest {

    private EAProbabilityModule module;

    @Before
    public void setUp() {
        module = new EAProbabilityModule();
    }

    @Test
    public void testNoMutationWhenListNullAndNotAlwaysMutate() {
        // Fresh module: no operator list, always-mutate off -> never mutates.
        assertFalse(module.calculateMutation());
    }

    @Test
    public void testMutationSelectedFromOperatorList() {
        LinkedList<EAMutationOperator> ops = new LinkedList<EAMutationOperator>();
        // Probability 100 guarantees selection regardless of the random factor in [0,100).
        ops.add(new EAMutationOperator(7, 100.0, "Always", true));
        module.addMutationOperatorList(ops);

        assertTrue(module.calculateMutation());
        assertEquals(7, module.getLastMOperator());
    }

    @Test
    public void testDisabledOperatorIsSkipped() {
        LinkedList<EAMutationOperator> ops = new LinkedList<EAMutationOperator>();
        ops.add(new EAMutationOperator(3, 0.0, "Disabled", false));
        module.addMutationOperatorList(ops);
        // Single disabled operator with 0 probability -> no mutation chosen.
        assertFalse(module.calculateMutation());
    }

    @Test
    public void testCalculateCrossoverAlwaysSelectsOperatorTwo() {
        module.addCrossoverOperatorList(new LinkedList<EACrossoverOperator>());
        assertTrue(module.calculateCrossover());
        assertEquals(2, module.getLastCrossOperator());
    }

    @Test
    public void testActivateAndDeactivateAlwaysMutateToggle() {
        module.activateAlwaysMutate();
        // No assertion on the platform-dependent branch; just confirm the toggle
        // and its inverse do not throw and leave the module usable.
        module.deactivateAlwaysMutate();
        assertFalse(module.calculateMutation());
    }
}
