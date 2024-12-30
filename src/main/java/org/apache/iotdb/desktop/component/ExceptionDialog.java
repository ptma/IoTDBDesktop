package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.ThemeUtil;
import org.apache.iotdb.desktop.util.Utils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * @author ptma
 */
public class ExceptionDialog extends JDialog {
    private static final int MIN_HEIGHT = 144;
    private static final int MAX_WIDTH = 600;

    private JPanel contentPane;
    private JButton buttonDetail;
    private JButton buttonClose;
    private JPanel mainPanel;
    private JTextArea messageArea;
    private JPanel stackPanel;
    private JTextArea stackArea;

    public static void open(Window owner, String message, Throwable throwable) {
        ExceptionDialog dialog = new ExceptionDialog(owner, message, throwable);
        dialog.setModal(true);
        Container cp = dialog.getContentPane();
        dialog.mainPanel.setPreferredSize(null);
        Dimension d = new Dimension(cp.getSize().width, dialog.mainPanel.getPreferredSize().height);
        cp.setPreferredSize(d);
        dialog.pack();
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    ExceptionDialog(Window owner, String message, Throwable throwable) {
        super(owner);
        initComponents();
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonClose);
        setMinimumSize(new Dimension(500, MIN_HEIGHT));

        setTitle(LangUtil.getString("Error"));

        Utils.UI.buttonText(buttonClose, LangUtil.getString("&Close"));
        Utils.UI.buttonText(buttonDetail, LangUtil.getString("&Detail") + " >>");

        buttonClose.addActionListener(e -> onClose());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }
        });
        contentPane.registerKeyboardAction(e -> onClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initText(message, throwable);
        buttonDetail.addActionListener(e -> toggleCollapsed());
    }

    private void initText(String message, Throwable throwable) {
        messageArea.setText(message);
        stackArea.setText(getStackTraceText(throwable));
        stackArea.setCaretPosition(0);
    }

    private void onClose() {
        dispose();
    }

    private void initComponents() {
        contentPane = new JPanel();
        contentPane.setLayout(new MigLayout("insets 20 10 10 10,hidemode 1", "[grow,fill][][]", "[][]"));
        mainPanel = new JPanel();
        contentPane.add(mainPanel, "cell 0 0, span 3");
        mainPanel.setLayout(new MigLayout("insets 0", "[16]10[grow]", "[][fill]"));
        mainPanel.add(new JLabel(UIManager.getIcon("OptionPane.errorIcon")), "cell 0 0,spany 2,ay top");
        messageArea = createTextArea(true);
        mainPanel.add(messageArea, "cell 1 0,grow,hmin 40");

        stackArea = createTextArea(false);
        stackArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        stackArea.setRows(10);
        stackPanel = new JPanel(new BorderLayout());
        stackPanel.add(new JScrollPane(stackArea), BorderLayout.CENTER);

        buttonClose = new JButton();
        contentPane.add(buttonClose, "cell 1 1");
        buttonDetail = new JButton();
        contentPane.add(buttonDetail, "cell 2 1");
    }

    private JTextArea createTextArea(boolean lineWrap) {
        JTextArea textArea = new JTextArea();
        textArea.setBorder(null);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setTabSize(4);
        if (lineWrap) {
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
        }
        return textArea;
    }

    private static String getStackTraceText(Throwable t) {
        StringWriter errors = new StringWriter();
        t.printStackTrace(new PrintWriter(errors));
        String errorString = errors.toString();
        return errorString;
    }

    private void toggleCollapsed() {
        Container cp = getContentPane();
        Dimension d;
        String buttonText;
        if (stackPanel.getParent() == null) {
            stackArea.setCaretPosition(0);
            mainPanel.add(stackPanel, "cell 1 1,grow");
            mainPanel.setPreferredSize(null);
            d = new Dimension(cp.getSize().width, mainPanel.getPreferredSize().height);
            buttonText = LangUtil.getString("&Detail") + " <<";
        } else {
            mainPanel.remove(stackPanel);
            mainPanel.setPreferredSize(null);
            d = new Dimension(cp.getSize().width, mainPanel.getPreferredSize().height);
            buttonText = LangUtil.getString("&Detail") + " >>";
        }
        cp.setPreferredSize(d);
        Utils.UI.buttonText(buttonDetail, buttonText);
        pack();
    }

    @Override
    public void pack() {
        super.pack();
        if (getWidth() > MAX_WIDTH) {
            setSize(MAX_WIDTH, getHeight());
        }
        if (getHeight() < MIN_HEIGHT) {
            setSize(getWidth(), MIN_HEIGHT);
        }
    }

}
