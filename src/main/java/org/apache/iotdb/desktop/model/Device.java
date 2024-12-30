package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Device implements Sessionable {

    private final String database;

    private final String name;

    private final boolean aligned;

    private final String template;

    private final String ttl;

    private final Session session;

    public String getPath() {
        return String.format("%s.%s", database, name);
    }

    @Override
    public String getKey() {
        return String.format("%s-%s", session.getId(), getPath());
    }

    @Override
    public String toString() {
        return getName();
    }

}
