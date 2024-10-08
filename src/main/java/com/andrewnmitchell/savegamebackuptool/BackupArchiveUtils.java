package com.andrewnmitchell.savegamebackuptool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static com.andrewnmitchell.savegamebackuptool.BackupUtils.*;
import static java.nio.file.Paths.*;

public class BackupArchiveUtils {
    private ArrayList<String> fileList;
    private String sourceFolder;

    public BackupArchiveUtils(String folder) {
        sourceFolder = folder;
        fileList = new ArrayList<>();
    }

    public void compress(String zipFile, BackupGUI gui) {
        byte[] buffer = new byte[1024];
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(zipFile);
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            print(addToTextArea(
                    "Creating backup archive: " + zipFile.substring(zipFile.lastIndexOf("/") + 1),
                    gui));
            println();
            FileInputStream fileInputStream = null;
            for (String file : fileList) {
                println(addToTextArea("Added " + file, gui));
                ZipEntry zipEntry = new ZipEntry(file);
                zipOutputStream.putNextEntry(zipEntry);
                try {
                    fileInputStream = new FileInputStream(sourceFolder + file);
                    int length;
                    while ((length = fileInputStream.read(buffer)) > 0)
                        zipOutputStream.write(buffer, 0, length);
                } finally {
                    if (fileInputStream != null)
                        fileInputStream.close();
                }
            }
            zipOutputStream.closeEntry();
            println(addToTextArea("Backup successful", gui));
        } catch (IOException e) {
        } finally {
            try {
                zipOutputStream.close();
            } catch (IOException e) {
            }
        }
    }

    public void generateFileList(File node) {
        if (node.isFile())
            fileList.add(generateZipEntry(node.getAbsolutePath().replace("\\", "/")));
        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String fileName : subNote)
                generateFileList(new File(node, fileName));
        }
    }

    private String generateZipEntry(String file) {
        return file.substring(get(sourceFolder).toFile().getAbsolutePath().length() + 1,
                file.length());
    }
}
