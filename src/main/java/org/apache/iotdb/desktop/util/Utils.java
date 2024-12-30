package org.apache.iotdb.desktop.util;

import cn.hutool.core.img.ColorUtil;
import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.iotdb.desktop.IotdbDesktopApp;
import org.apache.iotdb.desktop.component.ExceptionDialog;
import org.jdesktop.swingx.graphics.ColorUtilities;
import raven.toast.Notifications;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * @author ptma
 */
public class Utils {

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final ObjectMapper JSON_MAPPER = JsonMapper.builder()
        .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
        .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
        .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
        .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
        .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
        .enable(JsonReadFeature.ALLOW_MISSING_VALUES)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build();

    private static final Color DARKER_TEXT_COLOR = Color.BLACK;
    private static final Color LIGHTER_TEXT_COLOR = ColorUtil.hexToColor("#ABB2BF");

    public static class Toast {

        public static void info(String message) {
            Notifications.getInstance().show(Notifications.Type.INFO,
                Notifications.Location.TOP_RIGHT,
                5000,
                message
            );
        }

        public static void success(String message) {
            Notifications.getInstance().show(Notifications.Type.SUCCESS,
                Notifications.Location.TOP_RIGHT,
                5000,
                message
            );
        }

        public static void warn(String message) {
            Notifications.getInstance().show(Notifications.Type.WARNING,
                Notifications.Location.TOP_RIGHT,
                5000,
                message
            );
        }

        public static void error(String message) {
            Notifications.getInstance().show(Notifications.Type.ERROR,
                Notifications.Location.TOP_RIGHT,
                5000,
                message
            );
        }
    }

    public static class Message {

        public static void info(String message) {
            info(IotdbDesktopApp.frame, message);
        }

