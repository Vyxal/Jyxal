package io.github.seggan.jyxal.runtime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompressionTest {

    @Test
    public void testCompress() {
        Assertions.assertEquals("Hello, World!", Compression.decompress("\u0188\u1E61, \u019B\u20AC!"));
        Assertions.assertEquals("This is a test String to decompress.", Compression.decompress("\u03BB\u00AB is a \u2228\u1E0A \u00F8\u1E8F to de\u2022\u215B\u27D1\u013F."));
        Assertions.assertEquals("A test string.", Compression.decompress("A test string."));
    }
}
