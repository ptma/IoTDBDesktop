package org.apache.iotdb.commons.utils;

import org.apache.iotdb.commons.exception.IllegalPathException;
import org.apache.tsfile.exception.PathParseException;
import org.apache.tsfile.read.common.parser.PathNodesGenerator;

public class PathUtils {

    public static String[] splitPathToDetachedNodes(String path) throws IllegalPathException {
        if ("".equals(path)) {
            return new String[] {};
        }
        try {
            return PathNodesGenerator.splitPathToNodes(path);
        } catch (PathParseException e) {
            throw new IllegalPathException(path);
        }
    }

}
