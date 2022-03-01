package io.github.seggan.jyxal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import io.github.seggan.jyxal.Main;

public class TestHelper {

    private static final Pattern p = Pattern.compile("\\..*$");

    public static void run(String... args) throws IOException {
        String fileName = Path.of(args[0]).getFileName().toString();
        Main.doMain(args, true);
        Files.delete(Path.of(args[0]).resolveSibling(p.matcher(fileName).replaceAll(".jar")));
    }
}
