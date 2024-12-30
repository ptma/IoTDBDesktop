package org.apache.iotdb.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author ptma
 */
public interface Textable {

    @JsonIgnore
    String getText();

}
