package com.andrewnmitchell.savegamebackuptool;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.swing.JTextArea;
import java.io.File;
import java.io.FileReader;
import com.google.gson.stream.JsonReader;

class BackupSavePath {
    private String path;
    private boolean isAbsolute;

    public BackupSavePath(String path, boolean isAbsolute) {
        setPath(path);
        setPathIsAbsolute(isAbsolute);
    }

    public BackupSavePath() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Boolean getPathIsAbsolute(){
        return isAbsolute;
    }

    public void setPathIsAbsolute(boolean isAbsolute) {
        this.isAbsolute = isAbsolute;
    }
}

public class BackupWatchdog {
    private static Long getModifiedDate(Path savePath) throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMddHHmmss");
        date.setTimeZone(TimeZone.getDefault());
        return Long.parseLong(date.format(new Date(Files.getLastModifiedTime(savePath).toMillis())));
    }

    public static final String prompt = "> ";

    // This method makes it so that this program treats the filesystem as relative to its own path.
    public static String replaceLocalDotDirectory(String path) {
        String newPath = path, replacement = "";
        try {
             replacement = (BackupWatchdog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
             replacement = replacement.substring(0, replacement.lastIndexOf("/") + 1);
        } catch (URISyntaxException exception) {
        }
        if (path.startsWith(".") && (path.charAt(1) == '/' || path.length() < 3)) newPath = path.replaceFirst("./", replacement);
        if (System.getProperty("os.name").contains("Windows") && newPath.startsWith("/")) newPath = newPath.substring(1);
        return !newPath.equals(path) ? newPath : path;
    }

    public static String addTextToArea(String text, JTextArea textArea) {
        if (textArea != null) {
            textArea.append((textArea.getText().isEmpty() ? "" : "\n") + text);
            textArea.getCaret().setDot(Integer.MAX_VALUE);
        }
        return text;
    }

    public static boolean watchdog(String configFile, int configIndex, boolean usePrompt) throws IOException {
        return watchdog(configFile, null, configIndex, usePrompt);
    }

    public static boolean watchdog(String configFile, JTextArea textArea, int configIndex, boolean usePrompt, boolean enabled) throws IOException {
        if (enabled) return watchdog(configFile, textArea, configIndex, usePrompt);
        return false;
    }

    public static boolean watchdog(String configFile, JTextArea textArea, int configIndex, boolean usePrompt) throws IOException {
        String home = System.getProperty("user.home").replaceAll("\\\\", "/"), backupFolder = "", backupFileNamePrefix = "";

        Long lastBackupTime = Long.parseLong("0");

        ArrayList<BackupSavePath> savePaths = new ArrayList<BackupSavePath>();

        configFile = replaceLocalDotDirectory("./" + configFile);

        JsonReader reader = new JsonReader(new FileReader(configFile));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "searchableSavePaths": {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        BackupSavePath props = new BackupSavePath();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            name = reader.nextName();
                            switch (name) {
                                case "path": props.setPath(reader.nextString()); break;
                                case "isAbsolute": props.setPathIsAbsolute(reader.nextBoolean()); break;
                            }
                        }
                        reader.endObject();
                        savePaths.add(props);
                    }
                    reader.endArray();
                    break;
                }
                case "backupPath": {
                    BackupSavePath props = new BackupSavePath();
                    reader.beginObject();
                    while (reader.hasNext()) {
                        name = reader.nextName();
                        switch (name) {
                            case "path": props.setPath(reader.nextString()); break;
                            case "isAbsolute": props.setPathIsAbsolute(reader.nextBoolean()); break;
                        }
                    }
                    backupFolder = (props.getPathIsAbsolute() ? "" : (home + "/")) + props.getPath().replaceAll("\\\\", "/");
                    backupFolder = replaceLocalDotDirectory(backupFolder);
                    reader.endObject();
                    break;
                }
                case "backupFileNamePrefix": backupFileNamePrefix = reader.nextString(); break;
                case "lastBackupTime": lastBackupTime = reader.nextLong(); break;
            }
        }
        reader.endObject();

        Path savePath = null;
        for (int i = 0; i < savePaths.size(); i++) {
            String savePathString = (savePaths.get(i).getPathIsAbsolute() ? "" : (home + "/")) + savePaths.get(i).getPath();
            savePathString = replaceLocalDotDirectory(savePathString);
            if (Files.exists(Path.of(savePathString))) {
                savePath = Path.of(savePathString);
                break;
            }
        }
        if (savePath == null) {
            if (textArea == null && usePrompt) System.out.println();
            System.out.println(addTextToArea("No save file found.", textArea));
            if (textArea == null && usePrompt) System.out.print(prompt);
            return true;
        }
        String saveFolder = savePath.toString().substring(0, savePath.toString().replaceAll("\\\\", "/").lastIndexOf("/") + 1);

        BackupUtils backupArchive = new BackupUtils(saveFolder);
        backupArchive.generateFileList(new File(saveFolder));

        if (Files.notExists(Path.of(backupFolder))) Files.createDirectories(Path.of(backupFolder));

        try {
            if (getModifiedDate(savePath) > lastBackupTime) {
                lastBackupTime = getModifiedDate(savePath);

                if (textArea == null && usePrompt) System.out.println();
                String backup = backupFileNamePrefix + "+" + lastBackupTime + ".zip";
                if (Files.notExists(Path.of(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup))) {
                    // Create the backup archive file
                    backupArchive.compress(replaceLocalDotDirectory("./") + backup, textArea);
                    if (!backupFolder.equals(replaceLocalDotDirectory("./")))
                        Files.move(Path.of(replaceLocalDotDirectory("./") + backup),
                                   Path.of(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup));
                } else System.out.println(addTextToArea(backup + " already exists in " + backupFolder + ".\nBackup cancelled", textArea));

                // Rewrite the JSON file
                String configOutput = "{\n    \"searchableSavePaths\": [";
                for (int i = 0; i < savePaths.size(); i++)
                    configOutput += "\n        {\"path\": \"" + savePaths.get(i).getPath()
                                  + "\", \"isAbsolute\": " + savePaths.get(i).getPathIsAbsolute() + "}" + (i < savePaths.size() - 1 ? "," : "");
                configOutput += "\n    ],\n    \"backupPath\": {\"path\": \""
                              + backupFolder.substring(backupFolder.contains(home + "/") ? (home + "/").length() : 0, backupFolder.length())
                              + "\", \"isAbsolute\": " + !backupFolder.contains(home + "/") + "},"
                              + "\n    \"backupFileNamePrefix\": \"" + backupFileNamePrefix + "\","
                              + "\n    \"lastBackupTime\": "+ lastBackupTime + "\n}";
                Files.writeString(Path.of(configFile), configOutput);

                if (textArea == null && usePrompt) System.out.print(prompt);
            }
        // Sometimes on Linux, when Steam launches a game like Bully: Scholarship Edition, the path to the compatdata folder becomes briefly inaccessible.
        } catch (NoSuchFileException exception) {
        }
        return false;
    }
}
