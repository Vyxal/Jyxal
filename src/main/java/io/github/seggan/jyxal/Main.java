package io.github.seggan.jyxal;

import io.github.seggan.jyxal.antlr.VyxalLexer;
import io.github.seggan.jyxal.antlr.VyxalParser;
import io.github.seggan.jyxal.compiler.Compiler;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("Usage: java -jar jyxal.jar <file>");
        }

        System.out.println("Parsing program...");

        VyxalLexer lexer = new VyxalLexer(CharStreams.fromPath(Path.of(args[0]), StandardCharsets.UTF_8));
        VyxalParser parser = new VyxalParser(new CommonTokenStream(lexer));

        // uncomment to print out the tree
        //System.out.println(parser.file().toStringTree(parser));parser.reset();

        System.out.println("Compiling program...");
        byte[] main = Compiler.compile(parser, args[0]);

        System.out.println("Extracting runtime classes...");
        Set<String> resourceList = new HashSet<>();
        try (Scanner scanner = new Scanner(Objects.requireNonNull(
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
                try (InputStream in = Main.class.getResourceAsStream("/" + resource)) {
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

}
