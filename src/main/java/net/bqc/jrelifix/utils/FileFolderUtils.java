package net.bqc.jrelifix.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

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

    public static List<File> search4FilesContainName(File dir, String containName) {
        List<File> ans = new LinkedList();
        Queue<File> queue = new LinkedList();
        queue.add(dir);

        while(true) {
            while(!queue.isEmpty()) {
                File e = queue.poll();
                if (e.isFile()) {
                    if (e.getName().contains(containName) && !e.getName().endsWith("~")) {
                        ans.add(e);
                    }
                }
                else {
                    File[] lst = e.listFiles();
                    File[] arr = lst;
                    int len = lst.length;

                    for(int i = 0; i< len; ++i) {
                        File f = arr[i];
                        queue.add(f);
                    }
                }
            }
            return ans;
        }
    }

    public static String relativePath(String base, String absolutePath) {
        Path pathAbsolute = Paths.get(absolutePath);
        Path pathBase = Paths.get(base);
        Path pathRelative = pathBase.relativize(pathAbsolute);
        return pathRelative.toString();
    }

    public static void writeFile(String fileName, String content) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(writer);
        bw.write(content);
        bw.close();
    }

    public static String readFile(String filePath) throws IOException {
        Path path = Paths.get(new File(filePath).toURI());
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
