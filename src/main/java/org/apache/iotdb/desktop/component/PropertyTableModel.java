package org.apache.iotdb.desktop.component;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.desktop.util.LangUtil;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ptma
 */
public class PropertyTableModel extends AbstractTableModel {

    private final boolean editable;

    @Getter
    private List<Property> properties = new ArrayList<>();

    public PropertyTableModel(boolean editable) {
        this.editable = editable;
    }

    public int addProperty(String name, String value) {
        properties.add(Property.of(name, value));
        int insertedRow = properties.size() - 1;
        fireTableRowsInserted(insertedRow, insertedRow);
        return insertedRow;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
        fireTableDataChanged();
    }

    public void setProperties(Map<String, String> map) {
        this.properties.clear();
        map.forEach((key, value) -> this.properties.add(Property.of(key, value)));
        fireTableDataChanged();
    }

    public Map<String, String> getPropertiesAsMap() {
        return properties.stream()
            .collect(Collectors.toMap(Property::getName, Property::getValue));
    }

    public void clear() {
        properties.clear();
        fireTableDataChanged();
    }

    public void removeRow(int row) {
        properties.remove(row);
        fireTableRowsDeleted(row, row);
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return properties.size();
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (column == 0) {
            return properties.get(row).getName();
        } else {
            return properties.get(row).getValue();
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            properties.get(rowIndex).setName(aValue.toString());
        } else {
            properties.get(rowIndex).setValue(aValue.toString());
        }
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            return LangUtil.getString("KeyName");
        } else {
            return LangUtil.getString("KeyValue");
        }
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return editable;
    }

    @Getter
    @Setter
    @AllArgsConstructor(staticName = "of")
    public static class Property {

        private String name;

        private String value;

    }
}
