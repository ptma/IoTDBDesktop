package org.apache.iotdb.desktop.form;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.desktop.component.ColumnTableModel;
import org.apache.iotdb.desktop.component.MetricsTableRendererProvider;
import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.exception.VerificationException;
import org.apache.iotdb.desktop.model.Column;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Table;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TableInfo extends TabPanel {

    private static final DefaultCellEditor DATATYPE_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(TSDataType.values()).map(TSDataType::name).toArray()));
    private static final DefaultCellEditor COLUMNTYPE_EDITOR = new DefaultCellEditor(new JComboBox<>(new String[]{"TAG", "ATTRIBUTE", "FIELD", "TIME"}));

    private JPanel rootPanel;
    private JTextField nameField;
    private JButton btnAdd;
    private JButton btnRemove;
    private JButton btnApply;
    private JXTable columnsTable;
    private JScrollPane columnsScrollPanel;
    private JLabel labelName;
    private JLabel labelColumns;
    private JTextField ttlField;
    private JLabel labelTTL;

    private ColumnTableModel columnTableModel;

    private final Table table;

    public TableInfo(Table table) {
        super();
        this.table = table;
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        initComponents();

        loadTableColumns();

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                columnsTable.requestFocusInWindow();
            }
        });
    }

    private void initComponents() {
        nameField.setText(table.getPath());
        ttlField.setText(table.getTtl());
        columnsTable.setRowHeight(24);
        columnsTable.setEditable(true);
        columnsTable.setShowHorizontalLines(true);
        columnsTable.setShowVerticalLines(true);
        columnsTable.setRowSelectionAllowed(true);
        columnsTable.setSortable(false);
        columnsTable.setIntercellSpacing(new Dimension(1, 1));
        columnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        columnTableModel = new ColumnTableModel();
        DefaultTableRenderer metricsTableRenderer = new DefaultTableRenderer(new MetricsTableRendererProvider());
        columnsTable.setModel(columnTableModel);
        columnsTable.getTableHeader().setReorderingAllowed(false);
        columnsTable.setDefaultRenderer(Object.class, metricsTableRenderer);
        columnsTable.setDefaultRenderer(String.class, metricsTableRenderer);
        columnsTable.setDefaultRenderer(Number.class, metricsTableRenderer);
        columnsTable.setDefaultRenderer(Boolean.class, metricsTableRenderer);
        columnsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final int rowIndex = columnsTable.rowAtPoint(e.getPoint());
                    final int colIndex = columnsTable.columnAtPoint(e.getPoint());
                    boolean onRow = rowIndex >= 0;
                    boolean onColumn = colIndex >= 0;
                    if (onRow && onColumn) {
                        int[] selRows = columnsTable.getSelectedRows();
                        if (Arrays.stream(selRows).noneMatch(i -> i == rowIndex)) {
                            columnsTable.setRowSelectionInterval(rowIndex, rowIndex);
                        }
                        columnsTable.setColumnSelectionInterval(colIndex, colIndex);
                    }
                }
            }
        });
        btnRemove.setEnabled(false);
        columnsTable.getSelectionModel().addListSelectionListener(e -> {
            btnRemove.setEnabled(table.modifiable() && columnsTable.getSelectedRowCount() > 0);
        });
        columnTableModel.addTableModelListener(e -> {
            btnApply.setEnabled(columnTableModel.isModified());
        });
        btnAdd.addActionListener(e -> {
            columnTableModel.appendRow();
        });
        btnRemove.addActionListener(e -> {
            int[] selRows = columnsTable.getSelectedRows();
            if (selRows.length > 0) {
                for (int i = selRows.length - 1; i >= 0; i--) {
                    int modelRowIndex = columnsTable.convertRowIndexToModel(selRows[i]);
                    columnTableModel.removeRow(modelRowIndex);
                }
            }
        });
        btnApply.addActionListener(e -> applayColumns());
        btnAdd.setIcon(Icons.ADD);
        btnRemove.setIcon(Icons.DELETE);

        labelName.setText(LangUtil.getString("TableName"));
        labelTTL.setText(LangUtil.getString("TTL"));
        labelColumns.setText(LangUtil.getString("TableColumns"));
        btnAdd.setText(LangUtil.getString("AddColumn"));
        btnRemove.setText(LangUtil.getString("RemoveColumn"));
        btnApply.setText(LangUtil.getString("Apply"));

        btnAdd.setEnabled(table.modifiable());
    }

    private void initTableCellEditors() {
        columnsTable.getColumn(1).setCellEditor(DATATYPE_EDITOR);
        columnsTable.getColumn(2).setCellEditor(COLUMNTYPE_EDITOR);
    }

    private void loadTableColumns() {
        columnTableModel.clear();

        SwingWorker<List<Column>, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    columnTableModel.setColumns(get());
                    initTableCellEditors();
                    Utils.autoResizeTableColumns(columnsTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected List<Column> doInBackground() throws Exception {
                return table.getSession().loadTableColumns(table.getPath());
            }
        };
        worker.execute();
    }

    private void verifyFields() throws VerificationException {
        for (Column column : columnTableModel.getColumns()) {
            if (column.getName() == null || column.getName().isBlank()) {
                throw new VerificationException(LangUtil.getString("ColumnNameIsEmpty"));
            } else if (column.getDataType() == null || column.getDataType().isBlank()) {
                throw new VerificationException(LangUtil.getString("ColumnDataTypeIsEmpty"));
            } else if (column.getCategory() == null || column.getCategory().isBlank()) {
                throw new VerificationException(LangUtil.getString("ColumnCategoryIsEmpty"));
            }
        }
        Optional<String> duplicateName = columnTableModel.getColumns()
            .stream()
            .collect(Collectors.groupingBy(Column::getName, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .findFirst();
        if (duplicateName.isPresent()) {
            throw new VerificationException(LangUtil.format("ColumnNameIsDuplicated", duplicateName.get()));
        }
    }

    private void applayColumns() {
        try {
            verifyFields();
        } catch (VerificationException e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }

        SwingWorker<Throwable, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Throwable e = get();
                    if (e != null) {
                        Utils.Message.error(e.getMessage(), e);
                    } else {
                        Utils.Toast.success(LangUtil.getString("ModifiedSuccess"));
                        loadTableColumns();
                    }
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Throwable doInBackground() {
                try {
                    for (Column column : columnTableModel.getColumns()) {
                        if (!column.isExisting()) {
                            // 添加新的序列
                            // ALTER TABLE (IF EXISTS)? tableName=qualifiedName ADD COLUMN (IF NOT EXISTS)? columnDefinition
                            // columnDefinition
                            //    : identifier type (TAG | ATTRIBUTE | TIME | FIELD)
                            String alertSql = "alter table if exists tablename=" + table.getName() + " add column if not exists ";
                            alertSql += column.getName() + " " + column.getDataType() + " " + column.getCategory();

                            table.getSession().execute(alertSql);
                        }
                    }
                    // 删除列,  alter table (if exists)? tablename=qualifiedname drop column (if exists)? identifier
                    if (!columnTableModel.getDroppedColumns().isEmpty()) {
                        for (Column column : columnTableModel.getDroppedColumns()) {
                            String alertSql = "alter table if exists tablename=" + table.getName() + " drop column if exists " + column.getName();
                            table.getSession().execute(alertSql);
                        }
                    }
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            }
        };
        worker.execute();
    }

    @Override
    public Session getSession() {
        return table.getSession();
    }

    @Override
    public String getTabbedKey() {
        return table.getKey();
    }

    @Override
    public boolean refreshable() {
        return true;
    }

    @Override
    public void refresh() {
        loadTableColumns();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:noGrow,fill:100px:noGrow,left:4dlu:noGrow,fill:199px:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,top:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        labelName = new JLabel();
        labelName.setHorizontalAlignment(11);
        labelName.setText("表名");
        CellConstraints cc = new CellConstraints();
        rootPanel.add(labelName, cc.xy(1, 1));
        nameField = new JTextField();
        nameField.setEditable(false);
        nameField.setEnabled(true);
        rootPanel.add(nameField, cc.xyw(3, 1, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        labelColumns = new JLabel();
        labelColumns.setHorizontalAlignment(11);
        labelColumns.setText("列");
        rootPanel.add(labelColumns, cc.xy(1, 5));
        columnsScrollPanel = new JScrollPane();
        rootPanel.add(columnsScrollPanel, cc.xyw(3, 5, 4, CellConstraints.FILL, CellConstraints.FILL));
        columnsTable = new JXTable();
        columnsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        columnsScrollPanel.setViewportView(columnsTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, cc.xyw(3, 7, 4));
        btnAdd = new JButton();
        btnAdd.setText("添加列");
        panel1.add(btnAdd, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRemove = new JButton();
        btnRemove.setText("删除列");
        panel1.add(btnRemove, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnApply = new JButton();
        btnApply.setEnabled(false);
        btnApply.setText("应用");
        panel1.add(btnApply, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        labelTTL = new JLabel();
        labelTTL.setHorizontalAlignment(11);
        labelTTL.setText("TTL");
        rootPanel.add(labelTTL, cc.xy(1, 3));
        ttlField = new JTextField();
        ttlField.setEditable(false);
        ttlField.setHorizontalAlignment(11);
        rootPanel.add(ttlField, cc.xy(4, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
