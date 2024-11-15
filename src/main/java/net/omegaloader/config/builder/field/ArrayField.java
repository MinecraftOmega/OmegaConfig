package net.omegaloader.config.builder.field;

import net.omegaloader.config.Util;
import net.omegaloader.config.annotations.conditions.ArrayConditions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.function.Predicate;

public class ArrayField<T>  {

    ArrayField(Object ctx, Annotation ann, Field field) {

    }

    ArrayField(Object ctx, Annotation[] ann, T[] defValue) {

    }

    public static class Context<T> extends BaseConfigField.Context<T[]> {
        public Context(Object context, Annotation[] annotations, Field field) {
            super(context, annotations, field);
        }

        @Override
        public void accept(T[] stringValue) {
            Util.setField(field, context, stringValue);
        }

        @Override
        public T[] get() {
            return this.value;
        }
    }

    public static class Built<T> extends BaseConfigField.Built<T[]> {
        public Built(Object context, Annotation[] annotations, T[] defaultValue) {
            super(context, annotations, defaultValue);
        }

        @Override
        public void accept(T[] aByte) {
            this.value = aByte;
        }

        @Override
        public T[] get() {
            return value;
        }
    }

    public static class Options implements ArrayConditions {
        private boolean stringify = false;
        private boolean singleline = false;
        private int limit = Integer.MAX_VALUE;
        private Class<? extends Predicate> filter = Predicate.class;
        private Sorting sorting = Sorting.NONE;

        public Options() {

        }

        @Override
        public boolean stringify() {
            return stringify;
        }

        @Override
        public boolean singleline() {
            return singleline;
        }

        @Override
        public int limit() {
            return limit;
        }

        @Override
        public Class<? extends Predicate> filter() {
            return filter;
        }

        @Override
        public Sorting sorting() {
            return sorting;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return ArrayConditions.class;
        }
    }
}
