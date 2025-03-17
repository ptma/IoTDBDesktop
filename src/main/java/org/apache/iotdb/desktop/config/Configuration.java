package org.apache.iotdb.desktop.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.util.Const;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.isession.SessionConfig;
import org.apache.iotdb.isession.util.Version;

import java.io.Serializable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * @author ptma
 */
@Slf4j
public final class Configuration implements Serializable {

    private static class ConfigurationHolder {
        private final static Configuration INSTANCE = new Configuration();
    }

    public static Configuration instance() {
        return ConfigurationHolder.INSTANCE;
    }

    private final Preferences root;

    private Options options;

    private Configuration() {
        root = Preferences.userRoot().node("iotdb-desktop");
    }

    public Options options() {
        if (this.options == null) {
            this.options = loadOptions();
        }
        return this.options;
    }

    private Options loadOptions() {
        Options options = new Options();
        options.setTheme(getString(ConfKeys.THEME, Themes.LIGHT.name()));
        options.setLanguage(getString(ConfKeys.LANGUAGE, LangUtil.getBundle().getLocale().toLanguageTag()));
        options.setFontName(getString(ConfKeys.FONT_NAME, Const.EDITOR_FONT_NAME));
        options.setFontSize(getInt(ConfKeys.FONT_SIZE, Const.EDITOR_FONT_SIZE));
        options.setAutoCompletion(getBoolean(ConfKeys.AUTO_COMPLETION, true));
        options.setAutoCompletionDelay(getInt(ConfKeys.AUTO_COMPLETION_DELAY, 200));
        options.setAutoLoadDeviceNodes(getBoolean(ConfKeys.AUTO_LOAD_DEVICE_NODES, false));
        options.setLogInternalSql(getBoolean(ConfKeys.LOG_INTERNAL_SQL, false));
        options.setLogTimestamp(getBoolean(ConfKeys.LOG_TIMESTAMP, false));
        options.setTimeFormat(getString(ConfKeys.TIME_FORMAT, "yyyy-MM-dd HH:mm:ss.SSS"));

        options.setDblclickOpenEditor(getBoolean(ConfKeys.DBLCLICK_OPEN_EDITOR, false));
        options.setFlattenDeviceNodes(getBoolean(ConfKeys.FLATTEN_DEVICE_NODES, true));
        options.setEditorSortOrder(getString(ConfKeys.EDITOR_SORT_ORDER, "desc"));
        options.setEditorPageSize(getInt(ConfKeys.EDITOR_PAGE_SIZE, 500));
        options.setEditorAligned(getBoolean(ConfKeys.EDITOR_ALIGNED, true));
        return options;
    }

    public void saveOptions() {
        Options oldOptions = loadOptions();

        Configuration.instance().setString(ConfKeys.LANGUAGE, options().getLanguage());
        Configuration.instance().setString(ConfKeys.THEME, options().getTheme());
        Configuration.instance().setString(ConfKeys.FONT_NAME, options().getFontName());
        Configuration.instance().setInt(ConfKeys.FONT_SIZE, options().getFontSize());
        Configuration.instance().setBoolean(ConfKeys.AUTO_COMPLETION, options().isAutoCompletion());
        Configuration.instance().setInt(ConfKeys.AUTO_COMPLETION_DELAY, options().getAutoCompletionDelay());
        Configuration.instance().setBoolean(ConfKeys.AUTO_LOAD_DEVICE_NODES, options().isAutoLoadDeviceNodes());
        Configuration.instance().setBoolean(ConfKeys.LOG_INTERNAL_SQL, options().isLogInternalSql());
        Configuration.instance().setBoolean(ConfKeys.LOG_TIMESTAMP, options().isLogTimestamp());
        Configuration.instance().setString(ConfKeys.TIME_FORMAT, options().getTimeFormat());

        Configuration.instance().setBoolean(ConfKeys.DBLCLICK_OPEN_EDITOR, options.isDblclickOpenEditor());
        Configuration.instance().setBoolean(ConfKeys.FLATTEN_DEVICE_NODES, options.isFlattenDeviceNodes());
        Configuration.instance().setString(ConfKeys.EDITOR_SORT_ORDER, options.getEditorSortOrder());
        Configuration.instance().setInt(ConfKeys.EDITOR_PAGE_SIZE, options.getEditorPageSize());
        Configuration.instance().setBoolean(ConfKeys.EDITOR_ALIGNED, options.isEditorAligned());

        AppEvents.instance().applyEvent(l -> l.optionsChanged(options(), oldOptions));
    }

    public void saveSession(SessionProps props) {
        saveSession(root.node("sessions"), props);
    }

    private void saveSession(Preferences parnet, SessionProps props) {
        Preferences nodePref = parnet.node(props.getId());

        nodePref.put("name", props.getName());
        nodePref.put("host", props.getHost());
        nodePref.putInt("port", props.getPort());
        nodePref.put("username", props.getUsername());
        nodePref.put("password", props.getPassword());
        nodePref.putInt("fetchSize", props.getFetchSize());
        if (props.getZoneId() != null) {
            nodePref.put("zoneId", props.getZoneId().getId());
        }
        nodePref.putInt("thriftDefaultBufferSize", props.getThriftDefaultBufferSize());
        nodePref.putInt("thriftMaxFrameSize", props.getThriftMaxFrameSize());
        nodePref.putBoolean("enableRedirection", props.isEnableRedirection());
        nodePref.putBoolean("enableRecordsAutoConvertTablet", props.isEnableRecordsAutoConvertTablet());
        nodePref.put("version", props.getVersion().getVersion().name());
        nodePref.putLong("timeOut", props.getTimeOut());
        nodePref.putBoolean("enableAutoFetch", props.isEnableAutoFetch());
        nodePref.putBoolean("useSSL", props.isUseSSL());
        if (props.getTrustStore() != null) {
            nodePref.put("trustStore", props.getTrustStore());
        }
        if (props.getTrustStorePwd() != null) {
            nodePref.put("trustStorePwd", props.getTrustStorePwd());
        }
        nodePref.putInt("maxRetryCount", props.getMaxRetryCount());
        nodePref.putLong("retryIntervalInMs", props.getRetryIntervalInMs());
        nodePref.put("sqlDialect", props.getSqlDialect());
    }

