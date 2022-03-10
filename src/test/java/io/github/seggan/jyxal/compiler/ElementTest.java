package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.runtime.RuntimeMethods;
import io.github.seggan.jyxal.runtime.math.BigComplex;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ElementTest {

    @Test
    public void testCaseConversion() {
        Assertions.assertEquals("test", Element.screamingSnakeToCamel("TEST"));
        Assertions.assertEquals("testTest", Element.screamingSnakeToCamel("TEST_TEST"));
        Assertions.assertEquals("theLastTest", Element.screamingSnakeToCamel("THE_LAST_TEST"));
    }

    @Test
    public void assertMethodsFound() {
        for (Element element : Element.values()) {
            Element.LinkedMethodType type = element.type;
            if (type != null) {
                String methodName = Element.screamingSnakeToCamel(element.name());
                Assertions.assertDoesNotThrow(() -> {
                    //noinspection ResultOfMethodCallIgnored
                    RuntimeMethods.class.getMethod(
                            Element.screamingSnakeToCamel(element.name()),
                            type.argType
                    );
                }, "Method not found: " + methodName);
            }
        }

        RuntimeMethods.isPrime(BigComplex.valueOf(2809));
    }
}
