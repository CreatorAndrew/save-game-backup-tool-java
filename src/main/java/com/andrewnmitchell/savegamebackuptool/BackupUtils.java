package com.andrewnmitchell.savegamebackuptool;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import static java.lang.System.getProperty;

public class BackupUtils {
    protected static final String PROMPT = "> ";

    public static String addToTextArea(String text, BackupGUI gui) {
        if (gui != null)
            gui.addToTextArea(text);
        return text;
    }

    // This method makes it so that this program treats the filesystem as relative to its own path.
    public static String applyWorkingDirectory(String path) {
        String tempPath = path.replace("\\", "/"), replacement = "";
        try {
            replacement = (BackupWatchdog.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getPath().replace("\\", "/");
        } catch (URISyntaxException e) {
        }
        replacement = replacement.substring(0, replacement.lastIndexOf("/"));
        if (tempPath.equals("."))
            tempPath = tempPath.replace(".", replacement);
        else if (tempPath.equals(".."))
            tempPath =
                    tempPath.replace("..", replacement.substring(0, replacement.lastIndexOf("/")));
        else if (tempPath.startsWith("./"))
            tempPath = tempPath.replaceFirst("./", replacement + "/");
        else if (tempPath.startsWith("../"))
            tempPath = tempPath.replaceFirst("../",
                    replacement.substring(0, replacement.lastIndexOf("/") + 1));
        if (getProperty("os.name").contains("Windows") && tempPath.startsWith("/"))
            tempPath = tempPath.substring(1);
        return tempPath;
    }

    public static List<String> getFilesInLowerCase(String path) {
        List<String> files = new ArrayList<String>();
        for (String file : new File(path).list())
            files.add(file.toLowerCase());
        return files;
    }
}
