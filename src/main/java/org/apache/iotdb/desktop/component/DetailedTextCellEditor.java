package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.FlatClientProperties;
import org.apache.iotdb.desktop.form.DetailedTextForm;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;

public class DetailedTextCellEditor extends DefaultCellEditor {

    private final JTextField textField;

    public DetailedTextCellEditor(JTextField textField) {
        super(textField);
        this.textField = textField;
        JButton detailButton = new JButton("...");
        textField.setBorder(new LineBorder(Color.black));
        textField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, detailButton);
        detailButton.addActionListener(e -> {
            DetailedTextForm.open(textField.getText(), textField::setText);
        });
    }

}
