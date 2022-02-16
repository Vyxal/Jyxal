package io.github.seggan.jyxal.compiler;

import io.github.seggan.jyxal.runtime.ProgramStack;
import io.github.seggan.jyxal.runtime.RuntimeMethods;
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
            if (element.isLinkedToMethod) {
                String methodName = Element.screamingSnakeToCamel(element.name());
                Assertions.assertDoesNotThrow(() -> {
                    //noinspection ResultOfMethodCallIgnored
                    RuntimeMethods.class.getMethod(
                            methodName,
                            ProgramStack.class
                    );
                }, "Method not found: RuntimeMethods." + methodName);
            }
        }
    }
}
