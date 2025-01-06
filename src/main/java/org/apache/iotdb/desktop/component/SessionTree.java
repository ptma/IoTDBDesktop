package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.extras.components.FlatPopupMenu;
import lombok.Getter;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.config.SessionProps;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.form.*;
import org.apache.iotdb.desktop.model.*;
import org.apache.iotdb.desktop.util.Icons;
import org.apache.iotdb.desktop.util.LangUtil;
import org.apache.iotdb.desktop.util.Utils;
import org.jdesktop.swingx.JXTree;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.Callable;

public class SessionTree extends JXTree {

    @Getter
    private final DefaultMutableTreeNode rootNode;
    private final DefaultTreeModel treeModel;
    private FlatPopupMenu popupMenu;

    private JMenuItem menuOpenSession;
    private JMenuItem menuCloseSession;
    private JMenuItem menuEditProperties;
    private JPopupMenu.Separator topMenuSeparator;

    private JMenuItem menuNewQuery;
    private JMenuItem menuNewDatabase;
    private JMenuItem menuNewDevice;
    private JMenuItem menuDatabaseTtl;
    private JMenuItem menuRefresh;

    private JMenuItem menuDeviceData;

    private JMenuItem menuRemoveSession;
    private JMenuItem menuRemoveDatabase;
    private JMenuItem menuRemovePath;
    private JMenuItem menuRemoveDevice;

    private JMenuItem menuDetail;

    public SessionTree() {
        super();
        this.setShowsRootHandles(true);
        this.setRootVisible(false);
        this.setRowHeight(25);
        this.setDoubleBuffered(true);
        TreeSelectionModel selectionModel = new DefaultTreeSelectionModel();
        selectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.setSelectionModel(selectionModel);
        rootNode = new DefaultMutableTreeNode("root");
        treeModel = new DefaultTreeModel(rootNode);
        this.setModel(treeModel);
        this.setCellRenderer(new SessionTreeCellRenderer());
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.addTreeSelectionListener(e -> AppEvents.instance().applyEvent(l -> l.onTreeSelectionChange(e.getPath())));
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                final int rowIndex = SessionTree.this.getClosestRowForLocation(e.getX(), e.getY());
                if (rowIndex < 0) {
                    return;
                }
                TreePath clickedPath = SessionTree.this.getPathForRow(rowIndex);
                if (clickedPath == null) {
                    return;
                }
                Rectangle bounds = SessionTree.this.getPathBounds(clickedPath);
                if (bounds == null || e.getY() > (bounds.y + bounds.height)) {
                    return;
                }
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
                    nodeDoubleClick(rowIndex);
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    SessionTree.this.setSelectionRow(rowIndex);
                    changeMenuItemsStatus((DefaultMutableTreeNode) clickedPath.getLastPathComponent());
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        AppEvents.instance().addEventListener(new AppEventListenerAdapter() {

            @Override
            public void optionsChanged(Options options) {
                SwingUtilities.updateComponentTreeUI(popupMenu);
            }

        });

        popupMenu = new FlatPopupMenu();
        menuCloseSession = Utils.UI.createMenuItem(LangUtil.getString("CloseSession"), (e) -> closeSession());
        popupMenu.add(menuCloseSession);
        menuOpenSession = Utils.UI.createMenuItem(LangUtil.getString("OpenSession"), (e) -> openSession());
        popupMenu.add(menuOpenSession);
        menuEditProperties = Utils.UI.createMenuItem(LangUtil.getString("Properties..."), (e) -> editSessionProps());
        popupMenu.add(menuEditProperties);

        topMenuSeparator = new JPopupMenu.Separator();
        popupMenu.add(topMenuSeparator);

        menuNewQuery = Utils.UI.createMenuItem(LangUtil.getString("NewQuery..."), (e) -> newQuery(false));
        menuNewQuery.setIcon(Icons.QUERY);
        popupMenu.add(menuNewQuery);
        menuNewDatabase = Utils.UI.createMenuItem(LangUtil.getString("NewDatabase..."), (e) -> createNewDatabase());
        popupMenu.add(menuNewDatabase);
        menuNewDevice = Utils.UI.createMenuItem(LangUtil.getString("NewDevice..."), (e) -> createDevice());
        popupMenu.add(menuNewDevice);
        menuDatabaseTtl = Utils.UI.createMenuItem(LangUtil.getString("DatabaseTTL..."), (e) -> modifyDatabaseTTL());
        popupMenu.add(menuDatabaseTtl);
        menuDeviceData = Utils.UI.createMenuItem(LangUtil.getString("DeviceData..."), (e) -> openDeviceData());
        menuDeviceData.setIcon(Icons.TABLE_DATA);
        popupMenu.add(menuDeviceData);

        menuRefresh = Utils.UI.createMenuItem(LangUtil.getString("Refresh"), (e) -> refreshNode());
        menuRefresh.setIcon(Icons.REFRESH);
        popupMenu.add(menuRefresh);

        popupMenu.addSeparator();

        menuRemoveSession = Utils.UI.createMenuItem(LangUtil.getString("RemoveSession"), (e) -> removeSelectedObject());
        menuRemoveSession.setIcon(Icons.DELETE);
        popupMenu.add(menuRemoveSession);
        menuRemoveDatabase = Utils.UI.createMenuItem(LangUtil.getString("RemoveDatabase"), (e) -> removeSelectedObject());
        menuRemoveDatabase.setIcon(Icons.DELETE);
        popupMenu.add(menuRemoveDatabase);
        menuRemovePath = Utils.UI.createMenuItem(LangUtil.getString("RemovePath"), (e) -> removeSelectedObject());
        menuRemovePath.setIcon(Icons.DELETE);
        popupMenu.add(menuRemovePath);
        menuRemoveDevice = Utils.UI.createMenuItem(LangUtil.getString("RemoveDevice"), (e) -> removeSelectedObject());
        menuRemoveDevice.setIcon(Icons.DELETE);
        popupMenu.add(menuRemoveDevice);

        popupMenu.addSeparator();

        menuDetail = Utils.UI.createMenuItem(LangUtil.getString("Detail..."), (e) -> showObjectDetail());
        menuDetail.setIcon(Icons.INFORMATION);
        popupMenu.add(menuDetail);
    }

