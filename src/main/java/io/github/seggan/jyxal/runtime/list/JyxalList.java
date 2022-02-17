package io.github.seggan.jyxal.runtime.list;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class JyxalList extends AbstractList<Object> implements List<Object> {

    public static JyxalList create(Supplier<Object> generator) {
        return new InfiniteList(generator);
    }

    public static JyxalList create(Object[] array) {
        return new FiniteList(List.of(array));
    }

    public static JyxalList create(Collection<?> collection) {
        return new FiniteList(collection);
    }

    public static JyxalList create() {
        return new FiniteList();
    }

    public static JyxalList range(int start, int end) {
        List<Object> list = new ArrayList<>(Math.abs(end - start));
        for (int i = start; i <= end; i++) {
            list.add(BigComplex.valueOf(new BigDecimal(Integer.toString(i))));
        }
        return new FiniteList(list);
    }

    public abstract void map(Function<Object, Object> f);

    public abstract void filter(Predicate<Object> p);

    protected static String vyxalListFormat(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        sb.append('⟨');
        Iterator<Object> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                return sb.append('⟩').toString();
            }
            sb.append(it.next());
            sb.append(" | ");
        }
    }
}
