package org.apache.iotdb.desktop.component;

import org.apache.iotdb.commons.exception.IllegalPathException;
import org.apache.iotdb.commons.utils.PathUtils;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.model.*;
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
        setText(value.toString());
        if (value instanceof DefaultMutableTreeNode treeNode) {
            String path = value.toString();
            if (treeNode.getUserObject() instanceof Session session) {
                setIcon(session.isOpened() ? Icons.TREE_NODE_IOTDB_ACTIVE : Icons.TREE_NODE_IOTDB);
            } else if (treeNode.getUserObject() instanceof Database database) {
                setIcon(database.isChildrenLoaded() ? Icons.TREE_NODE_DATABSE_OPENED : Icons.TREE_NODE_DATABSE);
            } else if (treeNode.getUserObject() instanceof PathGroup) {
                setIcon(Icons.TREE_NODE_GROUP);
                if (!Configuration.instance().options().isFlattenDeviceNodes() && path.contains(".")) {
                    try {
                        String[] splitPaths = PathUtils.splitPathToDetachedNodes(path);
                        if (splitPaths.length > 0) {
                            setText(splitPaths[splitPaths.length - 1]);
                        } else {
                            setText(path);
                        }
                    } catch (IllegalPathException e) {
                        setText(path);
                    }
                }
            } else if (treeNode.getUserObject() instanceof Device) {
                setIcon(Icons.TREE_NODE_TABLE);
                if (!Configuration.instance().options().isFlattenDeviceNodes() && path.contains(".")) {
                    setText(path.substring(path.lastIndexOf(".") + 1));
                    try {
                        String[] splitPaths = PathUtils.splitPathToDetachedNodes(path);
                        if (splitPaths.length > 0) {
                            setText(splitPaths[splitPaths.length - 1]);
                        } else {
                            setText(path);
                        }
                    } catch (IllegalPathException e) {
                        setText(path);
                    }
                }
            } else if (treeNode.getUserObject() instanceof Table) {
                setIcon(Icons.TREE_NODE_TABLE);
            } else {
                setIcon(null);
            }
        }
        setOpaque(false);
        return this;
    }
}
