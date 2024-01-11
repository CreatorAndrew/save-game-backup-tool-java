package com.andrewnmitchell.savegamebackuptool;
import com.google.gson.stream.JsonReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class BackupTool {
    private ArrayList<BackupThread> backupThreads;
    private ArrayList<BackupConfig> configs, configsUsed;
    private ArrayList<String> stopQueue;

    public BackupTool(String args[]) {
        try {
            run(args);
        } catch (IOException exception) {
        }
    }

    public void run(String args[]) throws IOException {
        backupThreads = new ArrayList<BackupThread>();
        configs = new ArrayList<BackupConfig>();
        configsUsed = new ArrayList<BackupConfig>();
        String configPath = "", defaultConfigName = "", masterConfigFile = BackupWatchdog.replaceLocalDotDirectory("./MasterConfig.json");
        double interval = 0;

        JsonReader reader = new JsonReader(new FileReader(masterConfigFile));
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "configurations": {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        BackupConfig props = new BackupConfig();
                        reader.beginObject();
                        while (reader.hasNext()) {
                            name = reader.nextName();
                            switch (name) {
                                case "name": props.setName(reader.nextString()); break;
                                case "file": props.setPath(reader.nextString()); break;
                            }
                        }
                        reader.endObject();
                        configs.add(props);
                    }
                    reader.endArray();
                    break;
                }
                case "default": defaultConfigName = reader.nextString(); break;
                case "interval": interval = reader.nextDouble(); break;
            }
        }
        reader.endObject();

        boolean skipChoice = false, noGUI = false;
        for (int i = 0; i < args.length; i++)
            switch (args[i].toLowerCase()) {
                case "--no-gui": noGUI = true; break;
                case "--skip-choice": skipChoice = true; break;
            }

        for (int i = 0; i < configs.size() && skipChoice; i++)
            if (configs.get(i).getName().equals(defaultConfigName)) configPath = configs.get(i).getPath();

        for (int i = 0; i < args.length && args.length > 1 && !skipChoice; i++)
            if (args[i].toLowerCase().equals("--config") && i < args.length - 1) {
                configPath = args[i + 1].replace(".json", "") + ".json";
                break;
            }

        if (noGUI) {
            stopQueue = new ArrayList<String>();
            boolean stopBackupTool = false;
            Scanner scanner = new Scanner(System.in);
            if (!configPath.equals("")) {
                backupThreads.add(new BackupThread(new BackupConfig(defaultConfigName, configPath), stopQueue, interval, false, this));
                backupThreads.get(backupThreads.size() - 1).start();
            } else System.out.print("Enter in \"help\" or \"?\" for assistance.\n" + BackupWatchdog.prompt);
            while (configPath.equals("")) {
                String input = scanner.nextLine();
                switch (input.toLowerCase()) {
                    case "start": {
                        BackupConfig config = addOrRemoveConfig(scanner, configPath, configs);
                        if (!configsUsed.contains(config)) {
                            configsUsed.add(config);
                            backupThreads.add(new BackupThread(configsUsed.get(configsUsed.size() - 1), stopQueue, interval, true, this));
                            backupThreads.get(backupThreads.size() - 1).start();
                        } else System.out.println("That configuration is already in use.");
                        break;
                    }
                    case "stop": {
                        BackupConfig config = addOrRemoveConfig(scanner, configPath, configs);
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
                        System.out.print("Enter in \"start\" to initialize a backup configuration.\n" +
                                         "Enter in \"stop\" to suspend a backup configuration.\n" +
                                         "Enter in \"end\", \"exit\", or \"quit\" to shut down this tool.\n" + BackupWatchdog.prompt);
                        break;
                    case "": System.out.print(BackupWatchdog.prompt); break;
                    default: System.out.print("Invalid command\n" + BackupWatchdog.prompt); break;
                }
                if (stopBackupTool) break;
            }
            scanner.close();
        } else {
            BackupGUI gui = new BackupGUI(configs, interval);
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

    public BackupConfig addOrRemoveConfig(Scanner input, String configPath, ArrayList<BackupConfig> configs) {
        BackupConfig config = null;
        if (configPath.equals("")) {
            System.out.println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++) System.out.println("    " + i + ": " + configs.get(i).getName());
            String choice = "";
            while (choice.equals("")) {
                System.out.print("Enter in an option number here: ");
                choice = input.next();
                try {
                    if (Integer.parseInt(choice) >= configs.size() || Integer.parseInt(choice) < 0) {
                        System.out.println("Not a valid option number. Try again.");
                        choice = "";
                    }
                } catch (NumberFormatException exception) {
                    System.out.println("Invalid input value. Try again with a numeric value.");
                    choice = "";
                }
            }
            config = configs.get(Integer.parseInt(choice));
        }
        return config;
    }
}
