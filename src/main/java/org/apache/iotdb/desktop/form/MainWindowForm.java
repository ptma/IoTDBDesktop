package org.apache.iotdb.desktop.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.components.FlatTabbedPane;
import com.formdev.flatlaf.ui.FlatTabbedPaneUI;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.*;
import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.model.*;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.BiConsumer;

/**
 * MainWindowForm
 *
 * @author ptma
 */
public class MainWindowForm {

    private JPanel contentPanel;
    private JSplitPane topSplitPanel;
    private FlatTabbedPane formTabPanel;

    private JScrollPane treeScrollPanel;
    private JSplitPane mainSplitPanel;
    private JToolBar mainToolbar;
    private JButton btnAddSession;
    private JButton btnNewQuery;
    private JButton btnImport;
    private JButton btnExport;
    private JButton btnLogs;
    private SessionTree sessionTree;
    private TextEditor logEditor;
    private boolean logEditorHasLines = false;
    private JMenuItem logClearMenuItem;
    private JMenuItem logSaveAsMenuItem;

    private static class MainWindowFormHolder {
        final static MainWindowForm INSTANCE = new MainWindowForm();
    }

    public static MainWindowForm instance() {
        return MainWindowFormHolder.INSTANCE;
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }

    private MainWindowForm() {
        $$$setupUI$$$();
        initComponents();
        initToolbarButtons();
        initEventListeners();
        contentPanel.setDoubleBuffered(true);
    }

