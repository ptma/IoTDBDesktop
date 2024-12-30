package org.apache.iotdb.desktop.form;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.desktop.component.MapCellEditor;
import org.apache.iotdb.desktop.component.MetricsTableModel;
import org.apache.iotdb.desktop.component.MetricsTableRendererProvider;
import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.exception.VerificationException;
import org.apache.iotdb.desktop.model.*;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DeviceInfo extends TabPanel {

    private static final DefaultCellEditor DATATYPE_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(TSDataType.values()).map(TSDataType::name).toArray()));
    private static final DefaultCellEditor ENCODING_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(TSEncoding.values()).map(TSEncoding::name).toArray()));
    private static final DefaultCellEditor COMPRESSION_EDITOR = new DefaultCellEditor(new JComboBox<>(Arrays.stream(CompressionType.values()).map(CompressionType::name).toArray()));
    private static final DefaultCellEditor TAGS_EDITOR = new MapCellEditor(LangUtil.getString("Tags"), new JTextField());
    private static final DefaultCellEditor ATTRS_EDITOR = new MapCellEditor(LangUtil.getString("Attributes"), new JTextField());

    private JPanel rootPanel;
    private JTextField nameField;
    private JRadioButton radioYes;
    private JRadioButton radioNo;
    private JButton btnAddMetric;
    private JButton btnRemoveMetric;
    private JButton btnApply;
    private JXTable metricsTable;
    private JScrollPane metricsScrollPanel;
    private JLabel labelName;
    private JLabel labelAlign;
    private JLabel labelMetrics;
    private JTextField templateField;
    private JLabel labelTemplate;
    private JTextField ttlField;
    private JLabel labelTTL;

    private MetricsTableModel metricsTableModel;

    private final Device device;

    public DeviceInfo(Device device) {
        super();
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        this.device = device;
        initComponents();

        loadTimeseries();

        this.addComponentListener(new ComponentAdapter() {
            public void componentShown(ComponentEvent e) {
                metricsTable.requestFocusInWindow();
            }
        });
    }

    private void initComponents() {
        nameField.setText(device.getPath());
        if (device.isAligned()) {
            radioYes.setSelected(true);
        } else {
            radioNo.setSelected(true);
        }
        templateField.setText(device.getTemplate());
        ttlField.setText(device.getTtl());
        metricsTable.setRowHeight(24);
        metricsTable.setEditable(true);
        metricsTable.setShowHorizontalLines(true);
        metricsTable.setShowVerticalLines(true);
        metricsTable.setRowSelectionAllowed(true);
        metricsTable.setSortable(false);
        metricsTable.setIntercellSpacing(new Dimension(1, 1));
        metricsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        metricsTableModel = new MetricsTableModel(device.getPath());
        DefaultTableRenderer metricsTableRenderer = new DefaultTableRenderer(new MetricsTableRendererProvider());
        metricsTable.setModel(metricsTableModel);
        metricsTable.getTableHeader().setReorderingAllowed(false);
        metricsTable.setDefaultRenderer(Object.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(String.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(Number.class, metricsTableRenderer);
        metricsTable.setDefaultRenderer(Boolean.class, metricsTableRenderer);
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
        metricsTableModel.addTableModelListener(e -> {
            btnApply.setEnabled(metricsTableModel.isModified());
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
        btnApply.addActionListener(e -> applayMetrics());
        btnAddMetric.setIcon(Icons.ADD);
        btnRemoveMetric.setIcon(Icons.DELETE);

        labelName.setText(LangUtil.getString("DeviceName"));
        labelAlign.setText(LangUtil.getString("IsAligned"));
        radioYes.setText(LangUtil.getString("Yes"));
        radioNo.setText(LangUtil.getString("No"));
        labelTTL.setText(LangUtil.getString("TTL"));
        labelTemplate.setText(LangUtil.getString("Template"));
        labelMetrics.setText(LangUtil.getString("DeviceMetrics"));
        btnAddMetric.setText(LangUtil.getString("AddMetric"));
        btnRemoveMetric.setText(LangUtil.getString("RemoveMetric"));
        btnApply.setText(LangUtil.getString("Apply"));
    }

    private void initTableCellEditors() {
        metricsTable.getColumn(3).setCellEditor(DATATYPE_EDITOR);
        metricsTable.getColumn(4).setCellEditor(ENCODING_EDITOR);
        metricsTable.getColumn(5).setCellEditor(COMPRESSION_EDITOR);
        metricsTable.getColumn(6).setCellEditor(TAGS_EDITOR);
        metricsTable.getColumn(7).setCellEditor(ATTRS_EDITOR);
    }

    private void loadTimeseries() {
        metricsTableModel.clear();

        SwingWorker<List<Metric>, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    metricsTableModel.setMetrics(get());
                    initTableCellEditors();
                    Utils.autoResizeTableColumns(metricsTable, 0);
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected List<Metric> doInBackground() throws Exception {
                String devicePath = device.getPath();
                return device.getSession().loadTimeseries(devicePath);
            }
        };
        worker.execute();
    }

    private void verifyFields() throws VerificationException {
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

    private void applayMetrics() {
        try {
            verifyFields();
        } catch (VerificationException e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }

        SwingWorker<Throwable, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Throwable e = get();
                    if (e != null) {
                        Utils.Message.error(e.getMessage(), e);
                    } else {
                        Utils.Toast.success(LangUtil.getString("ModifiedSuccess"));
                        loadTimeseries();
                    }
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Throwable doInBackground() {
                try {
                    for (Metric metric : metricsTableModel.getMetrics()) {
                        // 变更已有的序列
                        if (metric.isExisting() && metric.isModified()) {
                            // 删除标签和属性
                            // ALTER timeseries root.turbine.d1.s1 DROP tag1, tag2, attr2
                            Map<String, String> dropMap = metric.getTagsRemoved();
                            dropMap.putAll(metric.getAttributesRemoved());
                            if (!dropMap.isEmpty()) {
                                StringBuilder dropSql = new StringBuilder("alter timeseries ");
                                dropSql.append(device.getPath())
                                        .append(".")
                                        .append(metric.getName())
                                        .append(" drop ")
                                        .append(Utils.mapKeyToString(dropMap));
                                device.getSession().execute(dropSql.toString());
                            }
                            // 更新别名、标签和属性
                            // ALTER timeseries root.turbine.d1.s1 UPSERT ALIAS=newAlias TAGS(tag2=newV2, tag3=v3) ATTRIBUTES(attr3=v3, attr4=v4)

                            StringBuilder upsetSql = new StringBuilder("alter timeseries ");
                            upsetSql.append(device.getPath())
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
                            device.getSession().execute(upsetSql.toString());
                        } else if (!metric.isExisting()) {
                            // 添加新的序列
                            // CREATE ALIGNED TIMESERIES root.ln.wf01.GPS(latitude FLOAT encoding=PLAIN compressor=SNAPPY, longitude FLOAT encoding=PLAIN compressor=SNAPPY)
                            // CREATE timeseries root.turbine.d1.s1.temprature with
                            // datatype=FLOAT, encoding=RLE, compression=SNAPPY tags(tag1=v1, tag2=v2) attributes(attr1=v1, attr2=v2)
                            StringBuilder createSql = new StringBuilder("create ");
                            if (device.isAligned()) {
                                createSql.append("aligned timeseries ")
                                        .append(device.getPath())
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
                                        .append(device.getPath())
                                        .append(".")
                                        .append(metric.getName())
                                        .append(" with ")
                                        .append("datatype=").append(metric.getDataType()).append(",")
                                        .append("encoding=").append(metric.getEncoding()).append(",")
                                        .append("compression=").append(metric.getCompression());
                            }
                            device.getSession().execute(createSql.toString());

                            if (metric.isAliasModified() || metric.isTagsModified() || metric.isAttributesModified()) {
                                StringBuilder upsetSql = new StringBuilder("alter timeseries ");
                                upsetSql.append(device.getPath())
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
                                device.getSession().execute(upsetSql.toString());
                            }
                        }
                    }
                    // 删除序列,  delete timeseries root.ln.wf01.wt01.temperature, root.ln.wf02.wt02.hardware
                    if (!metricsTableModel.getDroppedMetrics().isEmpty()) {
                        StringJoiner deleteSql = new StringJoiner(",", "delete timeseries ", "");
                        for (Metric metric : metricsTableModel.getDroppedMetrics()) {
                            deleteSql.add(String.format("%s.%s", device.getPath(), metric.getName()));
                        }
                        device.getSession().execute(deleteSql.toString());
                    }
                    return null;
                } catch (Throwable e) {
                    return e;
                }
            }
        };
        worker.execute();
    }

    @Override
    public Session getSession() {
        return device.getSession();
    }

    @Override
    public String getTabbedKey() {
        return device.getKey();
    }

    @Override
    public boolean refreshable() {
        return true;
    }

    @Override
    public void refresh() {
        loadTimeseries();
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
        rootPanel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:50dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:100px:noGrow,left:4dlu:noGrow,fill:199px:grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,top:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        labelName = new JLabel();
        labelName.setHorizontalAlignment(11);
        labelName.setText("设备名称");
        CellConstraints cc = new CellConstraints();
        rootPanel.add(labelName, cc.xy(1, 1));
        labelAlign = new JLabel();
        labelAlign.setText("是否按设备对齐");
        rootPanel.add(labelAlign, cc.xy(1, 5));
        nameField = new JTextField();
        nameField.setEditable(false);
        nameField.setEnabled(true);
        rootPanel.add(nameField, cc.xyw(3, 1, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        radioYes = new JRadioButton();
        radioYes.setEnabled(false);
        radioYes.setText("是");
        rootPanel.add(radioYes, cc.xy(3, 5));
        radioNo = new JRadioButton();
        radioNo.setEnabled(false);
        radioNo.setText("否");
        rootPanel.add(radioNo, cc.xy(5, 5));
        labelMetrics = new JLabel();
        labelMetrics.setHorizontalAlignment(11);
        labelMetrics.setText("设备物理量");
        rootPanel.add(labelMetrics, cc.xy(1, 7));
        metricsScrollPanel = new JScrollPane();
        rootPanel.add(metricsScrollPanel, cc.xyw(3, 7, 9, CellConstraints.FILL, CellConstraints.FILL));
        metricsTable = new JXTable();
        metricsTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        metricsScrollPanel.setViewportView(metricsTable);
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(panel1, cc.xyw(3, 9, 9));
        btnAddMetric = new JButton();
        btnAddMetric.setText("添加物理量");
        panel1.add(btnAddMetric, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnRemoveMetric = new JButton();
        btnRemoveMetric.setText("删除物理量");
        panel1.add(btnRemoveMetric, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        btnApply = new JButton();
        btnApply.setEnabled(false);
        btnApply.setText("应用");
        panel1.add(btnApply, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        labelTTL = new JLabel();
        labelTTL.setHorizontalAlignment(11);
        labelTTL.setText("TTL");
        rootPanel.add(labelTTL, cc.xy(7, 5));
        ttlField = new JTextField();
        ttlField.setEditable(false);
        ttlField.setHorizontalAlignment(11);
        rootPanel.add(ttlField, cc.xy(9, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        labelTemplate = new JLabel();
        labelTemplate.setHorizontalAlignment(11);
        labelTemplate.setText("模板");
        rootPanel.add(labelTemplate, cc.xy(1, 3));
        templateField = new JTextField();
        templateField.setEditable(false);
        rootPanel.add(templateField, cc.xyw(3, 3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(radioYes);
        buttonGroup.add(radioNo);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
