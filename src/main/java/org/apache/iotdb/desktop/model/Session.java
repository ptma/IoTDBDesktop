package org.apache.iotdb.desktop.model;

import cn.hutool.core.text.StrSplitter;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.formdev.flatlaf.util.StringUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.SessionProps;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.thrift.TException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Getter
public class Session implements Sessionable {

    @Getter
    private final SessionProps props;
    private final org.apache.iotdb.session.Session iotdbSession;
    private boolean opened = false;
    @Setter
    private boolean databasesLoaded = false;

    private String activeDatabase;

    public Session(SessionProps props) {
        this.props = props;
        this.iotdbSession = new org.apache.iotdb.session.Session(props.toBuilder());
    }

    @Override
    public boolean isTableDialect() {
        return props.getSqlDialect().equalsIgnoreCase("table");
    }

    @Override
    public void changeDatabase(String database) {
        if (activeDatabase == null || !activeDatabase.equals(database)) {
            activeDatabase = database;
            try {
                executeNonQueryStatement("use " + database);
            } catch (Exception e) {
                Utils.Message.error(e.getMessage(), e);
            }
        }
    }

    public String getName() {
        return props.getName();
    }

    public String getId() {
        return props.getId();
    }

    @Override
    public String getKey() {
        return getId();
    }

    public void open() throws IoTDBConnectionException {
        if (!opened) {
            iotdbSession.open();
        }
        opened = true;
    }

    public void close() {
        try {
            if (opened) {
                iotdbSession.close();
            }
            opened = false;
        } catch (IoTDBConnectionException e) {
            Utils.Message.error(e.getMessage(), e);
        }
    }

