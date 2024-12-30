package org.apache.iotdb.desktop;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatInspector;
import com.formdev.flatlaf.extras.FlatUIDefaultsInspector;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.util.SystemInfo;
import org.apache.iotdb.desktop.frame.MainFrame;
import org.apache.iotdb.desktop.util.Const;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.ThemeUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.swing.*;

/**
 * @author ptma
 */
public class IotdbDesktopApp {

    public static MainFrame frame;

    public static void main(String[] args) {
        try {
            SLF4JBridgeHandler.removeHandlersForRootLogger();
            SLF4JBridgeHandler.install();

            if (SystemInfo.isMacOS) {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.application.name", Const.APP_NAME);
                System.setProperty("apple.awt.application.appearance", "system");
            }

            if (SystemInfo.isLinux) {
                JFrame.setDefaultLookAndFeelDecorated(true);
                JDialog.setDefaultLookAndFeelDecorated(true);
            }

            if (!SystemInfo.isJava_9_orLater && System.getProperty("flatlaf.uiScale") == null) {
                System.setProperty("flatlaf.uiScale", "2x");
            }

            SwingUtilities.invokeLater(() -> {

                FlatLaf.registerCustomDefaultsSource("org.apache.iotdb.desktop");
                ToolTipManager.sharedInstance().setInitialDelay(300);
                ToolTipManager.sharedInstance().setDismissDelay(20000);
                ToolTipManager.sharedInstance().setLightWeightPopupEnabled(true);
                FlatJetBrainsMonoFont.install();
                FlatInspector.install("ctrl shift alt X");
                FlatUIDefaultsInspector.install("ctrl shift alt Y");
                ThemeUtil.setupTheme();
                LangUtil.setupLanguage();

                frame = new MainFrame();
                if (SystemInfo.isMacFullWindowContentSupported) {
                    frame.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
                }

                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            });
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
        }
    }

}