        public static void info(Component parent, String message) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(IotdbDesktopApp.frame),
                message,
                LangUtil.getString("Information"), JOptionPane.INFORMATION_MESSAGE);
        }

        public static void info(String message, Throwable ex) {
            info(IotdbDesktopApp.frame, message, ex);
        }

        public static void info(Component parent, String message, Throwable ex) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(parent),
                message + "\n\n" + ex.getMessage(),
                LangUtil.getString("Information"), JOptionPane.INFORMATION_MESSAGE);
        }

        public static void warning(String message, Throwable ex) {
            warning(IotdbDesktopApp.frame, message, ex);
        }

        public static void warning(Component parent, String message, Throwable ex) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(parent),
                message + "\n\n" + ex.getMessage(),
                LangUtil.getString("Warning"), JOptionPane.WARNING_MESSAGE);
        }

        public static void warning(String message) {
            warning(IotdbDesktopApp.frame, message);
        }

        public static void warning(Component parent, String message) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(parent),
                message,
                LangUtil.getString("Warning"), JOptionPane.WARNING_MESSAGE);
        }

        public static void error(String message, Throwable ex) {
            error(IotdbDesktopApp.frame, message, ex);
        }

        public static void error(Component parent, String message, Throwable ex) {
            if (ex == null) {
                error(parent, message);
            } else {
                ExceptionDialog.open(SwingUtilities.windowForComponent(parent), message, ex);
            }
        }

        public static void error(String message) {
            error(IotdbDesktopApp.frame, message);
        }

        public static void error(Component parent, String message) {
            JOptionPane.showMessageDialog(SwingUtilities.windowForComponent(parent),
                message,
                LangUtil.getString("Error"), JOptionPane.ERROR_MESSAGE);
        }

        /**
         * @param message
         * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION
         */
        public static int confirm(String message) {
            return confirm(IotdbDesktopApp.frame, message);
        }

        /**
         * @param message
         * @return JOptionPane.YES_OPTION, JOptionPane.NO_OPTION
         */
        public static int confirm(Component parent, String message) {
            return JOptionPane.showConfirmDialog(parent, message,
                LangUtil.getString("Confirm"), JOptionPane.YES_NO_OPTION);
        }

    }

    public static class UI {

        public static void buttonText(AbstractButton component, String text) {
            StringBuilder result = new StringBuilder();
            boolean haveMnemonic = false;
            char mnemonic = '\0';
            int mnemonicIndex = -1;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == '&') {
                    i++;
                    if (i == text.length()) {
                        break;
                    }
                    if (!haveMnemonic && text.charAt(i) != '&') {
                        haveMnemonic = true;
                        mnemonic = text.charAt(i);
                        mnemonicIndex = result.length();
                    }
                }
                result.append(text.charAt(i));
            }
            component.setText(result.toString());
            if (haveMnemonic) {
                component.setMnemonic(mnemonic);
                component.setDisplayedMnemonicIndex(mnemonicIndex);
            }
        }

        public static JMenu createMenu(String menuText) {
            return createMenu(menuText, null);
        }

        public static JMenu createMenu(String menuText, ActionListener action) {
            JMenu menu = new JMenu();
            Utils.UI.buttonText(menu, menuText);

            if (action != null) {
                menu.addActionListener(action);
            }
            return menu;
        }

        public static JMenuItem createMenuItem(String menuText) {
            return createMenuItem(menuText, null);
        }

        public static JMenuItem createMenuItem(String menuText, ActionListener action) {
            JMenuItem menuItem = new JMenuItem();
            Utils.UI.buttonText(menuItem, menuText);

            if (action != null) {
                menuItem.addActionListener(action);
            }
            return menuItem;
        }

        public static JMenuItem createMenuItem(String menuText, Action action) {
            JMenuItem menuItem = new JMenuItem();
            if (action != null) {
                menuItem.setAction(action);
            }
            Utils.UI.buttonText(menuItem, menuText);
            return menuItem;
        }

        public static void tooltip(JLabel label, String langKey) {
            label.setToolTipText("<html>" + LangUtil.getString(langKey) + "</html>");
            label.setIcon(Icons.TIPS);
            label.setHorizontalTextPosition(SwingConstants.LEADING);
        }
    }

    public static class JSON {

        public static <T> T readObject(File jsonFile, Class<T> valueType) throws IOException {
            return JSON_MAPPER.readValue(jsonFile, valueType);
        }

        public static String toString(Object object) throws JsonProcessingException {
            return JSON_MAPPER.writeValueAsString(object);
        }

        public static ObjectNode toObject(String jsonString) throws JsonProcessingException {
            return JSON_MAPPER.readValue(jsonString, ObjectNode.class);
        }

        public static <T> T toObject(String jsonString, Class<T> valueType) throws JsonProcessingException {
            return JSON_MAPPER.readValue(jsonString, valueType);
        }
    }

    /**
     * 将时间字符解析为{@link Duration}.如: 1d, 15m, 1h15m.
     * 支持天(D,d),时(H,h),分(M,m),秒(s),毫秒(S)
     *
     * @param timeString 时间字符串
     * @return Duration
     */
    public static Duration parseDuration(String timeString) {

        char[] all = timeString.toCharArray();
        if ((all[0] == 'P') || (all[0] == '-' && all[1] == 'P')) {
            return Duration.parse(timeString);
        }
        Duration duration = Duration.ofSeconds(0);
        char[] tmp = new char[32];
        int numIndex = 0;
        for (char c : all) {
            if (c == '-' || (c >= '0' && c <= '9')) {
                tmp[numIndex++] = c;
                continue;
            }
            long val = new BigDecimal(tmp, 0, numIndex).longValue();
            numIndex = 0;
            Duration plus = null;
            if (c == 'D' || c == 'd') {
                plus = Duration.ofDays(val);
            } else if (c == 'H' || c == 'h') {
                plus = Duration.ofHours(val);
            } else if (c == 'M' || c == 'm') {
                plus = Duration.ofMinutes(val);
            } else if (c == 's' || c == 'S') {
                plus = Duration.ofSeconds(val);
            }
            if (plus != null) {
                duration = duration.plus(plus);
            }
        }
        if (numIndex != 0) {
            duration = duration.plus(Duration.ofMillis(new BigDecimal(tmp, 0, numIndex).longValue()));
        }
        return duration;
    }

    static final int SECONDS_PER_MINUTE = 60;
    static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * 60;
    static final int SECONDS_PER_DAY = SECONDS_PER_HOUR * 24;

    public static String durationToString(Duration duration) {
        long totalseconds = duration.toSeconds();
        long days = totalseconds / SECONDS_PER_DAY;
        long hours = (totalseconds % SECONDS_PER_DAY) / SECONDS_PER_HOUR;
        int minutes = (int) ((totalseconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE);
        int secs = (int) (totalseconds % SECONDS_PER_MINUTE);
        StringBuilder buf = new StringBuilder();
        if (days != 0) {
            buf.append(days).append('d');
        }
        if (hours != 0) {
            buf.append(hours).append('h');
        }
        if (minutes != 0) {
            buf.append(minutes).append('m');
        }
        if (secs != 0) {
            buf.append(secs).append('s');
        }
        return buf.toString();
    }

    /**
     * 获取比给定的颜色更亮的颜色
     *
     * @param color  指定的颜色。
     * @param factor 调节因子 (0, 1)，值越小颜色越亮。
     * @return 变亮后的新颜色。
     */
    public static Color brighter(Color color, float factor) {
        if (factor <= 0 || factor >= 1) {
            throw new IllegalArgumentException("The factor must be within the range of (0, 1)");
        }
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();

        float threshold = 1.0f - factor;

        if (r == 0 && g == 0 && b == 0) {
            int c = Math.min((int) (threshold * 255), 255);
            return new Color(c, c, c, alpha);
        }

        r = Math.min((int) (r / factor), 255);
        g = Math.min((int) (g / factor), 255);
        b = Math.min((int) (b / factor), 255);

        return new Color(r, g, b, alpha);
    }

    /**
     * 获取比给定的颜色更暗的颜色。
     *
     * @param color  指定的颜色。
     * @param factor 调节因子，[0, 1]，因子越小颜色越暗。
     * @return 变暗后的新颜色。
     */
    public static Color darker(Color color, float factor) {
        if (factor < 0 || factor > 1) {
            throw new IllegalArgumentException("The factor must be within the range of [0, 1]");
        }
        return new Color(Math.max((int) (color.getRed() * factor), 0),
            Math.max((int) (color.getGreen() * factor), 0),
            Math.max((int) (color.getBlue() * factor), 0),
            color.getAlpha());
    }

    public static Color generateRandomColor() {
        float lightness = (RANDOM.nextInt(40) + 30) / 100f;//0.3--0.7
        float randomHue = RANDOM.nextInt(360) / 360f;
        Color color = ColorUtilities.HSLtoRGB(randomHue, 1.0f, lightness);
        int randomAlpha = RANDOM.nextInt(30) + 10; // 10--40
        return Utils.mixColorsWithAlpha(UIManager.getColor("Table.background"), color, randomAlpha);
    }

    public static Color mixColorsWithAlpha(Color color1, Color color2, int alpha) {
        float factor = alpha / 255f;
        int red = (int) (color1.getRed() * (1 - factor) + color2.getRed() * factor);
        int green = (int) (color1.getGreen() * (1 - factor) + color2.getGreen() * factor);
        int blue = (int) (color1.getBlue() * (1 - factor) + color2.getBlue() * factor);
        return new Color(red, green, blue);
    }

    /**
     * 根据给定的颜色和界面主题，获取对应背景色的前景颜色（文本颜色）。
     * 该方法通过计算给定颜色的灰度级别来决定返回的文本颜色应该是深色还是浅色。
     *
     * @param bgColor 需要反转其前景色的颜色对象。
     * @param darkLaf 指示当前界面主题是否为暗色主题的布尔值。
     * @return 与给定颜色相反的文本颜色，基于界面主题的黑暗程度。
     */
    public static Color getReverseForegroundColor(Color bgColor, boolean darkLaf) {
        float grayLevel = (bgColor.getRed() * 299 + bgColor.getGreen() * 587 + bgColor.getBlue() * 114) / 1000f / 255;
        return grayLevel >= 0.45 ? DARKER_TEXT_COLOR : (
            darkLaf ? LIGHTER_TEXT_COLOR : Color.WHITE
        );
    }

    public static String md5(String content) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] buffer = content.getBytes();
        messageDigest.update(buffer, 0, buffer.length);
        byte[] digest = messageDigest.digest();
        StringBuilder builder = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }

    public static String findRegexMatchGroup(String regex, String content) {
        Pattern pattern = PatternPool.get(regex, Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.matches()) {
            return matcher.group(matcher.groupCount() >= 1 ? 1 : 0);
        } else {
            return null;
        }
    }

    public static Throwable getRootThrowable(Throwable throwable) {
        Throwable result = throwable;
        while (result.getCause() != null) {
            result = result.getCause();
        }
        return result;
    }

    public static void autoResizeTableColumns(JTable table, int maxColWidth) {
        TableCellRenderer headerRenderer = null;
        if (table.getTableHeader() != null) {
            headerRenderer = table.getTableHeader().getDefaultRenderer();
        }
        for (int i = 0; i < table.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int width = 0;
            if (headerRenderer != null) {
                Component colComp = headerRenderer.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, 0, i);
                width = colComp.getPreferredSize().width;
            }
            for (int j = 0; j < table.getRowCount(); j++) {
                Object value = table.getValueAt(j, i);
                TableCellRenderer cellRenderer = table.getCellRenderer(j, i);
                Component comp = cellRenderer.getTableCellRendererComponent(table, value, false, false, j, i);
                width = Math.max(width, comp.getPreferredSize().width);
            }
            if (maxColWidth > 0) {
                width = Math.min(maxColWidth, width);
            }
            column.setPreferredWidth(width + 10);
        }
        table.revalidate();
        table.repaint();
    }

    public static String mapToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        } else {
            StringJoiner joiner = new StringJoiner(",");
            map.forEach((key, value) -> joiner.add(key + "=" + value));
            return joiner.toString();
        }
    }

    public static Map<String, String> stringToMap(String context) {
        if (StrUtil.isNotBlank(context)) {
            return StrUtil.split(context, ",", true, true)
                .stream()
                .map(attrString -> StrUtil.split(attrString, "=", true, false))
                .collect(Collectors.toMap(item -> item.get(0), item -> item.get(1)));
        } else {
            return Collections.emptyMap();
        }
    }

    public static String mapKeyToString(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return "";
        } else {
            StringJoiner joiner = new StringJoiner(",");
            map.forEach((key, value) -> joiner.add(key));
            return joiner.toString();
        }
    }

    public static void copyToClipboard(String text) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
    }
}
