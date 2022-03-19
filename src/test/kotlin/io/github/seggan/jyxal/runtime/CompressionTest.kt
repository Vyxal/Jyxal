package io.github.seggan.jyxal.runtime

import io.github.seggan.jyxal.runtime.text.Compression.decompress
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CompressionTest {
    @Test
    fun testCompress() {
        Assertions.assertEquals("Hello, World!", decompress("ƈṡ, ƛ€!"))
        Assertions.assertEquals(
            "This is a test String to decompress.",
            decompress("λ« is a ∨Ḋ øẏ to de•⅛⟑Ŀ.")
        )
        Assertions.assertEquals("A test string.", decompress("A test string."))
    }
}