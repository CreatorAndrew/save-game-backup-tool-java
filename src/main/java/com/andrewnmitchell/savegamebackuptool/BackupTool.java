package com.andrewnmitchell.savegamebackuptool;

import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class MasterConfig {
    private BackupConfig[] configurations;
    @SerializedName("default")
    private String defaultConfigName;
    private Double interval;

    public BackupConfig[] getConfigs() {
        return configurations;
    }

    public String getDefaultConfigName() {
        return defaultConfigName;
    }

    public double getInterval() {
        if (interval == null)
            return 0;
        return interval;
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
        FileReader reader =
                new FileReader(BackupWatchdog.applyWorkingDirectory("./MasterConfig.json"));
        MasterConfig masterConfig = (new Gson()).fromJson(reader, MasterConfig.class);
        reader.close();
        setBackupThreads(new ArrayList<BackupThread>());
        setConfigs(Arrays.asList(masterConfig.getConfigs()));
        setConfigsUsed(new ArrayList<BackupConfig>());
        for (BackupConfig config : getConfigs())
            config.setUUID(UUID.randomUUID());
        String configPath = null;
        boolean skipChoice = false, noGUI = false;
        for (int i = 0; i < args.length; i++)
            switch (args[i].toLowerCase()) {
                case "--no-gui":
                    noGUI = true;
                    break;
                case "--skip-choice":
                    skipChoice = true;
                    break;
            }
        if (skipChoice)
            configPath = masterConfig.getDefaultConfigName();
        for (int i = 0; i < args.length && args.length > 1 && !skipChoice; i++)
            if (args[i].toLowerCase().equals("--config") && i < args.length - 1) {
                for (String file : new File(BackupWatchdog.applyWorkingDirectory(".")).list())
                    if (file.toLowerCase().endsWith(".json") && file.toLowerCase()
                            .equals(args[i + 1].toLowerCase().replace(".json", "") + ".json")) {
                        configPath = file;
                        break;
                    }
                break;
            }
        if (noGUI) {
            setStopQueue(new ArrayList<UUID>());
            if (configPath == null) {
                boolean continueRunning = true;
                BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter in \"help\" or \"?\" for assistance.");
                while (continueRunning) {
                    System.out.print(BackupWatchdog.PROMPT);
                    String choice = input.readLine();
                    switch (choice.toLowerCase()) {
                        case "start": {
                            BackupConfig config =
                                    addOrRemoveConfig(input, configPath, getConfigs());
                            if (getConfigsUsed().contains(config))
                                System.out.println("That configuration is already in use");
                            else
                                BackupThread.addConfig(this, config, masterConfig.getInterval());
                            break;
                        }
                        case "stop": {
                            BackupConfig config =
                                    addOrRemoveConfig(input, configPath, getConfigs());
                            if (getConfigsUsed().contains(config))
                                BackupThread.removeConfig(this, config);
                            else
                                System.out.println("That configuration was not in use.");
                            break;
                        }
                        case "end":
                        case "exit":
                        case "quit": {
                            for (BackupConfig config : getConfigsUsed()) {
                                getStopQueue().add(config.getUUID());
                                while (getBackupThreads().get(getConfigsUsed().indexOf(config))
                                        .getEnabled())
                                    System.out.print("");
                            }
                            setBackupThreads(new ArrayList<BackupThread>());
                            setConfigsUsed(new ArrayList<BackupConfig>());
                            setStopQueue(new ArrayList<UUID>());
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
                        new BackupConfig(masterConfig.getDefaultConfigName(), configPath),
                        masterConfig.getInterval(), false, this));
                getBackupThreads().get(getBackupThreads().size() - 1).start();
            }
        } else {
            BackupGUI gui = new BackupGUI(getConfigs(), masterConfig.getInterval());
        }
    }

    public static void main(String args[]) {
        BackupTool backupTool = new BackupTool(args);
    }

    public BackupConfig addOrRemoveConfig(BufferedReader input, String configPath,
            List<BackupConfig> configs) throws IOException {
        BackupConfig config = null;
        if (configPath == null) {
            System.out.println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++)
                System.out.println("    " + i + ": " + configs.get(i).getName());
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
