package com.andrewnmitchell.savegamebackuptool;

import java.util.UUID;

public class BackupConfig {
    private String name;
    private String title;
    private UUID uuid;

    public BackupConfig(String title, String name) {
        setTitle(title);
        setName(name);
    }

    public BackupConfig() {}

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
