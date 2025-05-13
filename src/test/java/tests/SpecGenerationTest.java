package tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SpecGenerationTest {

    @BeforeAll
    public static void test$load() {
        OmegaConfig.register(BaseTestConfig.class);
        BuilderTestConfig.init();
    }

    @Test
    public void test$init() throws InterruptedException {
        OmegaConfig.WORKER.join();
    }
}
