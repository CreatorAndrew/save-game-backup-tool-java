package com.andrewnmitchell.savegamebackuptool;

import com.google.gson.annotations.SerializedName;
import mslinks.ShellLink;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import static com.andrewnmitchell.savegamebackuptool.BackupThread.*;
import static com.andrewnmitchell.savegamebackuptool.BackupUtils.*;
import static java.lang.System.getProperty;

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

    public void run(String args[]) throws IOException {
        FileReader reader = new FileReader(applyWorkingDirectory("./MasterConfig.json"));
        MasterConfig masterConfig = (new Gson()).fromJson(reader, MasterConfig.class);
        reader.close();
        if (masterConfig.getCreateShortcut()) {
            if (getProperty("os.name").contains("Linux")) {
                String shortcutPath =
                        getProperty("user.home") + "/.local/share/applications/BackupTool.desktop";
                FileWriter shortcutCreator = new FileWriter(shortcutPath);
                shortcutCreator.write(String.join("\n", "[Desktop Entry]", "Type=Application",
                        "Categories=Game;Utility", "Name=Save Game Backup Tool",
                        "Exec=\"" + applyWorkingDirectory("./Launch.sh") + "\"",
                        "Icon=" + applyWorkingDirectory("./BackupTool.png")));
                shortcutCreator.close();
                Set<PosixFilePermission> perms = new HashSet<>();
                perms.add(PosixFilePermission.OWNER_READ);
                perms.add(PosixFilePermission.OWNER_EXECUTE);
                perms.add(PosixFilePermission.OWNER_WRITE);
                Files.setPosixFilePermissions(Paths.get(shortcutPath), perms);
            }
            if (getProperty("os.name").contains("Windows"))
                try {
                    ShellLink.createLink(applyWorkingDirectory("./BackupTool.jar"), System
                            .getenv("APPDATA")
                            + "/Microsoft/Windows/Start Menu/Programs/Save Game Backup Tool.lnk");
                } catch (Exception e) {
                    ShellLink.createLink(applyWorkingDirectory("./BackupTool.jar"),
                            getProperty("user.home")
                                    + "/Start Menu/Programs/Save Game Backup Tool.lnk");
                }
        }
        setBackupThreads(new ArrayList<BackupThread>());
        setConfigs(Arrays.asList(masterConfig.getConfigs()));
        setConfigsUsed(new ArrayList<BackupConfig>());
        for (BackupConfig config : getConfigs())
            config.setUUID(UUID.randomUUID());
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
            setStopQueue(new ArrayList<UUID>());
            if (configFile == null) {
                boolean continueRunning = true;
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter in \"help\" or \"?\" for assistance.");
                while (continueRunning) {
                    System.out.print(PROMPT);
                    String choice = input.readLine();
                    switch (choice.toLowerCase()) {
                        case "start": {
                            BackupConfig config =
                                    addOrRemoveConfig(input, configFile, getConfigs());
                            if (getConfigsUsed().contains(config))
                                System.out.println("That configuration is already in use");
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
                                System.out.println("That configuration was not in use.");
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
                            System.out.println(
                                    "Enter in \"start\" to initialize a backup configuration.\n"
                                            + "Enter in \"stop\" to suspend a backup configuration.\n"
                                            + "Enter in \"end\", \"exit\", or \"quit\" to shut down this tool.");
                            break;
                        case "":
                            break;
                        default:
                            System.out.println("Invalid command");
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

    public static void main(String args[]) {
        BackupTool backupTool = new BackupTool(args);
    }

    public BackupConfig addOrRemoveConfig(BufferedReader input, String configFile,
            List<BackupConfig> configs) throws IOException {
        BackupConfig config = null;
        if (configFile == null) {
            System.out.println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++)
                System.out.println("    " + i + ": " + configs.get(i).getTitle());
            String choice = null;
            while (choice == null) {
                System.out.print("Enter in an option number here: ");
                choice = input.readLine();
                try {
                    if (Integer.parseInt(choice) >= configs.size()
                            || Integer.parseInt(choice) < 0) {
                        System.out.println("Not a valid option number. Try again.");
                        choice = null;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input value. Try again with a numeric value.");
                    choice = null;
                }
            }
            config = configs.get(Integer.parseInt(choice));
        }
        return config;
    }
}
