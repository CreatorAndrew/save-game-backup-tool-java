package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.annotations.SerializedName;
import java.util.UUID;

public class BackupConfig {
    @SerializedName("title")
    private String name;
    @SerializedName("name")
    private String path;
    private UUID uuid;

    public BackupConfig(String name, String path) {
        setName(name);
        setPath(path);
    }

    public BackupConfig() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }
}
