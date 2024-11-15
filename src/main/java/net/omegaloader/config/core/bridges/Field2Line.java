package net.omegaloader.config.core.bridges;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Field2Line<T> {
    long fieldOffset; // file buffer offset
    long varOffset; // var offset without field offset
    Field2Line(String reader, Consumer<T> setter, Supplier<T> getter) {

    }

    public void refresh() {

    }
}
