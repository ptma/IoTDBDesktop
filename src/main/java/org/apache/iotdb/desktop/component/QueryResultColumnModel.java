package org.apache.iotdb.desktop.component;

import javax.swing.event.TableColumnModelEvent;
import javax.swing.table.DefaultTableColumnModel;

public class QueryResultColumnModel extends DefaultTableColumnModel {

    private final QueryResultTable table;

    public QueryResultColumnModel(QueryResultTable table) {
        this.table = table;
    }

    @Override
    public void moveColumn(int columnIndex, int newIndex) {
        if (columnIndex == 0) {
            return;
        } else {
            super.moveColumn(columnIndex, newIndex);
        }
    }

}
