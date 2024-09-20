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
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.UUID;
import static com.andrewnmitchell.savegamebackuptool.BackupThread.*;

public class BackupGUI extends JFrame {
    private final String DISABLED_LABEL = "Start", ENABLED_LABEL = "Stop";
    private final int FRAME_HEIGHT = 384, FRAME_WIDTH = 512;
    private BackupToolBase backupTool;
    private JButton[] buttons;
    private double interval;
    private JScrollPane scrollPane, textScrollPane;
    private BackupGUI self = this;
    private JTable table;
    private JTextArea textArea;

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private boolean isPushed;
        private String label;

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
            setBackground(UIManager.getColor("Button.background"));
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    public BackupGUI(List<BackupConfig> configs, double interval) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException
                | UnsupportedLookAndFeelException e) {
        }
        for (String key : new String[] {"Button.font", "Label.font", "TextArea.font"})
            UIManager.put(key,
                    UIManager.getLookAndFeel().getDefaults().getFont(key).deriveFont((float) 12));
        backupTool = new BackupToolBase();
        backupTool.setBackupThreads(new ArrayList<BackupThread>());
        backupTool.setConfigs(configs);
        backupTool.setConfigsUsed(new ArrayList<BackupConfig>());
        backupTool.setStopQueue(new ArrayList<UUID>());
        this.interval = interval;
        initButtons();
        initComponents();
        setTitle("Save Game Backup Tool");
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setLocationRelativeTo(null);
        setVisible(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                removeAllConfigs(backupTool, self);
                setVisible(false);
                dispose();
            }
        });
    }

    public void addToTextArea(String text) {
        textArea.append((textArea.getText().isEmpty() ? "" : "\n") + text);
        textArea.getCaret().setDot(Integer.MAX_VALUE);
    }

    public void drawTable(DefaultTableModel tableModel) {
        Object[][] rows = new Object[backupTool.getConfigs().size()][2];
        for (int i = 0; i < backupTool.getConfigs().size(); i++) {
            rows[i][0] = backupTool.getConfigs().get(i).getTitle();
            rows[i][1] = buttons[i].getText();
        }
        tableModel.setDataVector(rows, new Object[] {"configs", "buttons"});
        table.getColumn("buttons").setCellRenderer(new ButtonRenderer());
        table.getColumn("buttons").setCellEditor(new ButtonEditor(new JCheckBox()));
        // Make the titles of the backup configurations unselectable and uneditable
        table.getColumn("configs").setCellRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                return new JLabel(value.toString());
            }
        });
        table.getColumn("configs").setCellEditor(new TableCellEditor() {
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
        table.setBackground(UIManager.getColor("Panel.background"));
    }

    public void initButtons() {
        buttons = new JButton[backupTool.getConfigs().size()];
        for (int i = 0; i < buttons.length; i++)
            buttons[i] = new JButton(DISABLED_LABEL);
    }

    public void initComponents() {
        DefaultTableModel tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        drawTable(tableModel);
        scrollPane = new JScrollPane();
        scrollPane.setViewportView(table);
        add(scrollPane, BorderLayout.CENTER);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        textArea = new JTextArea();
        textArea.setColumns(20);
        textArea.setRows(5);
        textArea.setEditable(false);
        textScrollPane = new JScrollPane();
        textScrollPane.setViewportView(textArea);
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 1, Short.MAX_VALUE)
                .addComponent(textScrollPane, GroupLayout.Alignment.TRAILING));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(scrollPane, GroupLayout.PREFERRED_SIZE, 1,
                                        Short.MAX_VALUE)
                                .addComponent(textScrollPane, GroupLayout.PREFERRED_SIZE, 1,
                                        Short.MAX_VALUE)));
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
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(scrollPane, maxTableHeight, maxTableHeight,
                                        maxTableHeight)
                                .addComponent(textScrollPane, GroupLayout.PREFERRED_SIZE, 1,
                                        Short.MAX_VALUE)));
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
