package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Database implements Databaseable, Groupable {

    private final String name;

    @Setter
    private boolean childrenLoaded = false;

    private final Session session;

    @Override
    public String getPath() {
        return name;
    }

    @Override
    public String getKey() {
        return String.format("%s-%s", session.getId(), name);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public String getDatabase() {
        return getName();
    }
}
