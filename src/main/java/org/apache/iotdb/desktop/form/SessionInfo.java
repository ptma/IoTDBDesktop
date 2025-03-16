package org.apache.iotdb.desktop.form;

import org.apache.iotdb.desktop.component.*;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.model.QueryResult;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SessionInfo extends TabPanel {

    private final Session session;
    private JPanel rootPanel;
    private JTabbedPane tabbedPanel;

    private JXTable basicInfoTable;
    private PropertyTableModel basicInfoTableModel;

    private QueryResultTable variablesTable;
    private QueryResultTableModel variablesTableModel;

    private QueryResultTable storageGroupTable;
    private QueryResultTableModel storageGroupTableModel;

    private QueryResultTable clusterNodesTable;
    private QueryResultTableModel clusterNodesTableModel;

    public SessionInfo(Session session) {
        super();
        this.session = session;
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        initComponents();

        localization();

        if (session.isTableDialect()) {
            tabbedPanel.removeTabAt(0);
        } else {
            loadBasicInfo();
        }
        loadStorageGroup();
        loadVariables();
        loadClusterNodes();
    }

    private void initComponents() {
        basicInfoTable.setRowHeight(24);
        basicInfoTable.setEditable(false);
        basicInfoTable.setShowHorizontalLines(true);
        basicInfoTable.setShowVerticalLines(true);
        basicInfoTable.setRowSelectionAllowed(true);
        basicInfoTable.setSortable(false);
        basicInfoTable.setTableHeader(null);
        basicInfoTable.setIntercellSpacing(new Dimension(1, 1));
        basicInfoTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        basicInfoTableModel = new PropertyTableModel(false);
        DefaultTableRenderer cellRenderer = new DefaultTableRenderer(new PropertyTableRendererProvider(basicInfoTableModel));
        basicInfoTable.setModel(basicInfoTableModel);
        basicInfoTable.setDefaultRenderer(Object.class, cellRenderer);
        basicInfoTable.setDefaultRenderer(String.class, cellRenderer);
        basicInfoTable.setDefaultRenderer(Boolean.class, cellRenderer);
        basicInfoTable.setDefaultRenderer(Number.class, cellRenderer);
    }

    private void localization() {
        tabbedPanel.setTitleAt(0, LangUtil.getString("Basic"));
        tabbedPanel.setTitleAt(1, LangUtil.getString("StorageGroup"));
        tabbedPanel.setTitleAt(2, LangUtil.getString("ClusterParameters"));
        tabbedPanel.setTitleAt(3, LangUtil.getString("ClusterNodes"));
    }

    private void loadBasicInfo() {
        basicInfoTableModel.clear();

        SwingWorker<List<PropertyTableModel.Property>, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    List<PropertyTableModel.Property> properties = get();
                    basicInfoTableModel.setProperties(properties);
                    Utils.autoResizeTableColumns(basicInfoTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected List<PropertyTableModel.Property> doInBackground() throws Exception {
                List<PropertyTableModel.Property> properties = new ArrayList<>();

                QueryResult versionResult = session.query("show version", Configuration.instance().options().isLogInternalSql());
                if (!versionResult.getDatas().isEmpty()) {
                    for (Map.Entry<String, Object> entry : versionResult.getDatas().get(0).entrySet()) {
                        properties.add(PropertyTableModel.Property.of(LangUtil.getString(entry.getKey()), entry.getValue().toString()));
                    }
                }

                int storageGroupCount = session.countOne("count storage group");
                properties.add(PropertyTableModel.Property.of(LangUtil.getString("StorageGroupCount"), String.valueOf(storageGroupCount)));

                int devicesCount = session.countOne("count devices");
                properties.add(PropertyTableModel.Property.of(LangUtil.getString("DeviceCount"), String.valueOf(devicesCount)));

                int timeseriesCount = session.countOne("count timeseries");
                properties.add(PropertyTableModel.Property.of(LangUtil.getString("MetricCount"), String.valueOf(timeseriesCount)));

                return properties;
            }
        };
        worker.execute();
    }

    private void loadVariables() {
        variablesTableModel.clear();

        SwingWorker<QueryResult, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    variablesTableModel.setResult(get());
                    variablesTable.setSortOrder(1, SortOrder.ASCENDING);
                    Utils.autoResizeTableColumns(variablesTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected QueryResult doInBackground() throws Exception {
                return session.query("show variables", Configuration.instance().options().isLogInternalSql())
                    .columnNamesLocalization();
            }
        };
        worker.execute();
    }

    private void loadStorageGroup() {
        storageGroupTableModel.clear();

        SwingWorker<QueryResult, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    storageGroupTableModel.setResult(get());
                    storageGroupTable.setSortOrder(1, SortOrder.ASCENDING);
                    Utils.autoResizeTableColumns(storageGroupTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected QueryResult doInBackground() throws Exception {
                QueryResult result;
                if (session.isTableDialect()) {
                    result = session.query("show databases details", Configuration.instance().options().isLogInternalSql());
                } else {
                    result = session.query("show storage group", Configuration.instance().options().isLogInternalSql());
                    result.getColumns().add("DeviceCount");
                    result.getColumnTypes().add("INT32");
                    result.getDatas().forEach(row -> {
                        try {
                            int count = session.countOne("count devices " + row.get("Database") + ".**");
                            row.put("DeviceCount", count);
                        } catch (Exception e) {
                            row.put("DeviceCount", 0);
                        }
                    });
                }

                return result.columnNamesLocalization();
            }
        };
        worker.execute();
    }

    private void loadClusterNodes() {
        clusterNodesTableModel.clear();

        SwingWorker<QueryResult, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    clusterNodesTableModel.setResult(get());
                    Utils.autoResizeTableColumns(clusterNodesTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected QueryResult doInBackground() {
                return session.query("show cluster details", Configuration.instance().options().isLogInternalSql())
                    .columnNamesLocalization();
            }
        };
        worker.execute();
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public String getTabbedKey() {
        return session.getKey();
    }

    @Override
    public boolean refreshable() {
        return true;
    }

    @Override
    public void refresh() {
        if (!session.isTableDialect()) {
            loadBasicInfo();
        }
        loadStorageGroup();
        loadVariables();
        loadClusterNodes();
    }

    @Override
    public void dispose() {
        variablesTable.dispose();
        storageGroupTable.dispose();
        clusterNodesTable.dispose();
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
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        tabbedPanel = new JTabbedPane();
        rootPanel.add(tabbedPanel, BorderLayout.CENTER);
        final JScrollPane scrollPane1 = new JScrollPane();
        tabbedPanel.addTab("Basic", scrollPane1);
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        basicInfoTable = new JXTable();
        basicInfoTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollPane1.setViewportView(basicInfoTable);
        final JScrollPane scrollPane2 = new JScrollPane();
        tabbedPanel.addTab("Storage Group", scrollPane2);
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        storageGroupTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollPane2.setViewportView(storageGroupTable);
        final JScrollPane scrollPane3 = new JScrollPane();
        tabbedPanel.addTab("Cluster Parameters", scrollPane3);
        scrollPane3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        variablesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollPane3.setViewportView(variablesTable);
        final JScrollPane scrollPane4 = new JScrollPane();
        tabbedPanel.addTab("Cluster Nodes", scrollPane4);
        scrollPane4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        clusterNodesTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollPane4.setViewportView(clusterNodesTable);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private void createUIComponents() {
        variablesTableModel = new QueryResultTableModel();
        variablesTable = new QueryResultTable(variablesTableModel, session.isTableDialect());
        variablesTable.setEditable(false);

        storageGroupTableModel = new QueryResultTableModel();
        storageGroupTable = new QueryResultTable(storageGroupTableModel, session.isTableDialect());
        storageGroupTable.setEditable(false);

        clusterNodesTableModel = new QueryResultTableModel();
        clusterNodesTable = new QueryResultTable(clusterNodesTableModel, session.isTableDialect());
        clusterNodesTable.setEditable(false);
    }
}
