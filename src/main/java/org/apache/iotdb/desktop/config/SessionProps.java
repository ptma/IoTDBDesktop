package org.apache.iotdb.desktop.config;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.isession.SessionConfig;
import org.apache.iotdb.session.Session;

import java.io.Serializable;
import java.time.ZoneId;

/**
 * @author ptma
 */
@Getter
@Setter
public class SessionProps implements Serializable, Cloneable {

    private String id = IdUtil.getSnowflakeNextIdStr();
    private String name;
    private String host = "127.0.0.1";
    private int port = 6667;
    private String username;
    private String password;
    private int fetchSize = SessionConfig.DEFAULT_FETCH_SIZE;
    private ZoneId zoneId = null;
    private int thriftDefaultBufferSize = SessionConfig.DEFAULT_INITIAL_BUFFER_CAPACITY;
    private int thriftMaxFrameSize = SessionConfig.DEFAULT_MAX_FRAME_SIZE;
    private boolean enableRedirection = SessionConfig.DEFAULT_REDIRECTION_MODE;
    private boolean enableRecordsAutoConvertTablet = SessionConfig.DEFAULT_RECORDS_AUTO_CONVERT_TABLET;
    private IotdbVersion version = IotdbVersion.V_1_0;
    private long timeOut = SessionConfig.DEFAULT_QUERY_TIME_OUT;
    private boolean enableAutoFetch = SessionConfig.DEFAULT_ENABLE_AUTO_FETCH;
    private boolean useSSL = false;
    private String trustStore;
    private String trustStorePwd;
    private int maxRetryCount = SessionConfig.MAX_RETRY_COUNT;
    private long retryIntervalInMs = SessionConfig.RETRY_INTERVAL_IN_MS;

    @Override
    public SessionProps clone() throws CloneNotSupportedException {
        SessionProps clone = (SessionProps) super.clone();
        return clone;
    }

    public Session.Builder toBuilder() {
        Session.Builder builder = new Session.Builder();
        builder
            .host(host)
            .port(port)
            .username(username)
            .password(password)
            .fetchSize(fetchSize)
            .zoneId(zoneId)
            .thriftDefaultBufferSize(thriftDefaultBufferSize)
            .thriftMaxFrameSize(thriftMaxFrameSize)
            .enableRedirection(enableRedirection)
            .enableRecordsAutoConvertTablet(enableRecordsAutoConvertTablet)
            .version(version.getVersion())
            .timeOut(timeOut)
            .enableAutoFetch(enableAutoFetch)
            .useSSL(useSSL)
            .trustStore(trustStore)
            .trustStorePwd(trustStorePwd)
            .maxRetryCount(maxRetryCount)
            .retryIntervalInMs(retryIntervalInMs);
        return builder;
    }
}
