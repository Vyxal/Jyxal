package io.github.seggan.jyxal.runtime.list;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

class FiniteList extends JyxalList {
    
    private List<Object> backing;

    FiniteList(Collection<?> elements) {
        super();
        backing = new ArrayList<>(elements);
    }

    FiniteList() {
        super();
        backing = new ArrayList<>();
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
    public void mapInPlace(Function<Object, Object> f) {
        List<Object> newBacking = new ArrayList<>();
        for (Object o : backing) {
            newBacking.add(f.apply(o));
        }
        backing = newBacking;
    }

    @Override
    public void filterInPlace(Predicate<Object> p) {
        backing.removeIf(obj -> !p.test(obj));
    }

    @Override
    public String toString() {
        return vyxalListFormat(backing);
    }
}
