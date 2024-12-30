package org.apache.iotdb.desktop.event;

import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.model.Device;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Sessionable;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class AppEventListenerAdapter implements AppEventListener {

    @Override
    public void optionsChanged(Options options) {

    }

    @Override
    public void appendSqlLog(String sql, boolean isComment) {

    }

    @Override
    public void onTreeSelectionChange(TreePath treePath) {

    }

    @Override
    public void newTab(String title, Icon icon, TabPanel tabPanel) {

    }

    @Override
    public void newInfoTab(Sessionable sessionable) {

    }

    @Override
    public void newDeviceDataTab(Device device) {

    }

    @Override
    public void openDataExport(Session session) {

    }

    @Override
    public void openDataImport(Session session) {

    }

    @Override
    public void closeSession(Session session) {

    }
}
