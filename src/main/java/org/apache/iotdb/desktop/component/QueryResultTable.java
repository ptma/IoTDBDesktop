package org.apache.iotdb.desktop.component;

import cn.hutool.core.date.DateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.event.AppEventListener;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.event.DataEventListener;
import org.apache.iotdb.desktop.form.ExportDialog;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.*;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

public class QueryResultTable extends JXTable {

    private final QueryResultTableModel tableModel;
    @Getter
    private final boolean tableDialect;
    private final List<DataEventListener> dataEventListeners = new ArrayList<>();
    private AppEventListener appEventListener;

    private int popupRow;
    private int popupColumn;

    private JPopupMenu popupMenu;
    private JMenuItem copyMenuItem;
    private JMenu copyRowsAsMenu;
    private JMenuItem copyRowsAsJson;
    private JMenuItem copyRowsAsTabSeparated;
    private JMenuItem copyRowsAsCsv;

    private JMenuItem exportRows;

    public QueryResultTable(QueryResultTableModel tableModel, boolean tableDialect) {
        super(tableModel);
        this.tableModel = tableModel;
        this.tableDialect = tableDialect;
        tableModel.setTable(this);
        DefaultTableRenderer cellRenderer = new DefaultTableRenderer(new QueryResultRendererProvider(tableModel));
        this.setDefaultRenderer(Object.class, cellRenderer);
        this.setDefaultRenderer(String.class, cellRenderer);
        this.setDefaultRenderer(Number.class, cellRenderer);
        DefaultTableRenderer booleanCellRenderer = new DefaultTableRenderer(new QueryResultBooleanRendererProvider(tableModel));
        this.setDefaultRenderer(Boolean.class, booleanCellRenderer);

        this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        this.setCellEditor(new DefaultCellEditor(new JTextField()));
        this.setRowHeight(24);
        this.setEditable(true);
        this.setShowHorizontalLines(true);
        this.setShowVerticalLines(true);
        this.setAutoStartEditOnKeyStroke(true);
        this.setHorizontalScrollEnabled(true);
        this.setRowSelectionAllowed(true);
        this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        this.setIntercellSpacing(new Dimension(1, 1));
        this.setDefaultEditor(String.class, new DetailedTextCellEditor(new JTextField()));
        JCheckBox editorCheckbox = new JCheckBox();
        editorCheckbox.setHorizontalAlignment(JLabel.CENTER);
        this.setDefaultEditor(Boolean.class, new DefaultCellEditor(editorCheckbox));
        this.setSortOrderCycle(SortOrder.DESCENDING, SortOrder.ASCENDING, SortOrder.UNSORTED);
        this.setColumnModel(new QueryResultColumnModel(this));
        tableModel.addTableModelListener(e -> {
            if (this.getColumnCount() > 0) {
                this.getColumnExt(0).setSortable(false);
            }
        });
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupRow = QueryResultTable.this.rowAtPoint(e.getPoint());
                    popupColumn = QueryResultTable.this.columnAtPoint(e.getPoint());
                    boolean onRow = popupRow >= 0;
                    boolean onColumn = popupColumn >= 0;
                    if (onRow && onColumn) {
                        int[] selRows = QueryResultTable.this.getSelectedRows();
                        if (Arrays.stream(selRows).noneMatch(i -> i == popupRow)) {
                            QueryResultTable.this.setRowSelectionInterval(popupRow, popupRow);
                        }
                        popupMenu.show(QueryResultTable.this, e.getX(), e.getY());
                    }
                }
            }
        });
        getRowSorter().addRowSorterListener(e -> {
            if (e.getType().equals(RowSorterEvent.Type.SORT_ORDER_CHANGED)) {
                List<? extends RowSorter.SortKey> sortKeys = e.getSource().getSortKeys();
                if (!sortKeys.isEmpty()) {
                    RowSorter.SortKey sortKey = sortKeys.get(0);
                    String column = dataModel.getColumnName(sortKey.getColumn());
                    if ("#".equals(column)) {
                        setSortOrder(0, SortOrder.UNSORTED);
                    }
                }
            }
        });

        popupMenu = new JPopupMenu();
        copyMenuItem = Utils.UI.createMenuItem(LangUtil.getString("Copy"), (e) -> {
            if (popupRow >= 0 && popupColumn >= 0) {
                Object value = getValueAt(popupRow, popupColumn);
                if (value != null) {
                    Utils.copyToClipboard(value.toString());
                    Utils.Toast.success(LangUtil.getString("CopiedToClipboard"));
                }
            }
        });
        popupMenu.add(copyMenuItem);

        copyRowsAsMenu = Utils.UI.createMenu(LangUtil.getString("Copy&RowsAs"));
        popupMenu.add(copyRowsAsMenu);

        copyRowsAsJson = Utils.UI.createMenuItem(LangUtil.getString("JSON"), (e) -> {
            try {
                Utils.copyToClipboard(Utils.JSON.toString(getSelectedRowDatas()));
                Utils.Toast.success(LangUtil.getString("CopiedToClipboard"));
            } catch (JsonProcessingException ex) {
                Utils.Message.error(ex.getMessage(), ex);
            }
        });
        copyRowsAsMenu.add(copyRowsAsJson);

        copyRowsAsTabSeparated = Utils.UI.createMenuItem(LangUtil.getString("TabSeparatedValues"), (e) -> {
            copySelectedRows(false, "\t", false);
        });
        copyRowsAsMenu.add(copyRowsAsTabSeparated);

        copyRowsAsCsv = Utils.UI.createMenuItem(LangUtil.getString("CSV"), (e) -> {
            copySelectedRows(true, ",", true);
        });
        copyRowsAsMenu.add(copyRowsAsCsv);

        exportRows = Utils.UI.createMenuItem(LangUtil.getString("&Export"), (e) -> {
            ExportDialog.export(this, tableModel);
        });
        popupMenu.add(exportRows);

        if (tableModel.isEditable()) {
            popupMenu.addSeparator();
            JMenuItem deleteRows = Utils.UI.createMenuItem(LangUtil.getString("&DeleteRows"), (e) -> {
               deleteSelectedRows();
            });
            popupMenu.add(deleteRows);
        }

        appEventListener = new AppEventListenerAdapter() {
            @Override
            public void optionsChanged(Options options, Options oldOptions) {
                if (!options.getTheme().equals(oldOptions.getTheme())) {
                    SwingUtilities.updateComponentTreeUI(popupMenu);
                }
            }
        };
        AppEvents.instance().addEventListener(appEventListener);
    }

    public void doDataLoaded() {
        getColumnExt(0).setSortable(false);
    }

    public void doDataUpdated(long timestamp, String column, TSDataType dataType, Object value) {
        dataEventListeners.forEach(dataEventListener -> dataEventListener.dataUpdate(timestamp, column, dataType, value));
    }

    private List<Map<String, Object>> getSelectedRowDatas() {
        int[] rows = this.getSelectedRows();
        List<Map<String, Object>> exportingRows = Arrays.stream(rows)
            .map(QueryResultTable.this::convertColumnIndexToModel)
            .mapToObj(i -> tableModel.getResult().getDatas().get(i))
            .toList();
        exportingRows.forEach(row -> {
            row.entrySet().forEach((entry) -> {
                if (entry.getKey().equalsIgnoreCase("Time") && entry.getValue() != null) {
                    try {
                        entry.setValue(DateUtil.format(new Date((Long) entry.getValue()), Configuration.instance().options().getTimeFormat()));
                    } catch (Exception ignored) {
                    }
                }
            });
        });
        return exportingRows;
    }

    private void copySelectedRows(boolean withTitleRow, String columnSpliter, boolean aroundWithDoubleQuotes) {
        List<Map<String, Object>> datas = getSelectedRowDatas();
        StringBuilder sb = new StringBuilder();
        try {
            List<String> columnNames = tableModel.getResult().getColumns();
            // title
            if (withTitleRow) {
                StringJoiner columnsRow = new StringJoiner(columnSpliter);
                for (int i = 0; i < columnNames.size(); i++) {
                    if (aroundWithDoubleQuotes) {
                        columnsRow.add("\"" + tableModel.getResult().getLocaleColumnName(i).replaceAll("\"", "\"\"") + "\"");
                    } else {
                        columnsRow.add(tableModel.getResult().getLocaleColumnName(i));
                    }
                }
                sb.append(columnsRow.toString()).append("\n");
            }
            // data
            StringJoiner dataRow;
            Map<String, Object> rowData;
            String columnName;
            Object cellValue;
            String stringValue;
            for (Map<String, Object> data : datas) {
                dataRow = new StringJoiner(columnSpliter);
                rowData = data;
                for (String name : columnNames) {
                    columnName = name;
                    cellValue = rowData.get(columnName);
                    stringValue = cellValue == null ? "" : cellValue.toString();
                    if (aroundWithDoubleQuotes) {
                        dataRow.add("\"" + stringValue.replaceAll("\"", "\"\"").replaceAll("\n", "") + "\"");
                    } else {
                        dataRow.add(stringValue.replaceAll("\n", ""));
                    }
                }
                sb.append(dataRow).append("\n");
            }
            Utils.copyToClipboard(sb.toString());
            Utils.Toast.success(LangUtil.getString("CopiedToClipboard"));
        } catch (Exception ex) {
            Utils.Message.error(ex.getMessage(), ex);
        }
    }

    private void deleteSelectedRows() {
        if (Utils.Message.confirm(LangUtil.getString("ConfirmDeleteSelectedRows")) == JOptionPane.YES_OPTION) {
            int[] rows = this.getSelectedRows();
            long[] timestamps = Arrays.stream(rows).mapToLong(tableModel::getTimestamp).toArray();
            for (int i = rows.length - 1; i >= 0; i--) {
                tableModel.remove(convertRowIndexToModel(rows[i]));
            }
            dataEventListeners.forEach(dataEventListener -> dataEventListener.dataRemove(timestamps));
        }
    }

    public void addDataEventListener(DataEventListener dataEventListener) {
        dataEventListeners.add(dataEventListener);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int row = rowAtPoint(e.getPoint());
        int col = columnAtPoint(e.getPoint());
        String toolTipText = null;
        if (row > -1 && col > -1) {
            Object value = getValueAt(row, col);
            if (null != value && !"".equals(value)) {
                String stringValue = value.toString();
                if (stringValue.length() > 200) {
                    toolTipText = stringValue.substring(0, 200) + " [...]";
                } else {
                    toolTipText = stringValue;
                }
            }
        }
        return toolTipText;
    }

    @Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                int index = columnModel.getColumnIndexAtX(e.getX());
                if (index >= 0) {
                    int realIndex = columnModel.getColumn(index).getModelIndex();
                    if (realIndex >= 0) {
                        return "<html>" + tableModel.getColumnName(realIndex) + "<br/>(" + tableModel.getColumnDataType(realIndex).name() + ")</html>";
                    }
                }
                return null;
            }
        };
    }

    public void dispose() {
        if (appEventListener != null) {
            AppEvents.instance().removeEventListener(appEventListener);
        }
    }
}
