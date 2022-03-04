package io.github.seggan.jyxal.runtime.list;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

class InfiniteList extends JyxalList {

    private final Iterator<Object> generator;

    private List<Object> backing = new ArrayList<>();

    private Function<Object, Object> mapper = Function.identity();
    private Predicate<Object> filter = o -> true;

    InfiniteList(Iterator<Object> generator) {
        super();
        this.generator = generator;
    }

    @Override
    public Object get(int index) {
        fill(index);
        if (backing.size() > index) {
            return backing.get(index);
        } else {
            return BigComplex.ZERO;
        }
    }

    @Override
    public int size() {
        if (!generator.hasNext()) {
            return backing.size();
        } else {
            return -1;
        }
    }

    @Override
    public boolean hasInd(int ind) {
        fill(ind);
        return backing.size() > ind;
    }

    @Override
    public void add(int index, Object element) {
        fill(index);
        backing.add(index, element);
    }

    @Override
    public boolean add(Object o) {
        throw new UnsupportedOperationException("Cannot add to the end of an infinite list");
    }

    @Override
    public boolean isLazy() {
        return true;
    }

    @Override
    public Iterator<Object> iterator() {
        return new Iterator<>() {
            private int ind = 0;

            @Override
            public boolean hasNext() {
                return InfiniteList.this.hasInd(ind);
            }

            @Override
            public Object next() {
                Object elem = InfiniteList.this.get(ind);
                ind++;
                return elem;
            }
        };
    }

    @Override
    public void mapInPlace(Function<Object, Object> f) {
        List<Object> newBacking = new ArrayList<>();
        for (Object o : backing) {
            newBacking.add(f.apply(o));
        }
        backing = newBacking;
        mapper = mapper.andThen(f);
    }

    @Override
    public void filterInPlace(Predicate<Object> p) {
        backing.removeIf(obj -> !p.test(obj));
        filter = filter.and(p);
    }

    @Override
    public String toString() {
        return "An infinite list, including " + vyxalListFormat(backing);
    }

    private void fill(int index) {
        while (backing.size() <= index && generator.hasNext()) {
            Object next = mapper.apply(generator.next());
            if (filter.test(next)) {
                backing.add(next);
            }
        }
    }
}
