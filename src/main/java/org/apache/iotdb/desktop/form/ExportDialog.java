package org.apache.iotdb.desktop.form;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.QueryResultTable;
import org.apache.iotdb.desktop.component.QueryResultTableModel;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.tool.data.CSVPrinterWrapper;
import org.apache.iotdb.tool.data.TextPrinter;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

public class ExportDialog extends JDialog {
    private JPanel contentPane;
    private JButton startButton;
    private JButton closeButton;
    private JComboBox formatComboBox;
    private JRadioButton selectionRadio;
    private JRadioButton allRowsRadio;
    private JCheckBox includeColumnCheckBox;
    private JLabel formatLabel;
    private JCheckBox removeLinebreaksCheckBox;
    private JLabel optionsLabel;
    private JLabel recordsLabel;
    private JTextArea textOutput;

    private QueryResultTable table;
    private QueryResultTableModel tableModel;

    public static void export(QueryResultTable table, QueryResultTableModel tableModel) {
        ExportDialog exportDialog = new ExportDialog(IotdbDesktopApp.frame, table, tableModel);
        exportDialog.setMinimumSize(new Dimension(450, 300));
        exportDialog.setResizable(false);
        exportDialog.pack();
        exportDialog.setLocationRelativeTo(IotdbDesktopApp.frame);
        exportDialog.setVisible(true);
    }

