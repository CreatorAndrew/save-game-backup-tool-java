package com.andrewnmitchell.savegamebackuptool;
import javax.swing.DefaultCellEditor;
import javax.swing.event.CellEditorListener;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EventObject;

public class BackupGUI extends JFrame {
    private BackupGUI self = this;
    private JScrollPane scrollPane, textScrollPane;
    private JTable table;
    private double interval;
    protected JTextArea textArea;
    protected JButton[] buttons;
    protected final String enableLabel = "Start", disableLabel = "Stop";
    protected final int width = 512, height = 384;
    protected ArrayList<BackupThread> backupThreads;
    protected ArrayList<BackupConfig> configs, configsUsed;
    protected ArrayList<String> stopQueue;

    public BackupGUI(ArrayList<BackupConfig> configs, double interval) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException exception) {
        }

        backupThreads = new ArrayList<BackupThread>();
        this.configs = configs;
        configsUsed = new ArrayList<BackupConfig>();
        stopQueue = new ArrayList<String>();
        this.interval = interval;

        initButtons();
        initComponents();

        setTitle("Save Game Backup Tool");
        setMinimumSize(new Dimension(width, height));
        setLocationRelativeTo(null);

        setVisible(true);
    }

    public void removeConfig(BackupConfig config) {
        if (configsUsed.contains(config)) {
            stopQueue.add(configsUsed.get(configsUsed.indexOf(config)).getName());
            while (!backupThreads.get(configsUsed.indexOf(config)).getDisabled()) System.out.print("");
            stopQueue.remove(stopQueue.indexOf(configsUsed.get(configsUsed.indexOf(config)).getName()));
            backupThreads.remove(configsUsed.indexOf(config));
            configsUsed.remove(configsUsed.indexOf(config));
        }
    }

    public void drawTable(DefaultTableModel tableModel) {
        Object[][] rows = new Object[configs.size()][2];
        for (int i = 0; i < configs.size(); i++) {
            rows[i][0] = configs.get(i).getName();
            rows[i][1] = buttons[i].getText();
        }
        tableModel.setDataVector(rows, new Object[] { "Configuration", "Button" });
        table.getColumn("Button").setCellRenderer(new ButtonRenderer());
        table.getColumn("Button").setCellEditor(new ButtonEditor(new JCheckBox()));
        // Make the titles of the backup configurations unselectable and uneditable
        table.getColumn("Configuration").setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                return new JLabel(value.toString());
            }
        });
        table.getColumn("Configuration").setCellEditor(new TableCellEditor() {
            String label = "";

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                return false;
            }

            @Override
            public Object getCellEditorValue() {
                return new String(label);
            }

            @Override
            public boolean isCellEditable(EventObject anEvent) {
                return false;
            }

            @Override
            public boolean stopCellEditing() {
                return true;
            }

            @Override
            public void cancelCellEditing() {
            }

            @Override
            public void addCellEditorListener(CellEditorListener l) {
            }

            @Override
            public void removeCellEditorListener(CellEditorListener l) {
            }

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                label = value.toString();
                return new JLabel(label);
            }            
        });
        table.setDefaultRenderer(JButton.class, new ButtonRenderer());
        table.getTableHeader().setUI(null);
    }

    public void updateTable() {
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        drawTable(tableModel);
    }

    public void initComponents() {
        DefaultTableModel tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        drawTable(tableModel);

        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table); 
        add(scrollPane, BorderLayout.CENTER); 

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        textArea = new JTextArea();
        textArea.setColumns(20);
        textArea.setRows(5);
        textArea.setEditable(false);

        textScrollPane = new JScrollPane();
        textScrollPane.setViewportView(textArea);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
            .addComponent(textScrollPane, GroupLayout.Alignment.TRAILING)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                .addComponent(textScrollPane, GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE))
        );

        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.getTableHeader().setReorderingAllowed(false);
        pack();
    }

    public void initButtons() {
        buttons = new JButton[configs.size()];
        for (int i = 0; i < buttons.length; i++) buttons[i] = new JButton(enableLabel);
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setBackground(UIManager.getColor("Button.background"));
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    fireEditingStopped();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (!configsUsed.contains(configs.get(row))) {
                configsUsed.add(configs.get(row));
                backupThreads.add(new BackupThread(configsUsed.get(configsUsed.size() - 1), stopQueue, interval, false, self));
                backupThreads.get(backupThreads.size() - 1).start();
            } else removeConfig(configs.get(row));
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) label = label.equals(enableLabel) ? disableLabel : enableLabel;
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
}
