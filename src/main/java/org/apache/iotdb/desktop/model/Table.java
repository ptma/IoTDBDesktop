package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Table implements Databaseable {

    private final String database;

    private final String name;

    private final String ttl;

    private final Session session;

    public boolean modifiable() {
        return !"information_schema".equals(database);
    }

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
