package io.github.seggan.jyxal

import io.github.seggan.jyxal.antlr.VyxalLexer
import io.github.seggan.jyxal.antlr.VyxalParser
import io.github.seggan.jyxal.compiler.Compiler
import io.github.seggan.jyxal.runtime.text.Compression
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import java.io.FileOutputStream
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Scanner
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

object Main {
    private const val runtimeClasses = "/build/runtime-classes"

    @JvmStatic
    fun doMain(args: Array<String>, isTest: Boolean) {
        if (args.isEmpty()) {
            println("Usage: java -jar jyxal.jar <file>")
            return
        }
        CompilerOptions.fromString(if (args.size > 1) args[1] else "")

        println("Parsing program...")
        val bytes: ByteArray = Files.readAllBytes(Path.of(args[0]))
        val s: String = if (CompilerOptions.OPTIONS.contains(CompilerOptions.VYXAL_CODEPAGE)) {
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(Compression.CODEPAGE[b.toInt()])
            }
            sb.toString()
        } else {
            String(bytes, StandardCharsets.UTF_8)
        }
        val lexer = VyxalLexer(CharStreams.fromString(s))
        val parser = VyxalParser(CommonTokenStream(lexer))
        if (CompilerOptions.OPTIONS.contains(CompilerOptions.PRINT_DEBUG_TREE)) {
            println(parser.file().toStringTree(parser))
            parser.reset()
        }
        println("Compiling program...")
        val main = Compiler.compile(parser, args[0])
        val cr = ClassReader(main)
        FileOutputStream("debug.log").use { os ->
            val tcv = TraceClassVisitor(PrintWriter(os))
            cr.accept(tcv, 0)
        }
        println("Extracting runtime classes...")
        val resourceList: MutableSet<String> = HashSet()
        val buildDir: Path = Path.of(System.getProperty("user.dir"), runtimeClasses)
        Scanner(if (isTest) Files.newInputStream(buildDir.resolve("runtime.list")) else Main::class.java.getResourceAsStream("/runtime.list")!!).use { scanner ->
            while (scanner.hasNextLine()) {
                resourceList.add(scanner.nextLine())
            }
        }
        println("Writing to jar...")
        val fileName = args[0].substring(0, args[0].lastIndexOf('.'))
        JarOutputStream(FileOutputStream("$fileName.jar")).use { jar ->
            for (resource in resourceList) {
                val entry = JarEntry("runtime/$resource")
                entry.time = System.currentTimeMillis()
                jar.putNextEntry(entry)
                if (isTest) Files.newInputStream(buildDir.resolve(resource)) else Main::class.java.getResourceAsStream("/$resource").use { `in` ->
                    if (`in` == null) throw NullPointerException("Resource not found: $resource")
                    `in`.transferTo(jar)
                }
            }
            val manifest = Manifest()
            manifest.mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
            manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = "jyxal.Main"
            jar.putNextEntry(JarEntry("META-INF/MANIFEST.MF"))
            manifest.write(jar)
            val entry = JarEntry("jyxal/Main.class")
            entry.time = System.currentTimeMillis()
            jar.putNextEntry(entry)
            jar.write(main)
        }
        println("Done!")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        doMain(args, false)
    }
}