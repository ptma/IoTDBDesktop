package org.apache.iotdb.desktop.component;

import cn.hutool.core.util.NumberUtil;
import com.formdev.flatlaf.FlatLaf;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.tsfile.enums.TSDataType;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.CellContext;
import org.jdesktop.swingx.renderer.ComponentProvider;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.renderer.StringValue;

import javax.swing.*;
import java.time.Duration;

/**
 * @author ptma
 */
public class QueryResultRendererProvider extends ComponentProvider<JLabel> {

    private final QueryResultTableModel tableModel;

    public QueryResultRendererProvider(QueryResultTableModel tableModel) {
        this(tableModel, null);
    }

    public QueryResultRendererProvider(QueryResultTableModel tableModel, StringValue converter) {
        this(tableModel, converter, JLabel.LEADING);
    }

    public QueryResultRendererProvider(QueryResultTableModel tableModel, StringValue converter, int alignment) {
        super(converter, alignment);
        this.tableModel = tableModel;
    }

    @Override
    protected JLabel createRendererComponent() {
        JRendererLabel label = new JRendererLabel();
        label.setOpaque(true);
        label.setHorizontalAlignment(JLabel.LEFT);
        return label;
    }

    @Override
    protected void configureState(CellContext context) {
        rendererComponent.setHorizontalAlignment(getHorizontalAlignment());
    }

    @Override
    protected void format(CellContext context) {
        JXTable table = (JXTable) context.getComponent();
        TSDataType dataType = tableModel.getColumnDataType(context.getColumn());
        if (dataType.isNumeric()) {
            rendererComponent.setHorizontalAlignment(JLabel.RIGHT);
        } else {
            rendererComponent.setHorizontalAlignment(JLabel.LEFT);
        }
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
        String columnName = tableModel.getColumnName(context.getColumn());
        if ("TTL(ms)".equalsIgnoreCase(columnName)) {
            String ttlValue = getString(context.getValue());
            if (NumberUtil.isNumber(ttlValue)) {
                rendererComponent.setText(Utils.durationToString(Duration.ofMillis(Long.parseLong(ttlValue))));
            } else {
                rendererComponent.setText(ttlValue);
            }
        } else {
            rendererComponent.setText(context.getValue() == null ? "<NULL>" : getString(context.getValue()));
        }
    }

}
