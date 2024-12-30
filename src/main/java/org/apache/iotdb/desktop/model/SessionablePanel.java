package org.apache.iotdb.desktop.model;

public interface SessionablePanel {

    Session getSession();

    String getTabbedKey();

    /**
     * 是否可刷新
     */
    default boolean refreshable() {
        return false;
    }

    /**
     * 执行刷新
     */
    default void refresh() {

    }

    /**
     * 是否可关闭
     */
    default boolean disposeable() {
        return true;
    }

    /**
     * 执行关闭
     */
    default void dispose() {

    }

    /**
     * 是否确认关闭, 由具体 Tab 页负责
     */
    default boolean confirmDispose() {
        return true;
    }
}
