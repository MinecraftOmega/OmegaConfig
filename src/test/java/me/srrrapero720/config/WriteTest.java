package me.srrrapero720.config;

import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;

import java.io.IOException;

public class WriteTest {


    public static void main(String... args) throws IOException, InterruptedException {
        ConfigSpec spec = OmegaConfig.register(Sandbox.class);
        while (true) {
            Thread.sleep(5000);
        }
    }
}
