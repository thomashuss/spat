package io.github.thomashuss.spat.gui;

import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Point;

abstract class NodeFrame
        extends ResourceFrame
{
    protected JLabel nameLabel;
    private Dimension oldSize;

    public NodeFrame(MainGUI main, String title)
    {
        super(main, title);
    }

    public void shrinkToName()
    {
        oldSize = getSize();
        Point frameLoc = getLocationOnScreen();
        Point labelLoc = nameLabel.getLocationOnScreen();
        setSize((int) (labelLoc.getX() + nameLabel.getWidth() + HEADER_MARGIN + HEADER_MARGIN - frameLoc.getX()),
                (int) (labelLoc.getY() + nameLabel.getHeight() + HEADER_MARGIN + HEADER_MARGIN - frameLoc.getY()));
    }

    public void restoreSize()
    {
        if (oldSize != null) {
            setSize(oldSize);
            oldSize = null;
        }
    }
}