    private void initComponents() {

        sessionTree = new SessionTree();
        treeScrollPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, true, true, true));
        treeScrollPanel.setViewportView(sessionTree);

        formTabPanel = new FlatTabbedPane();
        formTabPanel.setUI(new FlatTabbedPaneUI() {
            @Override
            protected MouseListener createMouseListener() {
                MouseListener ml = super.createMouseListener();
                return new TabbedPanelMouseAdapter(formTabPanel, ml);
            }
        });
        formTabPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, true, true, true));
        formTabPanel.setTabPlacement(1);
        formTabPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ICON_PLACEMENT, SwingConstants.LEFT);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_SHOW_TAB_SEPARATORS, false);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_SCROLL_BUTTONS_PLACEMENT, FlatClientProperties.TABBED_PANE_PLACEMENT_BOTH);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_ALIGNMENT, SwingConstants.LEADING);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_AREA_ALIGNMENT, FlatClientProperties.TABBED_PANE_ALIGN_LEADING);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_MAXIMUM_TAB_WIDTH, 300);
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_INSETS, new Insets(0, 5, 0, 5));
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_TOOLTIPTEXT, LangUtil.getString("Close"));
        formTabPanel.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSE_CALLBACK, (BiConsumer<JTabbedPane, Integer>) (tabbedPane, tabIndex) -> {
            Component component = formTabPanel.getComponentAt(tabIndex);
            if (component instanceof SessionablePanel sessionablePanel) {
                if (sessionablePanel.disposeable() || sessionablePanel.confirmDispose()) {
                    sessionablePanel.dispose();
                    formTabPanel.removeTabAt(tabIndex);
                }
            }
        });

        topSplitPanel.setRightComponent(formTabPanel);
        topSplitPanel.setDividerLocation(Configuration.instance().getInt(ConfKeys.TREE_WIDTH, 250));
        topSplitPanel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Configuration.instance().setInt(ConfKeys.TREE_WIDTH, (int) evt.getNewValue());
        });

        Configuration.instance().loadSessionProps().forEach(props -> {
            sessionTree.addSession(props);
        });

        logEditor = new TextEditor("text/iotdbsql");
        logEditor.setText("");
        logEditor.setEditable(false);
        logEditor.setAutoscrolls(true);
        logEditor.getPopupMenu().addSeparator();
        logClearMenuItem = Utils.UI.createMenuItem(LangUtil.getString("ClearLog"), (e) -> {
            logEditor.setText("");
            logEditorHasLines = false;
        });
        logClearMenuItem.setIcon(Icons.CLEAR);
        logEditor.getPopupMenu().add(logClearMenuItem);
        logEditor.getPopupMenu().addSeparator();
        logSaveAsMenuItem = Utils.UI.createMenuItem(LangUtil.getString("SaveAsMenu"), (e) -> saveLogAs());
        logEditor.getPopupMenu().add(logSaveAsMenuItem);

        mainSplitPanel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (mainSplitPanel.getBottomComponent() != null) {
                Dimension size = mainSplitPanel.getSize();
                int logPanelHeight = size.height - (int) evt.getNewValue() - 5;
                Configuration.instance().setInt(ConfKeys.LOG_PANEL_HEIGHT, logPanelHeight);
            }
        });
        mainSplitPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (mainSplitPanel.getBottomComponent() != null) {
                    Dimension size = e.getComponent().getSize();
                    int logPanelHeight = Configuration.instance().getInt(ConfKeys.LOG_PANEL_HEIGHT, 100);
                    mainSplitPanel.setDividerLocation(size.height - logPanelHeight - 5);
                }
            }
        });
    }

    public void initToolbarButtons() {
        btnAddSession.setText(LangUtil.getString("NewSession"));
        btnAddSession.setIcon(Icons.TOOLBAR_SESSION);
        btnAddSession.addActionListener(e -> newSession());

        btnNewQuery.setText(LangUtil.getString("NewQuery"));
        btnNewQuery.setIcon(Icons.TOOLBAR_QUERY);
        btnNewQuery.addActionListener(e -> sessionTree.newQuery(false));
        btnNewQuery.setEnabled(false);

        btnImport.setText(LangUtil.getString("Import"));
        btnImport.setIcon(Icons.TOOLBAR_IMPORT);
        btnImport.addActionListener(e -> sessionTree.dataImport());
        btnImport.setEnabled(false);

        btnExport.setText(LangUtil.getString("Export"));
        btnExport.setIcon(Icons.TOOLBAR_EXPORT);
        btnExport.addActionListener(e -> sessionTree.dataExport());
        btnExport.setEnabled(false);

        btnLogs.setText(LangUtil.getString("SQLLog"));
        btnLogs.setIcon(Icons.TOOLBAR_CONSOLE);
        mainSplitPanel.setBottomComponent(btnLogs.isSelected() ? logEditor : null);
        btnLogs.addActionListener(e -> {
            toggleLogPanelVisible(!btnLogs.isSelected());
        });
    }

    public void onWindowOpened(WindowEvent e) {
        boolean logPanelVisible = Configuration.instance().getBoolean(ConfKeys.LOG_PANEL_VISIBLE, true);
        toggleLogPanelVisible(logPanelVisible);
    }

    private void initEventListeners() {
        AppEvents.instance().addEventListener(new AppEventListenerAdapter() {

            @Override
            public void optionsChanged(Options options, Options oldOptions) {
                if (!options.getTheme().equals(oldOptions.getTheme())) {
                    treeScrollPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, true, true, true));
                    formTabPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, true, true, true));
                }
            }

            @Override
            public void onTreeSelectionChange(TreePath treePath) {
                boolean sessionActived;
                if (treePath == null || treePath.getLastPathComponent() == null) {
                    sessionActived = false;
                } else {
                    DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) treePath.getLastPathComponent();
                    sessionActived = treeNode.getUserObject() instanceof Sessionable sessionable &&
                        sessionable.getSession().isOpened() &&
                        !sessionable.isTableDialect();
                }
                btnNewQuery.setEnabled(sessionActived);
                btnImport.setEnabled(sessionActived);
                btnExport.setEnabled(sessionActived);
            }

            @Override
            public void appendSqlLog(String sql, boolean isComment) {
                String printedSql = "";
                if (logEditorHasLines && !sql.startsWith("\n")) {
                    printedSql = "\n";
                }
                if (Configuration.instance().options().isLogTimestamp()) {
                    printedSql += "[" + DateUtil.format(new Date(), "HH:mm:ss.SSS") + "] ";
                }
                printedSql += sql;
                if (!sql.endsWith(";")) {
                    printedSql += ";";
                }
                logEditor.appendTextAndScrollToEnd(printedSql);
                logEditorHasLines = true;
            }

            @Override
            public void newTab(String title, Icon icon, TabPanel tabPanel) {
                formTabPanel.addTab(title, icon, tabPanel, title);
                formTabPanel.setSelectedComponent(tabPanel);
            }

            @Override
            public void newInfoTab(Sessionable sessionable) {
                for (int i = 0; i < formTabPanel.getTabCount(); i++) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.getTabbedKey().equals(sessionable.getKey())) {
                            formTabPanel.setSelectedComponent(tabComponent);
                            return;
                        }
                    }
                }
                if (sessionable instanceof Session session) {
                    SessionInfo infoForm = new SessionInfo(session);
                    String title = LangUtil.getString("Session") + ": " + session.getSession().getName();
                    formTabPanel.addTab(title, Icons.TREE_NODE_IOTDB_ACTIVE, infoForm, title);
                    formTabPanel.setSelectedComponent(infoForm);
                } else if (sessionable instanceof Database database) {
                    DatabaseInfo infoForm = new DatabaseInfo(database);
                    String title = LangUtil.getString("Database") + ": " + database.getName();
                    formTabPanel.addTab(title, Icons.TREE_NODE_DATABSE_OPENED, infoForm, title);
                    formTabPanel.setSelectedComponent(infoForm);
                } else if (sessionable instanceof Device device) {
                    DeviceInfo infoForm = new DeviceInfo(device);
                    String title = LangUtil.getString("Device") + ": " + device.getName();
                    formTabPanel.addTab(title, Icons.TREE_NODE_COLUMN, infoForm, title);
                    formTabPanel.setSelectedComponent(infoForm);
                } else if (sessionable instanceof Table table) {
                    TableInfo infoForm = new TableInfo(table);
                    String title = LangUtil.getString("Table") + ": " + table.getName();
                    formTabPanel.addTab(title, Icons.TREE_NODE_COLUMN, infoForm, title);
                    formTabPanel.setSelectedComponent(infoForm);
                }
            }

            @Override
            public void newDeviceDataTab(Device device) {
                String key = String.format("%s-%s", device.getKey(), "data");
                for (int i = 0; i < formTabPanel.getTabCount(); i++) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.getTabbedKey().equals(key)) {
                            formTabPanel.setSelectedComponent(tabComponent);
                            return;
                        }
                    }
                }
                DeviceData editor = new DeviceData(device);
                String title = LangUtil.getString("DeviceData") + " - " + device.getName() + " (" + device.getSession().getName() + ")";
                formTabPanel.addTab(title, Icons.TABLE_DATA, editor, title);
                formTabPanel.setSelectedComponent(editor);
            }

            @Override
            public void newTableDataTab(Table table) {
                String key = String.format("%s-%s", table.getKey(), "data");
                for (int i = 0; i < formTabPanel.getTabCount(); i++) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.getTabbedKey().equals(key)) {
                            formTabPanel.setSelectedComponent(tabComponent);
                            return;
                        }
                    }
                }
                TableData editor = new TableData(table);
                String title = LangUtil.getString("TableData") + " - " + table.getName() + " (" + table.getSession().getName() + ")";
                formTabPanel.addTab(title, Icons.TABLE_DATA, editor, title);
                formTabPanel.setSelectedComponent(editor);
            }

            @Override
            public void openDataImport(Session session) {
                String key = String.format("%s-%s", session.getKey(), DataImport.TABBED_KEY);
                for (int i = 0; i < formTabPanel.getTabCount(); i++) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.getTabbedKey().equals(key)) {
                            formTabPanel.setSelectedComponent(tabComponent);
                            return;
                        }
                    }
                }
                DataImport tabPanel = new DataImport(session);
                String title = LangUtil.getString("Import") + " (" + session.getName() + ")";
                formTabPanel.addTab(title, Icons.IMPORT, tabPanel, title);
                formTabPanel.setSelectedComponent(tabPanel);
            }

            @Override
            public void openDataExport(Session session) {
                String key = String.format("%s-%s", session.getKey(), DataExport.TABBED_KEY);
                for (int i = 0; i < formTabPanel.getTabCount(); i++) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.getTabbedKey().equals(key)) {
                            formTabPanel.setSelectedComponent(tabComponent);
                            return;
                        }
                    }
                }
                DataExport tabPanel = new DataExport(session);
                String title = LangUtil.getString("Export") + " (" + session.getName() + ")";
                formTabPanel.addTab(title, Icons.EXPORT, tabPanel, title);
                formTabPanel.setSelectedComponent(tabPanel);
            }

            @Override
            public void closeSession(Session session) {
                for (int i = formTabPanel.getTabCount() - 1; i >= 0; i--) {
                    Component tabComponent = formTabPanel.getComponentAt(i);
                    if (tabComponent instanceof SessionablePanel sessionablePanel) {
                        Session tabSession = sessionablePanel.getSession();
                        if (tabSession.equals(session)) {
                            formTabPanel.removeTabAt(i);
                        }
                    }
                }
            }
        });
    }

    private void toggleLogPanelVisible(boolean logPanelVisible) {
        mainSplitPanel.setBottomComponent(logPanelVisible ? logEditor : null);
        btnLogs.setSelected(logPanelVisible);
        Configuration.instance().setBoolean(ConfKeys.LOG_PANEL_VISIBLE, logPanelVisible);

        if (logPanelVisible) {
            int logPanelHeight = Configuration.instance().getInt(ConfKeys.LOG_PANEL_HEIGHT, 100);
            Dimension size = mainSplitPanel.getSize();
            mainSplitPanel.setDividerSize(5);
            mainSplitPanel.setDividerLocation(size.height - logPanelHeight - 5);
        } else {
            mainSplitPanel.setDividerSize(0);
        }
    }

    private void newSession() {
        SessionPropsEditorForm.open(null, (newProps) -> {
            int existIndex = sessionTree.indexOf(newProps);
            if (existIndex >= 0) {
                Utils.Message.info(String.format(LangUtil.getString("SessionExists"), newProps.getName()));
                sessionTree.setSelectionRow(existIndex);
                return;
            }
            sessionTree.addSession(newProps);
            Configuration.instance().saveSession(newProps);
        });
    }

    private void saveLogAs() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(LangUtil.getString("SqlFileFilter"), "sql"));
        jFileChooser.setDialogTitle(LangUtil.getString("SaveAs"));
        jFileChooser.setLocale(LangUtil.getLocale());
        int result = jFileChooser.showSaveDialog(IotdbDesktopApp.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            FileNameExtensionFilter currentFilter = (FileNameExtensionFilter) jFileChooser.getFileFilter();
            String ext = currentFilter.getExtensions()[0];
            String absolutePath = file.getAbsolutePath();
            if (!absolutePath.endsWith("." + ext)) {
                absolutePath += "." + ext;
                file = new File(absolutePath);
            }
            if (file.exists()) {
                int opt = Utils.Message.confirm(String.format(LangUtil.getString("FileOverwriteConfirm"), file.getName()));
                if (JOptionPane.YES_OPTION != opt) {
                    return;
                }
            }

            FileUtil.writeString(logEditor.getText(), file, StandardCharsets.UTF_8);
        }
    }

    public boolean closing() {
        for (int i = formTabPanel.getTabCount() - 1; i >= 0; i--) {
            Component component = formTabPanel.getComponentAt(i);
            if (component instanceof SessionablePanel sessionablePanel) {
                if (sessionablePanel.disposeable() || sessionablePanel.confirmDispose()) {
                    sessionablePanel.dispose();
                    formTabPanel.removeTabAt(i);
                }
            }
        }
        if (formTabPanel.getTabCount() > 0) {
            return false;
        }
        sessionTree.closeOpendSessions();
        return true;
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(0, 0));
        contentPanel.setEnabled(true);
        mainSplitPanel = new JSplitPane();
        mainSplitPanel.setDividerLocation(338);
        mainSplitPanel.setDividerSize(5);
        mainSplitPanel.setOrientation(0);
        mainSplitPanel.setResizeWeight(0.0);
        contentPanel.add(mainSplitPanel, BorderLayout.CENTER);
        topSplitPanel = new JSplitPane();
        topSplitPanel.setDividerLocation(250);
        topSplitPanel.setDividerSize(5);
        topSplitPanel.setResizeWeight(0.0);
        mainSplitPanel.setLeftComponent(topSplitPanel);
        treeScrollPanel = new JScrollPane();
        topSplitPanel.setLeftComponent(treeScrollPanel);
        mainToolbar = new JToolBar();
        contentPanel.add(mainToolbar, BorderLayout.NORTH);
        btnAddSession = new JButton();
        btnAddSession.setText("Add Session");
        mainToolbar.add(btnAddSession);
        btnNewQuery = new JButton();
        btnNewQuery.setText("New Query");
        mainToolbar.add(btnNewQuery);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        mainToolbar.add(toolBar$Separator1);
        btnImport = new JButton();
        btnImport.setText("Import");
        mainToolbar.add(btnImport);
        btnExport = new JButton();
        btnExport.setText("Export");
        mainToolbar.add(btnExport);
        final JToolBar.Separator toolBar$Separator2 = new JToolBar.Separator();
        mainToolbar.add(toolBar$Separator2);
        btnLogs = new JButton();
        btnLogs.setText("Logs");
        mainToolbar.add(btnLogs);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

}
