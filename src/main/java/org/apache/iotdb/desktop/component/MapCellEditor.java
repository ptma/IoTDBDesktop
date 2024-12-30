package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.FlatClientProperties;
import org.apache.iotdb.desktop.form.DetailedTextForm;
import org.apache.iotdb.desktop.form.MapValueForm;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class MapCellEditor extends DefaultCellEditor {

    private final String title;
    private final JTextField textField;

    public MapCellEditor(String title, JTextField textField) {
        super(textField);
        this.title = title;
        this.textField = textField;
        this.textField.setEditable(false);
        JButton detailButton = new JButton("...");
        textField.setBorder(new LineBorder(Color.black));
        textField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, detailButton);
        detailButton.addActionListener(e -> {
            MapValueForm.open(title, textField.getText(), textField::setText);
        });
    }

}
