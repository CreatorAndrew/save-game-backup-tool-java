package com.andrewnmitchell.savegamebackuptool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BackupThread extends Thread {
    private double interval;
    private boolean enabled = true, firstRun = true, usePrompt;
    private BackupTool backupTool;
    private BackupGUI gui;
    private BackupConfig config;
    private ArrayList<String> stopQueue;

    public BackupThread(BackupConfig config, ArrayList<String> stopQueue, double interval, boolean usePrompt, BackupTool backupTool) {
        gui = null;
        this.backupTool = backupTool;
        this.config = config;
        this.stopQueue = stopQueue;
        this.interval = interval;
        this.usePrompt = usePrompt;
    }

    public BackupThread(BackupConfig config, ArrayList<String> stopQueue, double interval, boolean usePrompt, BackupGUI gui) {
        this.gui = gui;
        backupTool = null;
        this.config = config;
        this.stopQueue = stopQueue;
        this.interval = interval;
        usePrompt = false;
    }

    public String getConfigName() {
        return config.getName();
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void run() {
        watchdog();
    }

    public void watchdog() {
        String stopFilePath = "./.stop" + config.getPath().substring(config.getPath().lastIndexOf("/") + 1).replace(".json", "");
        while (!stopQueue.contains(getConfigName()) && enabled) {
            try {
                try {
                    Thread.sleep((long) (interval * 1000));
                } catch (InterruptedException exception) {
                }
                if (gui == null) {
                    if (BackupWatchdog.watchdog(config.getPath(), usePrompt, firstRun)
                        || Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)))) {
                        removeStopFile(stopFilePath);
                        backupTool.removeConfig(config);
                    }
                } else {
                    if (BackupWatchdog.watchdog(config.getPath(), gui, usePrompt, firstRun)
                        || Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)))) {
                        removeStopFile(stopFilePath);
                        gui.redrawTable(config);
                    }
                }
                firstRun = false;
            } catch (IOException exception) {
            }
        }
        enabled = false;
    }

    private void removeStopFile(String stopFilePath) {
        while (Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath))))
            try {
                Files.delete(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)));
            // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
            } catch (IOException exception) {
            }
        enabled = false;
    }
}