    private void changeMenuItemsStatus(DefaultMutableTreeNode node) {
        if (node == null) {
            return;
        }
        boolean onSession = node.getUserObject() instanceof Session;
        boolean sessionOpend = node.getUserObject() instanceof Session session && session.isOpened();
        boolean onDatabase = node.getUserObject() instanceof Database;
        boolean onPathGroup = node.getUserObject() instanceof PathGroup;
        boolean onDevice = node.getUserObject() instanceof Device;

        menuCloseSession.setVisible(onSession && sessionOpend);
        menuOpenSession.setVisible(onSession && !sessionOpend);
        menuEditProperties.setVisible(onSession);
        menuDeviceData.setVisible(onDevice);
        topMenuSeparator.setVisible(onSession);

        menuNewQuery.setEnabled((onSession && sessionOpend) || onDatabase || onPathGroup || onDevice);
        menuNewDatabase.setVisible(onSession);
        menuNewDatabase.setEnabled(sessionOpend);
        menuNewDevice.setVisible(onDatabase);
        menuDatabaseTtl.setVisible(onDatabase);
        menuRefresh.setEnabled((onSession && sessionOpend) || onDatabase);

        menuRemoveSession.setVisible(onSession);
        menuRemoveSession.setEnabled(!sessionOpend);
        menuRemoveDatabase.setVisible(onDatabase);
        menuRemovePath.setVisible(onPathGroup);
        menuRemoveDevice.setVisible(onDevice);

        menuDetail.setEnabled((onSession && sessionOpend) || onDatabase || onDevice);
    }

