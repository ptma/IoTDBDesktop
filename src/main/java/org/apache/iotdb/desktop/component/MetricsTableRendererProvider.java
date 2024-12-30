package org.apache.iotdb.desktop.component;

import cn.hutool.core.util.ObjectUtil;
import com.formdev.flatlaf.FlatLaf;
import org.apache.iotdb.desktop.util.Utils;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.CellContext;
import org.jdesktop.swingx.renderer.ComponentProvider;
import org.jdesktop.swingx.renderer.JRendererLabel;

import javax.swing.*;

/**
 * @author ptma
 */
public class MetricsTableRendererProvider extends ComponentProvider<JLabel> {

    public MetricsTableRendererProvider() {
        super();
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

}
