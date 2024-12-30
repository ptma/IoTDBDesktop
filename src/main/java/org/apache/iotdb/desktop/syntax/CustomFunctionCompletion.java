package org.apache.iotdb.desktop.syntax;

import org.fife.ui.autocomplete.*;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.Position;
import java.util.ArrayList;
import java.util.List;

public class CustomFunctionCompletion extends FunctionCompletion {

    private List<Parameter> params;

    private String returnValDesc;

    private String compareString;

    public CustomFunctionCompletion(CompletionProvider provider, String name, String returnType) {
        super(provider, name, returnType);
    }


    @Override
    protected void addDefinitionString(StringBuilder sb) {
        sb.append("<html><b>");
        sb.append(getDefinitionString());
        sb.append("</b>");
    }


    /**
     * Adds HTML describing the parameters to this function to a buffer.
     *
     * @param sb The buffer to append to.
     */
    protected void addParameters(StringBuilder sb) {
        int paramCount = getParamCount();
        if (paramCount > 0) {
            sb.append("<b>Parameters:</b><br>");
            sb.append("<center><table width='90%'><tr><td>");
            for (int i = 0; i < paramCount; i++) {
                Parameter param = getParam(i);
                String paramName = param.getName() != null ? param.getName() : "param" + (i + 1);
                sb.append("<b>").append(paramName).append(":</b> ").append(param.getType());
                sb.append("&nbsp;");
                String desc = param.getDescription();
                if (desc != null) {
                    sb.append(desc);
                }
                sb.append("<br>");
            }
            sb.append("</td></tr></table></center><br>");
        }

        sb.append("<b>Returns:</b> ");
        String type = getType();
        if (type != null) {
            sb.append(type);
        } else {
            sb.append("unknown");
        }
        if (returnValDesc != null) {
            sb.append("<br><center><table width='90%'><tr><td>");
            sb.append(returnValDesc);
            sb.append("</td></tr></table></center>");
        }

    }


    /**
     * Overridden to compare methods by their comparison strings.
     *
     * @param c2 A <code>Completion</code> to compare to.
     * @return The sort order.
     */
    @Override
    public int compareTo(Completion c2) {

        int rc;

        if (c2 == this) {
            rc = 0;
        } else if (c2 instanceof CustomFunctionCompletion) {
            rc = getCompareString().compareTo(
                ((CustomFunctionCompletion) c2).getCompareString());
        } else {
            rc = super.compareTo(c2);
        }

        return rc;

    }


    @Override
    public boolean equals(Object other) {
        return other instanceof Completion && compareTo((Completion) other) == 0;
    }

    private String getCompareString() {
        if (compareString == null) {
            StringBuilder sb = new StringBuilder(getName());
            // NOTE: This will fail if a method has > 99 parameters (!)
            int paramCount = getParamCount();
            if (paramCount < 10) {
                sb.append('0');
            }
            sb.append(paramCount);
            for (int i = 0; i < paramCount; i++) {
                String type = getParam(i).getType();
                sb.append(type);
                if (i < paramCount - 1) {
                    sb.append(',');
                }
            }
            compareString = sb.toString();
        }

        return compareString;

    }

    @Override
    public String getDefinitionString() {

        StringBuilder sb = new StringBuilder();

        sb.append(getName());

        CompletionProvider provider = getProvider();
        char start = provider.getParameterListStart();
        if (start != 0) {
            sb.append(start);
        }
        for (int i = 0; i < getParamCount(); i++) {
            Parameter param = getParam(i);
            String name = param.getName();
            if (name != null) {
                sb.append(name);
            }
            if (i < params.size() - 1) {
                sb.append(provider.getParameterListSeparator());
            }
        }
        char end = provider.getParameterListEnd();
        if (end != 0) {
            sb.append(end);
        }

        return sb.toString();

    }


    @Override
    public ParameterizedCompletionInsertionInfo getInsertionInfo(
        JTextComponent tc, boolean replaceTabsWithSpaces) {

        ParameterizedCompletionInsertionInfo info =
            new ParameterizedCompletionInsertionInfo();

        StringBuilder sb = new StringBuilder();
        char paramListStart = getProvider().getParameterListStart();
        if (paramListStart != '\0') {
            sb.append(paramListStart);
        }
        int dot = tc.getCaretPosition() + sb.length();
        int paramCount = getParamCount();

        Position maxPos = null;
        try {
            maxPos = tc.getDocument().createPosition(dot - sb.length() + 1);
        } catch (BadLocationException ble) {
            ble.printStackTrace(); // Never happens
        }
        info.setCaretRange(dot, maxPos);
        int firstParamLen = 0;

        int start = dot;
        for (int i = 0; i < paramCount; i++) {
            Parameter param = getParam(i);
            String paramText = getParamText(param);
            if (i == 0) {
                firstParamLen = paramText.length();
            }
            sb.append(paramText);
            int end = start + paramText.length();
            info.addReplacementLocation(start, end);
            String sep = getProvider().getParameterListSeparator();
            if (i < paramCount - 1 && sep != null) {
                sb.append(sep);
                start = end + sep.length();
            }
        }

        char charListEnd = getProvider().getParameterListEnd();
        if (charListEnd > 0) {
            sb.append(getProvider().getParameterListEnd());
        }

        int endOffs = dot + sb.length();
        endOffs -= 1;
        info.addReplacementLocation(endOffs, endOffs);
        info.setDefaultEndOffs(endOffs);

        int selectionEnd = paramCount > 0 ? (dot + firstParamLen) : dot;
        info.setInitialSelection(dot, selectionEnd);
        info.setTextToInsert(sb.toString());
        return info;

    }


    @Override
    public Parameter getParam(int index) {
        return params.get(index);
    }


    @Override
    public int getParamCount() {
        return params == null ? 0 : params.size();
    }


    @Override
    public boolean getShowParameterToolTip() {
        return true;
    }

    private String getParamText(ParameterizedCompletion.Parameter param) {
        String text = param.getName();
        if (text == null) {
            text = param.getType();
            if (text == null) { // Shouldn't ever happen
                text = "arg";
            }
        }
        return text;
    }

    public String getReturnValueDescription() {
        return returnValDesc;
    }


    @Override
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        addDefinitionString(sb);
        if (!possiblyAddDescription(sb)) {
            sb.append("<br>");
        }
        addParameters(sb);
        possiblyAddDefinedIn(sb);
        return sb.toString();
    }


    @Override
    protected boolean possiblyAddDescription(StringBuilder sb) {
        if (getShortDescription() != null) {
            sb.append("<hr>");
            sb.append(getShortDescription());
            sb.append("<br><br>");
            return true;
        }
        return false;
    }

    @Override
    public String getToolTipText() {
        String text = getSummary();
        if (text == null) {
            text = getDefinitionString();
        }
        return text;
    }

    @Override
    public int hashCode() {

        int hashCode = super.hashCode();

        for (int i = 0; i < getParamCount(); i++) {
            hashCode ^= getParam(i).hashCode();
        }

        hashCode ^= returnValDesc != null ? returnValDesc.hashCode() : 0;
        hashCode ^= compareString != null ? compareString.hashCode() : 0;

        return hashCode;
    }

    public void setParams(List<Parameter> params) {
        if (params != null) {
            // Deep copy so parsing can re-use its array.
            this.params = new ArrayList<>(params);
        }
    }
    public void setReturnValueDescription(String desc) {
        this.returnValDesc = desc;
    }


}
