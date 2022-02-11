package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;

public class OtherMethods {

    private OtherMethods() {
    }

    public static boolean truthValue(Object obj) {
        if (obj instanceof JyxalList jyxalList) {
            return jyxalList.size() != 0;
        } else if (obj instanceof BigComplex bigComplex) {
            return (bigComplex.re.scale() != 0 || bigComplex.im.scale() != 0);
        }

        return true;
    }
}
