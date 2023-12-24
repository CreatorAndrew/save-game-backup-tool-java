package com.andrewnmitchell.savegamebackuptool;
import javax.swing.JTextArea;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupUtils {
    private ArrayList<String> fileList;
    private String sourceFolder;

    public BackupUtils(String folder) {
        sourceFolder = folder;
        fileList = new ArrayList<String>();
    }

    public void compress(String zipFile, JTextArea textArea) {
        byte[] buffer = new byte[1024];
        FileOutputStream fileOutputStream = null;
        ZipOutputStream zipOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(zipFile);
            zipOutputStream = new ZipOutputStream(fileOutputStream);
            System.out.print(BackupWatchdog.addToTextArea("Creating backup archive: " + zipFile.substring(zipFile.lastIndexOf("/") + 1), textArea));
            System.out.println();
            FileInputStream fileInputStream = null;

            for (String file : this.fileList) {
                System.out.println(BackupWatchdog.addToTextArea("Added " + file, textArea));
                ZipEntry zipEntry = new ZipEntry(file);
                zipOutputStream.putNextEntry(zipEntry);
                try {
                    fileInputStream = new FileInputStream(sourceFolder + file);
                    int length;
                    while ((length = fileInputStream.read(buffer)) > 0) zipOutputStream.write(buffer, 0, length);
                } finally {
                    if (fileInputStream != null) fileInputStream.close();
                }
            }

            zipOutputStream.closeEntry();
            System.out.println(BackupWatchdog.addToTextArea("Backup successful", textArea));
        } catch (IOException exception) {
        } finally {
            try {
                zipOutputStream.close();
            } catch (IOException exception) {
            }
        }
    }

    public void generateFileList(File node) {
        if (node.isFile()) fileList.add(generateZipEntry(node.getAbsolutePath().replaceAll("\\\\", "/")));

        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String fileName : subNote) generateFileList(new File(node, fileName));
        }
    }

    private String generateZipEntry(String file) {
        return file.substring(Paths.get(sourceFolder).toFile().getAbsolutePath().length() + 1, file.length());
    }
}
