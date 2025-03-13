package org.apache.iotdb.desktop.form;

import cn.hutool.core.util.StrUtil;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.iotdb.desktop.component.QueryResultTable;
import org.apache.iotdb.desktop.component.QueryResultTableModel;
import org.apache.iotdb.desktop.component.SingleLineBorder;
import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.event.AppEventListener;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.event.DataEventListener;
import org.apache.iotdb.desktop.model.QueryResult;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Table;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.RowSorterEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class TableData extends TabPanel {

    private JPanel rootPanel;
    private JPanel topPanel;
    private JButton nextPageButton;
    private JButton showAllButton;
    private JLabel pagingLabel;
    private JToolBar toolbar;
    private JScrollPane tableScrollPanel;
    private QueryResultTable dataTable;
    private QueryResultTableModel dataModel;
    private AppEventListener appEventListener;

    private final Table table;

    private final int defaultPageSize;

    private long limit;
    private long offset = 0;
    private long total = 0;
    private String sortColumn;
    private String sortOrder;

    public TableData(Table table) {
        super();
        this.table = table;
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);

        initComponents();

        defaultPageSize = Configuration.instance().options().getEditorPageSize();
        limit = defaultPageSize;
        sortColumn = "time";
        sortOrder = Configuration.instance().options().getEditorSortOrder();

        Utils.UI.buttonText(nextPageButton, LangUtil.getString("NextPage"));
        Utils.UI.buttonText(showAllButton, LangUtil.getString("ShowAll"));

        loadTableData(false);
    }

    private void initComponents() {
        nextPageButton.setEnabled(false);
        nextPageButton.setIcon(Icons.ARROW_FORWARD);
        showAllButton.setIcon(Icons.ARROW_LAST);
        topPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), false, false, true, false));

        nextPageButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                offset += defaultPageSize;
                loadTableData(false);
            }
        });

        showAllButton.setEnabled(false);
        showAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                offset = 0;
                limit = 0;
                loadTableData(true);
            }
        });

        dataTable.getRowSorter().addRowSorterListener(e -> {
            if (e.getType().equals(RowSorterEvent.Type.SORT_ORDER_CHANGED)) {
                List<? extends RowSorter.SortKey> sortKeys = e.getSource().getSortKeys();
                if (!sortKeys.isEmpty()) {
                    RowSorter.SortKey sortKey = sortKeys.get(0);
                    String column = dataModel.getColumnName(sortKey.getColumn());
                    if (!"#".equals(column)) {
                        sortColumn = column;
                        if (sortKey.getSortOrder().equals(SortOrder.ASCENDING)) {
                            sortOrder = "asc";
                        } else if (sortKey.getSortOrder().equals(SortOrder.DESCENDING)) {
                            sortOrder = "desc";
                        } else {
                            sortOrder = null;
                        }
                        offset = 0;
                        limit = defaultPageSize;
                        loadTableData(true);
                    }
                }
            }
        });

        dataTable.addDataEventListener(new DataEventListener() {
            @Override
            public void dataRemove(long[] timestamps) {
                removeDeviceDatas(timestamps);
            }

            @Override
            public void dataUpdate(long timestamp, String column, TSDataType dataType, Object value) {
                updateDeviceData(timestamp, column, dataType, value);
            }
        });

        appEventListener = new AppEventListenerAdapter() {
            @Override
            public void optionsChanged(Options options, Options oldOptions) {
                if (!options.getTheme().equals(oldOptions.getTheme())) {
                    topPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), false, false, true, false));
                }
            }
        };
        AppEvents.instance().addEventListener(appEventListener);
    }

    private void updatePagingInfo() {
        if (dataModel.getDataSize() < total) {
            pagingLabel.setText(LangUtil.format("LimitedPagingInfo", total, defaultPageSize, offset + defaultPageSize));
            nextPageButton.setEnabled(true);
            showAllButton.setEnabled(true);
        } else {
            pagingLabel.setText(LangUtil.format("FullPagingInfo", total));
            nextPageButton.setEnabled(false);
            showAllButton.setEnabled(false);
        }
    }

    private void loadTableData(final boolean clearPreviousResult) {
        SwingWorker<QueryResult, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    if (clearPreviousResult && dataModel.hasResult()) {
                        dataModel.clear();
                    }
                    dataModel.appendResult(get());
                    updatePagingInfo();
                    if (sortOrder != null) {
                        int col = dataTable.convertColumnIndexToView(dataModel.getColumnIndex(sortColumn));
                        dataTable.setSortOrder(col, "desc".equals(sortOrder) ? SortOrder.DESCENDING : SortOrder.ASCENDING);
                    }
                    Utils.autoResizeTableColumns(dataTable, 400);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected QueryResult doInBackground() throws Exception {
                pagingLabel.setText(LangUtil.getString("Loading"));

                total = table.getSession().countRows(table);

                StringBuilder sql = new StringBuilder();
                sql.append("select * from ")
                    .append(table.getDatabase())
                    .append(".")
                    .append(table.getName());

                if (!"information_schema".equalsIgnoreCase(table.getDatabase())) {
                    if (sortOrder != null) {
                        sql.append(" order by ").append(sortColumn).append(" ").append(sortOrder);
                    }
                    if (limit > 0) {
                        sql.append(" limit ").append(defaultPageSize)
                            .append(" offset ").append(offset);
                    }
                }

                return table.getSession().query(sql.toString(), true);
            }
        };
        worker.execute();
    }

    public void updateDeviceData(long timestamp, String column, TSDataType dataType, Object value) {
        SwingWorker<Exception, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Exception exception = get();
                    if (exception != null) {
                        Utils.Message.error(exception.getMessage(), exception);
                    }
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Exception doInBackground() {
                try {

                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        };
        worker.execute();
    }

    private void removeDeviceDatas(long[] timestamps) {
        SwingWorker<Exception, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Exception exception = get();
                    if (exception != null) {
                        Utils.Message.error(exception.getMessage(), exception);
                    }
                    updatePagingInfo();
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Exception doInBackground() {
                try {
                    for (long timestamp : timestamps) {
                        StringBuilder deleteSql = new StringBuilder();
                        deleteSql.append("delete from ")
                            .append(table.getDatabase())
                            .append(".")
                            .append(table.getName())
                            .append(" where time = ")
                            .append(timestamp);
                        table.getSession().execute(deleteSql.toString());
                        total--;
                        offset--;
                    }
                    return null;
                } catch (Exception e) {
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
        return String.format("%s-%s", table.getKey(), "data");
    }

    @Override
    public boolean refreshable() {
        return true;
    }

    @Override
    public void refresh() {
        offset = 0;
        loadTableData(true);
    }

    @Override
    public void dispose() {
        if (appEventListener != null) {
            AppEvents.instance().removeEventListener(appEventListener);
        }
    }

    private void createUIComponents() {
        dataModel = new QueryResultTableModel(true);
        dataTable = new QueryResultTable(dataModel, table.isTableDialect());
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        topPanel = new JPanel();
        topPanel.setLayout(new FormLayout("fill:p:noGrow,left:4dlu:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:max(d;4px):grow", "center:d:noGrow"));
        rootPanel.add(topPanel, BorderLayout.NORTH);
        toolbar = new JToolBar();
        CellConstraints cc = new CellConstraints();
        topPanel.add(toolbar, cc.xy(1, 1));
        nextPageButton = new JButton();
        nextPageButton.setText("Next Page");
        toolbar.add(nextPageButton);
        showAllButton = new JButton();
        showAllButton.setText("Show All");
        toolbar.add(showAllButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        toolbar.add(toolBar$Separator1);
        pagingLabel = new JLabel();
        pagingLabel.setText("Paging");
        topPanel.add(pagingLabel, cc.xy(3, 1));
        tableScrollPanel = new JScrollPane();
        rootPanel.add(tableScrollPanel, BorderLayout.CENTER);
        tableScrollPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        dataTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        tableScrollPanel.setViewportView(dataTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
