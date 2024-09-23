package com.andrewnmitchell.savegamebackuptool;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import static com.andrewnmitchell.savegamebackuptool.BackupUtils.*;
import static java.lang.Long.*;
import static java.lang.System.*;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.*;

class BackupConfigContents {
    private String backupFileNamePrefix;
    private BackupOrSavePath backupPath;
    private long lastBackupTime;
    private BackupOrSavePath[] searchableSavePaths;

    public String getBackupFileNamePrefix() {
        return backupFileNamePrefix;
    }

    public BackupOrSavePath getBackupPath() {
        return backupPath;
    }

    public long getLastBackupTime() {
        return lastBackupTime;
    }

    public BackupOrSavePath[] getSearchableSavePaths() {
        return searchableSavePaths;
    }

    public void setLastBackupTime(long time) {
        lastBackupTime = time;
    }
}


class BackupOrSavePath {
    private String path;
    private boolean startsWithUserPath;

    public BackupOrSavePath(String path, boolean startsWithUserPath) {
        setPath(path);
        setStartsWithUserPath(startsWithUserPath);
    }

    public BackupOrSavePath() {}

    public String getPath() {
        return path;
    }

    public Boolean getStartsWithUserPath() {
        return startsWithUserPath;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setStartsWithUserPath(boolean startsWithUserPath) {
        this.startsWithUserPath = startsWithUserPath;
    }
}


public class BackupWatchdog {
    public static long getModifiedTime(Path savePath) throws IOException {
        SimpleDateFormat date = new SimpleDateFormat("yyyyMMddHHmmss");
        date.setTimeZone(TimeZone.getDefault());
        return parseLong(date.format(new Date(getLastModifiedTime(savePath).toMillis())));
    }

    public static boolean watchdog(String configFile, BackupGUI gui, boolean usePrompt,
            boolean firstRun) throws IOException {
        for (String file : new File(applyWorkingDirectory(".")).list())
            if (file.toLowerCase().endsWith(".json") && file.toLowerCase()
                    .equals(configFile.toLowerCase().replace(".json", "") + ".json")) {
                configFile = applyWorkingDirectory("./" + file);
                break;
            }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileReader reader = new FileReader(configFile);
        BackupConfigContents config = gson.fromJson(reader, BackupConfigContents.class);
        reader.close();
        String backupFolder = applyWorkingDirectory(
                (config.getBackupPath().getStartsWithUserPath() ? (getProperty("user.home") + "/")
                        : "") + config.getBackupPath().getPath());
        String saveFile = null;
        for (int i = 0; i < config.getSearchableSavePaths().length; i++) {
            String tempSaveFile = applyWorkingDirectory(
                    (config.getSearchableSavePaths()[i].getStartsWithUserPath()
                            ? (getProperty("user.home") + "/")
                            : "") + config.getSearchableSavePaths()[i].getPath());
            if (exists(get(tempSaveFile))) {
                saveFile = tempSaveFile;
                break;
            }
        }
        if (saveFile == null) {
            if (firstRun) {
                if (gui == null && usePrompt)
                    println();
                println(addToTextArea("No save file found", gui));
                if (gui == null && usePrompt)
                    print(PROMPT);
                return true;
            }
            // Sometimes on Linux, when Steam launches a Windows game, the Proton prefix path
            // becomes briefly inaccessible.
            return false;
        }
        String saveFolder = saveFile.substring(0, saveFile.lastIndexOf("/") + 1);
        BackupArchiveUtils backupArchive = new BackupArchiveUtils(saveFolder);
        backupArchive.generateFileList(new File(saveFolder));
        if (notExists(get(backupFolder)))
            createDirectories(get(backupFolder));
        if (getModifiedTime(get(saveFile)) > config.getLastBackupTime()) {
            config.setLastBackupTime(getModifiedTime(get(saveFile)));
            String backup =
                    config.getBackupFileNamePrefix() + "+" + config.getLastBackupTime() + ".zip";
            if (gui == null && usePrompt)
                println();
            if (notExists(get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup))) {
                // Create the backup archive file
                backupArchive.compress(applyWorkingDirectory("./" + backup), gui);
                if (!backupFolder.equals(applyWorkingDirectory("./")))
                    move(get(applyWorkingDirectory("./" + backup)),
                            get(backupFolder + (backupFolder.endsWith("/") ? "" : "/") + backup));
            } else
                println(addToTextArea(backup + " already exists in "
                        + backupFolder.replace("/",
                                getProperty("os.name").contains("Windows") ? "\\" : "/")
                        + ".\nBackup cancelled", gui));
            if (gui == null && usePrompt)
                print(PROMPT);
            // Update the JSON file
            FileWriter writer = new FileWriter(configFile);
            writer.write(gson.toJson(gson.toJsonTree(config))
                    .replace("\n", getProperty("os.name").contains("Windows") ? "\r\n" : "\n")
                    .replace("  ", "    "));
            writer.close();
        }
        return false;
    }
}
