package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.annotations.SerializedName;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class MasterConfig {
    private BackupConfig[] configurations;
    @SerializedName("default")
    private String defaultConfigName;
    private Double interval;

    public BackupConfig[] getConfigurations() {
        return configurations;
    }

    public String getDefaultConfigName() {
        return defaultConfigName;
    }

    public double getInterval() {
        if (interval == null) return 0;
        return interval;
    }
}

public class BackupTool {
    private List<BackupThread> backupThreads;
    private List<BackupConfig> configs, configsUsed;
    private List<String> stopQueue;

    public BackupTool(String args[]) {
        try {
            run(args);
        } catch (IOException e) {
        }
    }

    public void run(String args[]) throws IOException {
        MasterConfig masterConfig = (new Gson()).fromJson(
            new FileReader(BackupWatchdog.replaceLocalDotDirectory("./MasterConfig.json")), MasterConfig.class
        );

        backupThreads = new ArrayList<BackupThread>();
        configs = Arrays.asList(masterConfig.getConfigurations());
        configsUsed = new ArrayList<BackupConfig>();

        String configPath = "";
        boolean skipChoice = false, noGUI = false;
        for (int i = 0; i < args.length; i++)
            switch (args[i].toLowerCase()) {
                case "--no-gui": noGUI = true; break;
                case "--skip-choice": skipChoice = true; break;
            }

        for (int i = 0; i < configs.size() && skipChoice; i++)
            if (configs.get(i).getName().equals(masterConfig.getDefaultConfigName())) configPath = configs.get(i).getPath();

        for (int i = 0; i < args.length && args.length > 1 && !skipChoice; i++)
            if (args[i].toLowerCase().equals("--config") && i < args.length - 1) {
                configPath = args[i + 1].replace(".json", "") + ".json";
                break;
            }

        if (noGUI) {
            stopQueue = new ArrayList<String>();
            boolean stopBackupTool = false;
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            if (!configPath.equals("")) {
                backupThreads.add(new BackupThread(
                    new BackupConfig(masterConfig.getDefaultConfigName(), configPath), stopQueue, masterConfig.getInterval(), false, this
                ));
                backupThreads.get(backupThreads.size() - 1).start();
            } else System.out.println("Enter in \"help\" or \"?\" for assistance.");
            while (configPath.equals("")) {
                System.out.print(BackupWatchdog.prompt);
                String choice = input.readLine();
                switch (choice.toLowerCase()) {
                    case "start": {
                        BackupConfig config = addOrRemoveConfig(input, configPath, configs);
                        if (!configsUsed.contains(config)) {
                            configsUsed.add(config);
                            backupThreads.add(new BackupThread(
                                configsUsed.get(configsUsed.size() - 1), stopQueue, masterConfig.getInterval(), true, this
                            ));
                            backupThreads.get(backupThreads.size() - 1).start();
                        } else System.out.println("That configuration is already in use");
                        break;
                    }
                    case "stop": {
                        BackupConfig config = addOrRemoveConfig(input, configPath, configs);
                        if (!configsUsed.contains(config)) System.out.println("That configuration was not in use.");
                        removeConfig(config);
                        break;
                    }
                    case "end":
                    case "exit":
                    case "quit": {
                        for (BackupConfig config : configsUsed) {
                            stopQueue.add(config.getName());
                            while (backupThreads.get(configsUsed.indexOf(config)).getEnabled()) System.out.print("");
                        }
                        backupThreads = new ArrayList<BackupThread>();
                        configsUsed = new ArrayList<BackupConfig>();
                        stopQueue = new ArrayList<String>();
                        stopBackupTool = true;
                        break;
                    }
                    case "help":
                    case "?":
                        System.out.println(
                            "Enter in \"start\" to initialize a backup configuration.\nEnter in \"stop\" to suspend a backup configuration.\n" +
                            "Enter in \"end\", \"exit\", or \"quit\" to shut down this tool."
                        );
                        break;
                    case "": break;
                    default: System.out.println("Invalid command"); break;
                }
                if (stopBackupTool) break;
            }
            input.close();
        } else {
            BackupGUI gui = new BackupGUI(configs, masterConfig.getInterval());
        }
    }

    public static void main(String args[]) {
        BackupTool backupTool = new BackupTool(args);
    }

    public void removeConfig(BackupConfig config, boolean wait) {
        if (configsUsed.contains(config)) {
            stopQueue.add(configsUsed.get(configsUsed.indexOf(config)).getName());
            while (wait && backupThreads.get(configsUsed.indexOf(config)).getEnabled()) System.out.print("");
            stopQueue.remove(configsUsed.indexOf(config));
            backupThreads.remove(configsUsed.indexOf(config));
            configsUsed.remove(configsUsed.indexOf(config));
        }
    }

    public void removeConfig(BackupConfig config) {
        removeConfig(config, true);
    }

    public BackupConfig addOrRemoveConfig(BufferedReader input, String configPath, List<BackupConfig> configs) throws IOException {
        BackupConfig config = null;
        if (configPath.equals("")) {
            System.out.println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++) System.out.println("    " + i + ": " + configs.get(i).getName());
            String choice = "";
            while (choice.equals("")) {
                System.out.print("Enter in an option number here: ");
                choice = input.readLine();
                try {
                    if (Integer.parseInt(choice) >= configs.size() || Integer.parseInt(choice) < 0) {
                        System.out.println("Not a valid option number. Try again.");
                        choice = "";
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input value. Try again with a numeric value.");
                    choice = "";
                }
            }
            config = configs.get(Integer.parseInt(choice));
        }
        return config;
    }
}
