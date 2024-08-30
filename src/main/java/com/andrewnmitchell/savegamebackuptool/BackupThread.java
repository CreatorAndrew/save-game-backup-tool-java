package com.andrewnmitchell.savegamebackuptool;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BackupThread extends Thread {
    private double interval;
    private boolean enabled = true, firstRun = true, usePrompt;
    private BackupToolBase backupTool;
    private BackupGUI gui;
    private BackupConfig config;
    private List<UUID> stopQueue;

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval, BackupGUI gui) {
        this(config, stopQueue, interval, false, null, gui);
    }

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval, boolean usePrompt, BackupToolBase backupTool) {
        this(config, stopQueue, interval, usePrompt, backupTool, null);
    }

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval, boolean usePrompt, BackupToolBase backupTool, BackupGUI gui) {
        this.gui = gui;
        this.backupTool = backupTool;
        this.config = config;
        this.stopQueue = stopQueue;
        this.interval = interval;
        this.usePrompt = usePrompt;
    }

    public static void removeConfig(BackupToolBase callback, BackupConfig config) {
        callback.stopQueue.add(config.getUUID());
        while (callback.backupThreads.get(callback.configsUsed.indexOf(config)).getEnabled()) System.out.print("");
        callback.stopQueue.remove(callback.stopQueue.indexOf(config.getUUID()));
        callback.backupThreads.remove(callback.configsUsed.indexOf(config));
        callback.configsUsed.remove(callback.configsUsed.indexOf(config));
    }

    public static void addConfig(BackupToolBase callback, BackupConfig config, double interval) {
        addConfig(callback, config, interval, null);
    }

    public static void addConfig(BackupToolBase callback, BackupConfig config, double interval, BackupGUI backupGUI) {
        callback.configsUsed.add(config);
        callback.backupThreads.add(new BackupThread(config, callback.stopQueue, interval, backupGUI == null, backupGUI == null ? callback : null, backupGUI));
        callback.backupThreads.get(callback.backupThreads.size() - 1).start();
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void run() {
        try {
            watchdog();
        } catch (IOException e) {
        }
    }

    public List<String> getFilesInLowerCase(String path) {
        List<String> files = new ArrayList<String>();
        for (String file : new File(path).list()) {
            files.add(file.toLowerCase());
        }
        return files;
    }

    public void watchdog() throws IOException {
        String stopFilePath = BackupWatchdog.applyWorkingDirectory(
            "./.stop" + config.getPath().substring(
                0,
                config.getPath().toLowerCase().endsWith(".json") ? config.getPath().toLowerCase().lastIndexOf(".json") : config.getPath().length()
            ).replace(".json", "")
        );
        while (!stopQueue.contains(config.getUUID()) && enabled) {
            try {
                Thread.sleep((long) (interval * 1000));
            } catch (InterruptedException e) {
            }
            if (
                BackupWatchdog.watchdog(config.getPath(), gui, usePrompt, firstRun) ||
                getFilesInLowerCase(BackupWatchdog.applyWorkingDirectory(".")).contains((stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1)).toLowerCase())
            ) {
                while (getFilesInLowerCase(BackupWatchdog.applyWorkingDirectory(".")).contains((stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1)).toLowerCase()))
                    for (String file : new File(BackupWatchdog.applyWorkingDirectory(".")).list())
                        if (file.equalsIgnoreCase(stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1)))
                            try {
                                Files.delete(Paths.get(BackupWatchdog.applyWorkingDirectory("./" + file)));
                            } catch (IOException e) {
                            }
                enabled = false;
                if (gui == null) removeConfig(backupTool, config);
                else gui.resetButton(config);
            }
            firstRun = false;
        }
        enabled = false;
    }
}
