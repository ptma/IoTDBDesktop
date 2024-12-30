package org.apache.iotdb.desktop.form;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Languages;
import org.apache.iotdb.desktop.config.Themes;
import org.apache.iotdb.desktop.component.TextableListRenderer;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.ThemeUtil;
import org.jdesktop.swingx.combobox.EnumComboBoxModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ptma
 */
public class OptionsForm extends JDialog {

    private boolean optionsChanged;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JPanel bottomPanel;
    private JPanel buttonPanel;
    private JPanel mainPanel;
    private JSpinner fontSizeField;
    private JComboBox<String> fontComboBox;
    private JComboBox<Themes> themeComboBox;
    private JComboBox<Languages> languageComboBox;
    private JLabel fontLabel;
    private JLabel fontSizeLabel;
    private JLabel themeLabel;
    private JLabel languageLabel;
    private JComboBox<String> timeFormatField;
    private JLabel timeFormatLabel;
    private JCheckBox autoCompletionChkBox;
    private JSpinner autoCompletionDelayField;
    private JLabel delayLabel;
    private JTabbedPane tabbedPane;
    private JCheckBox autoLoadDeviceNodesCheckBox;
    private JLabel autoCompletionLabel;
    private JLabel sessionTreeLabel;
    private JLabel sqlLogLabel;
    private JCheckBox logInternalSQLCheckBox;
    private JCheckBox addTimestampToLogsCheckBox;
    private JCheckBox dblClickOpenDeviceDataCheckBox;
    private JLabel deviceDataLabel;
    private JRadioButton descRadioButton;
    private JRadioButton ascRadioButton;
    private JSpinner pageSizeField;
    private JLabel rowsPerPageLabel;
    private JLabel orderByTimeLabel;
    private JCheckBox alwaysAlignByDeviceCheckBox;

    public static void open() {
        JDialog dialog = new OptionsForm(IotdbDesktopApp.frame);
        dialog.setMinimumSize(new Dimension(350, 380));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(IotdbDesktopApp.frame);
        dialog.setVisible(true);
    }

    private OptionsForm(Frame owner) {
        super(owner);
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        initComponents();

        buttonOK.addActionListener(e -> onOK());
        buttonCancel.addActionListener(e -> onCancel());

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        localization();
    }

    private void localization() {
        this.setTitle(LangUtil.getString("Options"));

        tabbedPane.setTitleAt(0, LangUtil.getString("UserInterface"));
        languageLabel.setText(LangUtil.getString("Language"));
        themeLabel.setText(LangUtil.getString("Theme"));

        tabbedPane.setTitleAt(1, LangUtil.getString("SqlEditor"));
        fontLabel.setText(LangUtil.getString("Font"));
        fontSizeLabel.setText(LangUtil.getString("FontSize"));
        autoCompletionLabel.setText(LangUtil.getString("AutoCompletion"));
        autoCompletionChkBox.setText(LangUtil.getString("Enable"));
        delayLabel.setText(LangUtil.getString("Delay"));

        tabbedPane.setTitleAt(2, LangUtil.getString("Other"));
        sessionTreeLabel.setText(LangUtil.getString("SessionTree"));
        autoLoadDeviceNodesCheckBox.setText(LangUtil.getString("AutoLoadDeviceNodes"));
        sqlLogLabel.setText(LangUtil.getString("SQLLog"));
        logInternalSQLCheckBox.setText(LangUtil.getString("LogInternalSQL"));
        addTimestampToLogsCheckBox.setText(LangUtil.getString("AddTimestampToLogs"));
        timeFormatLabel.setText(LangUtil.getString("TimeFormat"));

        deviceDataLabel.setText(LangUtil.getString("DeviceDataEditor"));
        dblClickOpenDeviceDataCheckBox.setText(LangUtil.getString("DblClickOpenDeviceData"));
        dblClickOpenDeviceDataCheckBox.setToolTipText("<html>" + LangUtil.getString("DblClickOpenDeviceDataToolTip") + "</html>");
        alwaysAlignByDeviceCheckBox.setText(LangUtil.getString("AlwaysAlignByDevice"));
        orderByTimeLabel.setText(LangUtil.getString("OrderByTime"));
        rowsPerPageLabel.setText(LangUtil.getString("RowsPerPage"));

        LangUtil.buttonText(buttonOK, "&Ok");
        LangUtil.buttonText(buttonCancel, "&Cancel");
    }

