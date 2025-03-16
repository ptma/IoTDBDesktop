package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PathGroup implements Groupable {

    private final String parent;

    private final String name;

    private final Session session;

    @Override
    public String getPath() {
        return String.format("%s.%s", parent, name);
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
