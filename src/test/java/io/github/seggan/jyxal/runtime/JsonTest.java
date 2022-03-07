package io.github.seggan.jyxal.runtime;

import io.github.seggan.jyxal.runtime.list.JyxalList;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import io.github.seggan.jyxal.runtime.text.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JsonTest {

    @Test
    public void testJson() {
        JyxalList expected = JyxalList.create(JyxalList.create("a", "b", "c"), JyxalList.create(
                BigComplex.valueOf(1),
                BigComplex.valueOf(2),
                BigComplex.valueOf(3)
        ), "a");
        JyxalList actual = (JyxalList) new JsonParser("[[\"a\",\"b\",\"c\"],[1,2,3],\"a\"]").parse();
        Assertions.assertEquals(expected, actual);
    }
}
