package org.apache.iotdb.desktop.frame;

import com.formdev.flatlaf.extras.FlatDesktop;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.form.AboutForm;
import org.apache.iotdb.desktop.form.OptionsForm;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;

import javax.swing.*;

/**
 * @author ptma
 */
public class MainMenu extends JMenuBar {

    public MainMenu() {
        initMenu();
    }

    public void initMenu() {

        JMenuItem fileMenu = Utils.UI.createMenu(LangUtil.getString("&File"));

        fileMenu.add(Utils.UI.createMenuItem(LangUtil.getString("&Options"),
            e -> OptionsForm.open()
        ));

        fileMenu.add(new JSeparator());
        // 退出程序
        fileMenu.add(Utils.UI.createMenuItem(LangUtil.getString("E&xit"),
            e -> {
                if (IotdbDesktopApp.frame.closing()) {
                    System.exit(0);
                }
            }
        ));
        this.add(fileMenu);

        JMenuItem helpMenu = Utils.UI.createMenu(LangUtil.getString("&Help"));
        helpMenu.add(Utils.UI.createMenuItem(LangUtil.getString("&About"),
            e -> {
                AboutForm.open();
            }
        ));
        this.add(helpMenu);
    }

}
