package io.github.seggan.jyxal.runtime.list;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class JyxalList extends AbstractList<Object> {

    public static JyxalList create(Iterator<Object> generator) {
        return new InfiniteList(generator);
    }

    /** Create an infinite list */
    public static JyxalList createInf(Supplier<Object> generator) {
        return new InfiniteList(
            new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Object next() {
                    return generator.get();
                }
            }
        );
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

    /** Whether or not there exists an element at the given index */
    public abstract boolean hasInd(int ind);

    public abstract void mapInPlace(Function<Object, Object> f);

    public abstract void filterInPlace(Predicate<Object> p);

    public JyxalList map(Function<Object, Object> f) {
        var it = this.iterator();
        return new InfiniteList(
            new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Object next() {
                    return f.apply(it.next());
                }
            }
        );
    }

    public JyxalList filter(Predicate<Object> pred) {
        var it = this.iterator();
        return new InfiniteList(
            new Iterator<>() {
                /** An element waiting to be pulled */
                Object waiting = null;

                @Override
                public boolean hasNext() {
                    if (waiting != null) {
                        return true;
                    }
                    findNext();
                    return waiting != null;
                }

                @Override
                public Object next() {
                    if (waiting != null) {
                        return waiting;
                    }
                    this.findNext();
                    assert waiting != null;
                    return waiting;
                }

                private void findNext() {
                    while (it.hasNext()) {
                        var next = it.next();
                        if (pred.test(next)) {
                            this.waiting = next;
                            break;
                        }
                    }
                }
            }
        );
    }

    protected static String vyxalListFormat(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("⟨");
        Iterator<Object> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                return sb.append("⟩").toString();
            }
            sb.append(it.next());
            sb.append(" | ");
        }
    }
}
