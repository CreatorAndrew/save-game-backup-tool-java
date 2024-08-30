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
import java.util.List;
import java.util.UUID;

public class BackupGUI extends JFrame {
    private final String enableLabel = "Start", disableLabel = "Stop";
    private final int width = 512, height = 384;
    private BackupGUI self = this;
    private JScrollPane scrollPane, textScrollPane;
    private JTable table;
    private JTextArea textArea;
    private JButton[] buttons;
    private double interval;
    private BackupToolBase backupTool;

    public BackupGUI(List<BackupConfig> configs, double interval) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
        }

        backupTool = new BackupToolBase();
        backupTool.backupThreads = new ArrayList<BackupThread>();
        backupTool.configs = configs;
        backupTool.configsUsed = new ArrayList<BackupConfig>();
        backupTool.stopQueue = new ArrayList<UUID>();
        this.interval = interval;

        initButtons();
        initComponents();

        setTitle("Save Game Backup Tool");
        setMinimumSize(new Dimension(width, height));
        setLocationRelativeTo(null);

        setVisible(true);
    }

    public void drawTable(DefaultTableModel tableModel) {
        Object[][] rows = new Object[backupTool.configs.size()][2];
        for (int i = 0; i < backupTool.configs.size(); i++) {
            rows[i][0] = backupTool.configs.get(i).getName();
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

    public void resetButton(BackupConfig config) {
        for (int i = 0; i < buttons.length; i++) buttons[i].setText(backupTool.configsUsed.contains(backupTool.configs.get(i)) ? disableLabel : enableLabel);
        buttons[backupTool.configs.indexOf(config)].setText(enableLabel);
        updateTable();
        removeConfig(config);
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
        buttons = new JButton[backupTool.configs.size()];
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
            if (!backupTool.configsUsed.contains(backupTool.configs.get(row))) BackupThread.addConfig(backupTool, backupTool.configs.get(row), interval, self);
            else removeConfig(backupTool.configs.get(row));
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

    public void addToTextArea(String text) {
        textArea.append((textArea.getText().isEmpty() ? "" : "\n") + text);
        textArea.getCaret().setDot(Integer.MAX_VALUE);
    }

    public void removeConfig(BackupConfig config) {
        BackupThread.removeConfig(backupTool, config, true);
    }
}
