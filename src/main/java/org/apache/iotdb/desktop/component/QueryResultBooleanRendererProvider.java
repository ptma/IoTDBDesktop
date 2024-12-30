package org.apache.iotdb.desktop.component;

import cn.hutool.core.util.ObjectUtil;
import com.formdev.flatlaf.FlatLaf;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.*;

import javax.swing.*;

/**
 * @author ptma
 */
public class QueryResultBooleanRendererProvider extends ComponentProvider<JCheckBox> {

    private QueryResultTableModel tableModel;

    public QueryResultBooleanRendererProvider(QueryResultTableModel tableModel) {
        this(tableModel, null);
    }

    public QueryResultBooleanRendererProvider(QueryResultTableModel tableModel, StringValue converter) {
        this(tableModel, converter, JLabel.CENTER);
    }

    public QueryResultBooleanRendererProvider(QueryResultTableModel tableModel, StringValue converter, int alignment) {
        super(converter, alignment);
        this.tableModel = tableModel;
    }

    @Override
    protected JCheckBox createRendererComponent() {
        JRendererCheckBox checkBox = new JRendererCheckBox();
        checkBox.setOpaque(true);
        checkBox.setHorizontalAlignment(JLabel.CENTER);
        return checkBox;
    }

    @Override
    protected void configureState(CellContext context) {
        rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
    }

    @Override
    protected void format(CellContext context) {
        JXTable table = (JXTable) context.getComponent();
        TSDataType dataType = tableModel.getColumnDataType(context.getColumn());
        boolean rowOdd = context.getRow() % 2 == 1;
        if (context.isSelected()) {
            if (context.getValue() == null) {
                rendererComponent.setForeground(Utils.darker(table.getSelectionForeground(), 0.7f));
            } else {
                rendererComponent.setForeground(table.getSelectionForeground());
            }
            rendererComponent.setBackground(table.getSelectionBackground());
        } else {
            if (FlatLaf.isLafDark()) {
                if (context.getValue() == null) {
                    rendererComponent.setForeground(Utils.darker(table.getForeground(), 0.5f));
                } else {
                    rendererComponent.setForeground(table.getForeground());
                }
                if (rowOdd) {
                    rendererComponent.setBackground(Utils.brighter(table.getBackground(), 0.8f));
                } else {
                    rendererComponent.setBackground(table.getBackground());
                }
            } else {
                if (context.getValue() == null) {
                    rendererComponent.setForeground(Utils.brighter(table.getForeground(), 0.5f));
                } else {
                    rendererComponent.setForeground(table.getForeground());
                }
                if (rowOdd) {
                    rendererComponent.setBackground(Utils.darker(table.getBackground(), 0.95f));
                } else {
                    rendererComponent.setBackground(table.getBackground());
                }
            }
        }
        rendererComponent.setText(context.getValue() == null ? "<NULL>" : getString(context.getValue()));
    }

    public String getString(Object value) {
        if (value instanceof Boolean booleanValue) {
            rendererComponent.setSelected(booleanValue);
            return "";
        } else {
            return formatter.getString(value);
        }
    }
}
