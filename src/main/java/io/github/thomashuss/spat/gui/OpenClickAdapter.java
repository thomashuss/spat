package io.github.thomashuss.spat.gui;

import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class OpenClickAdapter
        extends MouseAdapter
{
    private final JButton openButton;

    public OpenClickAdapter(JButton openButton)
    {
        this.openButton = openButton;
    }

    @Override
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 2) {
            openButton.doClick();
        }
    }
}
