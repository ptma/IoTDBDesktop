package org.apache.iotdb.desktop.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author ptma
 */
public final class Icons {

    public static final List<Image> WINDOW_ICON = FlatSVGUtils.createWindowIconImages("/svg/logo.svg");

    public static final Icon LOGO = new FlatSVGIcon("svg/logo.svg", 64, 64);

    public static final Icon TREE_NODE_IOTDB = new FlatSVGIcon("svg/icons/iotdb.svg", 16, 16);
    public static final Icon TREE_NODE_IOTDB_ACTIVE = new FlatSVGIcon("svg/icons/iotdb_active.svg", 16, 16);
    public static final Icon TREE_NODE_DATABSE = new FlatSVGIcon("svg/icons/db.svg", 16, 16);
    public static final Icon TREE_NODE_DATABSE_OPENED = new FlatSVGIcon("svg/icons/db_opened.svg", 16, 16);
    public static final Icon TREE_NODE_COLUMN = new FlatSVGIcon("svg/icons/column.svg");
    public static final Icon TREE_NODE_TABLE = new FlatSVGIcon("svg/icons/table.svg", 16, 16);
    public static final Icon TREE_NODE_GROUP = new FlatSVGIcon("svg/icons/folder.svg", 16, 16);

    public static final Icon TOOLBAR_SESSION = new FlatSVGIcon("svg/icons/toolbar_session.svg", 24, 24);
    public static final Icon TOOLBAR_QUERY = new FlatSVGIcon("svg/icons/toolbar_query.svg", 24, 24);
    public static final Icon TOOLBAR_IMPORT = new FlatSVGIcon("svg/icons/toolbar_import.svg", 24, 24);
    public static final Icon TOOLBAR_EXPORT = new FlatSVGIcon("svg/icons/toolbar_export.svg", 24, 24);
    public static final Icon TOOLBAR_CONSOLE = new FlatSVGIcon("svg/icons/toolbar_console.svg", 24, 24);

    public static final Icon TIPS = new FlatSVGIcon("svg/icons/infoOutline.svg", 12, 12);

    public static final Icon REFRESH = new FlatSVGIcon("svg/icons/refresh_colored.svg");

    public static final Icon INFORMATION = new FlatSVGIcon("svg/icons/information.svg");

    public static final Icon ADD = new FlatSVGIcon("svg/icons/add.svg");
    public static final Icon DELETE = new FlatSVGIcon("svg/icons/delete.svg");
    public static final Icon EXECUTE = new FlatSVGIcon("svg/icons/execute.svg");
    public static final Icon SAVE = new FlatSVGIcon("svg/icons/save.svg");
    public static final Icon OPEN = new FlatSVGIcon("svg/icons/folder_open.svg");
    public static final Icon QUERY = new FlatSVGIcon("svg/icons/toolbar_query.svg", 16, 16);
    public static final Icon TABLE_DATA = new FlatSVGIcon("svg/icons/table_data.svg");

    public static final Icon CLEAR = new FlatSVGIcon("svg/icons/clear.svg");
    public static final Icon IMPORT = new FlatSVGIcon("svg/icons/toolbar_import.svg", 16, 16);
    public static final Icon EXPORT = new FlatSVGIcon("svg/icons/toolbar_export.svg", 16, 16);

    public static final Icon FUNCTION = new FlatSVGIcon("svg/icons/function.svg");
    public static final Icon VARIABLES = new FlatSVGIcon("svg/icons/variables.svg");

    public static final Icon ARROW_FORWARD = new FlatSVGIcon("svg/icons/arrow_forward.svg");
    public static final Icon ARROW_LAST = new FlatSVGIcon("svg/icons/arrow_last.svg");
}
