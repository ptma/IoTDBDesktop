package org.apache.iotdb.desktop.form;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.component.TextEditor;
import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.desktop.util.Validator;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.RpcUtils;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.tool.data.CSVPrinterWrapper;
import org.apache.iotdb.tool.data.TextPrinter;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.exception.write.WriteProcessException;
import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.apache.tsfile.fileSystem.FSFactoryProducer;
import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.Path;
import org.apache.tsfile.read.common.RowRecord;
import org.apache.tsfile.write.TsFileWriter;
import org.apache.tsfile.write.record.Tablet;
import org.apache.tsfile.write.schema.MeasurementSchema;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javax.swing.SwingWorker.StateValue.STARTED;


public class DataExport extends TabPanel {
    public static final String TABBED_KEY = "export-data";
    private static final String SYSTEM_DATABASE = "root.__system";
    private static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

    private JPanel rootPanel;
    private JLabel fileFormatLabel;
    private JRadioButton csvRadio;
    private JRadioButton sqlRadio;
    private JTextField dumpDirectoryField;
    private JComboBox timeFormatField;
    private JSpinner maxRowsField;
    private JLabel targetDirectoryLabel;
    private JLabel timeFormatLabel;
    private JLabel maxRowsLabel;
    private JSpinner timeoutField;
    private JLabel sqlLabel;
    private JTextArea outputArea;
    private JButton executeButton;
    private TextEditor sqlEditor;
    private JTextField dumpFilenameField;
    private JLabel dumpFilenameLabel;
    private JCheckBox dataTypeInHeaderCheckBox;
    private JCheckBox alignedCheckBox;
    private JRadioButton tsFileRadio;
    private JLabel outputLabel;
    private JLabel timeoutLabel;
    private TextPrinter textPrinter;

    private final Session session;

    private String dumpType;
    private String dumpPath;
    private String dumpFilename;
    private String timeFormat;
    private int linesPerFile;
    private String timestampPrecision;
    private long timeout;
    private boolean withDataType;
    private boolean isAligned;

    private SwingWorker<Void, Void> dumpWorker;

    public DataExport(Session session) {
        super();
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        this.session = session;

        maxRowsField.setModel(new SpinnerNumberModel(10000, 1000, Integer.MAX_VALUE, 1));
        maxRowsField.setEditor(new JSpinner.NumberEditor(maxRowsField, "####"));

        timeoutField.setModel(new SpinnerNumberModel(-1, -1, Integer.MAX_VALUE, 1));
        timeoutField.setEditor(new JSpinner.NumberEditor(timeoutField, "####"));

        dumpFilenameField.setText("dump");

        ChangeListener dumpTypeChange = (e) -> {
            dataTypeInHeaderCheckBox.setEnabled(csvRadio.isSelected());
            timeFormatField.setEnabled(csvRadio.isSelected());
            maxRowsField.setEnabled(csvRadio.isSelected() || sqlRadio.isSelected());
            alignedCheckBox.setEnabled(csvRadio.isSelected() || sqlRadio.isSelected());
        };
        csvRadio.addChangeListener(dumpTypeChange);
        sqlRadio.addChangeListener(dumpTypeChange);
        tsFileRadio.addChangeListener(dumpTypeChange);

        JButton browserFileButton = new JButton(Icons.OPEN);
        JToolBar fileFieldToolbar = new JToolBar();
        fileFieldToolbar.add(browserFileButton);
        fileFieldToolbar.setBorder(null);
        dumpDirectoryField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, fileFieldToolbar);
        browserFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fileChooser.setDialogTitle(LangUtil.getString("SelectDirectory"));
            fileChooser.setLocale(LangUtil.getLocale());
            String dumpDirectory = Configuration.instance().getString(ConfKeys.DUMP_DIRECTORY, "");
            if (StrUtil.isNotBlank(dumpDirectory)) {
                fileChooser.setCurrentDirectory(new File(dumpDirectory));
            }

