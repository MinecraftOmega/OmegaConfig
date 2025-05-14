package me.srrrapero720.config;

import org.omegaconfig.api.formats.IFormatReader;
import org.omegaconfig.impl.formats.JSONFormat;

import java.io.File;
import java.io.IOException;

public class WriteTest {


    public static void main(String... args) throws IOException {
        JSONFormat.FormatReader reader = (JSONFormat.FormatReader) new JSONFormat().createReader(new File("test-json.json").toPath());

        reader.values.forEach((k, v) -> {
            System.out.println(k + "=" + v);
        });
    }

}
