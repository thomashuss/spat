package io.github.thomashuss.spat;

import io.github.thomashuss.spat.gui.MainGUI;

import javax.swing.SwingUtilities;

public class SpatGUI
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(MainGUI::createAndShowGUI);
    }
}
