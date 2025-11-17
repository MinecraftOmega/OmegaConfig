package me.srrrapero720.config;

import org.omegaconfig.ConfigSpec;
import org.omegaconfig.OmegaConfig;
import org.omegaconfig.api.annotations.Spec;
import org.omegaconfig.impl.formats.JSON5Format;
import org.omegaconfig.impl.formats.JSONFormat;

import java.io.File;
import java.io.IOException;

public class WriteTest {


    public static void main(String... args) throws IOException, InterruptedException {
        ConfigSpec spec = OmegaConfig.register(Sandbox.class);
        spec.setDirty(true);
        while (true) {
            Thread.sleep(5000);
        }
    }
}
