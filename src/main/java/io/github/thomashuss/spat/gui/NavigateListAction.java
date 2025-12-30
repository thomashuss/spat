package io.github.thomashuss.spat.gui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JList;
import javax.swing.KeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

class NavigateListAction<T>
        extends AbstractAction
{
    private final JList<T> list;

    public NavigateListAction(JList<T> list, InputMap inputMap, ActionMap actionMap)
    {
        this.list = list;
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "navigateList");
        actionMap.put("navigateList", this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent)
    {
        if (!list.hasFocus()) {
            list.grabFocus();
            if (list.getSelectedIndex() == -1) list.setSelectedIndex(0);
        }
    }
}
