package io.github.thomashuss.spat.gui;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.KeyStroke;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

abstract class SubFrame
        extends JInternalFrame
{
    protected final MainGUI main;
    protected final InputMap imap;
    protected final ActionMap amap;
    protected JInternalFrame parent;
    protected JInternalFrame child;

    public SubFrame(MainGUI main, String title)
    {
        super(title, true, true, true, true);
        this.main = main;
        imap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        amap = getActionMap();
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");
        amap.put("close", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                doDefaultCloseAction();
                if (parent != null && !parent.isClosed()) {
                    try {
                        parent.setSelected(true);
                    } catch (PropertyVetoException ignored) {
                    }
                }
            }
        });
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_DOWN_MASK), "parent");
        amap.put("parent", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (parent != null) {
                    if (parent.isClosed()) {
                        parent = null;
                    } else {
                        try {
                            parent.setSelected(true);
                        } catch (PropertyVetoException ignored) {
                        }
                    }
                }
            }
        });
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_DOWN_MASK), "child");
        amap.put("child", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (child != null) {
                    if (child.isClosed()) {
                        child = null;
                    } else {
                        try {
                            child.setSelected(true);
                        } catch (PropertyVetoException ignored) {
                        }
                    }
                }
            }
        });
    }

    protected static Box getLeftAligned(Component c)
    {
        Box box = Box.createHorizontalBox();
        box.add(c);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    public void setParent(JInternalFrame parent)
    {
        this.parent = parent;
    }

    public void setChild(JInternalFrame child)
    {
        this.child = child;
    }

    protected class APIButton
            extends JButton
            implements PropertyChangeListener
    {
        protected APIButton(String text)
        {
            super(text);
            SubFrame.this.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentShown(ComponentEvent e)
                {
                    setEnabled(main.hasAuth());
                    main.addStatePropertyChangeListener(APIButton.this);
                }
            });
            SubFrame.this.addInternalFrameListener(new InternalFrameAdapter()
            {
                @Override
                public void internalFrameClosed(InternalFrameEvent e)
                {
                    main.removeStatePropertyChangeListener(APIButton.this);
                }
            });
        }

        @Override
        public void propertyChange(PropertyChangeEvent event)
        {
            if (MainGUI.HAS_AUTH_KEY.equals(event.getPropertyName())) {
                setEnabled((Boolean) event.getNewValue());
            }
        }
    }
}
