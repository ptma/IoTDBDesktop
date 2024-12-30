package org.apache.iotdb.tool.data;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.List;

public class TextPrinter {

    private final JTextArea out;

    public TextPrinter(JTextArea out) {
        this.out = out;
    }

    public void printf(String format, Object... args) {
        out.append(String.format(format, args));
    }

    public void print(String msg) {
        out.append(msg);
    }

    public void printException(Exception msg) {
        out.append(msg.getMessage());
        newLine();
    }

    public void println() {
        newLine();
    }

    public void println(String msg) {
        out.append(msg);
        newLine();
    }

    public void printBlockLine(List<Integer> maxSizeList) {
        StringBuilder blockLine = new StringBuilder();
        for (Integer integer : maxSizeList) {
            blockLine.append("+").append(StringUtils.repeat("-", integer));
        }
        blockLine.append("+");
        println(blockLine.toString());
    }

    public void printRow(List<List<String>> lists, int i, List<Integer> maxSizeList) {
        printf("|");
        int count;
        int maxSize;
        String element;
        StringBuilder paddingStr;
        for (int j = 0; j < maxSizeList.size(); j++) {
            maxSize = maxSizeList.get(j);
            element = lists.get(j).get(i);
            count = computeHANCount(element);

            if (count > 0) {
                int remain = maxSize - (element.length() + count);
                if (remain > 0) {
                    paddingStr = padding(remain);
                    maxSize = maxSize - count;
                    element = paddingStr.append(element).toString();
                } else if (remain == 0) {
                    maxSize = maxSize - count;
                }
            }

            printf("%" + maxSize + "s|", element);
        }
        println();
    }

    public void printCount(int cnt) {
        if (cnt == 0) {
            println("Empty set.");
        } else {
            println("Total line number = " + cnt);
        }
    }

    public StringBuilder padding(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append(" ".repeat(Math.max(0, count)));
        return sb;
    }

    /** compute the number of Chinese characters included in the String */
    public int computeHANCount(String s) {
        return (int)
            s.codePoints()
                .filter(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN)
                .count();
    }

    public void newLine() {
        out.append("\n");
    }
}
