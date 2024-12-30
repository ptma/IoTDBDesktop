package org.apache.iotdb.desktop.config;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Options {

    private String language;
    private String theme;
    private String fontName;
    private int fontSize;
    private boolean autoCompletion;
    private int autoCompletionDelay;
    private String timeFormat;
    private boolean autoLoadDeviceNodes;
    private boolean logInternalSql;
    private boolean logTimestamp;
    
    private boolean dblclickOpenEditor;
    private String editorSortOrder;
    private int editorPageSize;
    private boolean editorAligned;
}
