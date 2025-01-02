package org.apache.iotdb.desktop.model;

import cn.hutool.core.date.DateUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.enums.TSDataType;

import java.util.*;

@Getter
public class QueryResult {
    private String sql;
    private long startTime;
    private long endTime;

    private Exception exception;

    private List<String> columns;
    private List<String> columnTypes;
    private List<Map<String, Object>> datas = new ArrayList<>();

    private Map<String, String> localeColumnNames = new HashMap<>();

    public QueryResult(String sql) {
        this.sql = sql;
        this.startTime = System.currentTimeMillis();
    }

    public void clearDatas() {
        this.datas.clear();
    }

    public String getLocaleColumnName(int index) {
        String columnName = this.columns.get(index);
        return Optional.ofNullable(localeColumnNames.get(columnName))
            .orElse(columnName);
    }

    public QueryResult columnNamesLocalization() {
        if (columns != null && !columns.isEmpty()) {
            for (String columnName : this.columns) {
                String localeColumnName = LangUtil.getString(columnName);
                this.localeColumnNames.put(columnName, localeColumnName);
            }
        }
        return this;
    }

    public int getColumnIndex(String columnName) {
        return columns.indexOf(Optional.ofNullable(localeColumnNames.get(columnName))
            .orElse(columnName));
    }

    public QueryResult error(Exception exception) {
        this.endTime = System.currentTimeMillis();
        this.columns = List.of("Error");
        this.columnTypes = List.of("TEXT");
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("Error", exception.getMessage());
        this.datas = List.of(rowData);
        this.exception = exception;
        return this;
    }

    public QueryResult success() {
        this.endTime = System.currentTimeMillis();
        this.columns = List.of("Info");
        this.columnTypes = List.of("TEXT");
        Map<String, Object> rowData = new HashMap<>();
        rowData.put("Info", "语句执行成功但无返回结果集");
        this.datas = List.of(rowData);
        return this;
    }

    public QueryResult success(SessionDataSet dataset) throws IoTDBConnectionException, StatementExecutionException {
        this.endTime = System.currentTimeMillis();

        this.columns = dataset.getColumnNames();
        this.columnTypes = dataset.getColumnTypes();
        SessionDataSet.DataIterator iterator = dataset.iterator();
        while (iterator.next()) {
            Map<String, Object> data = new HashMap<>();
            for (int i = 0; i < this.columns.size(); i++) {
                TSDataType dataType = TSDataType.valueOf(this.getColumnTypes().get(i));
                String columnName = this.columns.get(i);
                if (iterator.isNull(columnName)) {
                    data.put(columnName, null);
                } else {
                    switch (dataType) {
                        case BOOLEAN:
                            data.put(columnName, iterator.getBoolean(columnName));
                            break;
                        case INT32:
                        case DATE:
                            data.put(columnName, iterator.getInt(columnName));
                            break;
                        case INT64:
                        case TIMESTAMP:
                            data.put(columnName, iterator.getLong(columnName));
                            break;
                        case FLOAT:
                            data.put(columnName, iterator.getFloat(columnName));
                            break;
                        case DOUBLE:
                            data.put(columnName, iterator.getDouble(columnName));
                            break;
                        case TEXT:
                        case BLOB:
                        case STRING:
                            data.put(columnName, iterator.getString(columnName));
                            break;
                        default:
                            data.put(columnName, iterator.getObject(columnName));
                    }
                }
            }
            this.datas.add(data);
        }
        return this;
    }

    public boolean hasException() {
        return exception != null;
    }
}
