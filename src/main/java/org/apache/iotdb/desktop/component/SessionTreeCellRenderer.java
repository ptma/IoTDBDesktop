package org.apache.iotdb.desktop.component;

import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.model.Database;
import org.apache.iotdb.desktop.model.Device;
import org.apache.iotdb.desktop.util.Icons;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class SessionTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {

        if (selected) {
            setForeground(getTextSelectionColor());
        } else {
            setForeground(getTextNonSelectionColor());
        }
        if (value instanceof DefaultMutableTreeNode treeNode) {
            if (treeNode.getUserObject() instanceof Session session) {
                setIcon(session.isOpened() ? Icons.TREE_NODE_IOTDB_ACTIVE : Icons.TREE_NODE_IOTDB);
            } else if (treeNode.getUserObject() instanceof Database database) {
                setIcon(database.isDevicesLoaded() ? Icons.TREE_NODE_DATABSE_OPENED : Icons.TREE_NODE_DATABSE);
            } else if (treeNode.getUserObject() instanceof Device) {
                setIcon(Icons.TREE_NODE_TABLE);
            } else {
                setIcon(null);
            }
        }
        setText(value.toString());
        setOpaque(false);
        return this;
    }
}
