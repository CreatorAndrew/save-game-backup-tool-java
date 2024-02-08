package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class BackupConfigContents {
    private BackupSavePath[] searchableSavePaths;
    private BackupSavePath backupPath;
    private String backupFileNamePrefix;
    private long lastBackupTime;

    public BackupSavePath[] getSearchableSavePaths() {
        return searchableSavePaths;
    }

    public BackupSavePath getBackupPath() {
        return backupPath;
    }

    public String getBackupFileNamePrefix() {
        return backupFileNamePrefix;
    }

    public long getLastBackupTime() {
        return lastBackupTime;
    }

    public void setLastBackupTime(long time) {
        lastBackupTime = time;
    }
}

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

    public Boolean getPathIsAbsolute() {
        return isAbsolute;
    }

    public void setPathIsAbsolute(boolean isAbsolute) {
        this.isAbsolute = isAbsolute;
    }
}

public class BackupWatchdog {
    protected static final String prompt = "> ";

    private static Long getModifiedTime(Path savePath) throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMddHHmmss");
        date.setTimeZone(TimeZone.getDefault());
        return Long.parseLong(date.format(new Date(Files.getLastModifiedTime(savePath).toMillis())));
    }

    // This method makes it so that this program treats the filesystem as relative to its own path.
    public static String replaceLocalDotDirectory(String path) {
        String newPath = path, replacement = "";
        try {
            replacement = (BackupWatchdog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
            replacement = replacement.substring(0, replacement.lastIndexOf("/"));
        } catch (URISyntaxException e) {
        }
        if (path.equals(".")) newPath = path.replace(".", replacement);
        else if (path.equals("..")) {
            replacement = replacement.substring(0, replacement.lastIndexOf("/"));
            newPath = path.replace("..", replacement);
        }
        else if (path.startsWith("./")) newPath = path.replaceFirst("./", replacement + "/");
        else if (path.startsWith("../")) {
            replacement = replacement.substring(0, replacement.lastIndexOf("/") + 1);
            newPath = path.replaceFirst("../", replacement);
        }
        if (isRunningOnWindows() && newPath.startsWith("/")) newPath = newPath.substring(1);
        return newPath;
    }

    public static String addToTextArea(String text, BackupGUI gui) {
        if (gui != null) gui.addToTextArea(text);
        return text;
    }

    public static boolean watchdog(String configFile, BackupGUI gui, boolean usePrompt, boolean firstRun) throws IOException {
        configFile = replaceLocalDotDirectory("./" + configFile);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileReader reader = new FileReader(configFile);
        BackupConfigContents config = gson.fromJson(reader, BackupConfigContents.class);
        reader.close();

        String home = System.getProperty("user.home").replaceAll("\\\\", "/");
        String backupFolder = replaceLocalDotDirectory(
            (config.getBackupPath().getPathIsAbsolute() ? "" : (home + "/")) + config.getBackupPath().getPath().replaceAll("\\\\", "/")
        );

        Path saveFile = null;
        for (int i = 0; i < config.getSearchableSavePaths().length; i++) {
            String saveFileString = (config.getSearchableSavePaths()[i].getPathIsAbsolute() ? "" : (home + "/")) + config.getSearchableSavePaths()[i].getPath();
            saveFileString = replaceLocalDotDirectory(saveFileString);
            if (Files.exists(Paths.get(saveFileString))) {
                saveFile = Paths.get(saveFileString);
                break;
            }
        }
        if (saveFile == null) {
            if (firstRun) {
                if (gui == null && usePrompt) System.out.println();
                System.out.println(addToTextArea("No save file found", gui));
                if (gui == null && usePrompt) System.out.print(prompt);
                return true;
            }
            // Sometimes on Linux, when Steam launches a Windows game, the Proton prefix path becomes briefly inaccessible.
            return false;
        }
        String saveFolder = saveFile.toString().substring(0, saveFile.toString().replaceAll("\\\\", "/").lastIndexOf("/") + 1);

        BackupUtils backupArchive = new BackupUtils(saveFolder);
        backupArchive.generateFileList(new File(saveFolder));

        if (Files.notExists(Paths.get(backupFolder))) Files.createDirectories(Paths.get(backupFolder));

        if (getModifiedTime(saveFile) > config.getLastBackupTime()) {
            config.setLastBackupTime(getModifiedTime(saveFile));

            String backup = config.getBackupFileNamePrefix() + "+" + config.getLastBackupTime() + ".zip";

            if (gui == null && usePrompt) System.out.println();
            if (Files.notExists(Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup))) {
                // Create the backup archive file
                backupArchive.compress(replaceLocalDotDirectory("./") + backup, gui);
                if (!backupFolder.equals(replaceLocalDotDirectory("./"))) Files.move(
                    Paths.get(replaceLocalDotDirectory("./") + backup), Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup)
                );
            } else System.out.println(addToTextArea(
                backup + " already exists in " + backupFolder.replaceAll("/", isRunningOnWindows() ? "\\\\" : "/") + ".\nBackup cancelled", gui
            ));
            if (gui == null && usePrompt) System.out.print(prompt);

            // Rewrite the JSON file
            FileWriter fileWriter = new FileWriter(configFile);
            JsonWriter writer = gson.newJsonWriter(fileWriter);
            writer.setIndent("    ");
            gson.toJson(gson.toJsonTree(config), writer);
            writer.close();
            fileWriter.close();
        }
        return false;
    }

    private static boolean isRunningOnWindows() {
        return System.getProperty("os.name").contains("Windows");
    }
}
