package org.apache.iotdb.desktop.form;

import cn.hutool.core.util.StrUtil;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.exception.VerificationException;
import org.apache.iotdb.desktop.model.Database;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.desktop.util.Validator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.Duration;
import java.util.function.Consumer;

public class CreateDatabaseForm extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField databaseField;
    private JLabel databaseLabel;
    private JLabel ttlLabel;
    private JTextField ttlField;

    private Session session;
    private Consumer<Database> createdConsumer;

    public static void open(Session session, Consumer<Database> createdConsumer) {
        JDialog dialog = new CreateDatabaseForm(IotdbDesktopApp.frame, session, createdConsumer);
        dialog.setMinimumSize(new Dimension(450, 170));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(IotdbDesktopApp.frame);
        dialog.setVisible(true);
    }

    public CreateDatabaseForm(Frame owner, Session session, Consumer<Database> createdConsumer) {
        super(owner);
        this.session = session;
        this.createdConsumer = createdConsumer;
        $$$setupUI$$$();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        databaseLabel.setText(LangUtil.getString("DatabasePath"));
        ttlLabel.setText(LangUtil.getString("TTL"));
        Utils.UI.tooltip(ttlLabel, "TTLToolTip");
        setTitle(LangUtil.getString("NewDatabase"));
        LangUtil.buttonText(buttonOK, "&Ok");
        LangUtil.buttonText(buttonCancel, "&Cancel");
    }

    private void verifyFields() throws VerificationException {
        Validator.notEmpty(databaseField, () -> LangUtil.format("FieldRequiredValidation", databaseLabel.getText()));
        Validator.regex(ttlField, "^(INF)|(\\d+[dDhHMmSs])+$", () -> LangUtil.format("TTLValueNotValid", ttlLabel.getText()));
    }

    private void onOK() {
        try {
            verifyFields();
        } catch (Exception e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }
        SwingWorker<Exception, Integer> executeWorker = new SwingWorker<>() {
            @Override
            protected void done() {
                try {
                    Exception e = get();
                    if (e == null) {
                        Utils.Toast.success(String.format(LangUtil.getString("NewDatabaseSuccess"), databaseField.getText()));
                        dispose();
                        createdConsumer.accept(new Database(databaseField.getText().trim(), true, session));
                    } else {
                        Utils.Message.error(e.getMessage(), e);
                    }
                } catch (Exception ex) {
                    Utils.Message.error(ex.getMessage(), ex);
                }
            }

            @Override
            protected Exception doInBackground() throws Exception {
                try {
                    String ttlString = StrUtil.trim(ttlField.getText());
                    boolean ttlSpecified = false;
                    if (StrUtil.isNotBlank(ttlString)) {
                        if (!ttlString.equals("INF")) {
                            Duration duration = Utils.parseDuration(ttlString);
                            if (duration.toSeconds() == 0) {
                                return null;
                            }
                            ttlString = String.valueOf(duration.toMillis());
                            ttlSpecified = true;
                        }
                    }

                    if (session.isTableDialect()) {
                        session.createTableModeDatabase(databaseField.getText(), ttlString);
                    } else {
                        session.createTreeModeDatabase(databaseField.getText());
                        if (ttlSpecified) {
                            session.ttlToPath(databaseField.getText(), ttlString);
                        }
                    }
                    return null;
                } catch (Exception e) {
                    return e;
                }
            }
        };
        executeWorker.execute();
    }

    private void onCancel() {
        dispose();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:p:noGrow,left:4dlu:noGrow,fill:120px:noGrow,left:4dlu:noGrow,fill:max(d;4px):grow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        databaseLabel = new JLabel();
        databaseLabel.setHorizontalAlignment(11);
        databaseLabel.setText("Database");
        CellConstraints cc = new CellConstraints();
        panel3.add(databaseLabel, cc.xy(1, 1));
        databaseField = new JTextField();
        panel3.add(databaseField, cc.xyw(3, 1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
        ttlLabel = new JLabel();
        ttlLabel.setHorizontalAlignment(11);
        ttlLabel.setText("TTL");
        panel3.add(ttlLabel, cc.xy(1, 3));
        ttlField = new JTextField();
        ttlField.setHorizontalAlignment(11);
        panel3.add(ttlField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}
