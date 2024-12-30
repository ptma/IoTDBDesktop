package org.apache.iotdb.desktop.component;

import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.jdesktop.swingx.JXFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 状态持久化窗口， 包括窗口最大化、尺寸、位置
 *
 * @author ptma
 */
public abstract class StatePersistenceFrame extends JXFrame {

    private boolean stateAndSizeLoaded = false;
    private int windowState = 0;

    public StatePersistenceFrame() {
        this(null, false);
    }

    public StatePersistenceFrame(String title) {
        this(title, false);
    }

    public StatePersistenceFrame(String title, boolean exitOnClose) {
        super(title, null, exitOnClose);
        initWindowsListener();
    }

    protected abstract String getConfigKeyPrefix();

    protected void onWindowOpened(WindowEvent e) {

    }

    protected void onWindowClosing(WindowEvent e) {

    }

    /**
     * 需在窗体其它初始化完成后手动调用
     */
    private void initWindowsListener() {
        this.addWindowStateListener(e -> {
            Configuration.instance().setInt(getConfigKeyPrefix(), ConfKeys.WINDOW_STATE, e.getNewState());
            windowState = e.getNewState();
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (stateAndSizeLoaded && windowState != JFrame.MAXIMIZED_BOTH) {
                    Dimension size = e.getComponent().getSize();
                    Configuration.instance().setInt(getConfigKeyPrefix(), ConfKeys.WINDOW_WIDTH, size.width);
                    Configuration.instance().setInt(getConfigKeyPrefix(), ConfKeys.WINDOW_HEIGHT, size.height);
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                if (stateAndSizeLoaded && windowState != JFrame.MAXIMIZED_BOTH) {
                    Point location = e.getComponent().getLocation();
                    // 窗口最大化时, 坐标均会变成负数
                    if (location.x >= 0 && location.y >= 0) {
                        Configuration.instance().setInt(getConfigKeyPrefix(), ConfKeys.WINDOW_LEFT, location.x);
                        Configuration.instance().setInt(getConfigKeyPrefix(), ConfKeys.WINDOW_TOP, location.y);
                    }
                }
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                loadFrameSize();
                onWindowOpened(e);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing(e);
            }
        });
    }

    private Dimension getScreenSize() {
        GraphicsEnvironment graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = graphicsEnv.getScreenDevices();
        if (devices.length > 0) {
            Rectangle bounds = devices[0].getDefaultConfiguration().getBounds();
            return new Dimension(bounds.width, bounds.height);
        } else {
            return new Dimension(960, 640);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            loadFrameSize();
        }
        super.setVisible(visible);
    }

    private void loadFrameSize() {
        if (stateAndSizeLoaded) {
            return;
        }
        Dimension screenSize = getScreenSize(); // Toolkit.getDefaultToolkit().getScreenSize();
        int defaultWidth, defaultHeight, defaultLeft, defaultTop;
        if (screenSize.getWidth() > 1280) {
            defaultLeft = (screenSize.width - 1280) / 2;
            defaultTop = (screenSize.height - 800) / 2;
            defaultWidth = 1280;
            defaultHeight = 800;
        } else if (screenSize.getWidth() > 1024) {
            defaultLeft = (screenSize.width - 1200) / 2;
            defaultTop = (screenSize.height - 768) / 2;
            defaultWidth = 1200;
            defaultHeight = 768;
        } else {
            defaultLeft = (screenSize.width - 960) / 2;
            defaultTop = (screenSize.height - 640) / 2;
            defaultWidth = 960;
            defaultHeight = 640;
        }
        Integer windowWidth = Configuration.instance().getInt(getConfigKeyPrefix(), ConfKeys.WINDOW_WIDTH, defaultWidth);
        Integer windowHeight = Configuration.instance().getInt(getConfigKeyPrefix(), ConfKeys.WINDOW_HEIGHT, defaultHeight);
        if (windowWidth != null && windowHeight != null) {
            // Cannot exceeds screen size
            int width = Math.min(windowWidth, screenSize.width);
            int height = Math.min(windowHeight, screenSize.height);
            // Cannot be smaller than the minimum size
            width = Math.max(width, getMinimumSize().width);
            height = Math.max(height, getMinimumSize().height);
            Dimension size = new Dimension(width, height);
            this.setPreferredSize(size);
            this.setSize(size);

            Integer windowTop = Configuration.instance().getInt(getConfigKeyPrefix(), ConfKeys.WINDOW_TOP, defaultTop);
            Integer windowLeft = Configuration.instance().getInt(getConfigKeyPrefix(), ConfKeys.WINDOW_LEFT, defaultLeft);
            if (windowTop != null && windowLeft != null) {
                int x = Math.max(windowLeft, 0);
                int y = Math.max(windowTop, 0);
                x = Math.min(x, screenSize.width - width);
                y = Math.min(y, screenSize.height - height);
                this.setLocation(new Point(x, y));
            } else {
                this.setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);
            }
        } else {
            // Default size
            this.setLocation(defaultLeft, defaultTop);
            Dimension size = new Dimension(defaultWidth, defaultHeight);
            this.setPreferredSize(size);
            this.setSize(size);
        }

        int windowState = Configuration.instance().getInt(getConfigKeyPrefix(), ConfKeys.WINDOW_STATE, 0);
        if (windowState == JFrame.MAXIMIZED_BOTH) {
            this.windowState = windowState;
            this.setExtendedState(windowState);
        }
        stateAndSizeLoaded = true;
    }
}
