package io.github.seggan.jyxal.runtime.list;

import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class JyxalList extends AbstractList<Object> implements List<Object> {

    protected JyxalList delegate;

    private JyxalList(JyxalList delegate) {
        this.delegate = delegate;
    }

    protected JyxalList() {
        this.delegate = this;
    }

    public static JyxalList create(Supplier<Object> generator) {
        return new JyxalList(new InfiniteList(generator));
    }

    public static JyxalList create(Object[] array) {
        return new JyxalList(new FiniteList(array));
    }

    public static JyxalList range(int start, int end) {
        List<Object> list = new ArrayList<>(Math.abs(end - start));
        for (int i = start; i < end; i++) {
            list.add(BigComplex.valueOf(i));
        }
        return new JyxalList(new FiniteList(list));
    }

    @Override
    public Object get(int index) {
        return delegate.get(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public void add(int index, Object element) {
        delegate.add(index, element);
    }

    public void map(Function<Object, Object> f) {
        delegate.map(f);
    }

    public void filter(Predicate<Object> p) {
        delegate.filter(p);
    }
}
