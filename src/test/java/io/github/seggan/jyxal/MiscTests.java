package io.github.seggan.jyxal;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MiscTests {

    public static final String RESOURCES = "src/test/resources/";

    @Test
    public void testMapStuff() throws IOException {
        TestHelper.run(RESOURCES + "mapstuff.vy");
    }
}
