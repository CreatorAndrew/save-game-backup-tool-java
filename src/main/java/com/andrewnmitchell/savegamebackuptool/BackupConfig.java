package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.annotations.SerializedName;

public class BackupConfig {
    @SerializedName("title")
    private String name;
    @SerializedName("name")
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
