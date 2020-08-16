package net.bqc.lyfix.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class ClassPathUtils {

    public final static String CP_DELIMITER;
    static {
        String os = System.getProperty("os.name").toLowerCase();
        CP_DELIMITER = os.contains("win") ? ";" : ":";
    }

    public static URL[] parseClassPaths(String rawClassPath) {
        String[] paths = rawClassPath.split(CP_DELIMITER);
        List<URL> classPaths = new ArrayList<>();
        for (String path : paths) {
            try {
                URL url = new File(path).toURI().toURL();
                classPaths.add(url);
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return classPaths.toArray(new URL[0]);
    }

    public static void addClassPaths(String rawClassPath) {
        URL[] classPaths = parseClassPaths(rawClassPath);
        Method method = null;
        try {
            method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            for (URL cp : classPaths) {
                method.invoke(ClassLoader.getSystemClassLoader(), cp);
            }
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }
}