    public List<SessionProps> loadSessionProps() {
        return loadSessionProps(root.node("sessions"));
    }

    private List<SessionProps> loadSessionProps(Preferences parentPref) {
        List<SessionProps> sessions = new ArrayList<>();
        try {
            for (String id : parentPref.childrenNames()) {
                Preferences nodePref = parentPref.node(id);
                SessionProps props = new SessionProps();
                props.setId(id);
                props.setName(nodePref.get("name", ""));
                props.setHost(nodePref.get("host", "127.0.0.1"));
                props.setPort(nodePref.getInt("port", 6667));
                props.setUsername(nodePref.get("username", "root"));
                props.setPassword(nodePref.get("password", "root"));
                props.setFetchSize(nodePref.getInt("fetchSize", SessionConfig.DEFAULT_FETCH_SIZE));
                String zoneId = nodePref.get("zoneId", null);
                if (zoneId != null) {
                    props.setZoneId(ZoneId.of(zoneId));
                }
                props.setThriftDefaultBufferSize(nodePref.getInt("thriftDefaultBufferSize", SessionConfig.DEFAULT_INITIAL_BUFFER_CAPACITY));
                props.setThriftMaxFrameSize(nodePref.getInt("thriftMaxFrameSize", SessionConfig.DEFAULT_MAX_FRAME_SIZE));
                props.setEnableRedirection(nodePref.getBoolean("enableRedirection", SessionConfig.DEFAULT_REDIRECTION_MODE));
                props.setEnableRecordsAutoConvertTablet(nodePref.getBoolean("enableRecordsAutoConvertTablet", SessionConfig.DEFAULT_RECORDS_AUTO_CONVERT_TABLET));
                props.setVersion(IotdbVersion.of(Version.valueOf(nodePref.get("version", IotdbVersion.V_1_0.getVersion().name()))));
                props.setTimeOut(nodePref.getLong("timeOut", SessionConfig.DEFAULT_QUERY_TIME_OUT));
                props.setEnableAutoFetch(nodePref.getBoolean("enableAutoFetch", SessionConfig.DEFAULT_ENABLE_AUTO_FETCH));
                props.setUseSSL(nodePref.getBoolean("useSSL", false));
                String trustStore = nodePref.get("trustStore", null);
                if (trustStore != null) {
                    props.setTrustStore(trustStore);
                }
                String trustStorePwd = nodePref.get("trustStorePwd", null);
                if (trustStorePwd != null) {
                    props.setTrustStorePwd(trustStorePwd);
                }
                props.setMaxRetryCount(nodePref.getInt("maxRetryCount", SessionConfig.MAX_RETRY_COUNT));
                props.setRetryIntervalInMs(nodePref.getLong("retryIntervalInMs", SessionConfig.RETRY_INTERVAL_IN_MS));
                props.setSqlDialect(nodePref.get("sqlDialect", "tree"));
                sessions.add(props);
            }
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
        }
        return sessions;
    }

    public String getString(String key, String defaultValue) {
        return root.get(key, defaultValue);
    }

    public void setString(String key, String value) {
        root.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        return root.getInt(key, defaultValue);
    }

    public void setInt(String key, int value) {
        root.putInt(key, value);
    }

    public long getLong(String key, long defaultValue) {
        return root.getLong(key, defaultValue);
    }

    public void setLong(String key, long value) {
        root.putLong(key, value);
    }

    public float getFloat(String key, float defaultValue) {
        return root.getFloat(key, defaultValue);
    }

    public void setFloat(String key, float value) {
        root.putFloat(key, value);
    }

    public double getDouble(String key, double defaultValue) {
        return root.getDouble(key, defaultValue);
    }

    public void setDouble(String key, double value) {
        root.putDouble(key, value);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return root.getBoolean(key, defaultValue);
    }

    public void setBoolean(String key, boolean value) {
        root.putBoolean(key, value);
    }

    public String getString(String path, String key, String defaultValue) {
        return root.node(path).get(key, defaultValue);
    }

    public void setString(String path, String key, String value) {
        root.node(path).put(key, value);
    }

    public int getInt(String path, String key, int defaultValue) {
        return root.node(path).getInt(key, defaultValue);
    }

    public void setInt(String path, String key, int value) {
        root.node(path).putInt(key, value);
    }

    public long getLong(String path, String key, long defaultValue) {
        return root.node(path).getLong(key, defaultValue);
    }

    public void setLong(String path, String key, long value) {
        root.node(path).putLong(key, value);
    }

    public float getFloat(String path, String key, float defaultValue) {
        return root.node(path).getFloat(key, defaultValue);
    }

    public void setFloat(String path, String key, float value) {
        root.node(path).putFloat(key, value);
    }

    public double getDouble(String path, String key, double defaultValue) {
        return root.node(path).getDouble(key, defaultValue);
    }

    public void setDouble(String path, String key, double value) {
        root.node(path).putDouble(key, value);
    }

    public boolean getBoolean(String path, String key, boolean defaultValue) {
        return root.node(path).getBoolean(key, defaultValue);
    }

    public void setBoolean(String path, String key, boolean value) {
        root.node(path).putBoolean(key, value);
    }
}
