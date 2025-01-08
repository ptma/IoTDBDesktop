package org.apache.iotdb.desktop.event;

import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.model.Device;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Sessionable;

import javax.swing.*;
import javax.swing.tree.TreePath;

public interface AppEventListener {

    void optionsChanged(Options options, Options oldOptions);

    void appendSqlLog(String sql, boolean isComment);

    default void appendSqlLog(String sql) {
        appendSqlLog(sql, false);
    }

    void onTreeSelectionChange(TreePath treePath);

    void newTab(String title, Icon icon, TabPanel tabPanel);

    void newInfoTab(Sessionable sessionable);

    void newDeviceDataTab(Device device);

    void openDataExport(Session session);

    void openDataImport(Session session);

    void closeSession(Session session);
}
