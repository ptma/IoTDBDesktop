package org.apache.iotdb.desktop.model;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.desktop.util.Utils;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
public class Column {

    private String name;

    private String dataType;

    private String category;

    private boolean existing;

}
