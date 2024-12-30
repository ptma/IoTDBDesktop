package org.apache.iotdb.desktop.config;

import org.apache.iotdb.desktop.component.Textable;
import org.apache.iotdb.isession.util.Version;

public enum IotdbVersion implements Textable {

    V_0_12("v0.12", Version.V_0_12),
    V_0_13("v0.13", Version.V_0_13),
    V_1_0("v1.x", Version.V_1_0);

    private final String text;

    private final Version version;

    IotdbVersion(String text, Version version) {
        this.text = text;
        this.version = version;
    }

    @Override
    public String getText() {
        return text;
    }

    public Version getVersion() {
        return version;
    }

    public static IotdbVersion of(Version version) {
        for (IotdbVersion c : IotdbVersion.values()) {
            if (c.version.equals(version)) {
                return c;
            }
        }
        return IotdbVersion.V_1_0;
    }
}
