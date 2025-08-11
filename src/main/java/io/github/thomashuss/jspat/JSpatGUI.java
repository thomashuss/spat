package io.github.thomashuss.jspat;

import io.github.thomashuss.jspat.gui.MainGUI;

import javax.swing.SwingUtilities;

public class JSpatGUI
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(MainGUI::createAndShowGUI);
    }
}
