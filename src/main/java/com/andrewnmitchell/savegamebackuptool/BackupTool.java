package com.andrewnmitchell.savegamebackuptool;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mslinks.ShellLink;
import static com.andrewnmitchell.savegamebackuptool.BackupThread.*;
import static com.andrewnmitchell.savegamebackuptool.BackupUtils.*;
import static java.lang.Integer.*;
import static java.lang.String.*;
import static java.lang.System.*;
import static java.nio.file.attribute.PosixFilePermission.*;
import static java.nio.file.Files.*;
import static java.nio.file.Paths.*;
import static java.util.Arrays.*;
import static java.util.UUID.*;

class MasterConfig {
    @SerializedName("configurations")
    private BackupConfig[] configs;
    @SerializedName("default")
    private String defaultConfigName;
    private Double interval;
    private Boolean createShortcut, startHidden, hideOnClose;

    public BackupConfig[] getConfigs() {
        return configs;
    }

    public boolean getCreateShortcut() {
        if (createShortcut == null)
            return false;
        return createShortcut;
    }

    public String getDefaultConfigName() {
        return defaultConfigName;
    }

    public boolean getHideOnClose() {
        if (hideOnClose == null)
            return false;
        return hideOnClose;
    }

    public double getInterval() {
        if (interval == null)
            return 0;
        return interval;
    }

    public boolean getStartHidden() {
        if (startHidden == null)
            return false;
        return startHidden;
    }
}


public class BackupTool extends BackupToolBase {
    public BackupTool(String args[]) {
        try {
            run(args);
        } catch (IOException e) {
        }
    }

    public static BackupConfig addOrRemoveConfig(BufferedReader input, String configFile,
            List<BackupConfig> configs) throws IOException {
        BackupConfig config = null;
        if (configFile == null) {
            println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++)
                println("    " + i + ": " + configs.get(i).getTitle());
            String choice = null;
            while (choice == null) {
                print("Enter in an option number here: ");
                choice = input.readLine();
                try {
                    if (parseInt(choice) >= configs.size() || parseInt(choice) < 0) {
                        println("Not a valid option number. Try again.");
                        choice = null;
                    }
                } catch (NumberFormatException e) {
                    println("Invalid input value. Try again with a numeric value.");
                    choice = null;
                }
            }
            config = configs.get(parseInt(choice));
        }
        return config;
    }

    public static void createShortcutAt(String shortcutPath) throws IOException {
        new ShellLink().setIconLocation(applyWorkingDirectory("./BackupTool.ico"))
                .setTarget(applyWorkingDirectory("./BackupTool.jar")).saveTo(shortcutPath);
    }

    public static void main(String args[]) {
        BackupTool backupTool = new BackupTool(args);
    }

    public void run(String args[]) throws IOException {
        FileReader reader = new FileReader(applyWorkingDirectory("./MasterConfig.json"));
        MasterConfig masterConfig = (new Gson()).fromJson(reader, MasterConfig.class);
        reader.close();
        if (masterConfig.getCreateShortcut()) {
            if (getProperty("os.name").contains("Linux")) {
                String shortcutPath = getProperty("user.home") + "/.local/share/applications/";
                FileWriter launcherWriter = new FileWriter(shortcutPath + ".launchBackupTool");
                FileWriter shortcutWriter = new FileWriter(shortcutPath + "BackupTool.desktop");
                launcherWriter.write(join("\n", "#!/bin/bash",
                        "java -jar \"" + applyWorkingDirectory("./BackupTool.jar") + "\""));
                shortcutWriter.write(join("\n", "[Desktop Entry]", "Type=Application",
                        "Categories=Game;Utility", "Name=Save Game Backup Tool",
                        "Exec=\"" + shortcutPath + ".launchBackupTool" + "\"",
                        "Icon=" + applyWorkingDirectory("./BackupTool.ico")));
                launcherWriter.close();
                shortcutWriter.close();
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(OWNER_EXECUTE);
                perms.add(OWNER_READ);
                perms.add(OWNER_WRITE);
                setPosixFilePermissions(get(shortcutPath + ".launchBackupTool"), perms);
                setPosixFilePermissions(get(shortcutPath + "BackupTool.desktop"), perms);
            }
            if (getProperty("os.name").contains("Windows")) {
                try {
                    createShortcutAt(getenv("APPDATA")
                            + "/Microsoft/Windows/Start Menu/Programs/Save Game Backup Tool.lnk");
                } catch (IOException e) {
                    createShortcutAt(getProperty("user.home")
                            + "/Start Menu/Programs/Save Game Backup Tool.lnk");
                }
            }
        }
        setBackupThreads(new ArrayList<>());
        setConfigs(asList(masterConfig.getConfigs()));
        setConfigsUsed(new ArrayList<>());
        for (BackupConfig config : getConfigs())
            config.setUUID(randomUUID());
        String configFile = null;
        boolean skipChoice = false, noGUI = false;
        for (String arg : args)
            switch (arg.toLowerCase()) {
                case "--no-gui":
                    noGUI = true;
                    break;
                case "--skip-choice":
                    skipChoice = true;
                    break;
            }
        if (skipChoice)
            configFile = masterConfig.getDefaultConfigName();
        for (int i = 0; i < args.length && args.length > 1 && !skipChoice; i++)
            if (args[i].toLowerCase().equals("--config") && i < args.length - 1) {
                for (String file : new File(applyWorkingDirectory(".")).list())
                    if (file.toLowerCase().endsWith(".json") && file.toLowerCase()
                            .equals(args[i + 1].toLowerCase().replace(".json", "") + ".json")) {
                        configFile = file;
                        break;
                    }
                break;
            }
        if (noGUI) {
            setStopQueue(new ArrayList<>());
            if (configFile == null) {
                boolean continueRunning = true;
                BufferedReader input = new BufferedReader(new InputStreamReader(in));
                println("Enter in \"help\" or \"?\" for assistance.");
                while (continueRunning) {
                    print(PROMPT);
                    String choice = input.readLine();
                    switch (choice.toLowerCase()) {
                        case "start": {
                            BackupConfig config =
                                    addOrRemoveConfig(input, configFile, getConfigs());
                            if (getConfigsUsed().contains(config))
                                println("That configuration is already in use");
                            else
                                addConfig(this, config, masterConfig.getInterval());
                            break;
                        }
                        case "stop": {
                            BackupConfig config =
                                    addOrRemoveConfig(input, configFile, getConfigs());
                            if (getConfigsUsed().contains(config))
                                removeConfig(this, config);
                            else
                                println("That configuration was not in use.");
                            break;
                        }
                        case "end":
                        case "exit":
                        case "quit": {
                            removeAllConfigs(this);
                            continueRunning = false;
                            break;
                        }
                        case "help":
                        case "?":
                            println(join("\n",
                                    "Enter in \"start\" to initialize a backup configuration.",
                                    "Enter in \"stop\" to suspend a backup configuration.",
                                    "Enter in \"end\", \"exit\", or \"quit\" to shut down this tool."));
                            break;
                        case "":
                            break;
                        default:
                            println("Invalid command");
                            break;
                    }
                }
                input.close();
            } else {
                getBackupThreads().add(new BackupThread(
                        new BackupConfig(masterConfig.getDefaultConfigName(), configFile),
                        masterConfig.getInterval(), false, this));
                getBackupThreads().get(getBackupThreads().size() - 1).start();
            }
        } else {
            BackupGUI gui = new BackupGUI(getConfigs(), masterConfig.getHideOnClose(),
                    masterConfig.getInterval(), masterConfig.getStartHidden());
        }
    }
}
