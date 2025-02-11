package tests;

import net.omegaloader.config.OmegaConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SpecGenerationTest {

    @BeforeAll
    public static void test$load() {
        OmegaConfig.load();
        OmegaConfig.register(BaseTestConfig.class, true);
    }

    @Test
    public void test$init() {

    }
}