    public Set<Database> loadDatabases() throws Exception {
        QueryResult result = query("show databases", Configuration.instance().options().isLogInternalSql());
        if (result.hasException()) {
            throw result.getException();
        } else {
            return result.getDatas().stream()
                .map(row -> new Database(row.get("Database").toString(), false, this))
                .sorted(Comparator.comparing(Database::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public Set<Device> loadDevices(String database) throws Exception {
        QueryResult result = query("show devices " + database + (database.endsWith(".**") ? "" : ".**"),
            Configuration.instance().options().isLogInternalSql()
        );
        if (result.hasException()) {
            throw result.getException();
        } else {
            return result.getDatas().stream()
                .map(row -> {
                    String device = row.get("Device").toString();
                    String name = device.substring(database.length() + 1);
                    boolean aligned = Boolean.parseBoolean(row.get("IsAligned").toString());
                    String template = Optional.ofNullable(row.get("Template"))
                        .map(Object::toString)
                        .orElse("");
                    String ttl = Optional.ofNullable(row.get("TTL(ms)"))
                        .map(Object::toString)
                        .map(s -> {
                            if (NumberUtil.isNumber(s)) {
                                return Utils.durationToString(Duration.ofMillis(Long.parseLong(s)));
                            } else {
                                return s;
                            }
                        })
                        .orElse("");
                    return new Device(database, name, aligned, template, ttl, this);
                })
                .sorted(Comparator.comparing(Device::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public Set<Table> loadTables(String database) throws Exception {
        QueryResult result = query("show tables from " + database,
            Configuration.instance().options().isLogInternalSql()
        );
        if (result.hasException()) {
            throw result.getException();
        } else {
            return result.getDatas().stream()
                .map(row -> {
                    String tableName = row.get("TableName").toString();
                    String ttl = Optional.ofNullable(row.get("TTL(ms)"))
                        .map(Object::toString)
                        .map(s -> {
                            if (NumberUtil.isNumber(s)) {
                                return Utils.durationToString(Duration.ofMillis(Long.parseLong(s)));
                            } else {
                                return s;
                            }
                        })
                        .orElse("");
                    return new Table(database, tableName, ttl, this);
                })
                .sorted(Comparator.comparing(Table::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    public List<Metric> loadTimeseries(String devicePath) throws IoTDBConnectionException, StatementExecutionException {
        if (!opened) {
            open();
        }
        SessionDataSet dataSet = null;
        try {

            String querySql = "show timeseries " + devicePath + (devicePath.endsWith(".**") ? "" : ".**");
            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(querySql));
            }

            dataSet = iotdbSession.executeQueryStatement(querySql);

            List<Metric> metrics = new ArrayList<>();
            SessionDataSet.DataIterator iterator = dataSet.iterator();
            Metric metric;
            while (iterator.next()) {
                metric = new Metric();
                String timeseriesName = iterator.getString("Timeseries");
                metric.setPath(devicePath);
                metric.setName(timeseriesName.substring(devicePath.length() + 1));
                metric.setDatabase(iterator.getString("Timeseries"));
                metric.setAlias(iterator.getString("Alias"));
                metric.setDatabase(iterator.getString("Database"));
                metric.setDataType(iterator.getString("DataType"));
                metric.setEncoding(iterator.getString("Encoding"));
                metric.setCompression(iterator.getString("Compression"));
                metric.setTags(iterator.getString("Tags"));
                metric.setAttributes(iterator.getString("Attributes"));
                metric.setDeadband(iterator.getString("Deadband"));
                metric.setDeadbandParameters(iterator.getString("DeadbandParameters"));
                metric.setViewType(iterator.getString("ViewType"));
                metric.setExisting(true);
                metrics.add(metric);
            }
            return metrics;
        } finally {
            closeDataSet(dataSet);
        }
    }

    public List<Column> loadTableColumns(String tableName) throws IoTDBConnectionException, StatementExecutionException {
        if (!opened) {
            open();
        }
        SessionDataSet dataSet = null;
        try {

            String querySql = "desc " + tableName + " details";
            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(querySql));
            }

            dataSet = iotdbSession.executeQueryStatement(querySql);

            List<Column> columns = new ArrayList<>();
            SessionDataSet.DataIterator iterator = dataSet.iterator();
            Column column;
            while (iterator.next()) {
                column = new Column();
                column.setName(iterator.getString("ColumnName"));
                column.setDataType(iterator.getString("DataType"));
                column.setCategory(iterator.getString("Category"));
                column.setExisting(true);
                columns.add(column);
            }
            return columns;
        } finally {
            closeDataSet(dataSet);
        }
    }

    public void createTreeModeDatabase(String database) throws IoTDBConnectionException, StatementExecutionException {
        execute("create database " + database);
    }

    public void createTableModeDatabase(String database, String ttl) throws IoTDBConnectionException, StatementExecutionException {
        String properties = StrUtil.isBlank(ttl) ? "" : " with(ttl=" + ttl + ")";
        execute("create database if not exists " + database + properties);
    }


    public void removeDatabase(String database) throws IoTDBConnectionException, StatementExecutionException {
        execute("drop database " + database);
    }

    public void removeDevice(String devicePath) throws IoTDBConnectionException, StatementExecutionException {
        execute("delete timeseries " + devicePath + (devicePath.endsWith(".**") ? "" : ".**"));
    }

    public void removeTable(String table) throws IoTDBConnectionException, StatementExecutionException {
        execute("drop table " + table);
    }

    public void ttlToDatabase(String database, String ttl) throws IoTDBConnectionException, StatementExecutionException {
        if (ttl != null) {
            execute(String.format("set ttl to %s %s", database, ttl));
        } else {
            execute(String.format("unset ttl from %s", database));
        }
    }

    public void ttlToPath(String path, String ttl) throws IoTDBConnectionException, StatementExecutionException {
        if (ttl != null) {
            execute(String.format("set ttl to %s %s", path.endsWith(".**") ? path : path + ".**", ttl));
        } else {
            execute(String.format("unset ttl from %s", path.endsWith(".**") ? path : path + ".**"));
        }
    }

    public String queryDatabaseTtl(String database) throws IoTDBConnectionException, StatementExecutionException {
        SessionDataSet dataSet = null;
        try {
            String sql = String.format("show ttl on %s", database);
            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(sql));
            }
            dataSet = iotdbSession.executeQueryStatement(sql);
            if (dataSet.iterator().next()) {
                return dataSet.iterator().getString("TTL(ms)");
            } else {
                return "0";
            }
        } finally {
            closeDataSet(dataSet);
        }
    }

    public String queryTtl(String path) throws IoTDBConnectionException, StatementExecutionException {
        SessionDataSet dataSet = null;
        try {
            String sql = String.format("show ttl on %s", path.endsWith(".**") ? path : path + ".**");
            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(sql));
            }
            dataSet = iotdbSession.executeQueryStatement(sql);
            if (dataSet.iterator().next()) {
                return dataSet.iterator().getString("TTL(ms)");
            } else {
                return "0";
            }
        } finally {
            closeDataSet(dataSet);
        }
    }

    public void execute(String sql) throws IoTDBConnectionException, StatementExecutionException {
        if (Configuration.instance().options().isLogInternalSql()) {
            AppEvents.instance().applyEvent(l -> l.appendSqlLog(sql));
        }
        iotdbSession.executeNonQueryStatement(sql);
    }

    public List<QueryResult> batchExplain(String sqlText, boolean withAnalyze) {
        List<QueryResult> results = new ArrayList<>();
        List<String> sqls = StringUtils.split(sqlText, ';', true, true);
        for (String sql : sqls) {
            String lowerCaseSql = StrUtil.trim(sql).toLowerCase();
            if (!lowerCaseSql.startsWith("explain")) {
                sql = "explain " + (withAnalyze ? "analyze " : "") + sql;
            }
            results.add(query(sql, true));
        }
        return results;
    }

    public List<QueryResult> batchQuery(String sqlText, boolean enableLog) {
        List<QueryResult> results = new ArrayList<>();
        List<String> sqls = StrSplitter.splitByRegex(sqlText, ";\\s*\\n", -1, true, true);
        for (String sql : sqls) {
            results.add(query(sql, enableLog));
        }
        return results;
    }

    public QueryResult query(String sql, boolean enableLog) {
        String querySql = StrUtil.trim(sql).replaceAll("\n", "");
        QueryResult result = new QueryResult(querySql);
        SessionDataSet dataSet = null;
        try {
            if (!opened) {
                open();
            }

            if (enableLog) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(querySql));
            }
            String lowerCaseSql = querySql.toLowerCase();
            if (
                !lowerCaseSql.startsWith("explain") &&
                    !lowerCaseSql.startsWith("show") &&
                    !lowerCaseSql.startsWith("list") &&
                    !lowerCaseSql.startsWith("select") &&
                    !lowerCaseSql.startsWith("count") &&
                    !lowerCaseSql.startsWith("tracing") &&
                    !lowerCaseSql.startsWith("desc")) {
                iotdbSession.executeNonQueryStatement(querySql);
                result.success();
            } else {
                dataSet = iotdbSession.executeQueryStatement(querySql);
                result.success(dataSet);
            }
        } catch (Exception e) {
            result.error(e);
        } finally {
            closeDataSet(dataSet);
        }
        return result;
    }

    public int countDevices(String database) throws IoTDBConnectionException, StatementExecutionException {
        return countOne("count devices " + database + (database.endsWith(".**") ? "" : ".**"));
    }

    public int countTimeseries(String devicePath) throws IoTDBConnectionException, StatementExecutionException {
        return countOne("count timeseries " + devicePath + (devicePath.endsWith(".**") ? "" : ".**"));
    }

    public int countOne(String countSql) throws IoTDBConnectionException, StatementExecutionException {
        SessionDataSet dataSet = null;
        try {
            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(countSql));
            }
            dataSet = iotdbSession.executeQueryStatement(countSql);
            if (dataSet.iterator().next()) {
                return dataSet.iterator().getInt(1);
            } else {
                return 0;
            }
        } finally {
            closeDataSet(dataSet);
        }
    }

