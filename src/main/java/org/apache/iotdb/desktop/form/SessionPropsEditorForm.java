package org.apache.iotdb.desktop.form;

import com.formdev.flatlaf.FlatClientProperties;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.config.SessionProps;
import org.apache.iotdb.desktop.exception.VerificationException;
import org.apache.iotdb.desktop.model.Session;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.apache.iotdb.desktop.util.Validator;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

/**
 * @author ptma
 */
@SuppressWarnings("ALL")
@Slf4j
public class SessionPropsEditorForm extends JDialog {
    private JPanel contentPanel;
    private JButton buttonOk;
    private JButton buttonCancel;
    private JPanel generalPanel;
    private JPanel bottomPanel;
    private JPanel bottomButtonPanel;

    private JLabel nameLabel;
    private JTextField nameField;

    private JLabel hostLabel;
    private JTextField hostField;

    private JLabel usernameLabel;
    private JTextField usernameField;
    private JLabel passwordLabel;
    private JPasswordField passwordField;

    private JButton buttonTest;
    private JCheckBox sslCheckBox;
    private JLabel useSSL;
    private JSpinner portField;
    private JLabel portLabel;
    private JSpinner fetchSizeField;
    private JLabel fetchSizeLabel;
    private JLabel modelLabel;
    private JComboBox modelField;

    private SessionProps editingProps;
    private final Consumer<SessionProps> consumer;

    public static void open(SessionProps editingProps, Consumer<SessionProps> consumer) {
        JDialog dialog = new SessionPropsEditorForm(IotdbDesktopApp.frame, editingProps, consumer);
        dialog.setMinimumSize(new Dimension(400, 360));
        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(IotdbDesktopApp.frame);
        dialog.setVisible(true);
    }

    private SessionPropsEditorForm(Frame owner, SessionProps editingProps, Consumer<SessionProps> consumer) {
        super(owner);
        this.consumer = consumer;
        this.editingProps = editingProps;
        $$$setupUI$$$();

        this.setModal(true);
        this.setContentPane(contentPanel);
        this.setResizable(false);
        getRootPane().setDefaultButton(buttonOk);

        initComponents();
        initComponentsAction();
        initFormFieldsData();
        localization();
    }

    private void initComponents() {
        if (editingProps == null) {
            this.setTitle(LangUtil.getString("NewSession"));
        } else {
            this.setTitle(LangUtil.getString("EditSession"));
        }

        portField.setModel(new SpinnerNumberModel(6667, 1, 99999, 1));
        portField.setEditor(new JSpinner.NumberEditor(portField, "####"));
        passwordField.putClientProperty(FlatClientProperties.STYLE, "showRevealButton: true");

        fetchSizeField.setModel(new SpinnerNumberModel(5000, 1000, 99999999, 1));
        fetchSizeField.setEditor(new JSpinner.NumberEditor(fetchSizeField, "####"));
    }

