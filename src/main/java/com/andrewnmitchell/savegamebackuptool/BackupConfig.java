package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.annotations.SerializedName;

public class BackupConfig {
    private String name;
    @SerializedName("file")
    private String path;

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
}
