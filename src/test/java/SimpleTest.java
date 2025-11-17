import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.omegaconfig.OmegaConfig;
import specs.MainSpec;
import specs.NumberSpec;
import specs.BuilderSpec;
import specs.SandboxSpec;

import static org.junit.jupiter.api.Assertions.*;

public class SimpleTest {

    @BeforeAll
    public static void test$load() throws InterruptedException {
        OmegaConfig.register(SandboxSpec.class);
        BuilderSpec.init();
        OmegaConfig.register(MainSpec.class);
        OmegaConfig.register(NumberSpec.class);
        Thread.sleep(5000);
    }

    @Test
    public void test$checkRegistered() throws Exception {
        assertTrue(OmegaConfig.isRegistered("sandbox"), "SandboxSpec not registered");
        assertTrue(OmegaConfig.isRegistered("buildercfg"), "BuilderSpec not registered");
        assertTrue(OmegaConfig.isRegistered("main"), "MainSpec not registered");
        assertTrue(OmegaConfig.isRegistered("number"), "NumberSpec not registered");
    }
}
