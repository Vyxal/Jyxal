package io.github.seggan.jyxal;

import io.github.seggan.jyxal.antlr.VyxalLexer;
import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.compiler.Compiler;
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

    private static final String codepage = "\u03BB\u019B\u00AC\u2227\u27D1\u2228\u27C7\u00F7\u00D7\u00AB\n\u00BB\u00B0\u2022\u00DF\u2020\u20AC\u00BD\u2206\u00F8\u2194\u00A2\u2310\u00E6\u0280\u0281\u027E\u027D\u00DE\u0188\u221E\u00A8 !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]`^_abcdefghijklmnopqrstuvwxyz{|}~\u2191\u2193\u2234\u2235\u203A\u2039\u2237\u00A4\u00F0\u2192\u2190\u03B2\u03C4\u0227\u1E03\u010B\u1E0B\u0117\u1E1F\u0121\u1E23\u1E2D\u0140\u1E41\u1E45\u022F\u1E57\u1E59\u1E61\u1E6B\u1E87\u1E8B\u1E8F\u017C\u221A\u27E8\u27E9\u201B\u2080\u2081\u2082\u2083\u2084\u2085\u2086\u2087\u2088\u00B6\u204B\u00A7\u03B5\u00A1\u2211\u00A6\u2248\u00B5\u0226\u1E02\u010A\u1E0A\u0116\u1E1E\u0120\u1E22\u0130\u013F\u1E40\u1E44\u022E\u1E56\u1E58\u1E60\u1E6A\u1E86\u1E8A\u1E8E\u017B\u208C\u208D\u2070\u00B9\u00B2\u2207\u2308\u230A\u00AF\u00B1\u20B4\u2026\u25A1\u21B3\u21B2\u22CF\u22CE\uA60D\uA71D\u2105\u2264\u2265\u2260\u207C\u0192\u0256\u222A\u2229\u228D\u00A3\u00A5\u21E7\u21E9\u01CD\u01CE\u01CF\u01D0\u01D1\u01D2\u01D3\u01D4\u207D\u2021\u226C\u207A\u21B5\u215B\u00BC\u00BE\u03A0\u201E\u201F";

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
                sb.append(codepage.charAt(b));
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
