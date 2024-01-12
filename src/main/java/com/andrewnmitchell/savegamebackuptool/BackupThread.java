package com.andrewnmitchell.savegamebackuptool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class BackupThread extends Thread {
    private double interval;
    private boolean enabled = true, firstRun = true, usePrompt;
    private BackupTool backupTool;
    private BackupGUI gui;
    private BackupConfig config;
    private List<String> stopQueue;

    public BackupThread(BackupConfig config, List<String> stopQueue, double interval, BackupGUI gui) {
        this(config, stopQueue, interval, false, null, gui);
    }

    public BackupThread(BackupConfig config, List<String> stopQueue, double interval, boolean usePrompt, BackupTool backupTool) {
        this(config, stopQueue, interval, usePrompt, backupTool, null);
    }

    public BackupThread(BackupConfig config, List<String> stopQueue, double interval, boolean usePrompt, BackupTool backupTool, BackupGUI gui) {
        this.gui = gui;
        this.backupTool = backupTool;
        this.config = config;
        this.stopQueue = stopQueue;
        this.interval = interval;
        this.usePrompt = usePrompt;
    }

    public String getConfigName() {
        return config.getName();
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

    public void watchdog() throws IOException {
        String stopFilePath = BackupWatchdog.replaceLocalDotDirectory(
            "./.stop" + config.getPath().substring(config.getPath().lastIndexOf("/") + 1).replace(".json", "")
        );
        while (!stopQueue.contains(getConfigName()) && enabled) {
            try {
                Thread.sleep((long) (interval * 1000));
            } catch (InterruptedException e) {
            }
            if (BackupWatchdog.watchdog(config.getPath(), gui, usePrompt, firstRun) || Files.exists(Paths.get(stopFilePath))) {
                removeStopFile(stopFilePath);
                if (gui == null) backupTool.removeConfig(config);
                else gui.resetButton(config);
            }
            firstRun = false;
        }
        enabled = false;
    }

    private void removeStopFile(String stopFilePath) {
        while (Files.exists(Paths.get(stopFilePath)))
            try {
                Files.delete(Paths.get(stopFilePath));
            // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
            } catch (IOException e) {
            }
        enabled = false;
    }
}
