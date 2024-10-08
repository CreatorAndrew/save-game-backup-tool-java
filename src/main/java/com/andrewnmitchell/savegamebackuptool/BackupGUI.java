package com.andrewnmitchell.savegamebackuptool;

import javax.swing.DefaultCellEditor;
import javax.swing.event.CellEditorListener;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import static com.andrewnmitchell.savegamebackuptool.BackupThread.*;
import static com.andrewnmitchell.savegamebackuptool.BackupUtils.*;
import static java.awt.SystemTray.*;
import static java.awt.Window.Type.*;
import static java.lang.Integer.*;
import static javax.imageio.ImageIO.*;
import static javax.swing.GroupLayout.Alignment.*;
import static javax.swing.UIManager.*;

public class BackupGUI extends JFrame {
    private final String DISABLED_LABEL = "Start", ENABLED_LABEL = "Stop", EXIT_LABEL = "Exit",
            HIDDEN_LABEL = "Show", SHOWN_LABEL = "Hide", TITLE = "Save Game Backup Tool";
    private final int FRAME_HEIGHT = 384, FRAME_WIDTH = 512;
    private BackupToolBase backupTool;
    private JButton[] buttons;
    private BufferedImage icon;
    private double interval;
    private JFrame invisibleWindow;
    private JScrollPane scrollPane, textScrollPane;
    private BackupGUI self = this;
    private SystemTray systemTray = getSystemTray();
    private JTable table;
    private JTextArea textArea;
    private JMenuItem toggleShownItem;
    private TrayIcon trayIcon;
    private JPopupMenu trayMenu;

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private boolean isPushed;
        private String label;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
                int row, int column) {
            if (backupTool.getConfigsUsed().contains(backupTool.getConfigs().get(row)))
                removeConfig(backupTool, backupTool.getConfigs().get(row));
            else
                addConfig(backupTool, backupTool.getConfigs().get(row), interval, self);
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed)
                label = label.equals(DISABLED_LABEL) ? ENABLED_LABEL : DISABLED_LABEL;
            isPushed = false;
            return new String(label);
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        protected void fireEditingStopped() {
            super.fireEditingStopped();
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {}

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(getColor("Button.background"));
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    public BackupGUI(List<BackupConfig> configs, boolean hideOnClose, double interval,
            boolean startHidden) {
        try {
            setLookAndFeel(getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                | UnsupportedLookAndFeelException e) {
        }
        for (String key : new String[] {"Button", "Label", "MenuItem", "TextArea"})
            put(key + ".font",
                    getLookAndFeel().getDefaults().getFont(key + ".font").deriveFont(12f));
        try {
            icon = read(new File(applyWorkingDirectory("./BackupTool.png")));
        } catch (IOException e) {
        }
        backupTool = new BackupToolBase();
        backupTool.setBackupThreads(new ArrayList<>());
        backupTool.setConfigs(configs);
        backupTool.setConfigsUsed(new ArrayList<>());
        backupTool.setStopQueue(new ArrayList<>());
        this.interval = interval;
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                toggleShownItem.setText(HIDDEN_LABEL);
                if (hideOnClose)
                    setVisible(false);
                else
                    exit();
            }
        });
        setIconImage(icon);
        buttons = new JButton[backupTool.getConfigs().size()];
        for (int i = 0; i < buttons.length; i++)
            buttons[i] = new JButton(DISABLED_LABEL);
        DefaultTableModel tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        drawTable(tableModel);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
        add(scrollPane, "Center");
        setDefaultCloseOperation(0);
        textArea = new JTextArea();
        textArea.setColumns(20);
        textArea.setRows(5);
        textArea.setEditable(false);
        textScrollPane = new JScrollPane();
        textScrollPane.setViewportView(textArea);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
                .addComponent(scrollPane, -2, 1, MAX_VALUE).addComponent(textScrollPane, TRAILING));
        layout.setVerticalGroup(layout.createParallelGroup(LEADING).addGroup(TRAILING,
                layout.createSequentialGroup().addComponent(scrollPane, -2, 1, MAX_VALUE)
                        .addComponent(textScrollPane, -2, 1, MAX_VALUE)));
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        pack();
        double cellHeight;
        try {
            cellHeight = (double) table.getHeight() / backupTool.getConfigs().size();
        } catch (ArithmeticException e) {
            cellHeight = .0;
        }
        int maxTableHeight = (table.getHeight() > (int) (cellHeight * 5) ? (int) (cellHeight * 5)
                : table.getHeight()) + 5;
        layout.setVerticalGroup(layout.createParallelGroup(LEADING).addGroup(TRAILING,
                layout.createSequentialGroup()
                        .addComponent(scrollPane, maxTableHeight, maxTableHeight, maxTableHeight)
                        .addComponent(textScrollPane, -2, 1, MAX_VALUE)));
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setTitle(TITLE);
        invisibleWindow = new JFrame();
        invisibleWindow.setType(UTILITY);
        invisibleWindow.setUndecorated(true);
        invisibleWindow.setOpacity(0f);
        invisibleWindow.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {}

            @Override
            public void focusLost(FocusEvent e) {
                if (trayMenu.isVisible())
                    trayMenu.setVisible(false);
                invisibleWindow.setVisible(false);
            }
        });
        setVisible(!startHidden);
        setLocationRelativeTo(null);
        trayMenu = new JPopupMenu();
        JMenuItem exitItem = new JMenuItem(EXIT_LABEL);
        toggleShownItem = new JMenuItem(isVisible() ? SHOWN_LABEL : HIDDEN_LABEL);
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trayMenu.setVisible(false);
                exit();
            }
        });
        toggleShownItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trayMenu.setVisible(false);
                toggleShownItem.setText(isVisible() ? HIDDEN_LABEL : SHOWN_LABEL);
                setVisible(!isVisible());
            }
        });
        trayMenu.add(toggleShownItem);
        trayMenu.add(exitItem);
        trayIcon = new TrayIcon(icon, TITLE);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                switch (e.getButton()) {
                    case 1: {
                        toggleShownItem.setText(isVisible() ? HIDDEN_LABEL : SHOWN_LABEL);
                        setVisible(!isVisible());
                        break;
                    }
                    case 3: {
                        invisibleWindow.setVisible(true);
                        trayMenu.setLocation(e.getX(), e.getY() - 50);
                        trayMenu.setVisible(true);
                        break;
                    }
                }
            }
        });
        trayIcon.setImageAutoSize(true);
        try {
            systemTray.add(trayIcon);
        } catch (AWTException awtException) {
        }
    }

    public void addToTextArea(String text) {
        textArea.append((textArea.getText().isEmpty() ? "" : "\n") + text);
        textArea.getCaret().setDot(MAX_VALUE);
    }

    public void drawTable(DefaultTableModel tableModel) {
        Object[][] rows = new Object[buttons.length][2];
        for (int i = 0; i < buttons.length; i++) {
            rows[i][0] = backupTool.getConfigs().get(i).getTitle();
            rows[i][1] = buttons[i].getText();
        }
        tableModel.setDataVector(rows, new Object[] {"configs", "buttons"});
        table.getColumn("buttons").setCellRenderer(new ButtonRenderer());
        table.getColumn("buttons").setCellEditor(new ButtonEditor(new JCheckBox()));
        table.getColumn("configs").setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                return new JLabel(value.toString());
            }
        });
        // Make the titles of the backup configurations unselectable and uneditable
        table.getColumn("configs").setCellEditor(new TableCellEditor() {
            String label = "";

            @Override
            public boolean shouldSelectCell(EventObject e) {
                return false;
            }

            @Override
            public Object getCellEditorValue() {
                return new String(label);
            }

            @Override
            public boolean isCellEditable(EventObject e) {
                return false;
            }

            @Override
            public boolean stopCellEditing() {
                return true;
            }

            @Override
            public void cancelCellEditing() {}

            @Override
            public void addCellEditorListener(CellEditorListener l) {}

            @Override
            public void removeCellEditorListener(CellEditorListener l) {}

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                label = value.toString();
                return new JLabel(label);
            }
        });
        table.setDefaultRenderer(JButton.class, new ButtonRenderer());
        table.getTableHeader().setUI(null);
        table.setBackground(getColor("Panel.background"));
    }

    public void exit() {
        systemTray.remove(trayIcon);
        removeAllConfigs(backupTool, self);
        setVisible(false);
        invisibleWindow.dispose();
        dispose();
    }

    public void resetButton(BackupConfig config) {
        for (int i = 0; i < buttons.length; i++)
            buttons[i].setText(backupTool.getConfigsUsed().contains(backupTool.getConfigs().get(i))
                    ? ENABLED_LABEL
                    : DISABLED_LABEL);
        buttons[backupTool.getConfigs().indexOf(config)].setText(DISABLED_LABEL);
        updateTable();
        removeConfig(backupTool, config);
    }

    public void updateTable() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        drawTable(tableModel);
    }
}
