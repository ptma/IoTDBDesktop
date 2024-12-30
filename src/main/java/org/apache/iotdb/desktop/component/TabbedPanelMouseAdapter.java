package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.extras.components.FlatTabbedPane;
import org.apache.iotdb.desktop.model.SessionablePanel;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class TabbedPanelMouseAdapter implements MouseListener {

    private final FlatTabbedPane tabbedPanel;
    private final MouseListener originalMouseListener;
    private final JPopupMenu popupMenu;
    private int selectedTabIndex = -1;

    private JMenuItem refreshMenu;
    private JMenuItem closeMenu;
    private JMenuItem closeOtherMenu;
    private JMenuItem closeAllMenu;

    public TabbedPanelMouseAdapter(FlatTabbedPane tabbedPanel, MouseListener originalMouseListener) {
        super();
        this.tabbedPanel = tabbedPanel;
        this.originalMouseListener = originalMouseListener;
        this.popupMenu = new JPopupMenu();
        initPopupMenuItems();
    }

    private void initPopupMenuItems() {
        refreshMenu = new JMenuItem(LangUtil.getString("Refresh"));
        refreshMenu.setIcon(Icons.REFRESH);
        refreshMenu.addActionListener(e -> {
            if (selectedTabIndex != -1) {
                Component component = tabbedPanel.getComponentAt(selectedTabIndex);
                if (component instanceof SessionablePanel sessionablePanel && sessionablePanel.refreshable()) {
                    sessionablePanel.refresh();
                }
            }
        });
        popupMenu.add(refreshMenu);

        closeMenu = new JMenuItem(LangUtil.getString("Close"));
        closeMenu.addActionListener(e -> tabbedPanel.removeTabAt(selectedTabIndex));
        popupMenu.add(closeMenu);

        closeOtherMenu = new JMenuItem(LangUtil.getString("CloseOtherTabs"));
        closeOtherMenu.addActionListener(e -> {
            for (int i = tabbedPanel.getTabCount() - 1; i >= 0; i--) {
                if (i != selectedTabIndex) {
                    Component component = tabbedPanel.getComponentAt(i);
                    if (component instanceof SessionablePanel sessionablePanel) {
                        if (sessionablePanel.disposeable() || sessionablePanel.confirmDispose()) {
                            sessionablePanel.dispose();
                            tabbedPanel.removeTabAt(i);
                        }
                    }
                }
            }
        });
        popupMenu.add(closeOtherMenu);

        closeAllMenu = new JMenuItem(LangUtil.getString("CloseAllTabs"));
        closeAllMenu.addActionListener(e -> {
            for (int i = tabbedPanel.getTabCount() - 1; i >= 0; i--) {
                Component component = tabbedPanel.getComponentAt(i);
                if (component instanceof SessionablePanel sessionablePanel) {
                    if (sessionablePanel.disposeable() || sessionablePanel.confirmDispose()) {
                        sessionablePanel.dispose();
                        tabbedPanel.removeTabAt(i);
                    }
                }
            }
        });
        popupMenu.add(closeAllMenu);

        popupMenu.addSeparator();
    }

    private void loadTabMenus() {
        if (popupMenu.getComponentCount() > 5) {
            for (int i = popupMenu.getComponentCount() - 1; i >= 5; i--) {
                popupMenu.remove(i);
            }
        }
        ButtonGroup tabMenuGroup = new ButtonGroup();
        for (int i = 0; i < tabbedPanel.getTabCount(); i++) {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(tabbedPanel.getTitleAt(i));
            final int tabIndex = i;
            menuItem.addActionListener(e -> {
                tabbedPanel.setSelectedIndex(tabIndex);
            });
            if (tabbedPanel.getSelectedIndex() == i) {
                menuItem.setSelected(true);
            }
            tabMenuGroup.add(menuItem);
            popupMenu.add(menuItem);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        originalMouseListener.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        selectedTabIndex = tabbedPanel.getUI().tabForCoordinate(tabbedPanel, e.getX(), e.getY());
        if (selectedTabIndex != -1) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                originalMouseListener.mousePressed(e);
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                tabbedPanel.removeTabAt(selectedTabIndex);
            } else if (SwingUtilities.isRightMouseButton(e)) {
                Component component = tabbedPanel.getComponentAt(selectedTabIndex);
                if (component instanceof SessionablePanel sessionablePanel) {
                    refreshMenu.setEnabled(sessionablePanel.refreshable());
                }
                loadTabMenus();
                popupMenu.show(tabbedPanel, e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        originalMouseListener.mouseReleased(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        originalMouseListener.mouseEntered(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        originalMouseListener.mouseExited(e);
    }

}
