package io.github.seggan.jyxal;

import io.github.seggan.jyxal.antlr.VyxalLexer;
import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.compiler.Compiler;
import io.github.seggan.jyxal.runtime.Compression;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class Main {

    private static final String runtimeClasses = "/build/runtime-classes";

    public static void doMain(String[] args, boolean isTest) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java -jar jyxal.jar <file>");
            return;
        }

        CompilerOptions.fromString(args.length > 1 ? args[1] : "");

        System.out.println("Parsing program...");

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        String s;
        if (CompilerOptions.OPTIONS.contains(CompilerOptions.VYXAL_CODEPAGE)) {
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(Compression.CODEPAGE.charAt(b));
            }
            s = sb.toString();
        } else {
            s = new String(bytes, StandardCharsets.UTF_8);
        }

        VyxalLexer lexer = new VyxalLexer(CharStreams.fromString(s));
        VyxalParser parser = new VyxalParser(new CommonTokenStream(lexer));

        if (CompilerOptions.OPTIONS.contains(CompilerOptions.PRINT_DEBUG_TREE)) {
            System.out.println(parser.file().toStringTree(parser));
            parser.reset();
        }

        System.out.println("Compiling program...");
        byte[] main = Compiler.compile(parser, args[0]);

        ClassReader cr = new ClassReader(main);
        try (OutputStream os = new FileOutputStream("debug.log")) {
            TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(os));
            cr.accept(tcv, 0);
        }

        System.out.println("Extracting runtime classes...");
        Set<String> resourceList = new HashSet<>();
        Path buildDir = Path.of(System.getProperty("user.dir"), runtimeClasses);
        try (Scanner scanner = new Scanner(Objects.requireNonNull(
                isTest ?
                        Files.newInputStream(buildDir.resolve("runtime.list"))
                        :
                        Main.class.getResourceAsStream("/runtime.list")))) {
            while (scanner.hasNextLine()) {
                resourceList.add(scanner.nextLine());
            }
        }

        System.out.println("Writing to jar...");
        String fileName = args[0].substring(0, args[0].lastIndexOf('.'));
        try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(fileName + ".jar"))) {
            for (String resource : resourceList) {
                JarEntry entry = new JarEntry("runtime/" + resource);
                entry.setTime(System.currentTimeMillis());
                jar.putNextEntry(entry);
                try (InputStream in = isTest ?
                        Files.newInputStream(buildDir.resolve(resource))
                        :
                        Main.class.getResourceAsStream("/" + resource)) {
                    if (in == null) throw new NullPointerException("Resource not found: " + resource);
                    in.transferTo(jar);
                }
            }

            Manifest manifest = new Manifest();
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
            manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "jyxal.Main");
            jar.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            manifest.write(jar);

            JarEntry entry = new JarEntry("jyxal/Main.class");
            entry.setTime(System.currentTimeMillis());
            jar.putNextEntry(entry);
            jar.write(main);
        }

        System.out.println("Done!");
    }

    public static void main(String[] args) throws IOException {
        doMain(args, false);
    }

}
