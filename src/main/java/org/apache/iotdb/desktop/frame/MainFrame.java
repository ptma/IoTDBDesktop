package org.apache.iotdb.desktop.frame;

import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.icons.FlatTabbedPaneCloseIcon;
import org.apache.iotdb.desktop.component.StatePersistenceFrame;
import org.apache.iotdb.desktop.form.MainWindowForm;
import org.apache.iotdb.desktop.util.Const;
import org.apache.iotdb.desktop.util.Icons;
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import raven.toast.Notifications;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;

/**
 * @author ptma
 */
public class MainFrame extends StatePersistenceFrame {

    private static final Dimension MIN_DIMENSION = new Dimension(950, 600);

    public MainFrame() {
        super(Const.APP_NAME, true);
        setMinimumSize(MIN_DIMENSION);
        setIconImages(Icons.WINDOW_ICON);
        setJMenuBar(new MainMenu());
        initGlobalComponentStyles();

        initMainWindowForm();

        FlatDesktop.setQuitHandler(response -> {
            if (MainFrame.this.closing()) {
                response.performQuit();
            } else {
                response.cancelQuit();
            }
        });
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        boolean abortFlag = false;
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            abortFlag = !MainFrame.this.closing();
        }
        if (!abortFlag) {
            super.processWindowEvent(e);
        }
    }

    @Override
    protected String getConfigKeyPrefix() {
        return "main";
    }

    @Override
    protected void onWindowOpened(WindowEvent e) {
        super.onWindowOpened(e);
        MainWindowForm.instance().onWindowOpened(e);
    }

    @Override
    protected void onWindowClosing(WindowEvent e) {

    }

    private void initMainWindowForm() {
        Container container = getContentPane();
        container.setLayout(new BorderLayout());
        container.add(MainWindowForm.instance().getContentPanel(), BorderLayout.CENTER);
    }

    private void initGlobalComponentStyles() {
        UIManager.put("TitlePane.unifiedBackground", false);
        UIManager.put("MenuItem.selectionType", true);
        UIManager.put("MenuItem.selectionArc", 5);
        UIManager.put("MenuItem.selectionInsets", new Insets(0, 2, 0, 2));
        UIManager.put("Component.borderWidth", 1);
        UIManager.put("Component.focusWidth", 0);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.arrowType", "chevron");
        UIManager.put("ScrollBar.showButtons", true);
        UIManager.put("ScrollBar.thumbInsets", new Insets(1, 1, 1, 1));
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("TextComponent.arc", 5);
        UIManager.put("SplitPaneDivider.style", "grip");
        UIManager.put("SplitPane.centerOneTouchButtons", true);

        UIManager.put("PasswordField.showRevealButton", true);

        UIManager.put("Table.intercellSpacing", new Dimension(1, 1));
        UIManager.put("Table.cellMargins", new Insets(2, 5, 2, 5));

        UIManager.put("TabbedPane.closeArc", 999);
        UIManager.put("TabbedPane.closeCrossFilledSize", 5.5f);
        UIManager.put("TabbedPane.closeIcon", new FlatTabbedPaneCloseIcon());
        UIManager.put("TabbedPane.tabsOpaque", false);

        // Swing-Toast-Notifications style
        Notifications.getInstance().setJFrame(this);
        UIManager.put("Toast.maximumWidth", 400);
        UIManager.put("Toast.effectWidth", 1000);

        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/iotdbsql", "org.apache.iotdb.desktop.syntax.IotdbSqlTokenMaker");
    }

    public boolean closing() {
        return MainWindowForm.instance().closing();
    }

}