    public ExportDialog(Frame owner, QueryResultTable table, QueryResultTableModel tableModel) {
        super(owner);
        this.table = table;
        this.tableModel = tableModel;
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(startButton);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        closeButton.addActionListener(new ActionListener() {
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

        formatLabel.setText(LangUtil.getString("Format"));
        recordsLabel.setText(LangUtil.getString("Records"));
        selectionRadio.setText(LangUtil.getString("Selection"));
        allRowsRadio.setText(LangUtil.getString("AllRows"));
        optionsLabel.setText(LangUtil.getString("Options"));
        includeColumnCheckBox.setText(LangUtil.getString("IncludeColumnNames"));
        removeLinebreaksCheckBox.setText(LangUtil.getString("RemoveLinebreaks"));
        setTitle(LangUtil.getString("Export"));
        LangUtil.buttonText(startButton, "&Start");
        LangUtil.buttonText(closeButton, "&Close");
    }

    private List<Map<String, Object>> getExportingRows() {
        List<Map<String, Object>> exportingRows;
        if (selectionRadio.isSelected()) {
            int[] rows = table.getSelectedRows();
            exportingRows = Arrays.stream(rows)
                    .map(table::convertColumnIndexToModel)
                    .mapToObj(i -> tableModel.getResult().getDatas().get(i))
                    .toList();
        } else {
            exportingRows = tableModel.getResult().getDatas();
        }
        exportingRows.forEach(row -> {
            row.entrySet().forEach((entry) -> {
                if (entry.getKey().equalsIgnoreCase("Time") && entry.getValue() != null) {
                    try {
                        entry.setValue(DateUtil.format(new Date((Long) entry.getValue()), Configuration.instance().options().getTimeFormat()));
                    } catch (Exception ignored) {
                    }
                }
            });
        });
        return exportingRows;
    }

    private void exportToJson() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(LangUtil.getString("JsonFileFilter"), "json"));
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

            textOutput.setText("");
            TextPrinter textPrinter = new TextPrinter(textOutput);
            textPrinter.println(LangUtil.format("ExportTargetFile", file.getAbsolutePath()));
            textPrinter.println(LangUtil.format("ExportTargetFormat", "JSON"));
            try {
                startButton.setEnabled(false);
                List<Map<String, Object>> exportingRows = getExportingRows();
                FileUtil.writeString(Utils.JSON.toString(exportingRows), file, StandardCharsets.UTF_8);
                textPrinter.println(LangUtil.format("SuccessfullyExportedRows", exportingRows.size()));
            } catch (JsonProcessingException ex) {
                textPrinter.printException(ex);
                startButton.setEnabled(true);
            }
        }
    }

    private void exportToCsv() {
        JFileChooser jFileChooser = new JFileChooser();
        jFileChooser.setAcceptAllFileFilterUsed(false);
        jFileChooser.addChoosableFileFilter(new FileNameExtensionFilter(LangUtil.getString("CsvFileFilter"), "csv"));
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
            textOutput.setText("");
            TextPrinter textPrinter = new TextPrinter(textOutput);
            textPrinter.println(LangUtil.format("ExportTargetFile", file.getAbsolutePath()));
            textPrinter.println(LangUtil.format("ExportTargetFormat", "CSV"));
            try {
                startButton.setEnabled(false);
                CSVPrinterWrapper csvPrinter = new CSVPrinterWrapper(file.getAbsolutePath(), textPrinter);
                List<String> columnNames = tableModel.getResult().getColumns();
                // title
                if (includeColumnCheckBox.isSelected()) {
                    csvPrinter.printRecord(columnNames);
                }
                // data
                List<Map<String, Object>> exportingRows = getExportingRows();
                for (Map<String, Object> data : exportingRows) {
                    final Map<String, Object> rowData = data;
                    csvPrinter.printRecord(
                            columnNames.stream().map(columnName -> {
                                Object cellValue = rowData.get(columnName);
                                String stringValue = cellValue == null ? "" : cellValue.toString();
                                if (removeLinebreaksCheckBox.isSelected()) {
                                    stringValue = stringValue.replaceAll("\n", "");
                                }
                                return stringValue;
                            }).toList()
                    );
                }
                csvPrinter.flush();
                csvPrinter.close();
                textPrinter.println(LangUtil.format("SuccessfullyExportedRows", exportingRows.size()));
            } catch (Exception ex) {
                textPrinter.printException(ex);
                startButton.setEnabled(true);
            }
        }
    }

    private void onOK() {
        String format = (String) formatComboBox.getSelectedItem();
        switch (format) {
            case "CSV":
                exportToCsv();
                break;
            case "JSON":
                exportToJson();
                break;
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
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
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, 10));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        startButton = new JButton();
        startButton.setText("Start");
        panel2.add(startButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        closeButton = new JButton();
        closeButton.setText("Close");
        panel2.add(closeButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow"));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        formatLabel = new JLabel();
        formatLabel.setHorizontalAlignment(11);
        formatLabel.setText("Format");
        CellConstraints cc = new CellConstraints();
        panel3.add(formatLabel, cc.xy(1, 1));
        formatComboBox = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("JSON");
        defaultComboBoxModel1.addElement("CSV");
        formatComboBox.setModel(defaultComboBoxModel1);
        panel3.add(formatComboBox, cc.xyw(3, 1, 3));
        recordsLabel = new JLabel();
        recordsLabel.setHorizontalAlignment(11);
        recordsLabel.setText("Records");
        panel3.add(recordsLabel, cc.xy(1, 3));
        optionsLabel = new JLabel();
        optionsLabel.setHorizontalAlignment(11);
        optionsLabel.setText("Options");
        panel3.add(optionsLabel, cc.xy(1, 5));
        includeColumnCheckBox = new JCheckBox();
        includeColumnCheckBox.setSelected(true);
        includeColumnCheckBox.setText("Include Column Names");
        panel3.add(includeColumnCheckBox, cc.xyw(3, 5, 3));
        removeLinebreaksCheckBox = new JCheckBox();
        removeLinebreaksCheckBox.setText("Remove Linebreaks");
        panel3.add(removeLinebreaksCheckBox, cc.xyw(3, 7, 3));
        allRowsRadio = new JRadioButton();
        allRowsRadio.setText("All Rows");
        panel3.add(allRowsRadio, cc.xy(5, 3));
        selectionRadio = new JRadioButton();
        selectionRadio.setSelected(true);
        selectionRadio.setText("Selection");
        panel3.add(selectionRadio, cc.xy(3, 3));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel3.add(scrollPane1, cc.xyw(1, 9, 5, CellConstraints.FILL, CellConstraints.FILL));
        textOutput = new JTextArea();
        textOutput.setEditable(false);
        scrollPane1.setViewportView(textOutput);
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(selectionRadio);
        buttonGroup.add(allRowsRadio);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
