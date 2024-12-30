package org.apache.iotdb.desktop.model;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Getter;
import lombok.Setter;
import org.apache.iotdb.desktop.util.Utils;

import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Getter
@Setter
public class Metric {

    private String path;

    private String name;

    private String alias;

    private String aliasModified;

    private String database;

    private String dataType;

    private String encoding;

    private String compression;

    private Map<String, String> tags;

    private Map<String, String> tagsModified;

    private Map<String, String> attributes;

    private Map<String, String> attributesModified;

    private String deadband;

    private String deadbandParameters;

    private String viewType;

    private boolean existing;

    public void setAlias(String alias) {
        this.alias = alias;
        this.aliasModified = alias;
    }

    public void setTags(String json) {
        if (StrUtil.isNotBlank(json)) {
            try {
                Map<String, String> map = Utils.JSON_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
                });
                this.tags = map;
                this.tagsModified = map;
            } catch (JsonProcessingException ignore) {

            }
        }
    }

    public void setTagsModified(String tagsModified) {
        this.tagsModified = Utils.stringToMap(tagsModified);
    }

    public void setAttributes(String json) {
        if (StrUtil.isNotBlank(json)) {
            try {
                Map<String, String> map = Utils.JSON_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
                });
                this.attributes = map;
                this.attributesModified = map;
            } catch (JsonProcessingException ignore) {

            }
        }
    }

    public void setAttributesModified(String attributesModified) {
        this.attributesModified = Utils.stringToMap(attributesModified);
    }

    public String displayableAlias() {
        return aliasModified == null ? alias : aliasModified;
    }

    public String displayableTags() {
        if (tagsModified != null) {
            return Utils.mapToString(tagsModified);
        } else if (tags != null && !tags.isEmpty()) {
            return Utils.mapToString(tags);
        } else {
            return null;
        }
    }

    public String displayableAttributes() {
        if (attributesModified != null) {
            return Utils.mapToString(attributesModified);
        } else if (attributes != null && !attributes.isEmpty()) {
            return Utils.mapToString(attributes);
        } else {
            return null;
        }
    }

    /**
     * 别名是否已被修改
     */
    public boolean isAliasModified() {
        return stringNotEquals(alias, aliasModified);
    }

    /**
     * 标签是否已被修改
     */
    public boolean isTagsModified() {
        return mapNotEquals(tags, tagsModified);
    }

    public Map<String, String> getTagsRemoved() {
        return getRemovedEntries(tags, tagsModified);
    }

    public Map<String, String> getTagsModified() {
        return getModifiedEntries(tags, tagsModified);
    }

    /**
     * 属性是否已被修改
     */
    public boolean isAttributesModified() {
        return mapNotEquals(attributes, attributesModified);
    }

    public Map<String, String> getAttributesRemoved() {
        return getRemovedEntries(attributes, attributesModified);
    }

    public Map<String, String> getAttributesModified() {
        return getModifiedEntries(attributes, attributesModified);
    }

    public boolean isModified() {
        return isAliasModified() || isTagsModified() || isAttributesModified();
    }

    private boolean isMapEmpty(Map<String, String> map) {
        return map == null || map.isEmpty();
    }

    private boolean stringNotEquals(String str1, String str2) {
        if (StrUtil.isBlank(str1)) {
            return !StrUtil.isBlank(str2);
        } else if (StrUtil.isBlank(str2)) {
            return true;
        } else {
            return !str1.equals(str2);
        }
    }

    private boolean mapNotEquals(Map<String, String> map1, Map<String, String> map2) {
        if (map1 == null || map1.isEmpty()) {
            return !(map2 == null || map2.isEmpty());
        } else if (map2 == null || map2.isEmpty()) {
            return true;
        } else {
            return !map1.equals(map2);
        }
    }

    private Map<String, String> getRemovedEntries(Map<String, String> oldMap, Map<String, String> modifiedMap) {
        if (isMapEmpty(oldMap)) {
            return Collections.emptyMap();
        } else if (isMapEmpty(modifiedMap)) {
            return oldMap;
        } else {
            return oldMap.entrySet()
                .stream()
                .filter(entry -> !modifiedMap.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }

    private Map<String, String> getModifiedEntries(Map<String, String> oldMap, Map<String, String> modifiedMap) {
        if (isMapEmpty(modifiedMap)) {
            return Collections.emptyMap();
        } else if (isMapEmpty(oldMap)) {
            return modifiedMap;
        } else {
            return modifiedMap.entrySet()
                .stream()
                .filter(entry -> !oldMap.containsKey(entry.getKey()) || !oldMap.get(entry.getKey()).equals(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
    }
}
