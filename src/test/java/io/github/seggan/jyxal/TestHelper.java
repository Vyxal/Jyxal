package io.github.seggan.jyxal;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

public class TestHelper {

    private static final Pattern p = Pattern.compile("\\..*$");

    public static void run(String... args) throws IOException {
        String fileName = Path.of(args[0]).getFileName().toString();
        Main.doMain(args, true);
        Path jar = Path.of(args[0]).resolveSibling(p.matcher(fileName).replaceAll(".jar"));
        try (URLClassLoader cl = new URLClassLoader(
            new URL[]{jar.toUri().toURL()},
            TestHelper.class.getClassLoader()
        )) {
            Class<?> clazz = cl.loadClass("jyxal.Main");
            clazz.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        Files.delete(jar);
    }
}
