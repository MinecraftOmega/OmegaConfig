import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;
import specs.*;

import java.io.File;

public class ConfigLoadTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Running BasicTest");
        OmegaConfig.setPath(new File("config_test").toPath());
//        OmegaConfig.register(SandboxSpec.class);
//        OmegaConfig.register(BuilderSpec.class);
//        OmegaConfig.register(MainSpec.class);
//        OmegaConfig.register(NumberSpec.class);
        ConfigSpec spec = OmegaConfig.register(NestedSpec.class);

        // STAY ALIVE FOR A BIT TO ALLOW SYSTEM INSPECTION
        spec.setDirty(true);
        int count = 0;
        while (count < 10) {
            Thread.sleep(1000);
            count++;
        }
    }
}