    public long countRows(Device device, boolean aligned) throws IoTDBConnectionException, StatementExecutionException {
        SessionDataSet dataSet = null;
        try {
            StringBuilder countSql = new StringBuilder();
            countSql.append("select count_time(*) as total_rows from ")
                .append(device.getDatabase())
                .append(".")
                .append(device.getName());
            if (aligned) {
                countSql.append(" align by device");
            }

            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(countSql.toString()));
            }
            dataSet = iotdbSession.executeQueryStatement(countSql.toString());
            if (dataSet.iterator().next()) {
                return dataSet.iterator().getLong("total_rows");
            } else {
                return 0;
            }
        } finally {
            closeDataSet(dataSet);
        }
    }

    public long countRows(Table table) throws IoTDBConnectionException, StatementExecutionException {
        SessionDataSet dataSet = null;
        try {
            StringBuilder countSql = new StringBuilder();
            countSql.append("select count(*) as total_rows from ")
                .append(table.getDatabase())
                .append(".")
                .append(table.getName());

            if (Configuration.instance().options().isLogInternalSql()) {
                AppEvents.instance().applyEvent(l -> l.appendSqlLog(countSql.toString()));
            }
            dataSet = iotdbSession.executeQueryStatement(countSql.toString());
            if (dataSet.iterator().next()) {
                return dataSet.iterator().getLong("total_rows");
            } else {
                return 0;
            }
        } finally {
            closeDataSet(dataSet);
        }
    }

    public SessionDataSet executeQueryStatement(String sql) throws IoTDBConnectionException, StatementExecutionException {
        return iotdbSession.executeQueryStatement(sql);
    }

    public SessionDataSet executeQueryStatement(String sql, long timeoutInMs) throws IoTDBConnectionException, StatementExecutionException {
        return iotdbSession.executeQueryStatement(sql, timeoutInMs);
    }

    public void executeNonQueryStatement(String sql) throws IoTDBConnectionException, StatementExecutionException {
        iotdbSession.executeNonQueryStatement(sql);
    }

    private void closeDataSet(SessionDataSet dataSet) {
        try {
            if (dataSet != null) {
                dataSet.close();
            }
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
        }
    }

    public String getTimeZone() {
        return iotdbSession.getTimeZone();
    }

    public String getTimestampPrecision() throws TException {
        return iotdbSession.getTimestampPrecision();
    }

    /**
     * Node name for tree node
     */
    @Override
    public String toString() {
        return this.props.getName();
    }

    public Session getSession() {
        return this;
    }

}
