package com.andrewnmitchell.savegamebackuptool;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class BackupThread extends Thread {
        private double interval;
        private boolean disabled = false, firstRun = true, usePrompt;
        private BackupTool backupTool;
        private BackupGUI gui;
        private BackupConfig backupConfig;
        private ArrayList<String> stopQueue;

        public BackupThread(BackupConfig backupConfig, ArrayList<String> stopQueue, double interval, boolean usePrompt, BackupTool backupTool) {
            gui = null;
            this.backupTool = backupTool;
            this.backupConfig = backupConfig;
            this.stopQueue = stopQueue;
            this.interval = interval;
            this.usePrompt = usePrompt;
        }

        public BackupThread(BackupConfig backupConfig, ArrayList<String> stopQueue, double interval, boolean usePrompt, BackupGUI gui) {
            this.gui = gui;
            backupTool = null;
            this.backupConfig = backupConfig;
            this.stopQueue = stopQueue;
            this.interval = interval;
            usePrompt = false;
        }

        public String getConfigName() {
            return backupConfig.getName();
        }

        public boolean getDisabled() {
            return disabled;
        }

        public void run() {
            watchdog();
        }

        public void watchdog() {
            String stopFilePath = "./.stop" + backupConfig.getPath().substring(backupConfig.getPath().lastIndexOf("/") + 1).replace(".json", "");
            while (!stopQueue.contains(getConfigName()) && !disabled) {
                try {
                    try {
                        Thread.sleep((long) (interval * 1000));
                    } catch (InterruptedException exception) {
                    }
                    if (gui == null) {
                        if (BackupWatchdog.watchdog(backupConfig.getPath(), usePrompt, firstRun)
                         || Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)))) {
                            removeStopFile(stopFilePath);
                            backupTool.removeConfig(backupConfig);
                         }
                    } else {
                        if (BackupWatchdog.watchdog(backupConfig.getPath(), gui.textArea, usePrompt, firstRun)
                         || Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)))) {
                            removeStopFile(stopFilePath);
                            for (int i = 0; i < gui.buttons.length; i++)
                                gui.buttons[i].setText(gui.configsUsed.contains(gui.configs.get(i)) ? gui.disableLabel : gui.enableLabel);
                            gui.buttons[gui.configs.indexOf(backupConfig)].setText(gui.enableLabel);
                            gui.updateTable();
                            gui.removeConfig(backupConfig);
                        }
                    }
                    firstRun = false;
                } catch (IOException exception) {
                }
            }
            disabled = true;
        }

        private void removeStopFile(String stopFilePath) {
            while (Files.exists(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath))))
                try {
                    Files.delete(Paths.get(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)));
                // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
                } catch (IOException exception) {
                }
            disabled = true;
        }
    }
