package io.github.seggan.jyxal.runtime.list;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.math.BigInteger;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class JyxalList extends AbstractList<Object> {

    public static JyxalList create(Iterator<Object> generator) {
        return new InfiniteList(generator);
    }

    public static JyxalList create(Object... array) {
        return new FiniteList(List.of(array));
    }

    public static JyxalList create(Collection<?> collection) {
        return new FiniteList(collection);
    }

    public static JyxalList create() {
        return new FiniteList();
    }

    /**
     * Create an infinite list
     */
    public static JyxalList createInf(Supplier<Object> generator) {
        return new InfiniteList(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Object next() {
                return generator.get();
            }
        });
    }

    public static JyxalList range(BigComplex start, BigComplex end) {
        return new InfiniteList(new Iterator<>() {
            BigComplex cur = start;

            @Override
            public boolean hasNext() {
                return cur.compareTo(end) < 0;
            }

            @Override
            public Object next() {
                Object ret = cur;
                cur = cur.add(BigComplex.ONE);
                return ret;
            }
        });
    }

    protected static String vyxalListFormat(List<Object> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("\u27E8");
        Iterator<Object> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                sb.delete(sb.length() - 3, sb.length());
                return sb.append("\u27E9").toString();
            }
            sb.append(it.next());
            sb.append(" | ");
        }
    }

    /**
     * Whether there exists an element at the given index
     */
    public abstract boolean hasInd(int ind);

    public abstract void mapInPlace(Function<Object, Object> f);

    public abstract void filterInPlace(Predicate<Object> p);

    public JyxalList removeAtIndex(BigInteger ind) {
        Iterator<Object> it = this.iterator();
        return new InfiniteList(new Iterator<>() {
            BigInteger i = BigInteger.ZERO;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                if (i.equals(ind)) {
                    it.next();
                    i = i.add(BigInteger.ONE);
                }
                i = i.add(BigInteger.ONE);
                return it.next();
            }
        });
    }

    public JyxalList map(Function<Object, Object> f) {
        Iterator<Object> it = this.iterator();
        return new InfiniteList(new Iterator<>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return f.apply(it.next());
            }
        });
    }

    public JyxalList filter(Predicate<Object> pred) {
        Iterator<Object> it = this.iterator();
        return new InfiniteList(new Iterator<>() {
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
                    Object next = it.next();
                    if (pred.test(next)) {
                        this.waiting = next;
                        break;
                    }
                }
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JyxalList other)) return false;

        Iterator<Object> it1 = this.iterator();
        Iterator<Object> it2 = other.iterator();
        while (it1.hasNext() && it2.hasNext()) {
            if (!Objects.equals(it1.next(), it2.next())) {
                return false;
            }
        }

        return !it1.hasNext() && !it2.hasNext();
    }
}
