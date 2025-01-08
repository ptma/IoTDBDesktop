package org.apache.iotdb.desktop.form;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.*;
import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.event.AppEventListener;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.model.*;
import org.apache.iotdb.desktop.syntax.AutoCompletionProvider;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringJoiner;

public class QueryForm extends TabPanel {
    private JPanel rootPanel;
    private JSplitPane mainSplitPanel;
    private JPanel editorPanel;
    private JToolBar editorToolbar;
    private JButton executeButton;
    private JTabbedPane tabbedPanel;
    private JButton saveButton;
    private JButton openButton;
    private final Sessionable sessionable;
    private TextEditor sqlEditor;
    private TextEditor queryInfoEditor;
    private AppEventListener appEventListener;

    public QueryForm(Sessionable sessionable, boolean autoExecute) {
        super();
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        this.sessionable = sessionable;
        initComponents();

        mainSplitPanel.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            Configuration.instance().setInt(ConfKeys.QUERY_EDITOR_HEIGHT, (int) evt.getNewValue());
        });
        mainSplitPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                mainSplitPanel.setDividerLocation(Configuration.instance().getInt(ConfKeys.QUERY_EDITOR_HEIGHT, 200));
            }
        });
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                sqlEditor.textArea().requestFocusInWindow();
                ShortcutManager.instance().registerShortcut(KeyStroke.getKeyStroke("F9"), QueryForm.this::executeQuery);
                ShortcutManager.instance().registerShortcut(KeyStroke.getKeyStroke("ctrl S"), QueryForm.this::saveSqlAs);
                ShortcutManager.instance().registerShortcut(KeyStroke.getKeyStroke("ctrl O"), QueryForm.this::loadSqlFromFile);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                ShortcutManager.instance().unRegisterShortcut(KeyStroke.getKeyStroke("F9"));
                ShortcutManager.instance().unRegisterShortcut(KeyStroke.getKeyStroke("ctrl S"));
                ShortcutManager.instance().unRegisterShortcut(KeyStroke.getKeyStroke("ctrl O"));
            }
        });

        appEventListener = new AppEventListenerAdapter() {
            @Override
            public void optionsChanged(Options options, Options oldOptions) {
                if (!options.getTheme().equals(oldOptions.getTheme())) {
                    sqlEditor.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, false, false, false));
                    editorPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), false, false, true, false));
                    tabbedPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, false, false, false));
                }
            }
        };

        AppEvents.instance().addEventListener(appEventListener);

        generateSql(autoExecute);
    }

    private void initComponents() {
        sqlEditor = new TextEditor("text/iotdbsql");

        sqlEditor.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, false, false, false));
        editorPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), false, false, true, false));
        editorPanel.add(sqlEditor, BorderLayout.CENTER);
        sqlEditor.installAutoCompletion(AutoCompletionProvider.createCompletionProvider());
        sqlEditor.getPopupMenu().add(new JPopupMenu.Separator(), 0);
        JMenuItem explainAnalyzeMenuItem = Utils.UI.createMenuItem("Explain Analyze", (e) -> executeExplain(true));
        sqlEditor.getPopupMenu().add(explainAnalyzeMenuItem, 0);
        JMenuItem explainMenuItem = Utils.UI.createMenuItem("Explain", (e) -> executeExplain(false));
        sqlEditor.getPopupMenu().add(explainMenuItem, 0);


        tabbedPanel.setBorder(new SingleLineBorder(UIManager.getColor("Component.borderColor"), true, false, false, false));

        executeButton.setIcon(Icons.EXECUTE);
        executeButton.setText(LangUtil.getString("Execute"));
        executeButton.addActionListener(e -> this.executeQuery());

        saveButton.setIcon(Icons.SAVE);
        saveButton.setText(LangUtil.getString("Save"));
        saveButton.addActionListener(e -> saveSqlAs());

        openButton.setIcon(Icons.OPEN);
        openButton.setText(LangUtil.getString("Open"));
        openButton.addActionListener(e -> loadSqlFromFile());

        queryInfoEditor = new TextEditor("text/iotdbsql");
        queryInfoEditor.setText("");
        queryInfoEditor.setEditable(false);
        queryInfoEditor.setAutoscrolls(true);
        queryInfoEditor.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        tabbedPanel.addTab(LangUtil.getString("InfoTabbedTitle"), queryInfoEditor);
    }

    private void generateSql(boolean autoExecute) {
        if (sessionable instanceof Session) {

        } else if (sessionable instanceof Database) {

        } else if (sessionable instanceof Device device) {
            StringBuilder sb = new StringBuilder();
            sb.append("select * from ")
                .append(device.getDatabase())
                .append(".")
                .append(device.getName())
                .append(" order by time desc limit 100");
            if (device.isAligned()) {
                sb.append(" align by device");
            }
            sqlEditor.setText(sb.toString());
            if (autoExecute) {
                executeQuery();
            }
        }
    }

    private void executeQuery() {
        String sql = sqlEditor.getText();
        if (StrUtil.isNotBlank(sql)) {
            SwingWorker<List<QueryResult>, Integer> worker = new SwingWorker<>() {
                @Override
                protected void done() {
                    try {
                        displayResults(get());
                    } catch (Exception ex) {
                        Utils.Message.error(ex.getMessage(), ex);
                    }
                }

                @Override
                protected List<QueryResult> doInBackground() {
                    return sessionable.getSession().batchQuery(sql, true);
                }
            };
            worker.execute();
        }
    }

    private void executeExplain(boolean withAnalyze) {
        String sql = sqlEditor.getText();
        if (StrUtil.isNotBlank(sql)) {
            SwingWorker<List<QueryResult>, Integer> worker = new SwingWorker<>() {
                @Override
                protected void done() {
                    try {
                        displayResults(get());
                    } catch (Exception ex) {
                        Utils.Message.error(ex.getMessage(), ex);
                    }
                }

                @Override
                protected List<QueryResult> doInBackground() {
                    return sessionable.getSession().batchExplain(sql, withAnalyze);
                }
            };
            worker.execute();
        }
    }

    public void displayResults(List<QueryResult> results) {
        StringJoiner info = new StringJoiner("\n");
        for (int i = tabbedPanel.getTabCount() - 1; i > 0; i--) {
            Component tabComponent = tabbedPanel.getComponentAt(i);
            if (tabComponent instanceof QueryResultPanel queryResultPanel) {
                queryResultPanel.dispose();
            }
            tabbedPanel.removeTabAt(i);
        }
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) {
                info.add("");
            }
            QueryResult result = results.get(i);
            info.add(result.getSql());
            if (result.hasException()) {
                info.add("-- Error: " + result.getException().getMessage());
            } else {
                info.add("-- OK");
            }
            QueryResultPanel queryResultPanel = new QueryResultPanel();
            queryResultPanel.setQueryResult(result);
            tabbedPanel.addTab(LangUtil.format("ResultTabbedTitle", i + 1), queryResultPanel);
            if (i == 0) {
                tabbedPanel.setSelectedComponent(queryResultPanel);
            }
            info.add("-- 耗时: " + (result.getEndTime() - result.getStartTime()) + "ms");
        }
        queryInfoEditor.setText(info.toString());
    }

    private void saveSqlAs() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(LangUtil.getString("SqlFileFilter"), "sql"));
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.setDialogTitle(LangUtil.getString("SaveAs"));
        jFileChooser.setLocale(LangUtil.getLocale());
        String sqlDirectory = Configuration.instance().getString(ConfKeys.SQL_DIRECTORY, "");
        if (StrUtil.isNotBlank(sqlDirectory)) {
            jFileChooser.setCurrentDirectory(new File(sqlDirectory));
        }
        int result = jFileChooser.showSaveDialog(IotdbDesktopApp.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            Configuration.instance().setString(ConfKeys.SQL_DIRECTORY, file.getParentFile().getAbsolutePath());
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

            FileUtil.writeString(sqlEditor.getText(), file, StandardCharsets.UTF_8);
        }
    }

    private void loadSqlFromFile() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(LangUtil.getString("SqlFileFilter"), "sql"));
        jFileChooser.setAcceptAllFileFilterUsed(true);
        jFileChooser.setDialogTitle(LangUtil.getString("Open"));
        jFileChooser.setLocale(LangUtil.getLocale());
        String sqlDirectory = Configuration.instance().getString(ConfKeys.SQL_DIRECTORY, "");
        if (StrUtil.isNotBlank(sqlDirectory)) {
            jFileChooser.setCurrentDirectory(new File(sqlDirectory));
        }
        int result = jFileChooser.showOpenDialog(IotdbDesktopApp.frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = jFileChooser.getSelectedFile();
            Configuration.instance().setString(ConfKeys.SQL_DIRECTORY, file.getParentFile().getAbsolutePath());
            sqlEditor.setText(FileUtil.readUtf8String(file));
        }
    }

    @Override
    public Session getSession() {
        return sessionable.getSession();
    }

    @Override
    public String getTabbedKey() {
        return String.format("%s-%s", getSession().getId(), IdUtil.fastUUID());
    }

    @Override
    public void dispose() {
        for (int i = tabbedPanel.getTabCount() - 1; i > 0; i--) {
            Component tabComponent = tabbedPanel.getComponentAt(i);
            if (tabComponent instanceof QueryResultPanel queryResultPanel) {
                queryResultPanel.dispose();
            }
        }
        sqlEditor.dispose();
        queryInfoEditor.dispose();
        if (appEventListener != null) {
            AppEvents.instance().removeEventListener(appEventListener);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new BorderLayout(0, 0));
        mainSplitPanel = new JSplitPane();
        mainSplitPanel.setDividerLocation(300);
        mainSplitPanel.setOrientation(0);
        rootPanel.add(mainSplitPanel, BorderLayout.CENTER);
        editorPanel = new JPanel();
        editorPanel.setLayout(new BorderLayout(0, 0));
        mainSplitPanel.setLeftComponent(editorPanel);
        editorToolbar = new JToolBar();
        editorPanel.add(editorToolbar, BorderLayout.NORTH);
        executeButton = new JButton();
        executeButton.setText("Execute");
        editorToolbar.add(executeButton);
        final JToolBar.Separator toolBar$Separator1 = new JToolBar.Separator();
        editorToolbar.add(toolBar$Separator1);
        openButton = new JButton();
        openButton.setText("Open");
        editorToolbar.add(openButton);
        saveButton = new JButton();
        saveButton.setText("Save");
        editorToolbar.add(saveButton);
        tabbedPanel = new JTabbedPane();
        mainSplitPanel.setRightComponent(tabbedPanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
