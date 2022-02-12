package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.compiler.JyxalCompileException;
import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

public class OtherMethods {

    private OtherMethods() {
    }

    public static boolean truthValue(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.size() != 0;
        } else if (obj instanceof BigComplex bigComplex) {
            return (bigComplex.re.scale() != 0 || bigComplex.im.scale() != 0);
        }

        return true;
    }

    public static Iterator<Object> forify(ProgramStack stack) {
        Object obj = stack.pop();
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.iterator();
        } else if (obj instanceof BigComplex bigComplex) {
            return new Iterator<>() {
                private BigComplex current = BigComplex.ONE;

                @Override
                public boolean hasNext() {
                    return current.re.compareTo(bigComplex.re) <= 0;
                }

                @Override
                public Object next() {
                    BigComplex next = current;
                    current = current.add(BigComplex.ONE);
                    return next;
                }
            };
        } else {
            String s = obj.toString();
            return new Iterator<>() {
                private int i = 0;

                @Override
                public boolean hasNext() {
                    return i < s.length();
                }

                @Override
                public Object next() {
                    return Character.toString(s.charAt(i++));
                }
            };
        }
    }

    public static void dup(ProgramStack stack) {
        Object obj = Objects.requireNonNull(stack.peek());
        if (obj instanceof JyxalList jyxalList) {
            // deep copy
            stack.push(deepCopy(jyxalList));
        } else {
            stack.push(obj);
        }
    }

    private static JyxalList deepCopy(JyxalList list) {
        JyxalList copy = JyxalList.create();
        for (Object obj : list) {
            if (obj instanceof JyxalList jyxalList) {
                copy.add(deepCopy(jyxalList));
            } else {
                copy.add(obj);
            }
        }
        return copy;
    }

    public static boolean vectorise(int arity, Consumer<ProgramStack> consumer, ProgramStack stack) {
        switch (arity) {
            case 1 -> {
                Object obj = stack.pop();
                if (obj instanceof JyxalList jyxalList) {
                    jyxalList.map(o -> {
                        ProgramStack newStack = new ProgramStack(o);
                        consumer.accept(newStack);
                        return newStack.pop();
                    });
                    return true;
                }
                stack.push(obj);
            }
            case 2 -> {
                Object right = stack.pop();
                Object left = stack.pop();
                if (left instanceof JyxalList leftList) {
                    if (right instanceof JyxalList rightList) {
                        JyxalList list = JyxalList.create();
                        int size = slen(leftList, rightList);
                        for (int i = 0; i < size; i++) {
                            ProgramStack newStack = new ProgramStack(leftList.get(i), rightList.get(i));
                            consumer.accept(newStack);
                            list.add(newStack.pop());
                        }
                        stack.push(list);
                    } else {
                        leftList.map(obj -> {
                            ProgramStack newStack = new ProgramStack(obj, right);
                            consumer.accept(newStack);
                            return newStack.pop();
                        });
                        stack.push(leftList);
                    }
                    return true;
                } else if (right instanceof JyxalList rightList) {
                    rightList.map(obj -> {
                        ProgramStack newStack = new ProgramStack(left, obj);
                        consumer.accept(newStack);
                        return newStack.pop();
                    });
                    stack.push(rightList);
                    return true;
                }
                stack.push(left);
                stack.push(right);
            }
            case 3 -> {
                Object right = stack.pop();
                Object middle = stack.pop();
                Object left = stack.pop();
                if (left instanceof JyxalList leftList) {
                    if (middle instanceof JyxalList middleList) {
                        if (right instanceof JyxalList rightList) {
                            JyxalList list = JyxalList.create();
                            int size = slen(leftList, middleList, rightList);
                            for (int i = 0; i < size; i++) {
                                ProgramStack newStack = new ProgramStack(leftList.get(i), middleList.get(i), rightList.get(i));
                                consumer.accept(newStack);
                                list.add(newStack.pop());
                            }
                            stack.push(list);
                            return true;
                        }
                        JyxalList list = JyxalList.create();
                        int size = slen(leftList, middleList);
                        for (int i = 0; i < size; i++) {
                            ProgramStack newStack = new ProgramStack(leftList.get(i), middleList.get(i), right);
                            consumer.accept(newStack);
                            list.add(newStack.pop());
                        }
                        stack.push(list);
                        return true;
                    }
                    if (right instanceof JyxalList rightList) {
                        JyxalList list = JyxalList.create();
                        int size = slen(leftList, rightList);
                        for (int i = 0; i < size; i++) {
                            ProgramStack newStack = new ProgramStack(leftList.get(i), middle, rightList.get(i));
                            consumer.accept(newStack);
                            list.add(newStack.pop());
                        }
                        stack.push(list);
                        return true;
                    }
                    leftList.map(obj -> {
                        ProgramStack newStack = new ProgramStack(obj, middle, right);
                        consumer.accept(newStack);
                        return newStack.pop();
                    });
                    stack.push(leftList);
                    return true;
                }
                if (middle instanceof JyxalList middleList) {
                    if (right instanceof JyxalList rightList) {
                        JyxalList list = JyxalList.create();
                        int size = slen(middleList, rightList);
                        for (int i = 0; i < size; i++) {
                            ProgramStack newStack = new ProgramStack(left, middleList.get(i), rightList.get(i));
                            consumer.accept(newStack);
                            list.add(newStack.pop());
                        }
                        stack.push(list);
                        return true;
                    }
                    middleList.map(obj -> {
                        ProgramStack newStack = new ProgramStack(left, obj, right);
                        consumer.accept(newStack);
                        return newStack.pop();
                    });
                    stack.push(middleList);
                    return true;
                }
                if (right instanceof JyxalList rightList) {
                    rightList.map(obj -> {
                        ProgramStack newStack = new ProgramStack(left, middle, obj);
                        consumer.accept(newStack);
                        return newStack.pop();
                    });
                    stack.push(rightList);
                    return true;
                }
                stack.push(left);
                stack.push(middle);
                stack.push(right);
            }
            default -> throw new IllegalArgumentException("Invalid arity " + arity);
        }

        return false;
    }

    private static int slen(JyxalList first, JyxalList... rest) {
        int size = first.size();
        for (JyxalList list : rest) {
            size = Math.min(size, list.size());
        }
        return size;
    }
}
