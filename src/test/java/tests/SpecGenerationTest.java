package tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omegaconfig.OmegaConfig;

public class SpecGenerationTest {

    @BeforeAll
    public static void test$load() {
        OmegaConfig.register(BaseTestConfig.class);
    }

    @Test
    public void test$init() throws InterruptedException {
        OmegaConfig.WORKER.join();
    }
}
