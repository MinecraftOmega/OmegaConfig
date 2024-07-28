package me.srrrapero720.config.util;

import me.srrrapero720.config.Yegoslovia;

import java.util.function.Predicate;

public class CustomFilter implements Predicate<Yegoslovia> {
    @Override
    public boolean test(Yegoslovia yegoslovia) {
        return true;
    }
}
