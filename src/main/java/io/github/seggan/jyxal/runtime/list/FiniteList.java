package io.github.seggan.jyxal.runtime.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

class FiniteList extends JyxalList {
    
    private List<Object> backing;
    
    FiniteList(Object[] elements) {
        super();
        backing = new ArrayList<>(List.of(elements));
    }

    FiniteList(Collection<?> elements) {
        super();
        backing = new ArrayList<>(elements);
    }

    @Override
    public Object get(int index) {
        return backing.get(index);
    }

    @Override
    public int size() {
        return backing.size();
    }

    @Override
    public void add(int index, Object element) {
        backing.add(index, element);
    }

    @Override
    public Iterator<Object> iterator() {
        return backing.iterator();
    }

    @Override
    public void map(Function<Object, Object> f) {
        backing = backing.stream().map(f).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public void filter(Predicate<Object> p) {
        backing = backing.stream().filter(p).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public String toString() {
        return backing.toString();
    }
}
