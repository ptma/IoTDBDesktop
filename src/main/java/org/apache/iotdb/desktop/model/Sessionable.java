package org.apache.iotdb.desktop.model;

import org.apache.iotdb.desktop.config.SessionProps;

public interface Sessionable {

    Session getSession();

    String getKey();

    default void changeDatabase(String database) {
        getSession().changeDatabase(database);
    }

    default boolean isTableDialect() {
        return getSession().isTableDialect();
    }
    default SessionProps getProps() {
        return getSession().getProps();
    }
}