    private void initComponents() {
        themeComboBox.setModel(new EnumComboBoxModel(Themes.class));
        themeComboBox.setRenderer(new TextableListRenderer());
        themeComboBox.addActionListener(e -> {
            if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
                optionsChanged = true;
            }
        });
        themeComboBox.setSelectedItem(Themes.of(Configuration.instance().options().getTheme()));

        languageComboBox.setModel(new EnumComboBoxModel(Languages.class));
        languageComboBox.setRenderer(new TextableListRenderer());
        languageComboBox.addActionListener(e -> {
            if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
                optionsChanged = true;
            }
        });
        languageComboBox.setSelectedItem(Languages.of(Configuration.instance().options().getLanguage()));

        Set<String> monospaceFonts = loadMonospaceFonts();
        for (String fontName : monospaceFonts) {
            fontComboBox.addItem(fontName);
        }
        fontComboBox.setSelectedItem(Configuration.instance().options().getFontName());
        fontComboBox.addActionListener(e -> {
            if ("comboBoxChanged".equalsIgnoreCase(e.getActionCommand())) {
                optionsChanged = true;
            }
        });

        int fontSize = Configuration.instance().options().getFontSize();
        fontSizeField.setModel(new SpinnerNumberModel(fontSize, 9, 24, 1));
        fontSizeField.setEditor(new JSpinner.NumberEditor(fontSizeField, "####"));
        fontSizeField.addChangeListener(e -> optionsChanged = true);

        boolean autoCompletion = Configuration.instance().options().isAutoCompletion();
        autoCompletionChkBox.setSelected(autoCompletion);
        autoCompletionChkBox.addActionListener(e -> {
            optionsChanged = true;
            autoCompletionDelayField.setEnabled(autoCompletionChkBox.isSelected());
        });

        int delay = Configuration.instance().options().getAutoCompletionDelay();
        autoCompletionDelayField.setModel(new SpinnerNumberModel(delay, 200, 2000, 1));
        autoCompletionDelayField.setEditor(new JSpinner.NumberEditor(autoCompletionDelayField, "####"));
        autoCompletionDelayField.addChangeListener(e -> optionsChanged = true);
        autoCompletionDelayField.setEnabled(autoCompletion);

        boolean autoLoadDeviceNodes = Configuration.instance().options().isAutoLoadDeviceNodes();
        autoLoadDeviceNodesCheckBox.setSelected(autoLoadDeviceNodes);
        autoLoadDeviceNodesCheckBox.addActionListener(e -> optionsChanged = true);

        boolean logInternalSql = Configuration.instance().options().isLogInternalSql();
        logInternalSQLCheckBox.setSelected(logInternalSql);
        logInternalSQLCheckBox.addActionListener(e -> optionsChanged = true);

        boolean addTimestampToLogs = Configuration.instance().options().isLogTimestamp();
        addTimestampToLogsCheckBox.setSelected(addTimestampToLogs);
        addTimestampToLogsCheckBox.addActionListener(e -> optionsChanged = true);

        String timeFormat = Configuration.instance().options().getTimeFormat();
        timeFormatField.setSelectedItem(timeFormat);
        timeFormatField.addActionListener(e -> optionsChanged = true);

        boolean dblclickOpenEditor = Configuration.instance().options().isDblclickOpenEditor();
        dblClickOpenDeviceDataCheckBox.setSelected(dblclickOpenEditor);
        dblClickOpenDeviceDataCheckBox.addActionListener(e -> optionsChanged = true);

        int editorPageSize = Configuration.instance().options().getEditorPageSize();
        pageSizeField.setModel(new SpinnerNumberModel(editorPageSize, 100, 10000, 1));
        pageSizeField.setEditor(new JSpinner.NumberEditor(pageSizeField, "####"));
        pageSizeField.addChangeListener(e -> optionsChanged = true);

        String editorSortOrder = Configuration.instance().options().getEditorSortOrder();
        if (editorSortOrder.equals("desc")) {
            descRadioButton.setSelected(true);
        } else {
            ascRadioButton.setSelected(true);
        }
        ascRadioButton.addActionListener(e -> optionsChanged = true);
        descRadioButton.addActionListener(e -> optionsChanged = true);

        boolean editorAligned = Configuration.instance().options().isEditorAligned();
        alwaysAlignByDeviceCheckBox.setSelected(editorAligned);
        alwaysAlignByDeviceCheckBox.addActionListener(e -> optionsChanged = true);
    }

    private Set<String> loadMonospaceFonts() {
        Set<String> monospacedFonts = new HashSet<>(Arrays.asList(
                "Consolas", "Courier New", "DejaVu Sans Mono", "Droid Sans Mono",
                "Inconsolata", "JetBrains Mono", "Lucida Console", "Monaco",
                "Roboto Mono", "Source Code Pro", "Ubuntu Mono"));
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        List<String> availableFontFamilys = Arrays.asList(graphicsEnvironment.getAvailableFontFamilyNames());
        return monospacedFonts.stream().filter(availableFontFamilys::contains).collect(Collectors.toSet());
    }

    private void onOK() {
        if (optionsChanged) {
            Configuration.instance().options().setLanguage(
                    Optional.ofNullable(languageComboBox.getSelectedItem())
                            .orElse(Locale.getDefault().toLanguageTag()).toString()
            );
            Configuration.instance().options().setTheme(
                    Optional.ofNullable(themeComboBox.getSelectedItem())
                            .orElse(Themes.LIGHT).toString()
            );
            Configuration.instance().options().setFontName((String) fontComboBox.getSelectedItem());
            Configuration.instance().options().setFontSize((int) fontSizeField.getValue());
            Configuration.instance().options().setAutoCompletion(autoCompletionChkBox.isSelected());
            Configuration.instance().options().setAutoCompletionDelay((int) autoCompletionDelayField.getValue());
            Configuration.instance().options().setAutoLoadDeviceNodes(autoLoadDeviceNodesCheckBox.isSelected());
            Configuration.instance().options().setLogInternalSql(logInternalSQLCheckBox.isSelected());
            Configuration.instance().options().setLogTimestamp(addTimestampToLogsCheckBox.isSelected());
            Configuration.instance().options().setTimeFormat((String) timeFormatField.getSelectedItem());

            Configuration.instance().options().setDblclickOpenEditor(dblClickOpenDeviceDataCheckBox.isSelected());
            Configuration.instance().options().setEditorPageSize((int) pageSizeField.getValue());
            Configuration.instance().options().setEditorSortOrder(descRadioButton.isSelected() ? "desc" : "asc");
            Configuration.instance().options().setEditorAligned(alwaysAlignByDeviceCheckBox.isSelected());

            ThemeUtil.setupTheme();
            Configuration.instance().saveOptions();
        }
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(bottomPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        bottomPanel.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        bottomPanel.add(buttonPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        buttonPanel.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        buttonPanel.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(0, 0));
        contentPane.add(mainPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        tabbedPane = new JTabbedPane();
        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:max(p;60dlu):noGrow,left:4dlu:noGrow,fill:p:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow"));
        tabbedPane.addTab("User Interface", panel1);
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        themeLabel = new JLabel();
        themeLabel.setHorizontalAlignment(11);
        themeLabel.setText("Theme");
        CellConstraints cc = new CellConstraints();
        panel1.add(themeLabel, cc.xy(1, 1));
        themeComboBox = new JComboBox();
        panel1.add(themeComboBox, cc.xy(3, 1));
        languageLabel = new JLabel();
        languageLabel.setHorizontalAlignment(11);
        languageLabel.setText("Language");
        panel1.add(languageLabel, cc.xy(1, 3));
        languageComboBox = new JComboBox();
        panel1.add(languageComboBox, cc.xy(3, 3));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FormLayout("fill:max(p;60dlu):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:234px:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        tabbedPane.addTab("SQL Editor", panel2);
        panel2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        fontLabel = new JLabel();
        fontLabel.setHorizontalAlignment(11);
        fontLabel.setText("Font");
        panel2.add(fontLabel, cc.xy(1, 1));
        fontComboBox = new JComboBox();
        panel2.add(fontComboBox, cc.xyw(3, 1, 3));
        fontSizeLabel = new JLabel();
        fontSizeLabel.setHorizontalAlignment(11);
        fontSizeLabel.setText("Font Size");
        panel2.add(fontSizeLabel, cc.xy(1, 3));
        fontSizeField = new JSpinner();
        panel2.add(fontSizeField, cc.xyw(3, 3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        autoCompletionDelayField = new JSpinner();
        panel2.add(autoCompletionDelayField, cc.xy(5, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
        autoCompletionLabel = new JLabel();
        autoCompletionLabel.setHorizontalAlignment(11);
        autoCompletionLabel.setText("Auto Completion");
        panel2.add(autoCompletionLabel, cc.xy(1, 5));
        delayLabel = new JLabel();
        delayLabel.setHorizontalAlignment(10);
        delayLabel.setText("Delay");
        panel2.add(delayLabel, cc.xy(3, 7));
        autoCompletionChkBox = new JCheckBox();
        autoCompletionChkBox.setText("Auto Completion");
        panel2.add(autoCompletionChkBox, cc.xyw(3, 5, 5));
        final JLabel label1 = new JLabel();
        label1.setText("ms");
        panel2.add(label1, cc.xy(7, 7));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:max(p;60dlu):noGrow,left:4dlu:noGrow,fill:p:noGrow,left:p:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:p:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:noGrow"));
        tabbedPane.addTab("Other", panel3);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        timeFormatLabel = new JLabel();
        timeFormatLabel.setHorizontalAlignment(11);
        timeFormatLabel.setText("Time Format");
        panel3.add(timeFormatLabel, cc.xy(1, 15));
        timeFormatField = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ss.SSS");
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ss");
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ss.SSSZZ");
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ss.SSSz");
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ssZZ");
        defaultComboBoxModel1.addElement("yyyy-MM-dd HH:mm:ssz");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ss.SSS");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ss");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ss.SSSZZ");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ss.SSSz");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ssZZ");
        defaultComboBoxModel1.addElement("yyyy/MM/dd HH:mm:ssz");
        defaultComboBoxModel1.addElement("timestamp");
        timeFormatField.setModel(defaultComboBoxModel1);
        panel3.add(timeFormatField, cc.xyw(3, 15, 7));
        autoLoadDeviceNodesCheckBox = new JCheckBox();
        autoLoadDeviceNodesCheckBox.setText("Auto Load Device Nodes");
        panel3.add(autoLoadDeviceNodesCheckBox, cc.xyw(3, 1, 7));
        sessionTreeLabel = new JLabel();
        sessionTreeLabel.setHorizontalAlignment(11);
        sessionTreeLabel.setText("Session Tree");
        panel3.add(sessionTreeLabel, cc.xy(1, 1));
        sqlLogLabel = new JLabel();
        sqlLogLabel.setHorizontalAlignment(11);
        sqlLogLabel.setText("SQL Log");
        panel3.add(sqlLogLabel, cc.xy(1, 11));
        logInternalSQLCheckBox = new JCheckBox();
        logInternalSQLCheckBox.setText("Log Internal SQL");
        panel3.add(logInternalSQLCheckBox, cc.xyw(3, 11, 7));
        addTimestampToLogsCheckBox = new JCheckBox();
        addTimestampToLogsCheckBox.setText("Add Timestamp to All Log Messages");
        panel3.add(addTimestampToLogsCheckBox, cc.xyw(3, 13, 7));
        dblClickOpenDeviceDataCheckBox = new JCheckBox();
        dblClickOpenDeviceDataCheckBox.setText("Double Click The Device Node To Open The Data Editor");
        panel3.add(dblClickOpenDeviceDataCheckBox, cc.xyw(3, 3, 7));
        ascRadioButton = new JRadioButton();
        ascRadioButton.setText("Asc");
        panel3.add(ascRadioButton, cc.xy(9, 7));
        descRadioButton = new JRadioButton();
        descRadioButton.setText("Desc");
        panel3.add(descRadioButton, cc.xy(7, 7));
        pageSizeField = new JSpinner();
        panel3.add(pageSizeField, cc.xyw(7, 9, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        rowsPerPageLabel = new JLabel();
        rowsPerPageLabel.setText("Rows Per Page");
        panel3.add(rowsPerPageLabel, cc.xy(5, 9));
        orderByTimeLabel = new JLabel();
        orderByTimeLabel.setText("Order by Time");
        panel3.add(orderByTimeLabel, cc.xy(5, 7));
        alwaysAlignByDeviceCheckBox = new JCheckBox();
        alwaysAlignByDeviceCheckBox.setText("Always Align By Device");
        panel3.add(alwaysAlignByDeviceCheckBox, cc.xyw(5, 5, 5));
        deviceDataLabel = new JLabel();
        deviceDataLabel.setHorizontalAlignment(11);
        deviceDataLabel.setText("Device Data");
        panel3.add(deviceDataLabel, cc.xy(1, 5));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(descRadioButton);
        buttonGroup.add(ascRadioButton);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
