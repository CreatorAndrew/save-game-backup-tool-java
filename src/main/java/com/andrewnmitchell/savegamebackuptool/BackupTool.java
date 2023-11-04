package com.andrewnmitchell.savegamebackuptool;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import com.google.gson.stream.JsonReader;

class BackupConfig {
    private String name, path;

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

public class BackupTool {
    private static ArrayList<BackupThread> backupThreads;
    private static ArrayList<BackupConfig> configs;
    private static ArrayList<String> configsUsed;

    private static class BackupThread extends Thread {
        private String configPath;
        private int configIndex;
        private BackupGUI gui;
        private boolean disabled = false, usePrompt;

        public BackupThread(BackupGUI gui) {
            this.gui = gui;
            usePrompt = false;
        }

        public BackupThread(String path, int index, boolean usePrompt) {
            configPath = path;
            configIndex = index;
            this.usePrompt = usePrompt;
        }

        public void disable() {
            disabled = true;
        }

        public void run() {
            if (gui != null) watchdog(gui);
            else if (configPath != null) watchdog(configPath, configIndex);
        }

        public void watchdog(String configPath, int configIndex) {
            String stopFilePath = "./.stop" + configPath.substring(configPath.lastIndexOf("/") + 1).replace(".json", "");
            while (Files.notExists(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePath))) && !disabled)
                try {
                    if (BackupWatchdog.watchdog(configPath, configIndex, usePrompt)) break;
                } catch (IOException exception) {
                }
            while (Files.exists(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePath))))
                try {
                    Files.delete(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePath)));
                // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
                } catch (IOException exception) {
                }
            removeConfig(configPath);
        }

        public void watchdog(BackupGUI gui) {
            String[] stopFilePaths = new String[gui.configs.size()];
            for (int i = 0; i < stopFilePaths.length; i++)
                stopFilePaths[i] = "./.stop" + gui.configs.get(i).getPath().substring(gui.configs.get(i).getPath().lastIndexOf("/") + 1).replace(".json", "");
            while (!disabled) for (BackupConfig config : gui.configs)
                try {
                    // For some reason on Windows, without the following line, the program will be stuck.
                    System.out.print("");
                    if (BackupWatchdog.watchdog(config.getPath(), gui.textArea, gui.configs.indexOf(config),
                                                usePrompt, gui.configsUsed[gui.configs.indexOf(config)])) {
                        gui.configsUsedInvalid[gui.configs.indexOf(config)] = true;
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException exception) {
                        }
                    }
                    if (Files.exists(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePaths[gui.configs.indexOf(config)])))) {
                        for (int i = 0; i < gui.buttons.length; i++) gui.buttons[i].setText(gui.configsUsed[i] ? gui.disableLabel : gui.enableLabel);
                        gui.configsUsed[gui.configs.indexOf(config)] = false;
                        gui.buttons[gui.configs.indexOf(config)].setText(gui.enableLabel);
                        gui.updateTable();
                    }
                    while (Files.exists(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePaths[gui.configs.indexOf(config)]))))
                        try {
                            Files.delete(Path.of(BackupWatchdog.replaceLocalDotDirectory(stopFilePaths[gui.configs.indexOf(config)])));
                        // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
                        } catch (IOException exception) {
                        }
                // On Windows, when a stop file is created, it cannot be immediately deleted by Java as it is briefly taken up by another process.
                } catch (IOException exception) {
                }
        }
    }

    public BackupTool(BackupGUI gui) {
        Thread backupThread = new Thread(new BackupThread(gui));
        backupThread.start();
    }

    public static void main(String args[]) throws IOException{
        backupThreads = new ArrayList<BackupThread>();
        configs = new ArrayList<BackupConfig>();
        configsUsed = new ArrayList<String>();
        String configPath = "", defaultConfigName = "", masterConfigFile = BackupWatchdog.replaceLocalDotDirectory("./MasterConfig.json");

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
                configPath = args[i + 1];
                break;
            }

        if (noGUI) {
            boolean stopBackupTool = false;
            Scanner scanner = new Scanner(System.in);
            if (!configPath.equals("")) {
                backupThreads.add(new BackupThread(configPath, backupThreads.size(), false));
                backupThreads.get(backupThreads.size() - 1).start();
            } else System.out.print("Enter in \"help\" or \"?\" for assistance.\n" + BackupWatchdog.prompt);
            while (configPath.equals("")) {
                String input = scanner.nextLine();
                switch (input.toLowerCase()) {
                    case "start": {
                        String config = addOrRemoveConfig(scanner, configPath, configs);
                        if (!configsUsed.contains(config)) {
                            configsUsed.add(config);
                            backupThreads.add(new BackupThread(configsUsed.get(configsUsed.size() - 1), backupThreads.size(), true));
                            backupThreads.get(backupThreads.size() - 1).start();
                        } else System.out.println("That configuration is already in use.");
                        break;
                    }
                    case "stop": {
                        String config = addOrRemoveConfig(scanner, configPath, configs);
                        removeConfig(config);
                        break;
                    }
                    case "end":
                    case "quit":
                    case "exit": {
                        for (BackupThread backupThread : backupThreads) backupThread.disable();
                        backupThreads = new ArrayList<BackupThread>();
                        configsUsed = new ArrayList<String>();
                        stopBackupTool = true;
                        break;
                    }
                    case "?":
                    case "help":
                        System.out.print("Enter in \"start\" to initialize a backup configuration.\n" +
                                         "Enter in \"stop\" to suspend a backup configuration.\n" +
                                         "Enter in \"exit\", \"quit\", or \"end\" to shut down this tool.\n" + BackupWatchdog.prompt);
                        break;
                    case "": System.out.print(BackupWatchdog.prompt); break;
                    default: System.out.print("Invalid command\n" + BackupWatchdog.prompt); break;
                }
                if (stopBackupTool) break;
            }
            scanner.close();
        } else {
            BackupGUI gui = new BackupGUI(configs);
        }
    }

    public static void removeConfig(String config) {
        if (configsUsed.contains(config)) {
            backupThreads.get(configsUsed.indexOf(config)).disable();
            backupThreads.remove(configsUsed.indexOf(config));
            configsUsed.remove(configsUsed.indexOf(config));
        } else System.out.println("That configuration was not in use.");
    }

    public static String addOrRemoveConfig(Scanner input, String configPath, ArrayList<BackupConfig> configs) {
        if (configPath.equals("")) {
            System.out.println("Select one of the following configurations:");
            for (int i = 0; i < configs.size(); i++) System.out.print((i + ": " + configs.get(i).getName()).indent(4));
            String choice = "";
            while (choice.equals("")) {
                System.out.print("Enter in an option number here: ");
                choice = input.next();
                try {
                    if (Integer.parseInt(choice) >= configs.size() || Integer.parseInt(choice) < 0) {
                        System.out.println("Not a valid option number. Try again.");
                        choice = "";
                    }
                }
                catch (NumberFormatException exception) {
                    System.out.println("Invalid input value. Try again with a numeric value.");
                    choice = "";
                }
            }
            configPath = configs.get(Integer.parseInt(choice)).getPath();
        }
        return configPath;
    }
}
