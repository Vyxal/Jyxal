package io.github.seggan.jyxal.compiler

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ElementTest {

    @Test
    fun testCaseConversion() {
        Assertions.assertEquals("test", screamingSnakeToCamel("TEST"))
        Assertions.assertEquals("testTest", screamingSnakeToCamel("TEST_TEST"))
        Assertions.assertEquals("theLastTest", screamingSnakeToCamel("THE_LAST_TEST"))
    }

    @Test
    fun assertMethodsFound() {
        for (element in Element.values()) {
            val type = element.type
            if (type != null) {
                val methodName = screamingSnakeToCamel(element.name)
                Assertions.assertDoesNotThrow<Any>({
                    Class.forName("io.github.seggan.jyxal.runtime.RuntimeMethods").getMethod(
                        screamingSnakeToCamel(element.name),
                        type.argType.java
                    )
                }, "Method not found: $methodName")
            }
        }
    }
}