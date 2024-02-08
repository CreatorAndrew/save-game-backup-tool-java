package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import static java.lang.System.getProperty;

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
        String tempPath = path.replace("\\", "/"), replacement = "";
        try {
            replacement = (BackupWatchdog.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath().replace("\\", "/");
        } catch (URISyntaxException e) {
        }
        replacement = replacement.substring(0, replacement.lastIndexOf("/"));
        if (tempPath.equals(".")) tempPath = tempPath.replace(".", replacement);
        else if (tempPath.equals("..")) tempPath = tempPath.replace("..", replacement.substring(0, replacement.lastIndexOf("/")));
        else if (tempPath.startsWith("./")) tempPath = tempPath.replaceFirst("./", replacement + "/");
        else if (tempPath.startsWith("../")) tempPath = tempPath.replaceFirst("../", replacement.substring(0, replacement.lastIndexOf("/") + 1));
        if (getProperty("os.name").contains("Windows") && tempPath.startsWith("/")) tempPath = tempPath.substring(1);
        return tempPath;
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

        String backupFolder = replaceLocalDotDirectory(
            (config.getBackupPath().getPathIsAbsolute() ? "" : (getProperty("user.home") + "/")) + config.getBackupPath().getPath()
        );

        String saveFile = null;
        for (int i = 0; i < config.getSearchableSavePaths().length; i++) {
            String tempSaveFile = replaceLocalDotDirectory(
                (config.getSearchableSavePaths()[i].getPathIsAbsolute() ? "" : (getProperty("user.home") + "/")) + config.getSearchableSavePaths()[i].getPath()
            );
            if (Files.exists(Paths.get(tempSaveFile))) {
                saveFile = tempSaveFile;
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
        String saveFolder = saveFile.substring(0, saveFile.lastIndexOf("/") + 1);

        BackupUtils backupArchive = new BackupUtils(saveFolder);
        backupArchive.generateFileList(new File(saveFolder));

        if (Files.notExists(Paths.get(backupFolder))) Files.createDirectories(Paths.get(backupFolder));

        if (getModifiedTime(Paths.get(saveFile)) > config.getLastBackupTime()) {
            config.setLastBackupTime(getModifiedTime(Paths.get(saveFile)));

            String backup = config.getBackupFileNamePrefix() + "+" + config.getLastBackupTime() + ".zip";

            if (gui == null && usePrompt) System.out.println();
            if (Files.notExists(Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup))) {
                // Create the backup archive file
                backupArchive.compress(replaceLocalDotDirectory("./" + backup), gui);
                if (!backupFolder.equals(replaceLocalDotDirectory("./"))) Files.move(
                    Paths.get(replaceLocalDotDirectory("./" + backup)), Paths.get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup)
                );
            } else System.out.println(addToTextArea(
                backup + " already exists in " + backupFolder.replace("/", getProperty("os.name").contains("Windows") ? "\\" : "/") + ".\nBackup cancelled",
                gui
            ));
            if (gui == null && usePrompt) System.out.print(prompt);

            // Update the JSON file
            FileWriter writer = new FileWriter(configFile);
            writer.write(
                gson.toJson(gson.toJsonTree(config)).replace("\n", getProperty("os.name").contains("Windows") ? "\r\n" : "\n").replace("  ", "    ")
            );
            writer.close();
        }
        return false;
    }
}
