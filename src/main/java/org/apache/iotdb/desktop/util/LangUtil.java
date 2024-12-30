package org.apache.iotdb.desktop.util;

import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;

import javax.swing.*;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author ptma
 */
public class LangUtil {

    private static final String BUNDLE_NAME = "org.apache.iotdb.desktop.Lang";
    private static ResourceBundle bundle;

    public static ResourceBundle getBundle() {
        if (bundle != null) {
            return bundle;
        }
        bundle = ResourceBundle.getBundle(BUNDLE_NAME);
        return bundle;
    }

    public static void setupLanguage() {
        try {
            String languageTag = Configuration.instance().options().getLanguage();
            LangUtil.setLocale(Locale.forLanguageTag(languageTag));
        } catch (Exception e) {
            LangUtil.setLocale(Locale.getDefault());
        }
    }

    public static void setLocale(Locale locale) {
        Locale.setDefault(locale);
        bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
    }

    public static Locale getLocale() {
        return getBundle().getLocale();
    }

    public static String getString(String key) {
        return getString(key, key);
    }

    public static String getString(String key, String defaultValue) {
        ResourceBundle tmpBundle = getBundle();
        if (tmpBundle.containsKey(key)) {
            return tmpBundle.getString(key);
        } else {
            tmpBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
            if (tmpBundle.containsKey(key)) {
                return tmpBundle.getString(key);
            } else {
                return defaultValue;
            }
        }
    }

    public static String format(String key, Object... args) {
        ResourceBundle bundle = getBundle();
        if (bundle.containsKey(key)) {
            return String.format(bundle.getString(key), args);
        } else {
            return key;
        }
    }

    public static boolean contains(String key) {
        return getBundle().containsKey(key);
    }

    public static void buttonText(AbstractButton component, String key) {
        Utils.UI.buttonText(component, getString(key));
    }
}
