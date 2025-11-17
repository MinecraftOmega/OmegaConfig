package specs.util;

import java.util.function.Predicate;

public class EnumPredicateFilter implements Predicate<EnumTest> {
    @Override
    public boolean test(EnumTest enumTest) {
        return true;
    }
}
