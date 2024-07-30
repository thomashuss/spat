package io.github.thomashuss.spat.gui;

import javax.swing.JButton;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

class OpenKeyAdapter
        extends KeyAdapter
{
    private final JButton openButton;

    public OpenKeyAdapter(JButton openButton)
    {
        this.openButton = openButton;
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            openButton.doClick();
            e.consume();
        }
    }
}
