package com.andrewnmitchell.savegamebackuptool;
import javax.swing.DefaultCellEditor;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.awt.event.ActionListener;

public class BackupGUI extends JFrame {
    private JScrollPane scrollPane, textScrollPane;
    private JTable table;
    protected JTextArea textArea;
    protected JButton[] buttons;
    protected final String enableLabel = "Start", disableLabel = "Stop";
    protected final int width = 512, height = 384;
    protected ArrayList<BackupConfig> configs;
    protected boolean[] configsUsed;

    public BackupGUI(ArrayList<BackupConfig> configs, double interval) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException exception) {
        }

        this.configs = configs;
        configsUsed = new boolean[configs.size()];
        for (int i = 0; i < configsUsed.length; i++) configsUsed[i] = false;

        initButtons();
        initComponents();

        setTitle("Save Game Backup Tool");
        setMinimumSize(new Dimension(width, height));
        setLocationRelativeTo(null);

        setVisible(true);

        BackupTool backupTool = new BackupTool(this, interval);
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
            if (isSelected) setBackground(table.getBackground());
            else setBackground(UIManager.getColor("Button.background"));
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
            label = (value == null) ? "" : value.toString();
            configsUsed[row] = label.equals(enableLabel);
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
