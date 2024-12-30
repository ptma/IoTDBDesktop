package org.apache.iotdb.desktop.component;

import lombok.Getter;
import org.apache.iotdb.desktop.model.Metric;
import org.apache.iotdb.desktop.util.LangUtil;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ptma
 */
public class MetricsTableModel extends DefaultTableModel {

    private String path;
    @Getter
    private List<Metric> metrics = new ArrayList<>();
    @Getter
    private List<Metric> droppedMetrics = new ArrayList<>();

    public MetricsTableModel(String path) {
        this.path = path;
    }

    public boolean isModified() {
        if (metrics == null) {
            return false;
        }
        return !droppedMetrics.isEmpty() ||
            metrics.stream()
                .anyMatch(m -> !m.isExisting() || m.isModified());
    }

    public void setPath(String path) {
        this.path = path;
        metrics.forEach(m -> m.setPath(path));
        droppedMetrics.forEach(m -> m.setPath(path));
    }

    public void setMetrics(List<Metric> metrics) {
        if (metrics != null) {
            this.metrics = metrics;
        } else {
            this.metrics.clear();
        }
        this.droppedMetrics.clear();
        fireTableStructureChanged();
    }

    public void appendRow() {
        Metric appendedRow = new Metric();
        appendedRow.setPath(path);
        appendedRow.setDatabase("-");
        appendedRow.setDataType("TEXT");
        appendedRow.setEncoding("PLAIN");
        appendedRow.setCompression("LZ4");
        appendedRow.setDeadband("-");
        appendedRow.setDeadbandParameters("-");
        appendedRow.setViewType("-");
        metrics.add(appendedRow);
        int row = metrics.size() - 1;
        this.fireTableRowsInserted(row, row);
    }

    public void removeRow(int row) {
        if (row >= 0 && row < metrics.size()) {
            Metric droped = metrics.remove(row);
            if (droped != null && droped.isExisting()) {
                droppedMetrics.add(droped);
            }
            this.fireTableRowsDeleted(row, row);
        }
    }

    public void clear() {
        metrics.clear();
        droppedMetrics.clear();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return 11;
    }

    @Override
    public int getRowCount() {
        if (metrics != null) {
            return metrics.size();
        } else {
            return 0;
        }
    }

    @Override
    public Object getValueAt(int row, int column) {
        Metric metric = metrics.get(row);
        if (metric == null) {
            return null;
        } else {
            return switch (column) {
                case 0 -> metric.getName();
                case 1 -> metric.displayableAlias();
                case 2 -> metric.getDatabase();
                case 3 -> metric.getDataType();
                case 4 -> metric.getEncoding();
                case 5 -> metric.getCompression();
                case 6 -> metric.displayableTags();
                case 7 -> metric.displayableAttributes();
                case 8 -> metric.getDeadband();
                case 9 -> metric.getDeadbandParameters();
                case 10 -> metric.getViewType();
                default -> null;
            };
        }
    }

    @Override
    public void setValueAt(Object aValue, int row, int column) {
        Metric metric = metrics.get(row);
        if (metric != null) {
            switch (column) {
                case 0 -> metric.setName(aValue.toString());
                case 1 -> metric.setAliasModified(aValue.toString());
                case 2 -> metric.setDatabase(aValue.toString());
                case 3 -> metric.setDataType(aValue.toString());
                case 4 -> metric.setEncoding(aValue.toString());
                case 5 -> metric.setCompression(aValue.toString());
                case 6 -> metric.setTagsModified(aValue.toString());
                case 7 -> metric.setAttributesModified(aValue.toString());
                case 8 -> metric.setDeadband(aValue.toString());
                case 9 -> metric.setDeadbandParameters(aValue.toString());
                case 10 -> metric.setViewType(aValue.toString());
            }
            fireTableCellUpdated(row, column);
        }
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case 0 -> LangUtil.getString("Name");
            case 1 -> LangUtil.getString("Alias");
            case 2 -> LangUtil.getString("Database");
            case 3 -> LangUtil.getString("DataType");
            case 4 -> LangUtil.getString("Encoding");
            case 5 -> LangUtil.getString("Compression");
            case 6 -> LangUtil.getString("Tags");
            case 7 -> LangUtil.getString("Attributes");
            case 8 -> LangUtil.getString("Deadband");
            case 9 -> LangUtil.getString("DeadbandParameters");
            case 10 -> LangUtil.getString("ViewType");
            default -> "-";
        };
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        Metric metric = metrics.get(row);
        if (metric == null) {
            return false;
        } else if (metric.isExisting()) {
            return column == 1 || column == 6 || column == 7;
        } else {
            return column >= 0 && column != 2 && column <= 7;
        }
    }
}
