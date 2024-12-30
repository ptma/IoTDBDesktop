package org.apache.iotdb.desktop.component;

import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.model.QueryResult;
import org.apache.iotdb.desktop.util.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class QueryResultPanel extends JScrollPane {

    private final QueryResultTable dataTable;
    private final QueryResultTableModel dataModel;

    public QueryResultPanel() {
        super();
        setBorder(new EmptyBorder(0, 0, 0, 0));
        this.dataModel = new QueryResultTableModel();
        this.dataTable = new QueryResultTable(dataModel);
        setViewportView(this.dataTable);
    }

    public void setQueryResult(QueryResult result) {
        this.dataModel.setResult(result);
        Utils.autoResizeTableColumns(dataTable, 400);
    }

    public void dispose() {
        if (dataTable != null) {
            dataTable.dispose();
        }
    }
}
