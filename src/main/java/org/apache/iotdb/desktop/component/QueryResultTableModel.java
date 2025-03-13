package org.apache.iotdb.desktop.component;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.model.QueryResult;
import org.apache.tsfile.enums.TSDataType;

import javax.swing.table.AbstractTableModel;
import java.util.Date;
import java.util.Map;

/**
 * @author ptma
 */
public class QueryResultTableModel extends AbstractTableModel {

    @Getter
    private final boolean editable;
    @Getter
    private QueryResult result;

    private QueryResultTable table;

    public QueryResultTableModel() {
        this(false);
    }

    public QueryResultTableModel(boolean editable) {
        this.editable = editable;
    }

    public boolean hasResult() {
        return result != null;
    }

    public void setResult(QueryResult result) {
        this.result = result;
        fireTableStructureChanged();
        fireTableDataChanged();
        if (table != null) {
            table.doDataLoaded();
        }
    }

    public void appendResult(QueryResult result) {
        if (this.result == null) {
            this.result = result;
            fireTableStructureChanged();
        } else {
            this.result.getDatas().addAll(result.getDatas());
        }
        fireTableDataChanged();
        if (table != null) {
            table.doDataLoaded();
        }
    }

    public void setTable(QueryResultTable table) {
        this.table = table;
    }

    public int getDataSize() {
        if (result != null) {
            return result.getDatas().size();
        } else {
            return 0;
        }
    }

    public long getTimestamp(int row) {
        if (result != null && row < result.getDatas().size()) {
            if (table.isTableDialect()) {
                return (long) result.getDatas().get(row).getOrDefault("time", -1L);
            } else {
                return (long) result.getDatas().get(row).getOrDefault("Time", -1L);
            }
        } else {
            return -1L;
        }
    }

    public void remove(int row) {
        if (result != null && row < result.getDatas().size()) {
            result.getDatas().remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    public void clear() {
        if (result != null) {
            result.clearDatas();
            fireTableDataChanged();
        }
    }

    @Override
    public int getColumnCount() {
        if (result != null) {
            if (result.hasException()) {
                return 1;
            } else {
                return result.getColumns().size() + 1;
            }
        } else {
            return 0;
        }
    }

    @Override
    public int getRowCount() {
        if (result != null) {
            if (result.hasException()) {
                return 1;
            } else {
                return result.getDatas().size();
            }
        } else {
            return 0;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (result != null) {
            if (result.hasException()) {
                return result.getException().getMessage();
            } else {
                if (column == 0) {
                    return table == null ? String.valueOf(row + 1) : String.valueOf(table.convertRowIndexToView(row) + 1);
                } else {
                    String colName = result.getColumns().get(column - 1);
                    Object value = result.getDatas().get(row).get(colName);
                    if (colName.equalsIgnoreCase("Time") && value != null) {
                        try {
                            String timeFormat = Configuration.instance().options().getTimeFormat();
                            if (!timeFormat.equalsIgnoreCase("timestamp")) {
                                return DateUtil.format(new Date((Long) value), Configuration.instance().options().getTimeFormat());
                            }
                        } catch (Exception ignore) {
                        }
                    }
                    return value;
                }
            }
        } else {
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        if (result != null && editable) {
            Map<String, Object> rowMap = result.getDatas().get(row);
            String columnName = getColumnName(column);
            Object oldValue = rowMap.get(columnName);
            if (!ObjectUtil.equal(oldValue, aValue)) {
                rowMap.put(columnName, aValue);
                fireTableCellUpdated(row, column);
                table.doDataUpdated((long) rowMap.get("Time"), columnName, getColumnDataType(column), aValue);
            }
        }
    }

    public int getColumnIndex(String columnName) {
        if (result != null) {
            if (result.hasException()) {
                return 0;
            } else {
                if ("#".equals(columnName)) {
                    return 0;
                } else {
                    return result.getColumnIndex(columnName) + 1;
                }
            }
        } else {
            return 0;
        }
    }

    @Override
    public String getColumnName(int column) {
        if (result != null) {
            if (result.hasException()) {
                return "Error";
            } else {
                if (column == 0) {
                    return "#";
                } else {
                    return result.getLocaleColumnName(column - 1);
                }
            }
        } else {
            return "-";
        }
    }

    public TSDataType getColumnDataType(int column) {
        if (result != null && !result.hasException()) {
            if (column == 0) {
                return TSDataType.INT32;
            } else {
                String dataType = result.getColumnTypes().get(column - 1);
                return TSDataType.valueOf(dataType);
            }
        } else {
            return TSDataType.TEXT;
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        TSDataType dataType = getColumnDataType(column);
        switch (dataType) {
            case BOOLEAN:
                return Boolean.class;
            case INT32:
            case DATE:
                return Integer.class;
            case INT64:
            case TIMESTAMP:
                return Long.class;
            case FLOAT:
                return Float.class;
            case DOUBLE:
                return Double.class;
            default:
                return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        if (column != 0 && result != null && !result.hasException()) {
            String colName = result.getColumns().get(column - 1);
            return !colName.equalsIgnoreCase("Time") && !colName.equalsIgnoreCase("Device");
        } else {
            return false;
        }
    }
}