            int userSelection = fileChooser.showOpenDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFolder = fileChooser.getSelectedFile();
                Configuration.instance().setString(ConfKeys.DUMP_DIRECTORY, selectedFolder.getAbsolutePath());
                dumpDirectoryField.setText(selectedFolder.getAbsolutePath());
            }
        });

        sqlEditor.setSyntaxEditingStyle("text/iotdbsql");
        sqlEditor.getPopupMenu().insert(new JPopupMenu.Separator(), 0);
        JMenuItem loadSqlMenuItem = Utils.UI.createMenuItem(LangUtil.getString("LoadSQLFile"), (e) -> loadSqlFromFile());
        loadSqlMenuItem.setIcon(Icons.OPEN);
        sqlEditor.getPopupMenu().insert(loadSqlMenuItem, 0);

        textPrinter = new TextPrinter(outputArea);
        executeButton.addActionListener(e -> startDump());

        localization();
    }

    private void localization() {
        fileFormatLabel.setText(LangUtil.getString("FileFormat"));
        targetDirectoryLabel.setText(LangUtil.getString("DumpDirectory"));
        Utils.UI.tooltip(targetDirectoryLabel, LangUtil.getString("DumpDirectoryTip"));
        dumpFilenameLabel.setText(LangUtil.getString("DumpFileName"));
        Utils.UI.tooltip(dumpFilenameLabel, LangUtil.getString("DumpFileNameTip"));
        timeFormatLabel.setText(LangUtil.getString("DumpTimeFormat"));
        Utils.UI.tooltip(timeFormatLabel, LangUtil.getString("DumpTimeFormatTip"));
        dataTypeInHeaderCheckBox.setText(LangUtil.getString("DataTypeInCSVHeader"));
        dataTypeInHeaderCheckBox.setToolTipText(LangUtil.getString("DataTypeInCSVHeaderTip"));
        maxRowsLabel.setText(LangUtil.getString("MaxRowsPerFile"));
        Utils.UI.tooltip(maxRowsLabel, LangUtil.getString("MaxRowsPerFileTip"));
        alignedCheckBox.setText(LangUtil.getString("DumpAligned"));
        timeoutLabel.setText(LangUtil.getString("Timeout"));
        Utils.UI.tooltip(timeoutLabel, LangUtil.getString("TimeoutTip"));
        Utils.UI.tooltip(sqlLabel, LangUtil.getString("DumpSQLTip"));
        outputLabel.setText(LangUtil.getString("Output"));
        executeButton.setText(LangUtil.getString("StartExporting"));
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

    private void startDump() {
        try {
            if (sqlRadio.isSelected()) {
                dumpType = "sql";
            } else if (tsFileRadio.isSelected()) {
                dumpType = "ts";
            } else {
                dumpType = "csv";
            }
            dumpPath = dumpDirectoryField.getText();

            if (!StrUtil.endWith(dumpPath, FILE_SEPARATOR)) {
                dumpPath += FILE_SEPARATOR;
            }
            dumpFilename = dumpFilenameField.getText();
            Validator.notEmpty(dumpFilenameField, () -> LangUtil.format("FieldRequiredValidation", dumpFilenameLabel.getText()));
            timeFormat = timeFormatField.getSelectedItem().toString();
            linesPerFile = Integer.parseInt(maxRowsField.getValue().toString());
            timestampPrecision = session.getTimestampPrecision();
            timeout = Long.parseLong(timeoutField.getValue().toString());
            withDataType = dataTypeInHeaderCheckBox.isSelected();
            isAligned = alignedCheckBox.isSelected();
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
            return;
        }
        final String sqlText = sqlEditor.getText();
        if (StrUtil.isBlank(sqlText)) {
            Utils.Message.error(LangUtil.getString("DumpSqlIsEmpty"));
            return;
        }
        SwingUtilities.invokeLater(() -> {
            outputArea.setText("");
            executeButton.setEnabled(false);
        });
        dumpWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                List<String> sqls = StrSplitter.splitByRegex(sqlText, ";\\s*\\n", -1, true, true);
                for (int index = 0; index < sqls.size(); index++) {
                    final String sql = sqls.get(index);
                    if ("sql".equals(dumpType) || "ts".equals(dumpType)) {
                        legalCheck(sql);
                    }
                    textPrinter.println("Start export " + dumpType + ": " + sql);
                    if ("ts".equals(dumpType)) {
                        writeTsFile(sql, index);
                    } else {
                        final String path = dumpPath + dumpFilename + "_" + index;
                        try {
                            SessionDataSet sessionDataSet = session.executeQueryStatement(sql, timeout);
                            List<Object> headers = new ArrayList<>();
                            List<String> names = sessionDataSet.getColumnNames();
                            List<String> types = sessionDataSet.getColumnTypes();
                            if ("sql".equals(dumpType)) {
                                writeSqlFile(sessionDataSet, path, names, linesPerFile);
                            } else {
                                if (withDataType) {
                                    for (int i = 0; i < names.size(); i++) {
                                        if (!"Time".equals(names.get(i)) && !"Device".equals(names.get(i))) {
                                            headers.add(String.format("%s(%s)", names.get(i), types.get(i)));
                                        } else {
                                            headers.add(names.get(i));
                                        }
                                    }
                                } else {
                                    headers.addAll(names);
                                }
                                writeCsvFile(sessionDataSet, path, headers, linesPerFile);
                            }
                            sessionDataSet.closeOperationHandle();
                            textPrinter.println("Export completely!");
                        } catch (StatementExecutionException | IoTDBConnectionException | IOException e) {
                            textPrinter.println("Cannot dump result because: " + e.getMessage());
                        }
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    executeButton.setEnabled(true);
                });
                dumpWorker = null;
            }
        };
        dumpWorker.execute();
    }

    public void writeCsvFile(SessionDataSet sessionDataSet, String filePath, List<Object> headers, int linesPerFile)
        throws IOException, IoTDBConnectionException, StatementExecutionException {
        int fileIndex = 0;
        boolean hasNext = true;
        while (hasNext) {
            int i = 0;
            final String finalFilePath = filePath + "_" + fileIndex + ".csv";
            final CSVPrinterWrapper csvPrinterWrapper = new CSVPrinterWrapper(finalFilePath, textPrinter);
            csvPrinterWrapper.printRecord(headers);
            while (i++ < linesPerFile) {
                if (sessionDataSet.hasNext()) {
                    RowRecord rowRecord = sessionDataSet.next();
                    if (rowRecord.getTimestamp() != 0) {
                        csvPrinterWrapper.print(timeTrans(rowRecord.getTimestamp()));
                    }
                    rowRecord
                        .getFields()
                        .forEach(
                            field -> {
                                String fieldStringValue = field.getStringValue();
                                if (!"null".equals(field.getStringValue())) {
                                    if ((field.getDataType() == TSDataType.TEXT
                                        || field.getDataType() == TSDataType.STRING)
                                        && !fieldStringValue.startsWith("root.")) {
                                        fieldStringValue = "\"" + fieldStringValue + "\"";
                                    }
                                    csvPrinterWrapper.print(fieldStringValue);
                                } else {
                                    csvPrinterWrapper.print("");
                                }
                            });
                    csvPrinterWrapper.println();
                } else {
                    hasNext = false;
                    break;
                }
            }
            fileIndex++;
            csvPrinterWrapper.flush();
            csvPrinterWrapper.close();
        }
    }

    public void writeSqlFile(SessionDataSet sessionDataSet,
                             String filePath,
                             List<String> headers,
                             int linesPerFile) throws IOException, IoTDBConnectionException, StatementExecutionException {
        int fileIndex = 0;
        String deviceName = null;
        boolean writeNull = false;
        List<String> seriesList = new ArrayList<>(headers);
        if (CollectionUtil.isEmpty(headers) || headers.size() <= 1) {
            writeNull = true;
        } else {
            if (headers.contains("Device")) {
                seriesList.remove("Time");
                seriesList.remove("Device");
            } else {
                Path path = new Path(seriesList.get(1), true);
                deviceName = path.getDevice();
                seriesList.remove("Time");
                for (int i = 0; i < seriesList.size(); i++) {
                    String series = seriesList.get(i);
                    path = new Path(series, true);
                    seriesList.set(i, path.getMeasurement());
                }
            }
        }
        boolean hasNext = true;
        while (hasNext) {
            int i = 0;
            final String finalFilePath = filePath + "_" + fileIndex + ".sql";
            try (FileWriter writer = new FileWriter(finalFilePath)) {
                if (writeNull) {
                    break;
                }
                while (i++ < linesPerFile) {
                    if (sessionDataSet.hasNext()) {
                        RowRecord rowRecord = sessionDataSet.next();
                        List<Field> fields = rowRecord.getFields();
                        List<String> headersTemp = new ArrayList<>(seriesList);
                        List<String> timeseries = new ArrayList<>();
                        if (headers.contains("Device")) {
                            deviceName = fields.get(0).toString();
                            if (deviceName.startsWith(SYSTEM_DATABASE + ".")) {
                                continue;
                            }
                            for (String header : headersTemp) {
                                timeseries.add(deviceName + "." + header);
                            }
                        } else {
                            if (headers.get(1).startsWith(SYSTEM_DATABASE + ".")) {
                                continue;
                            }
                            timeseries.addAll(headers);
                            timeseries.remove(0);
                        }
                        String sqlMiddle = null;
                        if (isAligned) {
                            sqlMiddle = " ALIGNED VALUES (" + rowRecord.getTimestamp() + ",";
                        } else {
                            sqlMiddle = " VALUES (" + rowRecord.getTimestamp() + ",";
                        }
                        List<String> values = new ArrayList<>();
                        if (headers.contains("Device")) {
                            fields.remove(0);
                        }
                        for (int index = 0; index < fields.size(); index++) {
                            try (
                                SessionDataSet dataSet = session.executeQueryStatement("SHOW TIMESERIES " + timeseries.get(index), timeout)
                            ) {
                                RowRecord next = dataSet.next();
                                if (ObjectUtils.isNotEmpty(next)) {
                                    List<Field> timeseriesList = next.getFields();
                                    String value = fields.get(index).toString();
                                    if (value.equals("null")) {
                                        headersTemp.remove(seriesList.get(index));
                                        continue;
                                    }
                                    if ("TEXT".equalsIgnoreCase(timeseriesList.get(3).getStringValue())) {
                                        values.add("\"" + value.replaceAll("\"", "\"\"") + "\"");
                                    } else {
                                        values.add(value);
                                    }
                                } else {
                                    headersTemp.remove(seriesList.get(index));
                                }
                            }

                        }
                        if (CollectionUtil.isNotEmpty(headersTemp)) {
                            writer.write(
                                "INSERT INTO "
                                    + deviceName
                                    + "(TIMESTAMP,"
                                    + String.join(",", headersTemp)
                                    + ")"
                                    + sqlMiddle
                                    + String.join(",", values)
                                    + ");\n");
                        }

                    } else {
                        hasNext = false;
                        break;
                    }
                }
                fileIndex++;
                writer.flush();
            }
        }
    }

    private void writeTsFile(String sql, int index) {
        final String path = dumpPath + dumpFilename + "_" + index + ".tsfile";
        try (SessionDataSet sessionDataSet = session.executeQueryStatement(sql, timeout)) {
            long start = System.currentTimeMillis();
            writeWithTablets(sessionDataSet, path);
            long end = System.currentTimeMillis();
            textPrinter.println("Export completely!");
            textPrinter.println("Cost: " + (end - start) + " ms.");
        } catch (StatementExecutionException
                 | IoTDBConnectionException
                 | IOException
                 | WriteProcessException e) {
            textPrinter.println("Cannot dump result because: " + e.getMessage());
        }
    }

    private void writeWithTablets(SessionDataSet sessionDataSet, String filePath)
        throws IOException,
        IoTDBConnectionException,
        StatementExecutionException,
        WriteProcessException {
        List<String> columnNames = sessionDataSet.getColumnNames();
        List<String> columnTypes = sessionDataSet.getColumnTypes();
        File f = FSFactoryProducer.getFSFactory().getFile(filePath);
        if (f.exists()) {
            Files.delete(f.toPath());
        }

        try (TsFileWriter tsFileWriter = new TsFileWriter(f)) {
            // device -> column indices in columnNames
            Map<String, List<Integer>> deviceColumnIndices = new HashMap<>();
            Set<String> alignedDevices = new HashSet<>();
            Map<String, List<MeasurementSchema>> deviceSchemaMap = new LinkedHashMap<>();

            collectSchemas(
                columnNames, columnTypes, deviceSchemaMap, alignedDevices, deviceColumnIndices);

            List<Tablet> tabletList = constructTablets(deviceSchemaMap, alignedDevices, tsFileWriter);

            if (tabletList.isEmpty()) {
                textPrinter.println("!!!Warning:Tablet is empty,no data can be exported.");
                return;
            }

            writeWithTablets(sessionDataSet, tabletList, alignedDevices, tsFileWriter, deviceColumnIndices);

            tsFileWriter.flushAllChunkGroups();
        }
    }

    private void writeWithTablets(
        SessionDataSet sessionDataSet,
        List<Tablet> tabletList,
        Set<String> alignedDevices,
        TsFileWriter tsFileWriter,
        Map<String, List<Integer>> deviceColumnIndices)
        throws IoTDBConnectionException,
        StatementExecutionException,
        IOException,
        WriteProcessException {
        while (sessionDataSet.hasNext()) {
            RowRecord rowRecord = sessionDataSet.next();
            List<Field> fields = rowRecord.getFields();

            for (Tablet tablet : tabletList) {
                String deviceId = tablet.deviceId;
                List<Integer> columnIndices = deviceColumnIndices.get(deviceId);
                int rowIndex = tablet.rowSize++;
                tablet.addTimestamp(rowIndex, rowRecord.getTimestamp());
                List<MeasurementSchema> schemas = tablet.getSchemas();

                for (int i = 0, columnIndicesSize = columnIndices.size(); i < columnIndicesSize; i++) {
                    Integer columnIndex = columnIndices.get(i);
                    MeasurementSchema measurementSchema = schemas.get(i);
                    // -1 for time not in fields
                    Object value = fields.get(columnIndex - 1).getObjectValue(measurementSchema.getType());
                    if (value == null) {
                        tablet.bitMaps[i].mark(rowIndex);
                    }
                    tablet.addValue(measurementSchema.getMeasurementId(), rowIndex, value);
                }

                if (tablet.rowSize == tablet.getMaxRowNumber()) {
                    writeToTsFile(alignedDevices, tsFileWriter, tablet);
                    tablet.initBitMaps();
                    tablet.reset();
                }
            }
        }

        for (Tablet tablet : tabletList) {
            if (tablet.rowSize != 0) {
                writeToTsFile(alignedDevices, tsFileWriter, tablet);
            }
        }
    }

    private void collectSchemas(
        List<String> columnNames,
        List<String> columnTypes,
        Map<String, List<MeasurementSchema>> deviceSchemaMap,
        Set<String> alignedDevices,
        Map<String, List<Integer>> deviceColumnIndices)
        throws IoTDBConnectionException, StatementExecutionException {
        for (int i = 0; i < columnNames.size(); i++) {
            String column = columnNames.get(i);
            if (!column.startsWith("root.")) {
                continue;
            }
            TSDataType tsDataType = TSDataType.valueOf(columnTypes.get(i));
            Path path = new Path(column, true);
            String deviceId = path.getDevice();
            // query whether the device is aligned or not
            try (SessionDataSet deviceDataSet =
                     session.executeQueryStatement("show devices " + deviceId, timeout)) {
                List<Field> deviceList = deviceDataSet.next().getFields();
                if (deviceList.size() > 1 && "true".equals(deviceList.get(1).getStringValue())) {
                    alignedDevices.add(deviceId);
                }
            }

            // query timeseries metadata
            MeasurementSchema measurementSchema =
                new MeasurementSchema(path.getMeasurement(), tsDataType);
            List<Field> seriesList =
                session.executeQueryStatement("show timeseries " + column, timeout).next().getFields();
            measurementSchema.setEncoding(
                TSEncoding.valueOf(seriesList.get(4).getStringValue()).serialize());
            measurementSchema.setCompressor(
                CompressionType.valueOf(seriesList.get(5).getStringValue()).serialize());

            deviceSchemaMap.computeIfAbsent(deviceId, key -> new ArrayList<>()).add(measurementSchema);
            deviceColumnIndices.computeIfAbsent(deviceId, key -> new ArrayList<>()).add(i);
        }
    }

    private List<Tablet> constructTablets(
        Map<String, List<MeasurementSchema>> deviceSchemaMap,
        Set<String> alignedDevices,
        TsFileWriter tsFileWriter)
        throws WriteProcessException {
        List<Tablet> tabletList = new ArrayList<>(deviceSchemaMap.size());
        for (Map.Entry<String, List<MeasurementSchema>> stringListEntry : deviceSchemaMap.entrySet()) {
            String deviceId = stringListEntry.getKey();
            List<MeasurementSchema> schemaList = stringListEntry.getValue();
            Tablet tablet = new Tablet(deviceId, schemaList);
            tablet.initBitMaps();
            Path path = new Path(tablet.deviceId);
            if (alignedDevices.contains(tablet.deviceId)) {
                tsFileWriter.registerAlignedTimeseries(path, schemaList);
            } else {
                tsFileWriter.registerTimeseries(path, schemaList);
            }
            tabletList.add(tablet);
        }
        return tabletList;
    }

    private void writeToTsFile(
        Set<String> deviceFilterSet, TsFileWriter tsFileWriter, Tablet tablet)
        throws IOException, WriteProcessException {
        if (deviceFilterSet.contains(tablet.deviceId)) {
            tsFileWriter.writeAligned(tablet);
        } else {
            tsFileWriter.write(tablet);
        }
    }

    private void legalCheck(String sql) {
        String aggregatePattern =
            "\\b(count|sum|avg|extreme|max_value|min_value|first_value|last_value|max_time|min_time|stddev|stddev_pop|stddev_samp|variance|var_pop|var_samp|max_by|min_by)\\b\\s*\\(";
        Pattern pattern = Pattern.compile(aggregatePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            textPrinter.println("The sql you entered is invalid, please don't use aggregate query.");
        }
    }

    public String timeTrans(Long time) {
        ZoneId zoneId = ZoneId.of(session.getTimeZone());
        switch (timeFormat) {
            case "default":
                return RpcUtils.parseLongToDateWithPrecision(DateTimeFormatter.ISO_OFFSET_DATE_TIME, time, zoneId, timestampPrecision);
            case "timestamp":
            case "long":
            case "number":
                return String.valueOf(time);
            default:
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(time), zoneId)
                    .format(DateTimeFormatter.ofPattern(timeFormat));
        }
    }

    @Override
    public Session getSession() {
        return session;
    }

    @Override
    public String getTabbedKey() {
        return String.format("%s-%s", session.getKey(), TABBED_KEY);
    }

    @Override
    public void dispose() {
        if (dumpWorker != null && dumpWorker.getState() == STARTED) {
            dumpWorker.cancel(true);
        }
    }

    @Override
    public boolean disposeable() {
        return dumpWorker == null || dumpWorker.isDone() || dumpWorker.isCancelled();
    }

    @Override
    public boolean confirmDispose() {
        return Utils.Message.confirm(LangUtil.getString("ConfirmTerminateDataExportTask")) == JOptionPane.YES_OPTION;
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
        rootPanel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:max(p;130px):noGrow,left:4dlu:noGrow,fill:p:noGrow,left:5dlu:noGrow,fill:max(d;4px):grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:100px:noGrow,top:4dlu:noGrow,center:max(d;4px):grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        fileFormatLabel = new JLabel();
        fileFormatLabel.setHorizontalAlignment(11);
        fileFormatLabel.setText("File Format");
        CellConstraints cc = new CellConstraints();
        rootPanel.add(fileFormatLabel, cc.xy(1, 1));
        targetDirectoryLabel = new JLabel();
        targetDirectoryLabel.setHorizontalAlignment(11);
        targetDirectoryLabel.setText("Target Directory");
        rootPanel.add(targetDirectoryLabel, cc.xy(1, 3));
        dumpDirectoryField = new JTextField();
        rootPanel.add(dumpDirectoryField, cc.xyw(3, 3, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
        timeoutLabel = new JLabel();
        timeoutLabel.setHorizontalAlignment(11);
        timeoutLabel.setText("Timeout");
        rootPanel.add(timeoutLabel, cc.xy(1, 15));
        timeoutField = new JSpinner();
        rootPanel.add(timeoutField, cc.xy(3, 15, CellConstraints.FILL, CellConstraints.DEFAULT));
        sqlLabel = new JLabel();
        sqlLabel.setHorizontalAlignment(11);
        sqlLabel.setText("SQL");
        sqlLabel.setVerticalAlignment(0);
        sqlLabel.setVerticalTextPosition(0);
        rootPanel.add(sqlLabel, cc.xy(1, 17, CellConstraints.DEFAULT, CellConstraints.TOP));
        outputLabel = new JLabel();
        outputLabel.setHorizontalAlignment(11);
        outputLabel.setText("Output");
        rootPanel.add(outputLabel, cc.xy(1, 19, CellConstraints.DEFAULT, CellConstraints.TOP));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, cc.xyw(3, 19, 7, CellConstraints.FILL, CellConstraints.FILL));
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        scrollPane1.setViewportView(outputArea);
        sqlEditor = new TextEditor();
        rootPanel.add(sqlEditor, cc.xyw(3, 17, 7, CellConstraints.DEFAULT, CellConstraints.FILL));
        dumpFilenameLabel = new JLabel();
        dumpFilenameLabel.setHorizontalAlignment(11);
        dumpFilenameLabel.setText("File Name");
        rootPanel.add(dumpFilenameLabel, cc.xy(1, 5));
        dumpFilenameField = new JTextField();
        rootPanel.add(dumpFilenameField, cc.xyw(3, 5, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        rootPanel.add(panel1, cc.xyw(3, 1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        csvRadio = new JRadioButton();
        csvRadio.setSelected(true);
        csvRadio.setText("CSV");
        panel1.add(csvRadio, cc.xy(1, 1));
        sqlRadio = new JRadioButton();
        sqlRadio.setSelected(false);
        sqlRadio.setText("SQL");
        panel1.add(sqlRadio, cc.xy(3, 1));
        tsFileRadio = new JRadioButton();
        tsFileRadio.setText("TsFile");
        panel1.add(tsFileRadio, cc.xy(5, 1));
        executeButton = new JButton();
        executeButton.setText("Execute");
        rootPanel.add(executeButton, cc.xy(3, 21));
        alignedCheckBox = new JCheckBox();
        alignedCheckBox.setText("Aligned");
        rootPanel.add(alignedCheckBox, cc.xy(3, 11));
        dataTypeInHeaderCheckBox = new JCheckBox();
        dataTypeInHeaderCheckBox.setSelected(true);
        dataTypeInHeaderCheckBox.setText("DataType in CSV Header");
        rootPanel.add(dataTypeInHeaderCheckBox, cc.xy(3, 9));
        timeFormatLabel = new JLabel();
        timeFormatLabel.setHorizontalAlignment(11);
        timeFormatLabel.setText("Time Format");
        rootPanel.add(timeFormatLabel, cc.xy(1, 7));
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
        defaultComboBoxModel1.addElement("default");
        timeFormatField.setModel(defaultComboBoxModel1);
        rootPanel.add(timeFormatField, cc.xyw(3, 7, 3));
        maxRowsLabel = new JLabel();
        maxRowsLabel.setHorizontalAlignment(11);
        maxRowsLabel.setText("Max Rows");
        rootPanel.add(maxRowsLabel, cc.xy(1, 13));
        maxRowsField = new JSpinner();
        rootPanel.add(maxRowsField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(csvRadio);
        buttonGroup.add(sqlRadio);
        buttonGroup.add(tsFileRadio);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
