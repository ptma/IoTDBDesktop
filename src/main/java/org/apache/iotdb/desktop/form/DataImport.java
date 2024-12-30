package org.apache.iotdb.desktop.form;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.iotdb.commons.exception.IllegalPathException;
import org.apache.iotdb.commons.utils.PathUtils;
import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.util.DateTimeUtils;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.tool.data.CSVPrinterWrapper;
import org.apache.iotdb.tool.data.TextPrinter;
import org.apache.thrift.annotation.Nullable;
import org.apache.tsfile.common.constant.TsFileConstant;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.read.common.Field;
import org.apache.tsfile.read.common.RowRecord;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.time.ZoneId;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.swing.SwingWorker.StateValue.STARTED;
import static org.apache.tsfile.enums.TSDataType.STRING;
import static org.apache.tsfile.enums.TSDataType.TEXT;


public class DataImport extends TabPanel {

    public static final String TABBED_KEY = "import-data";

    private static final String SQL_SUFFIXS = "sql";
    private static final String CSV_SUFFIXS = "csv";
    private static final String TXT_SUFFIXS = "txt";
    private static String timeColumn = "Time";
    private static String deviceColumn = "Device";
    public static final String TIMESERIES = "Timeseries";
    public static final String DATATYPE = "DataType";
    private static final String INSERT_CSV_MEET_ERROR_MSG = "Meet error when insert csv because ";

    private static final String DATATYPE_BOOLEAN = "boolean";
    private static final String DATATYPE_INT = "int";
    private static final String DATATYPE_LONG = "long";
    private static final String DATATYPE_FLOAT = "float";
    private static final String DATATYPE_DOUBLE = "double";
    private static final String DATATYPE_TIMESTAMP = "timestamp";
    private static final String DATATYPE_DATE = "date";
    private static final String DATATYPE_BLOB = "blob";
    private static final String DATATYPE_NAN = "NaN";
    private static final String DATATYPE_TEXT = "text";

    private static final String DATATYPE_NULL = "null";

    private static final Map<String, TSDataType> TYPE_INFER_KEY_DICT = new HashMap<>();

    static {
        TYPE_INFER_KEY_DICT.put(DATATYPE_BOOLEAN, TSDataType.BOOLEAN);
        TYPE_INFER_KEY_DICT.put(DATATYPE_INT, TSDataType.FLOAT);
        TYPE_INFER_KEY_DICT.put(DATATYPE_LONG, TSDataType.DOUBLE);
        TYPE_INFER_KEY_DICT.put(DATATYPE_FLOAT, TSDataType.FLOAT);
        TYPE_INFER_KEY_DICT.put(DATATYPE_DOUBLE, TSDataType.DOUBLE);
        TYPE_INFER_KEY_DICT.put(DATATYPE_TIMESTAMP, TSDataType.TIMESTAMP);
        TYPE_INFER_KEY_DICT.put(DATATYPE_DATE, TSDataType.TIMESTAMP);
        TYPE_INFER_KEY_DICT.put(DATATYPE_BLOB, TSDataType.TEXT);
        TYPE_INFER_KEY_DICT.put(DATATYPE_NAN, TSDataType.DOUBLE);
    }

    private static final Map<String, TSDataType> TYPE_INFER_VALUE_DICT = new HashMap<>();

