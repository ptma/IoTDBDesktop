package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PathGroup implements Sessionable {

    private final String database;

    private final String name;

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
