package io.github.seggan.jyxal.runtime.list;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

class InfiniteList extends JyxalList {

    private final Supplier<Object> generator;

    private List<Object> backing = new ArrayList<>();

    private Function<Object, Object> mapper = Function.identity();
    private Predicate<Object> filter = o -> true;

    InfiniteList(Supplier<Object> generator) {
        super();
        this.generator = generator;
    }

    @Override
    public Object get(int index) {
        fill(index);
        return backing.get(index);
    }

    @Override
    public int size() {
        return -1;
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
    public void map(Function<Object, Object> f) {
        backing = backing.stream().map(f).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        mapper = mapper.andThen(f);
    }

    @Override
    public void filter(Predicate<Object> p) {
        backing = backing.stream().filter(p).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        filter = filter.and(p);
    }

    @Override
    public String toString() {
        return "An infinite list, including " + backing;
    }

    private void fill(int index) {
        while (backing.size() <= index) {
            Object next = mapper.apply(generator.get());
            if (filter.test(next)) {
                backing.add(next);
            }
        }
    }
}
