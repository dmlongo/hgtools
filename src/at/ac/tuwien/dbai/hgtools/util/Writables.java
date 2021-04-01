package at.ac.tuwien.dbai.hgtools.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Writables {

    private Writables() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Removes angular brackets from names
     * 
     * @param s A string s to be stringified
     * @return A stringified String
     */
    public static String stringify(String s) {
        String newS = s;
        newS = newS.replace('[', 'L');
        newS = newS.replace(']', 'J');
        return newS;
    }

    public static String stringify(int[] arr, char delimiter, int size, char lPar, char rPar) {
        StringBuilder sb = new StringBuilder(size);
        sb.append(lPar);
        for (int i = 0; i < arr.length; i++) {
            sb.append(Integer.toString(arr[i]));
            if (i < arr.length - 1) {
                sb.append(delimiter);
            }
        }
        sb.append(rPar);
        return sb.toString();
    }

    public static String stringify(int[] arr, char delimiter) {
        StringBuilder sb = new StringBuilder(arr.length * 5);
        for (int i = 0; i < arr.length; i++) {
            sb.append(Integer.toString(arr[i]));
            if (i < arr.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String stringify(Collection<String> col, char delimiter, int size) {
        StringBuilder sb = new StringBuilder(size);
        Iterator<String> it = col.iterator();
        while (it.hasNext()) {
            sb.append(stringify(it.next()));
            if (it.hasNext()) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String stringify(Collection<String> col, char delimiter) {
        return stringify(col, delimiter, 200);
    }

    public static void writeToFile(Writable w, String filename) throws IOException {
        Path filePath = Paths.get(filename);
        Files.createDirectories(filePath.getParent());
        if (!Files.exists(filePath))
            Files.createFile(filePath);
        Files.write(filePath, w.toFile(), StandardCharsets.UTF_8);
    }

    public static void writeToFile(Map<String, List<String>> mapping, String filename) throws IOException {
        writeToFile(() -> {
            ArrayList<String> lines = new ArrayList<>(mapping.size());
            for (Map.Entry<String, List<String>> entry : mapping.entrySet()) {
                StringBuilder sb = new StringBuilder(100);
                sb.append(entry.getKey());
                sb.append('=');
                Iterator<String> it = entry.getValue().iterator();
                while (it.hasNext()) {
                    sb.append(it.next());
                    if (it.hasNext()) {
                        sb.append(',');
                    }
                }
                lines.add(sb.toString());
            }
            return lines;
        }, filename);
    }

}
