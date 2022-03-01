package io.github.seggan.jyxal;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import io.github.seggan.jyxal.Main;

public class TestHelper {

    private static final Pattern p = Pattern.compile("\\..*$");

    public static void run(String... args) throws IOException {
        String fileName = Path.of(args[0]).getFileName().toString();
        Main.doMain(args, true);
        Path jar = Path.of(args[0]).resolveSibling(p.matcher(fileName).replaceAll(".jar"));
        URLClassLoader cl = new URLClassLoader(
                new URL[] { jar.toUri().toURL() },
                TestHelper.class.getClassLoader()
        );
        try {
            Class<?> clazz = cl.loadClass("jyxal.Main");
            clazz.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        cl.close();
        Files.delete(jar);
    }
}
