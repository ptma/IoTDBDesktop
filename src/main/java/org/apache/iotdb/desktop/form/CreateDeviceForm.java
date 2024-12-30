package org.apache.iotdb.desktop.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.MapCellEditor;
import org.apache.iotdb.desktop.component.MetricsTableModel;
import org.apache.iotdb.desktop.component.MetricsTableRendererProvider;
import org.apache.iotdb.desktop.exception.VerificationException;
import org.apache.iotdb.desktop.model.Database;
import org.apache.iotdb.desktop.model.Metric;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.desktop.util.Validator;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CreateDeviceForm extends JDialog {

    private static final DefaultCellEditor DATATYPE_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(TSDataType.values()).map(TSDataType::name).toArray()));
    private static final DefaultCellEditor ENCODING_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(TSEncoding.values()).map(TSEncoding::name).toArray()));
    private static final DefaultCellEditor COMPRESSION_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(CompressionType.values()).map(CompressionType::name).toArray()));
    private static final DefaultCellEditor TAGS_EDITOR = new MapCellEditor(LangUtil.getString("Tags"), new JTextField());
    private static final DefaultCellEditor ATTRS_EDITOR = new MapCellEditor(LangUtil.getString("Attributes"), new JTextField());

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField nameField;
    private JRadioButton radioNo;
    private JRadioButton radioYes;
    private JButton btnAddMetric;
    private JButton btnRemoveMetric;
    private JXTable metricsTable;
    private JScrollPane metricsScrollPanel;
    private JLabel labelMetrics;
    private JLabel labelAlign;
    private JLabel labelName;

    private Database database;
    private Runnable afterCreated;

    private MetricsTableModel metricsTableModel;

    public static void open(Database database, Runnable afterCreated) {
        JDialog dialog = new CreateDeviceForm(IotdbDesktopApp.frame, database, afterCreated);
        dialog.setMinimumSize(new Dimension(800, 400));
        dialog.setPreferredSize(new Dimension(800, 500));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(IotdbDesktopApp.frame);
        dialog.setVisible(true);
    }

    public CreateDeviceForm(Frame owner, Database database, Runnable afterCreated) {
        super(owner);
        this.database = database;
        this.afterCreated = afterCreated;
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initComponents();

        setTitle(LangUtil.getString("NewDevice"));
    }

    private void initComponents() {
        buttonOK.setEnabled(false);
        radioYes.setSelected(true);
        JTextField prefixField = new JTextField();
        prefixField.setText(database.getName() + ".");
        prefixField.setEnabled(false);
        prefixField.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
        nameField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, prefixField);

        metricsTable.setRowHeight(24);
        metricsTable.setEditable(true);
        metricsTable.setShowHorizontalLines(true);
        metricsTable.setShowVerticalLines(true);
        metricsTable.setRowSelectionAllowed(true);
        metricsTable.setSortable(false);
        metricsTable.setIntercellSpacing(new Dimension(1, 1));
        metricsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        metricsTableModel = new MetricsTableModel("");
        DefaultTableRenderer metricsTableRenderer = new DefaultTableRenderer(new MetricsTableRendererProvider());
        metricsTable.setModel(metricsTableModel);
        metricsTable.getTableHeader().setReorderingAllowed(false);
        metricsTable.setDefaultRenderer(Object.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(String.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(Boolean.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(Number.class, metricsTableRenderer);
        metricsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    final int rowIndex = metricsTable.rowAtPoint(e.getPoint());
                    final int colIndex = metricsTable.columnAtPoint(e.getPoint());
                    boolean onRow = rowIndex >= 0;
                    boolean onColumn = colIndex >= 0;
                    if (onRow && onColumn) {
                        int[] selRows = metricsTable.getSelectedRows();
                        if (Arrays.stream(selRows).noneMatch(i -> i == rowIndex)) {
                            metricsTable.setRowSelectionInterval(rowIndex, rowIndex);
                        }
                        metricsTable.setColumnSelectionInterval(colIndex, colIndex);
                    }
                }
            }
        });
        btnRemoveMetric.setEnabled(false);
        metricsTable.getSelectionModel().addListSelectionListener(e -> {
            btnRemoveMetric.setEnabled(metricsTable.getSelectedRowCount() > 0);
        });
        metricsTable.getColumn(3).setCellEditor(DATATYPE_EDITOR);
        metricsTable.getColumn(4).setCellEditor(ENCODING_EDITOR);
        metricsTable.getColumn(5).setCellEditor(COMPRESSION_EDITOR);
        metricsTable.getColumn(6).setCellEditor(TAGS_EDITOR);
        metricsTable.getColumn(7).setCellEditor(ATTRS_EDITOR);
        metricsTableModel.addTableModelListener(e -> {
            buttonOK.setEnabled(metricsTableModel.isModified());
        });
        btnAddMetric.addActionListener(e -> {
            metricsTableModel.appendRow();
        });
        btnRemoveMetric.addActionListener(e -> {
            int[] selRows = metricsTable.getSelectedRows();
            if (selRows.length > 0) {
                for (int i = selRows.length - 1; i >= 0; i--) {
                    int modelRowIndex = metricsTable.convertRowIndexToModel(selRows[i]);
                    metricsTableModel.removeRow(modelRowIndex);
                }
            }
        });

        btnAddMetric.setIcon(Icons.ADD);
        btnRemoveMetric.setIcon(Icons.DELETE);

        labelName.setText(LangUtil.getString("DeviceName"));
        labelAlign.setText(LangUtil.getString("IsAligned"));
        radioYes.setText(LangUtil.getString("Yes"));
        radioNo.setText(LangUtil.getString("No"));
        labelMetrics.setText(LangUtil.getString("DeviceMetrics"));
        btnAddMetric.setText(LangUtil.getString("AddMetric"));
        btnRemoveMetric.setText(LangUtil.getString("RemoveMetric"));
        LangUtil.buttonText(buttonOK, "&Ok");
        LangUtil.buttonText(buttonCancel, "&Cancel");
    }

    private void verifyFields() throws VerificationException {
        Validator.notEmpty(nameField, () -> LangUtil.format("FieldRequiredValidation", labelName.getText()));
        for (Metric metric : metricsTableModel.getMetrics()) {
            if (metric.getName() == null || metric.getName().isBlank()) {
                throw new VerificationException(LangUtil.getString("MetricNameIsEmpty"));
            } else if (metric.getDataType() == null || metric.getDataType().isBlank()) {
                throw new VerificationException(LangUtil.getString("MetricDataTypeIsEmpty"));
            } else if (metric.getEncoding() == null || metric.getEncoding().isBlank()) {
                throw new VerificationException(LangUtil.getString("MetricEncodingIsEmpty"));
            } else if (metric.getCompression() == null || metric.getCompression().isBlank()) {
                throw new VerificationException(LangUtil.getString("MetricCompressionIsEmpty"));
            }
        }
        Optional<String> duplicateName = metricsTableModel.getMetrics()
            .stream()
            .collect(Collectors.groupingBy(Metric::getName, Collectors.counting()))
            .entrySet()
            .stream()
            .filter(e -> e.getValue() > 1)
            .map(Map.Entry::getKey)
            .findFirst();
        if (duplicateName.isPresent()) {
            throw new VerificationException(LangUtil.format("MetricNameIsDuplicated", duplicateName.get()));
        }
    }

    private void onOK() {
        try {
            verifyFields();
        } catch (VerificationException e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }
        final String devicePath = database.getName() + "." + nameField.getText();
        SwingWorker<Throwable, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Throwable e = get();
                    if (e != null) {
                        Utils.Message.error(e.getMessage(), e);
                    } else {
                        Utils.Toast.success(String.format(LangUtil.getString("NewDeviceSuccess"), devicePath));
                        dispose();
                        afterCreated.run();
                    }
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Throwable doInBackground() {
                try {
                    metricsTableModel.setPath(devicePath);
                    for (Metric metric : metricsTableModel.getMetrics()) {
                        // 添加新的序列
                        // CREATE ALIGNED TIMESERIES root.ln.wf01.GPS(latitude FLOAT encoding=PLAIN compressor=SNAPPY, longitude FLOAT encoding=PLAIN compressor=SNAPPY)
                        // CREATE timeseries root.turbine.d1.s1.temprature with
                        // datatype=FLOAT, encoding=RLE, compression=SNAPPY tags(tag1=v1, tag2=v2) attributes(attr1=v1, attr2=v2)
                        StringBuilder createSql = new StringBuilder("create ");
                        if (radioYes.isSelected()) {
                            createSql.append("aligned timeseries ")
                                .append(devicePath)
                                .append("(")
                                .append(metric.getName())
                                .append(" ")
                                .append(metric.getDataType())
                                .append(" ")
                                .append("encoding=").append(metric.getEncoding())
                                .append(" ")
                                .append("compression=").append(metric.getCompression())
                                .append(")");
                        } else {
                            createSql.append("timeseries ")
                                .append(devicePath)
                                .append(".")
                                .append(metric.getName())
                                .append(" with ")
                                .append("datatype=").append(metric.getDataType()).append(",")
                                .append("encoding=").append(metric.getEncoding()).append(",")
                                .append("compression=").append(metric.getCompression());
                        }
                        database.getSession().execute(createSql.toString());

                        if (metric.isAliasModified() || metric.isTagsModified() || metric.isAttributesModified()) {
                            StringBuilder upsetSql = new StringBuilder("alter timeseries ");
                            upsetSql.append(devicePath)
                                .append(".")
                                .append(metric.getName())
                                .append(" upsert");
                            if (metric.isAliasModified()) {
                                upsetSql.append(" alias=").append(metric.getAliasModified());
                            }
                            Map<String, String> upsetTags = metric.getTagsModified();
                            if (!upsetTags.isEmpty()) {
                                upsetSql.append(" tags(").append(Utils.mapToString(upsetTags)).append(")");
                            }
                            Map<String, String> upsetAttrs = metric.getAttributesModified();
                            if (!upsetAttrs.isEmpty()) {
                                upsetSql.append(" attributes(").append(Utils.mapToString(upsetAttrs)).append(")");
                            }
                            database.getSession().execute(upsetSql.toString());
                        }
                    }
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            }
        };
        worker.execute();
    }

    private void onCancel() {
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        labelName = new JLabel();
        labelName.setHorizontalAlignment(11);
        labelName.setText("设备名称");
        CellConstraints cc = new CellConstraints();
        panel3.add(labelName, cc.xy(1, 1));
        nameField = new JTextField();
        panel3.add(nameField, cc.xyw(3, 1, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        labelAlign = new JLabel();
        labelAlign.setHorizontalAlignment(11);
        labelAlign.setText("是否按设备对齐");
        panel3.add(labelAlign, cc.xy(1, 3));
        radioNo = new JRadioButton();
        radioNo.setText("否");
        panel3.add(radioNo, cc.xy(6, 3));
        radioYes = new JRadioButton();
        radioYes.setText("是");
        panel3.add(radioYes, cc.xy(4, 3));
        labelMetrics = new JLabel();
        labelMetrics.setHorizontalAlignment(11);
        labelMetrics.setText("设备物理量");
        panel3.add(labelMetrics, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.TOP));
        metricsScrollPanel = new JScrollPane();
        panel3.add(metricsScrollPanel, cc.xyw(4, 5, 5, CellConstraints.FILL, CellConstraints.FILL));
        metricsTable = new JXTable();
        metricsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        metricsScrollPanel.setViewportView(metricsTable);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, cc.xyw(4, 7, 5));
        btnAddMetric = new JButton();
        btnAddMetric.setText("添加物理量");
        panel4.add(btnAddMetric, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRemoveMetric = new JButton();
        btnRemoveMetric.setText("删除物理量");
        panel4.add(btnRemoveMetric, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        labelName.setLabelFor(nameField);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioYes);
        buttonGroup.add(radioNo);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
