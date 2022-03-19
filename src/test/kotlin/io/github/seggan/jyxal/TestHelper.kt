package io.github.seggan.jyxal

import io.github.seggan.jyxal.Main.doMain
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern

object TestHelper {
    private val p = Pattern.compile("\\..*$")

    @Throws(IOException::class)
    fun run(vararg args: String) {
        val fileName = Path.of(args[0]).fileName.toString()
        doMain(arrayOf(*args), true)
        val jar = Path.of(args[0]).resolveSibling(p.matcher(fileName).replaceAll(".jar"))
        val cl = URLClassLoader(
            arrayOf(jar.toUri().toURL()),
            TestHelper::class.java.classLoader
        )
        try {
            val clazz = cl.loadClass("jyxal.Main")
            clazz.getMethod("main", Array<String>::class.java).invoke(null, arrayOfNulls<String>(0) as Any)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
        cl.close()
        Files.delete(jar)
    }
}