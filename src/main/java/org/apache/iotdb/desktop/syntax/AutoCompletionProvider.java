package org.apache.iotdb.desktop.syntax;

import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Languages;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.LanguageAwareCompletionProvider;

import java.io.IOException;
import java.util.Locale;

public class AutoCompletionProvider {

    public static CompletionProvider createCompletionProvider() {

        DefaultCompletionProvider cp = new DefaultCompletionProvider();
        try {
            Languages lng = Languages.of(Configuration.instance().getString(ConfKeys.LANGUAGE, Locale.getDefault().toLanguageTag()));
            if (lng == Languages.SIMPLIFIED_CHINESE) {
                cp.loadFromXML(AutoCompletionProvider.class.getResourceAsStream("/org/apache/iotdb/desktop/syntax/IoTDBCompletion_zh_CN.xml"));
            } else {
                cp.loadFromXML(AutoCompletionProvider.class.getResourceAsStream("/org/apache/iotdb/desktop/syntax/IoTDBCompletion.xml"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        LanguageAwareCompletionProvider provider = new LanguageAwareCompletionProvider(cp);
        cp.setAutoActivationRules(true, "");

        return provider;

    }
}