    private void initComponentsAction() {
        buttonTest.addActionListener(e -> onTest());
        buttonOk.addActionListener(e -> onOk());
        buttonCancel.addActionListener(e -> onCancel());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPanel.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    @SneakyThrows
    private void initFormFieldsData() {
        if (editingProps == null) {
            return;
        }
        nameField.setText(editingProps.getName());
        hostField.setText(editingProps.getHost());
        portField.setValue(editingProps.getPort());
        usernameField.setText(editingProps.getUsername());
        passwordField.setText(editingProps.getPassword());
        sslCheckBox.setSelected(editingProps.isUseSSL());
        fetchSizeField.setValue(editingProps.getFetchSize());
        modelField.setSelectedItem(editingProps.getSqlDialect());
    }

    public void localization() {
        nameLabel.setText(LangUtil.getString("Name"));
        hostLabel.setText(LangUtil.getString("Host"));
        portLabel.setText(LangUtil.getString("Port"));
        usernameLabel.setText(LangUtil.getString("Username"));
        passwordLabel.setText(LangUtil.getString("Password"));
        useSSL.setText(LangUtil.getString("UseSSL"));
        fetchSizeLabel.setText(LangUtil.getString("FetchSize"));
        modelLabel.setText(LangUtil.getString("SQLModel"));

        LangUtil.buttonText(buttonTest, "TestConnection");
        LangUtil.buttonText(buttonOk, "&Ok");
        LangUtil.buttonText(buttonCancel, "&Cancel");
    }

    private void verifyFields() throws VerificationException {
        Validator.notEmpty(nameField, () -> LangUtil.format("FieldRequiredValidation", nameLabel.getText()));
        Validator.notEmpty(hostField, () -> LangUtil.format("FieldRequiredValidation", hostLabel.getText()));
    }

    private SessionProps getEditingProps() {
        SessionProps props = new SessionProps();
        if (editingProps != null) {
            props.setId(editingProps.getId());
        }
        props.setName(nameField.getText());
        props.setHost(hostField.getText());
        props.setPort((Integer) portField.getValue());
        props.setUsername(usernameField.getText());
        props.setPassword(String.valueOf(passwordField.getPassword()));
        props.setUseSSL(sslCheckBox.isSelected());
        props.setFetchSize((Integer) fetchSizeField.getValue());
        props.setSqlDialect(modelField.getSelectedItem().toString());

        return props;
    }

    private void verifyProperties(Consumer<SessionProps> consumer) {
        try {
            verifyFields();
        } catch (VerificationException e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }

        consumer.accept(getEditingProps());
    }

    private void onTest() {
        try {
            verifyFields();
        } catch (VerificationException e) {
            Utils.Toast.warn(e.getMessage());
            return;
        }

        SwingWorker<Exception, Integer> worker = new SwingWorker<>() {
            @Override
            protected void done() {
                Exception ex = null;
                try {
                    ex = get();
                    if (ex == null) {
                        Utils.Toast.success(String.format(LangUtil.getString("TestConnectionSuccessful")));
                    } else {
                        Utils.Toast.error(String.format(LangUtil.getString("TestConnectionError"), ex.getMessage()));
                    }
                } catch (Exception e) {
                    Utils.Message.error(e.getMessage(), e);
                }
                buttonTest.setEnabled(true);
            }

            @Override
            protected Exception doInBackground() {
                buttonTest.setEnabled(false);
                Session testSession = new Session(getEditingProps());
                try {
                    testSession.open();
                } catch (Exception e) {
                    return e;
                } finally {
                    testSession.close();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void onOk() {
        verifyProperties(props -> {
            if (consumer != null) {
                consumer.accept(props);
            }
            dispose();
        });
    }

    private void onCancel() {
        // add your code here if necessary
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
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        generalPanel = new JPanel();
        generalPanel.setLayout(new FormLayout("fill:p:noGrow,left:4dlu:noGrow,fill:p:noGrow,left:4dlu:noGrow,fill:p:noGrow,fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:60px:grow(0.7)", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        contentPanel.add(generalPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        generalPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        hostLabel = new JLabel();
        hostLabel.setHorizontalAlignment(11);
        hostLabel.setText("Host");
        CellConstraints cc = new CellConstraints();
        generalPanel.add(hostLabel, cc.xy(1, 3));
        hostField = new JTextField();
        generalPanel.add(hostField, cc.xyw(3, 3, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        nameLabel = new JLabel();
        nameLabel.setHorizontalAlignment(11);
        nameLabel.setText("Name");
        generalPanel.add(nameLabel, cc.xy(1, 1));
        nameField = new JTextField();
        generalPanel.add(nameField, cc.xyw(3, 1, 6, CellConstraints.FILL, CellConstraints.DEFAULT));
        usernameLabel = new JLabel();
        usernameLabel.setHorizontalAlignment(11);
        usernameLabel.setText("Username");
        generalPanel.add(usernameLabel, cc.xy(1, 7));
        usernameField = new JTextField();
        generalPanel.add(usernameField, cc.xy(3, 7, CellConstraints.FILL, CellConstraints.DEFAULT));
        passwordLabel = new JLabel();
        passwordLabel.setHorizontalAlignment(11);
        passwordLabel.setText("Password");
        generalPanel.add(passwordLabel, cc.xy(1, 9));
        passwordField = new JPasswordField();
        generalPanel.add(passwordField, cc.xy(3, 9, CellConstraints.FILL, CellConstraints.DEFAULT));
        sslCheckBox = new JCheckBox();
        sslCheckBox.setText("");
        generalPanel.add(sslCheckBox, cc.xy(3, 11));
        useSSL = new JLabel();
        useSSL.setHorizontalAlignment(11);
        useSSL.setText("Use SSL");
        generalPanel.add(useSSL, cc.xy(1, 11));
        portField = new JSpinner();
        generalPanel.add(portField, cc.xy(3, 5, CellConstraints.FILL, CellConstraints.DEFAULT));
        portLabel = new JLabel();
        portLabel.setHorizontalAlignment(11);
        portLabel.setText("Port");
        generalPanel.add(portLabel, cc.xy(1, 5));
        fetchSizeLabel = new JLabel();
        fetchSizeLabel.setHorizontalAlignment(11);
        fetchSizeLabel.setText("Fetch Size");
        generalPanel.add(fetchSizeLabel, cc.xy(1, 13));
        fetchSizeField = new JSpinner();
        generalPanel.add(fetchSizeField, cc.xy(3, 13, CellConstraints.FILL, CellConstraints.DEFAULT));
        modelLabel = new JLabel();
        modelLabel.setText("Model");
        generalPanel.add(modelLabel, cc.xy(1, 15));
        modelField = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        defaultComboBoxModel1.addElement("Tree");
        defaultComboBoxModel1.addElement("Table");
        modelField.setModel(defaultComboBoxModel1);
        generalPanel.add(modelField, cc.xy(3, 15));
        bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(bottomPanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        bottomPanel.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        bottomButtonPanel = new JPanel();
        bottomButtonPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        bottomPanel.add(bottomButtonPanel, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOk = new JButton();
        buttonOk.setEnabled(true);
        buttonOk.setText("OK");
        bottomButtonPanel.add(buttonOk, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        bottomButtonPanel.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonTest = new JButton();
        buttonTest.setText("Test Connection");
        bottomPanel.add(buttonTest, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

}
