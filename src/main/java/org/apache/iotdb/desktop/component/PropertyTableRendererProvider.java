package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.FlatLaf;
import org.apache.iotdb.desktop.util.Utils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.CellContext;
import org.jdesktop.swingx.renderer.ComponentProvider;
import org.jdesktop.swingx.renderer.JRendererLabel;
import org.jdesktop.swingx.renderer.StringValue;

import javax.swing.*;

/**
 * @author ptma
 */
public class PropertyTableRendererProvider extends ComponentProvider<JLabel> {

    private PropertyTableModel tableModel;

    public PropertyTableRendererProvider(PropertyTableModel tableModel) {
        this(tableModel, null);
    }

    public PropertyTableRendererProvider(PropertyTableModel tableModel, StringValue converter) {
        this(tableModel, converter, JLabel.LEADING);
    }

    public PropertyTableRendererProvider(PropertyTableModel tableModel, StringValue converter, int alignment) {
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

        if (context.getColumn() == 0) {
            rendererComponent.setHorizontalAlignment(JLabel.RIGHT);
            if (FlatLaf.isLafDark()) {
                rendererComponent.setForeground(table.getForeground());
                rendererComponent.setBackground(Utils.brighter(table.getBackground(), 0.8f));
            } else {
                rendererComponent.setForeground(table.getForeground());
                rendererComponent.setBackground(Utils.darker(table.getBackground(), 0.95f));
            }
        } else {
            rendererComponent.setHorizontalAlignment(JLabel.LEFT);
        }
        rendererComponent.setText(getString(context.getValue()));
    }

}
