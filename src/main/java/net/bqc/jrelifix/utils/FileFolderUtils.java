package net.bqc.jrelifix.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class FileFolderUtils {

    public static ArrayList<File> walk(String path, String to_find, ArrayList<File> found) {
        File root = new File(path);
        File[] list = root.listFiles();
        if (list == null) {
            return null;
        } else {
            File[] arr$ = list;
            int len$ = list.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                File f = arr$[i$];
                if (f.isDirectory()) {
                    walk(f.getAbsolutePath(), to_find, found);
                } else {
                    String fname = f.getAbsoluteFile().toString();
                    if (fname.contains(to_find)) {
                        found.add(f);
                    }
                }
            }

            return found;
        }
    }

    public static String readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return new String(Files.readAllBytes(path));
    }

    public static void writeFile(String fileName, String content) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
        writer.write(content);
        writer.close();
    }

    public static String relativePath(String base, String absolutePath) {
        Path pathAbsolute = Paths.get(absolutePath);
        Path pathBase = Paths.get(base);
        Path pathRelative = pathBase.relativize(pathAbsolute);
        return pathRelative.toString();
    }
}
