package org.apache.iotdb.desktop.component;

import lombok.Getter;
import org.apache.iotdb.desktop.model.Column;
import org.apache.iotdb.desktop.util.LangUtil;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ptma
 */
public class ColumnTableModel extends DefaultTableModel {

    @Getter
    private List<Column> columns = new ArrayList<>();
    @Getter
    private List<Column> droppedColumns = new ArrayList<>();

    public ColumnTableModel() {

    }

    public boolean isModified() {
        if (columns == null) {
            return false;
        }
        return !droppedColumns.isEmpty() ||
            columns.stream()
                .anyMatch(m -> !m.isExisting());
    }

    public void setColumns(List<Column> columns) {
        if (columns != null) {
            this.columns = columns;
        } else {
            this.columns.clear();
        }
        this.droppedColumns.clear();
        fireTableStructureChanged();
    }

    public void appendRow() {
        Column appendedRow = new Column();
        appendedRow.setDataType("STRING");
        appendedRow.setCategory("FIELD");
        columns.add(appendedRow);
        int row = columns.size() - 1;
        this.fireTableRowsInserted(row, row);
    }

    public void removeRow(int row) {
        if (row >= 0 && row < columns.size()) {
            Column droped = columns.remove(row);
            if (droped != null && droped.isExisting()) {
                droppedColumns.add(droped);
            }
            this.fireTableRowsDeleted(row, row);
        }
    }

    public void clear() {
        columns.clear();
        droppedColumns.clear();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        if (columns != null) {
            return columns.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getValueAt(int row, int col) {
        Column column = columns.get(row);
        if (column == null) {
            return null;
        } else {
            return switch (col) {
                case 0 -> column.getName();
                case 1 -> column.getDataType();
                case 2 -> column.getCategory();
                default -> null;
            };
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        Column column = columns.get(row);
        if (column != null) {
            switch (col) {
                case 0 -> column.setName(aValue.toString());
                case 1 -> column.setDataType(aValue.toString());
                case 2 -> column.setCategory(aValue.toString());
            }
            fireTableCellUpdated(row, col);
        }
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> LangUtil.getString("Name");
            case 1 -> LangUtil.getString("DataType");
            case 2 -> LangUtil.getString("Category");
            default -> "-";
        };
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        Column column = columns.get(row);
        if (column == null) {
            return false;
        } else {
            return !column.isExisting();
        }
    }
}