    static {
        TYPE_INFER_VALUE_DICT.put(DATATYPE_BOOLEAN, TSDataType.BOOLEAN);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_INT, TSDataType.INT32);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_LONG, TSDataType.INT64);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_FLOAT, TSDataType.FLOAT);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_DOUBLE, TSDataType.DOUBLE);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_TIMESTAMP, TSDataType.TIMESTAMP);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_DATE, TSDataType.TIMESTAMP);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_BLOB, TSDataType.TEXT);
        TYPE_INFER_VALUE_DICT.put(DATATYPE_TEXT, TSDataType.TEXT);
    }

    private JPanel rootPanel;
    private JTextField sourceField;
    private JComboBox timePrecisionField;
    private JSpinner batchSizeField;
    private JLabel fileOrDirectoryLabel;
    private JLabel timePrecisionLabel;
    private JLabel batchSizeLabel;
    private JSpinner linesPerFailedFileField;
    private JTextArea outputArea;
    private JButton executeButton;
    private JCheckBox alignedCheckBox;
    private JLabel outputLabel;
    private JTextField failedDirectoryField;
    private JLabel linesPerFailedFileLabel;
    private JLabel failedDirectoryLabel;
    private JLabel typeInferenceRulesLabel;
    private JTextArea rulesArea;
    private TextPrinter textPrinter;

    private final Session session;

    private String source;
    private String failedFileDirectory;
    private boolean aligned;
    private String timestampPrecision;
    private int linesPerFailedFile;
    private int batchPointSize;
    private ZoneId zoneId;

    private SwingWorker<Void, Void> worker;

    public DataImport(Session session) {
        super();
        $$$setupUI$$$();
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        this.session = session;

        batchSizeField.setModel(new SpinnerNumberModel(100000, 1000, Integer.MAX_VALUE, 1));
        batchSizeField.setEditor(new JSpinner.NumberEditor(batchSizeField, "####"));

        linesPerFailedFileField.setModel(new SpinnerNumberModel(10000, 1000, Integer.MAX_VALUE, 1));
        linesPerFailedFileField.setEditor(new JSpinner.NumberEditor(linesPerFailedFileField, "####"));

        JButton browserFileButton = new JButton(Icons.OPEN);
        JToolBar fileFieldToolbar = new JToolBar();
        fileFieldToolbar.add(browserFileButton);
        fileFieldToolbar.setBorder(null);
        sourceField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, fileFieldToolbar);
        browserFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setDialogTitle(LangUtil.getString("SelectFileOrDirectory"));
            fileChooser.setFileFilter(new FileNameExtensionFilter(LangUtil.getString("SqlOrCsvFileFilter"), "sql", "csv"));
            fileChooser.setLocale(LangUtil.getLocale());
            String dumpDirectory = Configuration.instance().getString(ConfKeys.DUMP_DIRECTORY, "");
            if (StrUtil.isNotBlank(dumpDirectory)) {
                fileChooser.setCurrentDirectory(new File(dumpDirectory));
            }
            int userSelection = fileChooser.showOpenDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (selectedFile.isDirectory()) {
                    Configuration.instance().setString(ConfKeys.DUMP_DIRECTORY, selectedFile.getAbsolutePath());
                } else {
                    Configuration.instance().setString(ConfKeys.DUMP_DIRECTORY, selectedFile.getParentFile().getAbsolutePath());
                }
                sourceField.setText(selectedFile.getAbsolutePath());
            }
        });

        JButton browserDirectoryButton = new JButton(Icons.OPEN);
        JToolBar directoryToolbar = new JToolBar();
        directoryToolbar.add(browserDirectoryButton);
        directoryToolbar.setBorder(null);
        failedDirectoryField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, directoryToolbar);
        browserDirectoryButton.addActionListener(e -> {
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
                failedDirectoryField.setText(selectedFolder.getAbsolutePath());
            }
        });

        textPrinter = new TextPrinter(outputArea);
        executeButton.addActionListener(e -> startImport());

        localization();
    }

    private void localization() {
        fileOrDirectoryLabel.setText(LangUtil.getString("FileOrDirectory"));
        Utils.UI.tooltip(fileOrDirectoryLabel, LangUtil.getString("FileOrDirectoryTip"));
        failedDirectoryLabel.setText(LangUtil.getString("FailedDirectory"));
        Utils.UI.tooltip(failedDirectoryLabel, LangUtil.getString("FailedDirectoryTip"));
        alignedCheckBox.setText(LangUtil.getString("ImportAligned"));
        alignedCheckBox.setToolTipText(LangUtil.getString("ImportAlignedTip"));
        timePrecisionLabel.setText(LangUtil.getString("TimePrecision"));
        batchSizeLabel.setText(LangUtil.getString("BatchSize"));
        Utils.UI.tooltip(batchSizeLabel, LangUtil.getString("BatchSizeTip"));
        linesPerFailedFileLabel.setText(LangUtil.getString("LinesPerFailedFile"));
        Utils.UI.tooltip(linesPerFailedFileLabel, LangUtil.getString("LinesPerFailedFileTip"));
        typeInferenceRulesLabel.setText(LangUtil.getString("TypeInferenceRules"));
        Utils.UI.tooltip(typeInferenceRulesLabel, LangUtil.getString("TypeInferenceRulesTip"));

        outputLabel.setText(LangUtil.getString("Output"));
        executeButton.setText(LangUtil.getString("StartImporting"));
    }

    private void startImport() {
        try {
            zoneId = ZoneId.of(session.getTimeZone());
            source = sourceField.getText();
            if (!FileUtil.exist(source)) {
                Utils.Message.error(LangUtil.format("SourceFileOrDirectoryNotExist", source));
                return;
            }
            failedFileDirectory = failedDirectoryField.getText();
            if (StrUtil.isBlank(failedFileDirectory)) {
                failedFileDirectory = null;
            } else {
                if (!FileUtil.exist(failedFileDirectory)) {
                    Utils.Message.error(LangUtil.format("FailedFileDirectoryNotExist", failedFileDirectory));
                    return;
                }
            }
            aligned = alignedCheckBox.isSelected();
            batchPointSize = Integer.parseInt(batchSizeField.getValue().toString());
            timestampPrecision = timePrecisionField.getSelectedItem().toString();
            linesPerFailedFile = Integer.parseInt(linesPerFailedFileField.getValue().toString());
            String rules = rulesArea.getText();
            if (StrUtil.isNotBlank(rules)) {
                final String[] opTypeInferValues = rules.split(",");
                for (String opTypeInferValue : opTypeInferValues) {
                    if (opTypeInferValue.contains("=")) {
                        final String[] typeInfoExpressionArr = opTypeInferValue.split("=");
                        final String key = typeInfoExpressionArr[0];
                        final String value = typeInfoExpressionArr[1];
                        applyTypeInferArgs(key, value);
                    }
                }
            }
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
            return;
        }

        SwingUtilities.invokeLater(() -> {
            executeButton.setEnabled(false);
        });
        worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                importFromTargetPath(source);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    textPrinter.printException(e);
                }
                SwingUtilities.invokeLater(() -> {
                    executeButton.setEnabled(true);
                });
                worker = null;
            }
        };
        worker.execute();
    }

    private void applyTypeInferArgs(String key, String value) throws IllegalArgumentException {
        if (!TYPE_INFER_KEY_DICT.containsKey(key)) {
            throw new IllegalArgumentException("Unknown type infer key: " + key);
        }
        if (!TYPE_INFER_VALUE_DICT.containsKey(value)) {
            throw new IllegalArgumentException("Unknown type infer value: " + value);
        }
        if (key.equals(DATATYPE_NAN)
            && !(value.equals(DATATYPE_FLOAT)
            || value.equals(DATATYPE_DOUBLE)
            || value.equals(DATATYPE_TEXT))) {
            throw new IllegalArgumentException("NaN can not convert to " + value);
        }
        if (key.equals(DATATYPE_BOOLEAN)
            && !(value.equals(DATATYPE_BOOLEAN) || value.equals(DATATYPE_TEXT))) {
            throw new IllegalArgumentException("Boolean can not convert to " + value);
        }
        final TSDataType srcType = TYPE_INFER_VALUE_DICT.get(key);
        final TSDataType dstType = TYPE_INFER_VALUE_DICT.get(value);
        if (dstType.getType() < srcType.getType()) {
            throw new IllegalArgumentException(key + " can not convert to " + value);
        }
        TYPE_INFER_KEY_DICT.put(key, TYPE_INFER_VALUE_DICT.get(value));
    }

    private void cancelImport() {
        if (worker != null && worker.getState() == STARTED) {
            worker.cancel(true);
            SwingUtilities.invokeLater(() -> {
                executeButton.setEnabled(true);
            });
        }
    }

    @Override
    public Session getSession() {
        return session;
    }

    public void importFromTargetPath(String path) {
        File file = new File(path);
        if (file.isFile()) {
            if (file.getName().endsWith(SQL_SUFFIXS)) {
                importFromSqlFile(file);
            } else {
                importFromSingleFile(file);
            }
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                textPrinter.println("File not found!");
                return;
            }

            for (File subFile : files) {
                if (subFile.isFile()) {
                    if (subFile.getName().endsWith(SQL_SUFFIXS)) {
                        importFromSqlFile(subFile);
                    } else {
                        importFromSingleFile(subFile);
                    }
                }
            }
        } else {
            textPrinter.println("File not found!");
        }
    }

    private void importFromSqlFile(File file) {
        textPrinter.println("Start import from file: " + file.getAbsolutePath());
        ArrayList<List<Object>> failedRecords = new ArrayList<>();
        String failedFilePath = null;
        if (failedFileDirectory == null) {
            failedFilePath = file.getAbsolutePath() + ".failed";
        } else {
            failedFilePath = failedFileDirectory + file.getName() + ".failed";
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
            String sql;
            while ((sql = br.readLine()) != null) {
                try {
                    session.executeNonQueryStatement(sql);
                } catch (IoTDBConnectionException | StatementExecutionException e) {
                    failedRecords.add(List.of(sql));
                }
            }
            textPrinter.println(file.getName() + " Import completely!");
        } catch (IOException e) {
            textPrinter.println("SQL file read exception because: " + e.getMessage());
        }
        if (!failedRecords.isEmpty()) {
            Writer writer = null;
            try {
                writer = new FileWriter(failedFilePath);
                for (List<Object> failedRecord : failedRecords) {
                    writer.write(failedRecord.get(0).toString() + "\n");
                }
            } catch (IOException e) {
                textPrinter.println("Cannot dump fail result because: " + e.getMessage());
            } finally {
                if (ObjectUtils.isNotEmpty(writer)) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
    }

    /**
     * import the CSV file and load headers and records.
     *
     * @param file the File object of the CSV file that you want to import.
     */
    private void importFromSingleFile(File file) {
        if (file.getName().endsWith(CSV_SUFFIXS) || file.getName().endsWith(TXT_SUFFIXS)) {
            textPrinter.println("Start import from file: " + file.getAbsolutePath());
            try (CSVParser csvRecords = readCsvFile(file.getAbsolutePath())) {
                List<String> headerNames = csvRecords.getHeaderNames();
                Stream<CSVRecord> records = csvRecords.stream();
                if (headerNames.isEmpty()) {
                    textPrinter.println("Empty file!");
                    return;
                }
                if (!timeColumn.equalsIgnoreCase(filterBomHeader(headerNames.get(0)))) {
                    textPrinter.println("The first field of header must be `Time`!");
                    return;
                }
                String failedFilePath = null;
                if (failedFileDirectory == null) {
                    failedFilePath = file.getAbsolutePath() + ".failed";
                } else {
                    failedFilePath = failedFileDirectory + file.getName() + ".failed";
                }
                if (!deviceColumn.equalsIgnoreCase(headerNames.get(1))) {
                    writeDataAlignedByTime(headerNames, records, failedFilePath);
                } else {
                    writeDataAlignedByDevice(headerNames, records, failedFilePath);
                }
            } catch (IOException | IllegalPathException e) {
                textPrinter.println("CSV file read exception because: " + e.getMessage());
            }
        } else {
            textPrinter.println("The file name must end with \"csv\" or \"txt\"!");
        }
    }

    /**
     * if the data is aligned by time, the data will be written by this method.
     *
     * @param headerNames    the header names of CSV file
     * @param records        the records of CSV file
     * @param failedFilePath the directory to save the failed files
     */
    private void writeDataAlignedByTime(
        List<String> headerNames, Stream<CSVRecord> records, String failedFilePath)
        throws IllegalPathException {
        HashMap<String, List<String>> deviceAndMeasurementNames = new HashMap<>();
        HashMap<String, TSDataType> headerTypeMap = new HashMap<>();
        HashMap<String, String> headerNameMap = new HashMap<>();
        parseHeaders(headerNames, deviceAndMeasurementNames, headerTypeMap, headerNameMap);

        Set<String> devices = deviceAndMeasurementNames.keySet();
        if (headerTypeMap.isEmpty()) {
            queryType(devices, headerTypeMap, "Time");
        }

        List<String> deviceIds = new ArrayList<>();
        List<Long> times = new ArrayList<>();
        List<List<String>> measurementsList = new ArrayList<>();
        List<List<TSDataType>> typesList = new ArrayList<>();
        List<List<Object>> valuesList = new ArrayList<>();

        AtomicReference<Boolean> hasStarted = new AtomicReference<>(false);
        AtomicInteger pointSize = new AtomicInteger(0);

        ArrayList<List<Object>> failedRecords = new ArrayList<>();
        records.forEach(
            recordObj -> {
                if (Boolean.FALSE.equals(hasStarted.get())) {
                    hasStarted.set(true);
                } else if (pointSize.get() >= batchPointSize) {
                    writeAndEmptyDataSet(deviceIds, times, typesList, valuesList, measurementsList, 3);
                    pointSize.set(0);
                }

                boolean isFail = false;

                for (Map.Entry<String, List<String>> entry : deviceAndMeasurementNames.entrySet()) {
                    String deviceId = entry.getKey();
                    List<String> measurementNames = entry.getValue();
                    ArrayList<TSDataType> types = new ArrayList<>();
                    ArrayList<Object> values = new ArrayList<>();
                    ArrayList<String> measurements = new ArrayList<>();
                    for (String measurement : measurementNames) {
                        String header = deviceId + "." + measurement;
                        String value = recordObj.get(headerNameMap.get(header));
                        if (!"".equals(value)) {
                            TSDataType type;
                            if (!headerTypeMap.containsKey(header)) {
                                type = typeInfer(value);
                                if (type != null) {
                                    headerTypeMap.put(header, type);
                                } else {
                                    textPrinter.printf(
                                        "Line '%s', column '%s': '%s' unknown type%n",
                                        recordObj.getRecordNumber(), header, value);
                                    isFail = true;
                                }
                            }
                            type = headerTypeMap.get(header);
                            if (type != null) {
                                Object valueTrans = typeTrans(value, type);
                                if (valueTrans == null) {
                                    isFail = true;
                                    textPrinter.printf(
                                        "Line '%s', column '%s': '%s' can't convert to '%s'%n",
                                        recordObj.getRecordNumber(), header, value, type);
                                } else {
                                    measurements.add(header.replace(deviceId + '.', ""));
                                    types.add(type);
                                    values.add(valueTrans);
                                    pointSize.getAndIncrement();
                                }
                            }
                        }
                    }
                    if (!measurements.isEmpty()) {
                        times.add(parseTimestamp(recordObj.get(timeColumn)));
                        deviceIds.add(deviceId);
                        typesList.add(types);
                        valuesList.add(values);
                        measurementsList.add(measurements);
                    }
                }
                if (isFail) {
                    failedRecords.add(recordObj.stream().collect(Collectors.toList()));
                }
            });
        if (!deviceIds.isEmpty()) {
            writeAndEmptyDataSet(deviceIds, times, typesList, valuesList, measurementsList, 3);
            pointSize.set(0);
        }

        if (!failedRecords.isEmpty()) {
            writeFailedLinesFile(headerNames, failedFilePath, failedRecords);
        }
        if (Boolean.TRUE.equals(hasStarted.get())) {
            textPrinter.println("Import completely!");
        } else {
            textPrinter.println("No records!");
        }
    }

    /**
     * if the data is aligned by device, the data will be written by this method.
     *
     * @param headerNames    the header names of CSV file
     * @param records        the records of CSV file
     * @param failedFilePath the directory to save the failed files
     */
    private void writeDataAlignedByDevice(
        List<String> headerNames, Stream<CSVRecord> records, String failedFilePath) throws IllegalPathException {
        HashMap<String, TSDataType> headerTypeMap = new HashMap<>();
        HashMap<String, String> headerNameMap = new HashMap<>();
        parseHeaders(headerNames, null, headerTypeMap, headerNameMap);

        AtomicReference<String> deviceName = new AtomicReference<>(null);

        HashSet<String> typeQueriedDevice = new HashSet<>();

        // the data that interface need
        List<Long> times = new ArrayList<>();
        List<List<TSDataType>> typesList = new ArrayList<>();
        List<List<Object>> valuesList = new ArrayList<>();
        List<List<String>> measurementsList = new ArrayList<>();

        AtomicInteger pointSize = new AtomicInteger(0);

        ArrayList<List<Object>> failedRecords = new ArrayList<>();

        records.forEach(
            recordObj -> {
                // only run in first record
                if (deviceName.get() == null) {
                    deviceName.set(recordObj.get(1));
                } else if (!Objects.equals(deviceName.get(), recordObj.get(1))) {
                    // if device changed
                    writeAndEmptyDataSet(
                        deviceName.get(), times, typesList, valuesList, measurementsList, 3);
                    deviceName.set(recordObj.get(1));
                    pointSize.set(0);
                } else if (pointSize.get() >= batchPointSize) {
                    // insert a batch
                    writeAndEmptyDataSet(
                        deviceName.get(), times, typesList, valuesList, measurementsList, 3);
                    pointSize.set(0);
                }

                // the data of the record
                ArrayList<TSDataType> types = new ArrayList<>();
                ArrayList<Object> values = new ArrayList<>();
                ArrayList<String> measurements = new ArrayList<>();

                AtomicReference<Boolean> isFail = new AtomicReference<>(false);

                // read data from record
                for (Map.Entry<String, String> headerNameEntry : headerNameMap.entrySet()) {
                    // headerNameWithoutType is equal to headerName if the CSV column do not have data type.
                    String headerNameWithoutType = headerNameEntry.getKey();
                    String headerName = headerNameEntry.getValue();
                    String value = recordObj.get(headerName);
                    if (!"".equals(value)) {
                        TSDataType type;
                        // Get the data type directly if the CSV column have data type.
                        if (!headerTypeMap.containsKey(headerNameWithoutType)) {
                            boolean hasResult = false;
                            // query the data type in iotdb
                            if (!typeQueriedDevice.contains(deviceName.get())) {
                                if (headerTypeMap.isEmpty()) {
                                    Set<String> devices = new HashSet<>();
                                    devices.add(deviceName.get());
                                    queryType(devices, headerTypeMap, deviceColumn);
                                }
                                typeQueriedDevice.add(deviceName.get());
                            }
                            type = typeInfer(value);
                            if (type != null) {
                                headerTypeMap.put(headerNameWithoutType, type);
                            } else {
                                textPrinter.printf(
                                    "Line '%s', column '%s': '%s' unknown type%n",
                                    recordObj.getRecordNumber(), headerNameWithoutType, value);
                                isFail.set(true);
                            }
                        }
                        type = headerTypeMap.get(headerNameWithoutType);
                        if (type != null) {
                            Object valueTrans = typeTrans(value, type);
                            if (valueTrans == null) {
                                isFail.set(true);
                                textPrinter.printf(
                                    "Line '%s', column '%s': '%s' can't convert to '%s'%n",
                                    recordObj.getRecordNumber(), headerNameWithoutType, value, type);
                            } else {
                                values.add(valueTrans);
                                measurements.add(headerNameWithoutType);
                                types.add(type);
                                pointSize.getAndIncrement();
                            }
                        }
                    }
                }
                if (Boolean.TRUE.equals(isFail.get())) {
                    failedRecords.add(recordObj.stream().collect(Collectors.toList()));
                }
                if (!measurements.isEmpty()) {
                    times.add(parseTimestamp(recordObj.get(timeColumn)));
                    typesList.add(types);
                    valuesList.add(values);
                    measurementsList.add(measurements);
                }
            });
        if (!times.isEmpty()) {
            writeAndEmptyDataSet(deviceName.get(), times, typesList, valuesList, measurementsList, 3);
            pointSize.set(0);
        }
        if (!failedRecords.isEmpty()) {
            writeFailedLinesFile(headerNames, failedFilePath, failedRecords);
        }
        textPrinter.println("Import completely!");
    }

    private void writeFailedLinesFile(
        List<String> headerNames, String failedFilePath, ArrayList<List<Object>> failedRecords) {
        int fileIndex = 0;
        int from = 0;
        int failedRecordsSize = failedRecords.size();
        int restFailedRecords = failedRecordsSize;
        while (from < failedRecordsSize) {
            int step = Math.min(restFailedRecords, linesPerFailedFile);
            writeCsvFile(
                headerNames,
                failedRecords.subList(from, from + step),
                failedFilePath + "_" + fileIndex++);
            from += step;
            restFailedRecords -= step;
        }
    }

    private void writeAndEmptyDataSet(
        String device,
        List<Long> times,
        List<List<TSDataType>> typesList,
        List<List<Object>> valuesList,
        List<List<String>> measurementsList,
        int retryTime) {
        try {
            if (Boolean.FALSE.equals(aligned)) {
                session.getIotdbSession().insertRecordsOfOneDevice(device, times, measurementsList, typesList, valuesList);
            } else {
                session.getIotdbSession().insertAlignedRecordsOfOneDevice(
                    device, times, measurementsList, typesList, valuesList);
            }
        } catch (IoTDBConnectionException e) {
            if (retryTime > 0) {
                try {
                    session.getIotdbSession().open();
                } catch (IoTDBConnectionException ex) {
                    textPrinter.println(INSERT_CSV_MEET_ERROR_MSG + e.getMessage());
                }
                writeAndEmptyDataSet(device, times, typesList, valuesList, measurementsList, --retryTime);
            }
        } catch (StatementExecutionException e) {
            textPrinter.println(INSERT_CSV_MEET_ERROR_MSG + e.getMessage());
            cancelImport();
        } finally {
            times.clear();
            typesList.clear();
            valuesList.clear();
            measurementsList.clear();
        }
    }

    private void writeAndEmptyDataSet(
        List<String> deviceIds,
        List<Long> times,
        List<List<TSDataType>> typesList,
        List<List<Object>> valuesList,
        List<List<String>> measurementsList,
        int retryTime) {
        try {
            if (Boolean.FALSE.equals(aligned)) {
                session.getIotdbSession().insertRecords(deviceIds, times, measurementsList, typesList, valuesList);
            } else {
                session.getIotdbSession().insertAlignedRecords(deviceIds, times, measurementsList, typesList, valuesList);
            }
        } catch (IoTDBConnectionException e) {
            if (retryTime > 0) {
                try {
                    session.open();
                } catch (IoTDBConnectionException ex) {
                    textPrinter.println(INSERT_CSV_MEET_ERROR_MSG + e.getMessage());
                }
                writeAndEmptyDataSet(deviceIds, times, typesList, valuesList, measurementsList, --retryTime);
            }
        } catch (StatementExecutionException e) {
            textPrinter.println(INSERT_CSV_MEET_ERROR_MSG + e.getMessage());
            cancelImport();
        } finally {
            deviceIds.clear();
            times.clear();
            typesList.clear();
            valuesList.clear();
            measurementsList.clear();
        }
    }

    /**
     * query data type of timeseries from IoTDB
     *
     * @param deviceNames
     * @param headerTypeMap
     * @param alignedType
     * @throws IoTDBConnectionException
     * @throws StatementExecutionException
     */
    private void queryType(Set<String> deviceNames, HashMap<String, TSDataType> headerTypeMap, String alignedType) {
        for (String deviceName : deviceNames) {
            String sql = "show timeseries " + deviceName + ".*";
            SessionDataSet sessionDataSet = null;
            try {
                sessionDataSet = session.executeQueryStatement(sql);
                int tsIndex = sessionDataSet.getColumnNames().indexOf(TIMESERIES);
                int dtIndex = sessionDataSet.getColumnNames().indexOf(DATATYPE);
                while (sessionDataSet.hasNext()) {
                    RowRecord rowRecord = sessionDataSet.next();
                    List<Field> fields = rowRecord.getFields();
                    String timeseries = fields.get(tsIndex).getStringValue();
                    String dataType = fields.get(dtIndex).getStringValue();
                    if (Objects.equals(alignedType, "Time")) {
                        headerTypeMap.put(timeseries, getType(dataType));
                    } else if (Objects.equals(alignedType, deviceColumn)) {
                        String[] split = PathUtils.splitPathToDetachedNodes(timeseries);
                        String measurement = split[split.length - 1];
                        headerTypeMap.put(measurement, getType(dataType));
                    }
                }
            } catch (StatementExecutionException | IoTDBConnectionException | IllegalPathException e) {
                textPrinter.println(
                    "Meet error when query the type of timeseries because " + e.getMessage());
                cancelImport();
            }
        }
    }

    /**
     * read data from the CSV file
     *
     * @param path
     * @return CSVParser csv parser
     * @throws IOException when reading the csv file failed.
     */
    private CSVParser readCsvFile(String path) throws IOException {
        return CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader()
            .setSkipHeaderRecord(true)
            .setQuote('`')
            .setEscape('\\')
            .setIgnoreEmptyLines(true)
            .build()
            .parse(new InputStreamReader(new FileInputStream(path)));
    }

    private String filterBomHeader(String s) {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] bytes = Arrays.copyOf(s.getBytes(), 3);
        if (Arrays.equals(bom, bytes)) {
            return s.substring(1);
        }
        return s;
    }

    /**
     * parse deviceNames, measurementNames(aligned by time), headerType from headers
     *
     * @param headerNames
     * @param deviceAndMeasurementNames
     * @param headerTypeMap
     * @param headerNameMap
     */
    private void parseHeaders(
        List<String> headerNames,
        @Nullable HashMap<String, List<String>> deviceAndMeasurementNames,
        HashMap<String, TSDataType> headerTypeMap,
        HashMap<String, String> headerNameMap)
        throws IllegalPathException {
        String regex = "(?<=\\()\\S+(?=\\))";
        Pattern pattern = Pattern.compile(regex);
        for (String headerName : headerNames) {
            if ("Time".equalsIgnoreCase(filterBomHeader(headerName))) {
                timeColumn = headerName;
                continue;
            } else if ("Device".equalsIgnoreCase(headerName)) {
                deviceColumn = headerName;
                continue;
            }
            Matcher matcher = pattern.matcher(headerName);
            String type;
            String headerNameWithoutType;
            if (matcher.find()) {
                type = matcher.group();
                headerNameWithoutType = headerName.replace("(" + type + ")", "").replaceAll("\\s+", "");
                headerNameMap.put(headerNameWithoutType, headerName);
                headerTypeMap.put(headerNameWithoutType, getType(type));
            } else {
                headerNameWithoutType = headerName;
                headerNameMap.put(headerName, headerName);
            }
            String[] split = PathUtils.splitPathToDetachedNodes(headerNameWithoutType);
            String measurementName = split[split.length - 1];
            String deviceName = StringUtils.join(Arrays.copyOfRange(split, 0, split.length - 1), '.');
            if (deviceAndMeasurementNames != null) {
                deviceAndMeasurementNames.putIfAbsent(deviceName, new ArrayList<>());
                deviceAndMeasurementNames.get(deviceName).add(measurementName);
            }
        }
    }

    /**
     * return the TSDataType
     *
     * @param typeStr
     * @return
     */
    private TSDataType getType(String typeStr) {
        try {
            return TSDataType.valueOf(typeStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * if data type of timeseries is not defined in headers of schema, this method will be called to
     * do type inference
     *
     * @param strValue
     * @return
     */
    private TSDataType typeInfer(String strValue) {
        if (strValue.contains("\"")) {
            return strValue.length() <= 512 + 2 ? STRING : TEXT;
        }
        if (isBoolean(strValue)) {
            return TYPE_INFER_KEY_DICT.get(DATATYPE_BOOLEAN);
        } else if (isNumber(strValue)) {
            if (!strValue.contains(TsFileConstant.PATH_SEPARATOR)) {
                if (isConvertFloatPrecisionLack(StringUtils.trim(strValue))) {
                    return TYPE_INFER_KEY_DICT.get(DATATYPE_LONG);
                }
                return TYPE_INFER_KEY_DICT.get(DATATYPE_INT);
            } else {
                return TYPE_INFER_KEY_DICT.get(DATATYPE_FLOAT);
            }
        } else if (DATATYPE_NULL.equals(strValue) || DATATYPE_NULL.toUpperCase().equals(strValue)) {
            return null;
            // "NaN" is returned if the NaN Literal is given in Parser
        } else if (DATATYPE_NAN.equals(strValue)) {
            return TYPE_INFER_KEY_DICT.get(DATATYPE_NAN);
        } else if (strValue.length() <= 512) {
            return STRING;
        } else {
            return TEXT;
        }
    }

    private boolean isNumber(String s) {
        if (s == null || s.equals(DATATYPE_NAN)) {
            return false;
        }
        try {
            Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    private boolean isBoolean(String s) {
        return s.equalsIgnoreCase("true")
            || s.equalsIgnoreCase("false");
    }

    private boolean isConvertFloatPrecisionLack(String s) {
        return Long.parseLong(s) > (2 << 24);
    }

    /**
     * @param value
     * @param type
     * @return
     */
    private Object typeTrans(String value, TSDataType type) {
        try {
            switch (type) {
                case TEXT:
                case STRING:
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return value;
                case BOOLEAN:
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                        return null;
                    }
                    return Boolean.parseBoolean(value);
                case INT32:
                    return Integer.parseInt(value);
                case INT64:
                    return Long.parseLong(value);
                case FLOAT:
                    return Float.parseFloat(value);
                case DOUBLE:
                    return Double.parseDouble(value);
                case TIMESTAMP:
                case DATE:
                case BLOB:
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private long parseTimestamp(String str) {
        long timestamp;
        try {
            timestamp = Long.parseLong(str);
        } catch (NumberFormatException e) {
            timestamp = DateTimeUtils.convertDatetimeStrToLong(str, zoneId, timestampPrecision);
        }
        return timestamp;
    }

    /**
     * write data to CSV file.
     *
     * @param headerNames the header names of CSV file
     * @param records     the records of CSV file
     * @param filePath    the directory to save the file
     */
    public Boolean writeCsvFile(
        List<String> headerNames, List<List<Object>> records, String filePath) {
        try {
            final CSVPrinterWrapper csvPrinterWrapper = new CSVPrinterWrapper(filePath, textPrinter);
            if (headerNames != null) {
                csvPrinterWrapper.printRecord(headerNames);
            }
            for (List<Object> CsvRecord : records) {
                csvPrinterWrapper.printRecord(CsvRecord);
            }
            csvPrinterWrapper.flush();
            csvPrinterWrapper.close();
            return true;
        } catch (IOException e) {
            textPrinter.printException(e);
            return false;
        }
    }

    @Override
    public String getTabbedKey() {
        return String.format("%s-%s", session.getKey(), TABBED_KEY);
    }

    @Override
    public void dispose() {
        if (worker != null && worker.getState() == STARTED) {
            worker.cancel(true);
        }
    }

    @Override
    public boolean disposeable() {
        return worker == null || worker.isDone() || worker.isCancelled();
    }

    @Override
    public boolean confirmDispose() {
        return Utils.Message.confirm(LangUtil.getString("ConfirmTerminateDataImportTask")) == JOptionPane.YES_OPTION;
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
        rootPanel.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:p:noGrow,fill:max(d;4px):grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:25dlu:noGrow,top:4dlu:noGrow,center:max(d;4px):grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        rootPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        fileOrDirectoryLabel = new JLabel();
        fileOrDirectoryLabel.setHorizontalAlignment(11);
        fileOrDirectoryLabel.setText("File or Directory");
        CellConstraints cc = new CellConstraints();
        rootPanel.add(fileOrDirectoryLabel, cc.xy(1, 1));
        sourceField = new JTextField();
        rootPanel.add(sourceField, cc.xyw(3, 1, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        linesPerFailedFileLabel = new JLabel();
        linesPerFailedFileLabel.setHorizontalAlignment(11);
        linesPerFailedFileLabel.setText("Lines Per Failed File");
        rootPanel.add(linesPerFailedFileLabel, cc.xy(1, 11));
        linesPerFailedFileField = new JSpinner();
        rootPanel.add(linesPerFailedFileField, cc.xy(3, 11, CellConstraints.FILL, CellConstraints.DEFAULT));
        outputLabel = new JLabel();
        outputLabel.setHorizontalAlignment(11);
        outputLabel.setText("Output");
        rootPanel.add(outputLabel, cc.xy(1, 15, CellConstraints.DEFAULT, CellConstraints.TOP));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, cc.xyw(3, 15, 4, CellConstraints.FILL, CellConstraints.FILL));
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        scrollPane1.setViewportView(outputArea);
        timePrecisionField = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("ms");
        defaultComboBoxModel1.addElement("ns");
        defaultComboBoxModel1.addElement("us");
        timePrecisionField.setModel(defaultComboBoxModel1);
        rootPanel.add(timePrecisionField, cc.xy(3, 9));
        timePrecisionLabel = new JLabel();
        timePrecisionLabel.setHorizontalAlignment(11);
        timePrecisionLabel.setText("Time Precision");
        rootPanel.add(timePrecisionLabel, cc.xy(1, 9));
        executeButton = new JButton();
        executeButton.setText("Execute");
        rootPanel.add(executeButton, cc.xy(3, 17));
        failedDirectoryLabel = new JLabel();
        failedDirectoryLabel.setHorizontalAlignment(11);
        failedDirectoryLabel.setText("Failed Directory");
        rootPanel.add(failedDirectoryLabel, cc.xy(1, 3));
        failedDirectoryField = new JTextField();
        rootPanel.add(failedDirectoryField, cc.xyw(3, 3, 4, CellConstraints.FILL, CellConstraints.DEFAULT));
        alignedCheckBox = new JCheckBox();
        alignedCheckBox.setText("Aligned");
        rootPanel.add(alignedCheckBox, cc.xy(3, 5));
        typeInferenceRulesLabel = new JLabel();
        typeInferenceRulesLabel.setText("Type Inference Rules");
        rootPanel.add(typeInferenceRulesLabel, cc.xy(1, 13, CellConstraints.DEFAULT, CellConstraints.TOP));
        final JScrollPane scrollPane2 = new JScrollPane();
        rootPanel.add(scrollPane2, cc.xyw(3, 13, 4, CellConstraints.FILL, CellConstraints.FILL));
        rulesArea = new JTextArea();
        scrollPane2.setViewportView(rulesArea);
        batchSizeLabel = new JLabel();
        batchSizeLabel.setHorizontalAlignment(11);
        batchSizeLabel.setText("Batch Size");
        rootPanel.add(batchSizeLabel, cc.xy(1, 7));
        batchSizeField = new JSpinner();
        rootPanel.add(batchSizeField, cc.xy(3, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
