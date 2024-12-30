package org.apache.iotdb.desktop.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@AllArgsConstructor
public class Database implements Sessionable {

    private final String name;

    @Setter
    private boolean devicesLoaded = false;

    private final Session session;

    @Override
    public String getKey() {
        return String.format("%s-%s", session.getId(), name);
    }

    @Override
    public String toString() {
        return getName();
    }
}
