package org.apache.iotdb.desktop.syntax;

import org.apache.iotdb.desktop.util.Icons;
import org.fife.ui.autocomplete.Completion;
import org.fife.ui.autocomplete.CompletionCellRenderer;
import org.fife.ui.autocomplete.FunctionCompletion;
import org.fife.ui.autocomplete.VariableCompletion;

import javax.swing.*;

public class DefaultCellRenderer extends CompletionCellRenderer {

    public DefaultCellRenderer() {
    }


    @Override
    protected void prepareForOtherCompletion(JList<?> list,
                                             Completion c, int index, boolean selected, boolean hasFocus) {
        super.prepareForOtherCompletion(list, c, index, selected, hasFocus);
        setIcon(getEmptyIcon());
    }


    @Override
    protected void prepareForVariableCompletion(JList<?> list,
                                                VariableCompletion vc, int index, boolean selected,
                                                boolean hasFocus) {
        super.prepareForVariableCompletion(list, vc, index, selected,
            hasFocus);
        setIcon(Icons.VARIABLES);
    }

    @Override
    protected void prepareForFunctionCompletion(JList<?> list,
                                                FunctionCompletion fc, int index, boolean selected, boolean hasFocus) {
        setIcon(Icons.FUNCTION);

        StringBuilder sb = new StringBuilder("<html><nobr>");
        sb.append(fc.getName());

        char paramListStart = fc.getProvider().getParameterListStart();
        if (paramListStart!=0) { // 0 => no start char
            sb.append(paramListStart);
        }

        int paramCount = fc.getParamCount();
        for (int i=0; i<paramCount; i++) {
            FunctionCompletion.Parameter param = fc.getParam(i);
            String name = param.getName();
            if (name!=null) {
                sb.append(name);
            }
            if (i<paramCount-1) {
                sb.append(fc.getProvider().getParameterListSeparator());
            }
        }

        char paramListEnd = fc.getProvider().getParameterListEnd();
        if (paramListEnd!=0) { // 0 => No parameter list end char
            sb.append(paramListEnd);
        }
        setText(sb.toString());
    }

}
