package org.apache.iotdb.desktop.event;

import org.apache.iotdb.desktop.component.TabPanel;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.model.Device;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Sessionable;
import org.apache.tsfile.enums.TSDataType;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.util.Map;

public interface DataEventListener {

    void dataRemove(long[] timestamps);

    void dataUpdate(long timestamp, String column, TSDataType dataType, Object value);
}
