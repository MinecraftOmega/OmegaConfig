package me.srrrapero720.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class RandonRenameScript {

    public static void main(String... args) throws Exception {
        File file = new File("C:\\Users\\J-RAP\\Downloads\\copia libro");
        File[] lists = file.listFiles();

        // array filter
        Set<File> bSet = new HashSet<>();
        for (File f: lists) {
            if (!f.getName().contains("-clone")) {
                bSet.add(f);
            }
        }

        File[] numberedFiles = bSet.toArray(new File[0]);

        // sort numbers
        Arrays.sort(numberedFiles, Comparator.comparingInt(o -> Integer.parseInt(o.getName().split("\\.")[0])));

        for (File f: lists) {
            String name = f.getName();
            if (name.contains("-clone")) {
                int index = Integer.parseInt(name.split("-")[0]);
                index++;

                int i = numberedFiles.length - 1;
                File nFile;
                do {
                    nFile = numberedFiles[i--];
                    String nName = nFile.getName();
                    String[] nNameSplit = nName.split("\\.");
                    int nIndex = Integer.parseInt(nNameSplit[0]);
                    nIndex++;

                    System.out.println("Renaming " + nFile.getName() + " as " + nIndex + "." + nNameSplit[1]);
                    Files.move(nFile.toPath(), nFile.toPath().getParent().resolve(nIndex + "." + nNameSplit[1]));
                } while (Integer.parseInt(nFile.getName().split("\\.")[0]) != index);

                System.out.println("Renaming " + f.getName() + " as " + index + ".tif");
                Files.move(f.toPath(), f.toPath().getParent().resolve(index + ".tif"));

                break;
            }
        }
    }
}
