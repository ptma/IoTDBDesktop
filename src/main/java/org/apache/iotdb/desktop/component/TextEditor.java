package org.apache.iotdb.desktop.component;

import com.formdev.flatlaf.FlatLaf;
import org.apache.iotdb.desktop.config.ConfKeys;
import org.apache.iotdb.desktop.config.Configuration;
import org.apache.iotdb.desktop.config.Options;
import org.apache.iotdb.desktop.event.AppEventListener;
import org.apache.iotdb.desktop.event.AppEventListenerAdapter;
import org.apache.iotdb.desktop.event.AppEvents;
import org.apache.iotdb.desktop.syntax.DefaultCellRenderer;
import org.apache.iotdb.desktop.util.LangUtil;
import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.CompletionProvider;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.*;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.io.IOException;

/**
 * @author ptma
 */
public class TextEditor extends RTextScrollPane {

    private AppEventListener appEventListener;

    private final RSyntaxTextArea textArea;

    public TextEditor() {
        this(RSyntaxTextArea.SYNTAX_STYLE_NONE);
    }

    public TextEditor(String syntaxStyle) {
        super();
        this.textArea = new RSyntaxTextArea();
        this.textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        this.textArea.setLineWrap(true);
        this.textArea.setCodeFoldingEnabled(true);
        this.textArea.setPaintTabLines(false);
        this.textArea.setTabSize(2);
        this.textArea.setShowMatchedBracketPopup(false);
        this.textArea.setBracketMatchingEnabled(false);
        this.textArea.setMarkOccurrences(true);
        this.textArea.setHyperlinksEnabled(true);
        this.textArea.setAutoIndentEnabled(true);
        this.textArea.setMargin(new Insets(5, 5, 5, 10));
        this.textArea.setCaretStyle(RSyntaxTextArea.INSERT_MODE, CaretStyle.VERTICAL_LINE_STYLE);
        this.textArea.setSyntaxEditingStyle(syntaxStyle);

        this.textArea.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (Exception e1) {
                    // ignore
                }
            }
        });
        this.setViewportView(textArea);
        this.setLineNumbersEnabled(true);
        this.setFoldIndicatorEnabled(true);
        this.getGutter().setFoldIndicatorStyle(FoldIndicatorStyle.CLASSIC);
        this.getGutter().setBorder(new Gutter.GutterBorder(0, 5, 0, 2));

        JPopupMenu popupMenu = this.textArea.getPopupMenu();
        popupMenu.remove(0);
        popupMenu.remove(0);
        popupMenu.remove(0);
        popupMenu.remove(popupMenu.getComponentCount() - 1);
        popupMenu.remove(popupMenu.getComponentCount() - 1);

        loadOptions(Configuration.instance().options());

        appEventListener = new AppEventListenerAdapter() {
            @Override
            public void optionsChanged(Options options, Options oldOptions) {
                if (!options.getTheme().equals(oldOptions.getTheme()) ||
                    !options.getFontName().equals(oldOptions.getFontName()) ||
                    options.getFontSize() != oldOptions.getFontSize()
                ) {
                    loadOptions(options);
                }
            }
        };

        AppEvents.instance().addEventListener(appEventListener);
    }

    private void loadOptions(Options options) {
        Font font = StyleContext.getDefaultStyleContext().getFont(options.getFontName(), Font.PLAIN, options.getFontSize());
        this.textArea.setFont(font);
        try {
            Theme theme;
            if (FlatLaf.isLafDark()) {
                theme = Theme.load(getClass().getResourceAsStream("/org/apache/iotdb/desktop/theme/dark.xml"), font);
            } else {
                theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"), font);
            }
            theme.apply(textArea);
        } catch (IOException ioe) {
            // ignore
        }
    }

    public RSyntaxTextArea textArea() {
        return textArea;
    }

    public void setText(String text) {
        this.textArea.setText(text);
        this.textArea.setCaretPosition(0);
    }

    public void setSyntaxEditingStyle(String syntaxStyle) {
        this.textArea.setSyntaxEditingStyle(syntaxStyle);
    }
    public void appendTextAndScrollToEnd(String text) {
        synchronized (this.textArea) {
            this.textArea.append(text);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    TextEditor.this.textArea.setCaretPosition(TextEditor.this.textArea.getDocument().getLength());
                }
            });
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.textArea.setEnabled(enabled);
    }

    public void setEditable(boolean editable) {
        this.textArea.setEditable(editable);
    }

    public String getText() {
        return this.textArea.getText();
    }

    public JPopupMenu getPopupMenu() {
        return this.textArea.getPopupMenu();
    }

    public void installAutoCompletion(CompletionProvider provider) {
        AutoCompletion ac = new AutoCompletion(provider);
        ac.install(textArea);
        ac.setListCellRenderer(new DefaultCellRenderer());
        ac.setShowDescWindow(false);
        ac.setParameterAssistanceEnabled(false);

        ac.setAutoCompleteEnabled(true);
        ac.setAutoActivationEnabled(true);
        ac.setAutoCompleteSingleChoices(false);
        ac.setAutoActivationDelay(Configuration.instance().options().getAutoCompletionDelay());

        textArea.setToolTipSupplier((ToolTipSupplier) provider);
        ToolTipManager.sharedInstance().registerComponent(textArea);
    }

    public void uninstallAutoCompletion() {
        textArea.setToolTipSupplier(null);
        ToolTipManager.sharedInstance().unregisterComponent(textArea);
    }

    @Override
    public void updateUI() {
        super.updateUI();
    }

    public void dispose() {
        if (appEventListener != null) {
            AppEvents.instance().removeEventListener(appEventListener);
        }
        ToolTipManager.sharedInstance().unregisterComponent(textArea);
    }
}
