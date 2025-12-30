package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.LibraryResource;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

abstract class ResourceFrame
        extends SubFrame
        implements ResourceComponent
{
    protected static final int HEADER_MARGIN = 5;
    protected static final Border HEADER_BORDER = BorderFactory.createEmptyBorder(HEADER_MARGIN, HEADER_MARGIN, 0, HEADER_MARGIN);
    private static final Icon ICON = UIManager.getIcon("FileChooser.detailsViewIcon");

    public ResourceFrame(MainGUI main, String title)
    {
        super(main, title);
        setFrameIcon(ICON);
    }

    protected <T extends LibraryResource> void addResourceLabelsToBox(T[] resources, Box box)
    {
        if (resources != null) {
            int i = 0;
            if (i < resources.length) {
                while (true) {
                    box.add(new ResourceLabel(resources[i++]));
                    if (i < resources.length) box.add(new JLabel(", "));
                    else break;
                }
            }
        }
        box.add(Box.createHorizontalGlue());
    }

    protected <T extends LibraryResource> void updateResourceLabelsInBox(T[] resources, Box box)
    {
        if (resources != null) {
            int count = box.getComponentCount();
            if (count == 0) {
                box.removeAll();
                addResourceLabelsToBox(resources, box);
                box.revalidate();
                box.repaint();
            } else {
                int i = 0;
                int j = 0;
                Component comp;
                for (; i < count && j < resources.length; i += 2) {
                    comp = box.getComponent(i);
                    if (comp instanceof ResourceLabel) {
                        ((ResourceLabel) comp).setResource(resources[j++]);
                    }
                }
                if (j < resources.length) {
                    box.add(new JLabel(", "), i++);
                    while (true) {
                        box.add(new ResourceLabel(resources[j++]), i++);
                        if (j < resources.length) box.add(new JLabel(", "), i++);
                        else break;
                    }
                    box.revalidate();
                    box.repaint();
                } else if (i != count) {
                    for (int k = count - 2; k >= i; ) {
                        box.remove(k--);
                        box.remove(k--);
                    }
                    box.revalidate();
                    box.repaint();
                }
            }
        }
    }

    protected JButton createOpenButton()
    {
        JButton openButton = new JButton("Open in Spotify");
        openButton.setToolTipText("Open in the Spotify client or web player.");
        openButton.addActionListener(actionEvent -> {
            if (Spat.preferences.getBoolean(Spat.P_OPEN_IN_SPOTIFY, true))
                main.openUri(((AbstractSpotifyResource) getResource()).getOpenUri());
            else
                main.openUri(((AbstractSpotifyResource) getResource()).getWebUri());
        });
        return openButton;
    }

    class ResourceLabel
            extends JLabel
            implements ResourceComponent
    {
        private LibraryResource resource;

        protected ResourceLabel()
        {
            super();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new ResourceMouseListener());
        }

        protected ResourceLabel(LibraryResource resource)
        {
            super(resource.getName());
            this.resource = resource;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new ResourceMouseListener());
            main.desktopPane.trackComponentForResource(this, resource);
        }

        @Override
        public void update()
        {
            setText(resource.getName());
        }

        @Override
        public LibraryResource getResource()
        {
            return resource;
        }

        public void setResource(LibraryResource resource)
        {
            if (this.resource != null && this.resource.equals(resource)) return;
            this.resource = resource;
            main.desktopPane.reassignComponentToResource(this, resource);
            setText(resource.getName());
        }

        private class ResourceMouseListener
                extends MouseAdapter
        {
            @Override
            public void mouseClicked(MouseEvent mouseEvent)
            {
                main.desktopPane.openFrameForResource(resource, ResourceFrame.this);
            }
        }
    }
}
