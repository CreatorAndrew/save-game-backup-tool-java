package com.andrewnmitchell.savegamebackuptool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BackupThread extends Thread {
    private BackupToolBase backupTool;
    private BackupConfig config;
    private boolean enabled = true, firstRun = true, usePrompt;
    private BackupGUI gui;
    private double interval;
    private List<UUID> stopQueue;

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval, BackupGUI gui) {
        this(config, stopQueue, interval, false, null, gui);
    }

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval,
            boolean usePrompt, BackupToolBase backupTool) {
        this(config, stopQueue, interval, usePrompt, backupTool, null);
    }

    public BackupThread(BackupConfig config, List<UUID> stopQueue, double interval,
            boolean usePrompt, BackupToolBase backupTool, BackupGUI gui) {
        this.backupTool = backupTool;
        this.config = config;
        this.gui = gui;
        this.interval = interval;
        this.stopQueue = stopQueue;
        this.usePrompt = usePrompt;
    }

    public static void addConfig(BackupToolBase backupTool, BackupConfig config, double interval) {
        addConfig(backupTool, config, interval, null);
    }

    public static void addConfig(BackupToolBase backupTool, BackupConfig config, double interval,
            BackupGUI backupGUI) {
        backupTool.getConfigsUsed().add(config);
        backupTool.getBackupThreads().add(new BackupThread(config, backupTool.getStopQueue(),
                interval, backupGUI == null, backupGUI == null ? backupTool : null, backupGUI));
        backupTool.getBackupThreads().get(backupTool.getBackupThreads().size() - 1).start();
    }

    public static void removeConfig(BackupToolBase backupTool, BackupConfig config) {
        backupTool.getStopQueue().add(config.getUUID());
        while (backupTool.getBackupThreads().get(backupTool.getConfigsUsed().indexOf(config))
                .getEnabled())
            System.out.print("");
        backupTool.getStopQueue().remove(backupTool.getStopQueue().indexOf(config.getUUID()));
        backupTool.getBackupThreads().remove(backupTool.getConfigsUsed().indexOf(config));
        backupTool.getConfigsUsed().remove(backupTool.getConfigsUsed().indexOf(config));
    }

    public boolean getEnabled() {
        return enabled;
    }

    public List<String> getFilesInLowerCase(String path) {
        List<String> files = new ArrayList<String>();
        for (String file : new File(path).list()) {
            files.add(file.toLowerCase());
        }
        return files;
    }

    public void run() {
        try {
            watchdog();
        } catch (IOException e) {
        }
    }

    public void watchdog() throws IOException {
        String stopFilePath = BackupWatchdog.applyWorkingDirectory("./.stop" + config.getPath()
                .substring(0,
                        config.getPath().toLowerCase().endsWith(".json")
                                ? config.getPath().toLowerCase().lastIndexOf(".json")
                                : config.getPath().length())
                .replace(".json", ""));
        while (!stopQueue.contains(config.getUUID()) && enabled) {
            try {
                Thread.sleep((long) (interval * 1000));
            } catch (InterruptedException e) {
            }
            if (BackupWatchdog.watchdog(config.getPath(), gui, usePrompt, firstRun)
                    || getFilesInLowerCase(BackupWatchdog.applyWorkingDirectory("."))
                            .contains((stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1))
                                    .toLowerCase())) {
                while (getFilesInLowerCase(BackupWatchdog.applyWorkingDirectory(".")).contains(
                        (stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1)).toLowerCase()))
                    for (String file : new File(BackupWatchdog.applyWorkingDirectory(".")).list())
                        if (file.equalsIgnoreCase(
                                stopFilePath.substring(stopFilePath.lastIndexOf("/") + 1)))
                            try {
                                Files.delete(Paths
                                        .get(BackupWatchdog.applyWorkingDirectory("./" + file)));
                            } catch (IOException e) {
                            }
                enabled = false;
                if (gui == null)
                    removeConfig(backupTool, config);
                else
                    gui.resetButton(config);
            }
            firstRun = false;
        }
        enabled = false;
    }
}
