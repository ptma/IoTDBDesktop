package org.apache.iotdb.desktop.model;

import org.apache.iotdb.desktop.config.SessionProps;

public interface Sessionable {

    Session getSession();

    String getKey();

    default SessionProps getProps() {
        return getSession().getProps();
    }
}
