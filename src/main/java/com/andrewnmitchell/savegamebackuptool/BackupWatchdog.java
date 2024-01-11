package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

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

    protected static final String prompt = "> ";

    // This method makes it so that this program treats the filesystem as relative to its own path.
    public static String replaceLocalDotDirectory(String path) {
        String newPath = path, replacement = "";
        try {
             replacement = (BackupWatchdog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
             replacement = replacement.substring(0, replacement.lastIndexOf("/"));
        } catch (URISyntaxException exception) {
        }
        if (path.startsWith("./")) newPath = path.replaceFirst("./", replacement + "/");
        else if (path.startsWith("../")) {
            replacement = replacement.substring(0, replacement.lastIndexOf("/") + 1);
            newPath = path.replaceFirst("../", replacement);
        }
        if (System.getProperty("os.name").contains("Windows") && newPath.startsWith("/")) newPath = newPath.substring(1);
        return newPath;
    }

    public static String addToTextArea(String text, BackupGUI gui) {
        if (gui != null) gui.addToTextArea(text);
        return text;
    }

    public static boolean watchdog(String configFile, boolean usePrompt, boolean firstRun) throws IOException {
        return watchdog(configFile, null, usePrompt, firstRun);
    }

    public static boolean watchdog(String configFile, BackupGUI gui, boolean usePrompt, boolean firstRun) throws IOException {
        String home = System.getProperty("user.home").replaceAll("\\\\", "/"), backupFolder = "", backupFileNamePrefix = "";

        long lastBackupTime = 0;

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
            if (Files.exists(Paths.get(savePathString))) {
                savePath = Paths.get(savePathString);
                break;
            }
        }
        if (savePath == null) {
            if (firstRun) {
                if (gui == null && usePrompt) System.out.println();
                System.out.println(addToTextArea("No save file found", gui));
                if (gui == null && usePrompt) System.out.print(prompt);
                return true;
            }
            // Sometimes on Linux, when Steam launches a Windows game, the Proton prefix path becomes briefly inaccessible.
            return false;
        }
        String saveFolder = savePath.toString().substring(0, savePath.toString().replaceAll("\\\\", "/").lastIndexOf("/") + 1);

        BackupUtils backupArchive = new BackupUtils(saveFolder);
        backupArchive.generateFileList(new File(saveFolder));

        if (Files.notExists(Paths.get(backupFolder))) Files.createDirectories(Paths.get(backupFolder));

        if (getModifiedDate(savePath) > lastBackupTime) {
            lastBackupTime = getModifiedDate(savePath);

            if (gui == null && usePrompt) System.out.println();
            String backup = backupFileNamePrefix + "+" + lastBackupTime + ".zip";
            if (Files.notExists(Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup))) {
                // Create the backup archive file
                backupArchive.compress(replaceLocalDotDirectory("./") + backup, gui);
                if (!backupFolder.equals(replaceLocalDotDirectory("./")))
                    Files.move(Paths.get(replaceLocalDotDirectory("./") + backup),
                               Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup));
            } else System.out.println(addToTextArea(backup + " already exists in " +
                                                    backupFolder.replaceAll("/", System.getProperty("os.name").contains("Windows") ? "\\\\" : "/") +
                                                    ".\nBackup cancelled", gui));

            // Rewrite the JSON file
            String configOutput = "{\n    \"searchableSavePaths\": [";
            for (int i = 0; i < savePaths.size(); i++)
                configOutput += "\n        {\n            \"path\": \"" + savePaths.get(i).getPath() + "\",\n            "
                              + "\"isAbsolute\": " + savePaths.get(i).getPathIsAbsolute() + "\n        }" + (i < savePaths.size() - 1 ? "," : "");
            configOutput += "\n    ],\n    \"backupPath\": {\n        \"path\": \""
                          + backupFolder.substring(backupFolder.contains(home + "/") ? (home + "/").length() : 0, backupFolder.length())
                          + "\",\n        \"isAbsolute\": " + !backupFolder.contains(home + "/") + "\n    },"
                          + "\n    \"backupFileNamePrefix\": \"" + backupFileNamePrefix + "\","
                          + "\n    \"lastBackupTime\": "+ lastBackupTime + "\n}";
            FileWriter fileWriter = new FileWriter(configFile);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.write(configOutput.replaceAll("\n", System.getProperty("os.name").contains("Windows") ? "\r\n" : "\n"));
            writer.close();

            if (gui == null && usePrompt) System.out.print(prompt);
        }
        return false;
    }
}
