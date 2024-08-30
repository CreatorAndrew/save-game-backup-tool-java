package com.andrewnmitchell.savegamebackuptool;
import java.util.List;
import java.util.UUID;

public class BackupToolBase {
    public List<BackupThread> backupThreads;
    public List<BackupConfig> configs, configsUsed;
    public List<UUID> stopQueue;
}