    private void nodeDoubleClick(int rowIndex) {
        TreePath clickedPath = this.getPathForRow(rowIndex);
        if (clickedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) clickedPath.getLastPathComponent();
            if (selectNode.getUserObject() instanceof Session session) {
                openSession();
            } else if (selectNode.getUserObject() instanceof Database database) {
                if (!database.isDevicesLoaded()) {
                    loadDevices(selectNode, database, true, false);
                }
            } else if (selectNode.getUserObject() instanceof Device device && Configuration.instance().options().isDblclickOpenEditor()) {
                AppEvents.instance().applyEvent(l -> l.newDeviceDataTab(device));
            }
        }
    }

    private void editSessionProps() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Session session) {
                if (session.isOpened()) {
                    if (Utils.Message.confirm(LangUtil.getString("ConfirmCloseSessionBeforeEditingProperties")) == JOptionPane.YES_OPTION) {
                        closeSession();
                        AppEvents.instance().applyEvent(l -> l.closeSession(session));
                    } else {
                        return;
                    }
                }
                SessionProps sessionProps = session.getProps();
                SessionPropsEditorForm.open(sessionProps, newProps -> {
                    selectNode.setUserObject(new Session(newProps));
                    treeModel.nodeChanged(selectNode);
                    Configuration.instance().saveSession(newProps);
                });
            }
        }
    }

    private void openSession() {
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    TreePath selectedPath = getSelectionPath();
                    if (selectedPath != null) {
                        final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                        if (selectNode != null && selectNode.getUserObject() instanceof Session session) {
                            if (!session.isOpened()) {
                                session.open();
                                loadDatabases(selectNode, session);
                            }
                        }
                        AppEvents.instance().applyEvent(l -> l.onTreeSelectionChange(selectedPath));
                    }
                } catch (Exception e) {
                    Utils.Message.error(e.getMessage(), e);
                }
                return null;
            }
        }.execute();
    }

    private void closeSession() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Session session) {
                if (session.isOpened()) {
                    AppEvents.instance().applyEvent(l -> l.closeSession(session));
                    session.close();
                    selectNode.removeAllChildren();
                    this.treeModel.nodeStructureChanged(selectNode);
                }
            }
            AppEvents.instance().applyEvent(l -> l.onTreeSelectionChange(selectedPath));
        }
    }

    public void dataImport() {
        TreePath selectedPath = SessionTree.this.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof Sessionable sessionable) {
                AppEvents.instance().applyEvent(l -> l.openDataImport(sessionable.getSession()));
            }
        }
    }

    public void dataExport() {
        TreePath selectedPath = SessionTree.this.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof Sessionable sessionable) {
                AppEvents.instance().applyEvent(l -> l.openDataExport(sessionable.getSession()));
            }
        }
    }

    public void newQuery(boolean autoExecute) {
        TreePath selectedPath = SessionTree.this.getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof Sessionable sessionable) {
                QueryForm queryForm = new QueryForm(sessionable, autoExecute);
                String title = LangUtil.getString("Query") + " (" + sessionable.getSession().getName() + ")";
                AppEvents.instance().applyEvent(l -> l.newTab(title, Icons.QUERY, queryForm));
            }
        }
    }

    private void createNewDatabase() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Session session) {
                if (session.isOpened()) {
                    CreateDatabaseForm.open(session, (newDatabase) -> {
                        DefaultMutableTreeNode databaseTreeNode = new DefaultMutableTreeNode(newDatabase);
                        databaseTreeNode.setAllowsChildren(true);
                        treeModel.insertNodeInto(databaseTreeNode, selectNode, selectNode.getChildCount());
                    });
                }
            }
        }
    }

    private void createDevice() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Database database) {
                CreateDeviceForm.open(database, () -> {
                    loadDevices(selectNode, database, true, false);
                });
            }
        }
    }

    private void openDeviceData() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Device device) {
                AppEvents.instance().applyEvent(l -> l.newDeviceDataTab(device));
            }
        }
    }

    private void modifyDatabaseTTL() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectNode != null && selectNode.getUserObject() instanceof Database database) {
                SetTtlForm.open(database);
            }
        }
    }

    private void refreshNode() {
        new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                TreePath selectedPath = getSelectionPath();
                if (selectedPath != null) {
                    final DefaultMutableTreeNode selectNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
                    if (selectNode != null) {
                        if (selectNode.getUserObject() instanceof Session session) {
                            loadDatabases(selectNode, session);
                        } else if (selectNode.getUserObject() instanceof Database database) {
                            loadDevices(selectNode, database, true, true);
                        }
                    }
                }
                return null;
            }
        }.execute();
    }

    private void showObjectDetail() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof Sessionable sessionable) {
                AppEvents.instance().applyEvent(l -> l.newInfoTab(sessionable));
            }
        }
    }

    public int indexOf(SessionProps props) {
        for (int i = 0; i < rootNode.getChildCount(); i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            Session session = (Session) node.getUserObject();
            if (session.getProps().getName().equals(props.getName())) {
                return i;
            }
        }
        return -1;
    }

    public void addSession(SessionProps sessionProps) {
        Session session = new Session(sessionProps);
        DefaultMutableTreeNode sessionNode = new DefaultMutableTreeNode(session);
        treeModel.insertNodeInto(sessionNode, rootNode, rootNode.getChildCount());
        expandNode(rootNode);
    }

    private void loadDatabases(DefaultMutableTreeNode sessionNode, Session session) {
        sessionNode.removeAllChildren();
        treeModel.reload(sessionNode);
        try {
            Set<Database> databases = session.loadDatabases();
            for (Database database : databases) {
                DefaultMutableTreeNode databaseTreeNode = new DefaultMutableTreeNode(database);
                databaseTreeNode.setAllowsChildren(true);
                treeModel.insertNodeInto(databaseTreeNode, sessionNode, sessionNode.getChildCount());
                if (Configuration.instance().options().isAutoLoadDeviceNodes()) {
                    loadDevices(databaseTreeNode, database, false, true);
                }
            }
            session.setDatabasesLoaded(true);
            expandNode(sessionNode);
        } catch (Exception e) {
            Utils.Message.error(e.getMessage(), e);
        }
    }

    private void loadDevices(DefaultMutableTreeNode databaseNode, Database database, boolean expand, boolean inWorker) {
        Callable<Void> loadTask = () -> {
            databaseNode.removeAllChildren();
            treeModel.reload(databaseNode);
            Set<Device> devices = database.getSession().loadDevices(database.getName());
            for (Device device : devices) {
                if (Configuration.instance().options().isFlattenDeviceNodes() || !device.getName().contains(".")) {
                    DefaultMutableTreeNode deviceTreeNode = new DefaultMutableTreeNode(device);
                    deviceTreeNode.setAllowsChildren(false);
                    treeModel.insertNodeInto(deviceTreeNode, databaseNode, databaseNode.getChildCount());
                } else {
                    String deviceName = device.getName();
                    StringJoiner paths = new StringJoiner(".");
                    DefaultMutableTreeNode parentNode = databaseNode;
                    DefaultMutableTreeNode groupNode;
                    while (deviceName.contains(".")) {
                        paths.add(deviceName.substring(0, deviceName.indexOf(".")));
                        deviceName = deviceName.substring(deviceName.indexOf(".") + 1);
                        // TODO: 是否已存在节点
                        boolean pathExist = false;
                        for (int i = 0; i < parentNode.getChildCount(); i++) {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
                            if (node.getUserObject() instanceof PathGroup path && path.getName().equals(paths.toString())) {
                                parentNode = node;
                                pathExist = true;
                                break;
                            }
                        }
                        if (!pathExist) {
                            PathGroup pathGroup = new PathGroup(database.getName(), paths.toString(), database.getSession());
                            groupNode = new DefaultMutableTreeNode(pathGroup);
                            groupNode.setAllowsChildren(true);
                            treeModel.insertNodeInto(groupNode, parentNode, parentNode.getChildCount());
                            parentNode = groupNode;
                        }
                    }
                    DefaultMutableTreeNode deviceTreeNode = new DefaultMutableTreeNode(device);
                    deviceTreeNode.setAllowsChildren(false);
                    treeModel.insertNodeInto(deviceTreeNode, parentNode, parentNode.getChildCount());
                }
            }
            database.setDevicesLoaded(true);
            if (expand) {
                expandNode(databaseNode);
            }
            return null;
        };

        if (!inWorker) {
            new SwingWorker<>() {
                @Override
                protected Void doInBackground() throws Exception {
                    return loadTask.call();
                }
            }.execute();
        } else {
            try {
                loadTask.call();
            } catch (Exception e) {
                Utils.Message.error(e.getMessage(), e);
            }
        }
    }

    private void removeSelectedObject() {
        TreePath selectedPath = getSelectionPath();
        if (selectedPath != null) {
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectedPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof Session session) {
                if (Utils.Message.confirm(String.format(LangUtil.getString("RemoveSessionConfirm"), session.getName())) == JOptionPane.YES_OPTION) {
                    treeModel.removeNodeFromParent(selectedNode);
                }
            } else if (selectedNode.getUserObject() instanceof Database database) {
                try {
                    int deviceCount = database.getSession().countDevices(database.getName());
                    String confirmMessage;
                    if (deviceCount > 1) {
                        confirmMessage = String.format(LangUtil.getString("RemoveDatabaseConfirmWithMultiDevice"), deviceCount, database.getName());
                    } else if (deviceCount == 1) {
                        confirmMessage = String.format(LangUtil.getString("RemoveDatabaseConfirmWithSingleDevice"), database.getName());
                    } else {
                        confirmMessage = String.format(LangUtil.getString("RemoveDatabaseConfirm"), database.getName());
                    }
                    if (Utils.Message.confirm(confirmMessage) == JOptionPane.YES_OPTION) {
                        database.getSession().removeDatabase(database.getName());
                        treeModel.removeNodeFromParent(selectedNode);
                    }
                } catch (Exception e) {
                    Utils.Message.error(e.getMessage(), e);
                }
            } else if (selectedNode.getUserObject() instanceof PathGroup pathGroup) {
                try {
                    int deviceCount = pathGroup.getSession().countDevices(pathGroup.getPath());
                    String confirmMessage;
                    if (deviceCount > 1) {
                        confirmMessage = String.format(LangUtil.getString("RemovePathConfirmWithMultiDevice"), deviceCount, pathGroup.getPath());
                    } else if (deviceCount == 1) {
                        confirmMessage = String.format(LangUtil.getString("RemovePathConfirmWithSingleDevice"), pathGroup.getPath());
                    } else {
                        confirmMessage = String.format(LangUtil.getString("RemovePathConfirm"), pathGroup.getPath());
                    }
                    if (Utils.Message.confirm(confirmMessage) == JOptionPane.YES_OPTION) {
                        pathGroup.getSession().removeDevice(pathGroup.getPath());
                        treeModel.removeNodeFromParent(selectedNode);
                    }
                } catch (Exception e) {
                    Utils.Message.error(e.getMessage(), e);
                }
            } else if (selectedNode.getUserObject() instanceof Device device) {
                try {
                    int timeseriesCount = device.getSession().countTimeseries(device.getPath());
                    String confirmMessage;
                    if (timeseriesCount > 1) {
                        confirmMessage = String.format(LangUtil.getString("RemoveDeviceConfirmWithMultiTimerseries"), timeseriesCount, device.getPath());
                    } else if (timeseriesCount == 1) {
                        confirmMessage = String.format(LangUtil.getString("RemoveDeviceConfirmWithSingleTimerseries"), device.getPath());
                    } else {
                        confirmMessage = String.format(LangUtil.getString("RemoveDeviceConfirm"), device.getPath());
                    }
                    if (Utils.Message.confirm(confirmMessage) == JOptionPane.YES_OPTION) {
                        device.getSession().removeDevice(device.getPath());
                        treeModel.removeNodeFromParent(selectedNode);
                    }
                } catch (Exception e) {
                    Utils.Message.error(e.getMessage(), e);
                }
            }
        }
    }

    public void closeOpendSessions() {
        for (int i=0; i<rootNode.getChildCount();i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootNode.getChildAt(i);
            if (node.getUserObject() instanceof Session session && session.isOpened()) {
                session.close();
            }
        }
    }

    public void expandNode(DefaultMutableTreeNode node) {
        SwingUtilities.invokeLater(() -> {
            expandPath(new TreePath(treeModel.getPathToRoot(node)));
        });
    }
}
