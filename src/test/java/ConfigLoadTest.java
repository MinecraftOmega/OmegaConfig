import org.omegaconfig.OmegaConfig;
import specs.BuilderSpec;
import specs.MainSpec;
import specs.NumberSpec;
import specs.SandboxSpec;

public class ConfigLoadTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Running BasicTest");
        OmegaConfig.register(SandboxSpec.class);
        OmegaConfig.register(BuilderSpec.class);
        OmegaConfig.register(MainSpec.class);
        OmegaConfig.register(NumberSpec.class);

        // STAY ALIVE FOR A BIT TO ALLOW SYSTEM INSPECTION
        int count = 0;
        while (count < 10) {
            Thread.sleep(1000);
            count++;
        }
    }
}
