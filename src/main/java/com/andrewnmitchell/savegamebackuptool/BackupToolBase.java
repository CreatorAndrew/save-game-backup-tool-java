package com.andrewnmitchell.savegamebackuptool;

import java.util.List;
import java.util.UUID;

public class BackupToolBase {
    private List<BackupThread> backupThreads;
    private List<BackupConfig> configs, configsUsed;
    private List<UUID> stopQueue;

    public List<BackupThread> getBackupThreads() {
        return backupThreads;
    }

    public List<BackupConfig> getConfigs() {
        return configs;
    }

    public List<BackupConfig> getConfigsUsed() {
        return configsUsed;
    }

    public List<UUID> getStopQueue() {
        return stopQueue;
    }

    public void setBackupThreads(List<BackupThread> backupThreads) {
        this.backupThreads = backupThreads;
    }

    public void setConfigs(List<BackupConfig> configs) {
        this.configs = configs;
    }

    public void setConfigsUsed(List<BackupConfig> configsUsed) {
        this.configsUsed = configsUsed;
    }

    public void setStopQueue(List<UUID> stopQueue) {
        this.stopQueue = stopQueue;
    }
}
