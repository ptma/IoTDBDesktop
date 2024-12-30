package org.apache.iotdb.tool.data;

import lombok.Getter;
import org.apache.iotdb.desktop.component.Textable;
import org.apache.iotdb.desktop.util.LangUtil;

@Getter
public enum Operation implements Textable {
    NONE("none"),
    MV("mv"),
    CP("cp"),
    DELETE("delete");

    private final String value;

    Operation(String value) {
        this.value = value;
    }

    @Override
    public String getText() {
        return LangUtil.getString("Operation." + getValue());
    }
}
