package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Album;
import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.Genre;
import io.github.thomashuss.spat.library.Label;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.Track;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyVetoException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

class ResourceDesktopPane
        extends JDesktopPane
{
    private static final int FRAME_OFFSET = 30;
    private static final Color TO_ARTIST = Color.ORANGE;
    private static final Color TO_ALBUM = Color.MAGENTA;
    private static final Color TO_GENRE = Color.GREEN;
    private static final Color TO_LABEL = Color.CYAN;
    private final MainGUI main;
    private final ReferenceQueue<ResourceComponent> rq;
    private final Map<LibraryResource, ComponentNode> compMap;

    ResourceDesktopPane(MainGUI main)
    {
        super();
        this.main = main;
        rq = new ReferenceQueue<>();
        compMap = new HashMap<>();
    }

    private void setParentAndChild(ResourceFrame frame, JInternalFrame parent)
    {
        frame.setParent(parent);
        if (parent instanceof SubFrame) ((SubFrame) parent).setChild(frame);
    }

    public void openFrameForResource(LibraryResource resource, JInternalFrame parent)
    {
        openFrameForResource(resource, parent, this::newFrameForResource);
    }

    public void openFrameForResource(LibraryResource resource,
                                     JInternalFrame parent,
                                     Function<LibraryResource, ResourceFrame> frameConstructor)
    {
        ResourceFrame frame = getFrameForResource(resource);
        if (frame == null) {
            if ((frame = frameConstructor.apply(resource)) != null) {
                frame.addComponentListener(new ConnectComponentAdapter());
                frame.addInternalFrameListener(new DisconnectComponentAdapter());
                putResourceComponent(resource, frame);
                openNewInternalFrame(frame);
                setParentAndChild(frame, parent);
            }
        } else {
            showInternalFrame(frame);
            setParentAndChild(frame, parent);
        }
    }

    private ResourceFrame newFrameForResource(LibraryResource resource)
    {
        ResourceFrame ret = null;
        synchronized (main.client) {
            if (resource instanceof Playlist) {
                ret = new SavedTrackCollectionFrame(main, (Playlist) resource);
            } else if (resource instanceof Track) {
                ret = new TrackFrame(main, (Track) resource);
            } else if (resource instanceof Artist) {
                ret = new ArtistFrame(main, (Artist) resource);
            } else if (resource instanceof Album) {
                ret = new AlbumFrame(main, (Album) resource);
            } else if (resource instanceof Genre) {
                ret = new GenreFrame(main, (Genre) resource);
            } else if (resource instanceof Label) {
                ret = new LabelFrame(main, (Label) resource);
            }
        }
        return ret;
    }

    private ResourceFrame getFrame(LibraryResource resource)
    {
        ComponentNode node = compMap.get(resource);
        ResourceComponent comp;
        if (node != null && (comp = node.get()) instanceof ResourceFrame) {
            return (ResourceFrame) comp;
        }
        return null;
    }

    public ResourceFrame getFrameForResource(LibraryResource resource)
    {
        expungeStaleComponentNodes();
        return getFrame(resource);
    }

    public void reassignComponentToResource(ResourceComponent component, LibraryResource resource)
    {
        expungeStaleComponentNodes();
        ComponentNode node = compMap.get(component.getResource());
        ResourceComponent search;
        while (node != null) {
            if ((search = node.get()) != null && search.equals(component)) {
                node.remove();
                break;
            }
            node = node.next;
        }
        putResourceComponent(resource, component);
    }

    public void trackComponentForResource(ResourceComponent component, LibraryResource resource)
    {
        expungeStaleComponentNodes();
        putResourceComponent(resource, component);
    }

    public void updateComponentsForResource(LibraryResource resource)
    {
        expungeStaleComponentNodes();
        ComponentNode node = compMap.get(resource);
        ResourceComponent comp;
        while (node != null) {
            if ((comp = node.get()) != null) {
                synchronized (main.client) {
                    comp.update();
                }
            }
            node = node.next;
        }
    }

    void putResourceComponent(LibraryResource resource, ResourceComponent component)
    {
        ComponentNode newNode;
        synchronized (rq) {
            newNode = new ComponentNode(component, resource);
        }
        ComponentNode oldNode;
        if (component instanceof ResourceFrame) {
            oldNode = compMap.put(resource, newNode);
            if (oldNode != null) {
                oldNode.prev = newNode;
                newNode.next = oldNode;
            }
        } else {
            oldNode = compMap.get(resource);
            if (oldNode != null && oldNode.get() instanceof ResourceFrame) {
                newNode.prev = oldNode;
                newNode.next = oldNode.next;
                if (oldNode.next != null) oldNode.next.prev = newNode;
                oldNode.next = newNode;
            } else {
                oldNode = compMap.put(resource, newNode);
                if (oldNode != null) {
                    oldNode.prev = newNode;
                    newNode.next = oldNode;
                }
            }
        }
    }

    public void enableNodeView()
    {
        for (JInternalFrame frame : getAllFrames()) {
            if (frame instanceof NodeFrame) {
                ((NodeFrame) frame).shrinkToName();
            } else {
                try {
                    frame.setIcon(true);
                } catch (PropertyVetoException ignored) {
                }
            }
        }
    }

    public void disableNodeView()
    {
        JInternalFrame[] frames = getAllFrames();
        JInternalFrame frame = null;
        for (int i = frames.length - 1; i >= 0; i--) {
            frame = frames[i];
            if (frame instanceof NodeFrame) {
                ((NodeFrame) frame).restoreSize();
                frame.moveToFront();
            } else {
                try {
                    frame.setIcon(false);
                } catch (PropertyVetoException ignored) {
                }
            }
        }
        if (frame != null) {
            try {
                frame.setSelected(true);
            } catch (PropertyVetoException ignored) {
            }
        }
    }

    private void expungeStaleComponentNodes()
    {
        synchronized (rq) {
            Reference<? extends ResourceComponent> r;
            while ((r = rq.poll()) != null) ((ComponentNode) r).remove();
        }
    }

    private void connectParentAndChild(Graphics g, ResourceFrame frame, LibraryResource other)
    {
        if (other == null) return;
        ResourceFrame parent = getFrame(other);
        if (parent != null && frame.isShowing() && parent.isShowing())
            g.drawLine(frame.getX() + frame.getWidth() / 2,
                    frame.getY() + frame.getHeight() / 2,
                    parent.getX() + parent.getWidth() / 2,
                    parent.getY() + parent.getHeight() / 2);
    }

    private void connectTrackResources(Graphics g, TrackFrame frame)
    {
        Track track = (Track) frame.getResource();
        g.setColor(TO_ALBUM);
        connectParentAndChild(g, frame, track.getAlbum());
        Artist[] artists = track.getArtists();
        if (artists != null) {
            g.setColor(TO_ARTIST);
            for (Artist a : artists) {
                connectParentAndChild(g, frame, a);
            }
        }
    }

    private void connectAlbumResources(Graphics g, AlbumFrame frame)
    {
        Album album = (Album) frame.getResource();
        g.setColor(TO_LABEL);
        connectParentAndChild(g, frame, album.getLabel());
        Artist[] artists = album.getArtists();
        if (artists != null) {
            g.setColor(TO_ARTIST);
            for (Artist a : artists) {
                connectParentAndChild(g, frame, a);
            }
        }
        Genre[] genres = album.getGenres();
        if (genres != null) {
            g.setColor(TO_GENRE);
            for (Genre ge : genres) {
                connectParentAndChild(g, frame, ge);
            }
        }
    }

    private void connectArtistResources(Graphics g, ArtistFrame frame)
    {
        Artist artist = (Artist) frame.getResource();
        Genre[] genres = artist.getGenres();
        if (genres != null) {
            g.setColor(TO_GENRE);
            for (Genre ge : genres) {
                connectParentAndChild(g, frame, ge);
            }
        }
    }

    private void connectAllResources(Graphics g)
    {
        int count = getComponentCount();
        Component comp;
        for (int i = 0; i < count; i++) {
            comp = getComponent(i);
            if (comp instanceof ResourceFrame) {
                if (comp instanceof TrackFrame) connectTrackResources(g, (TrackFrame) comp);
                else if (comp instanceof AlbumFrame) connectAlbumResources(g, (AlbumFrame) comp);
                else if (comp instanceof ArtistFrame) connectArtistResources(g, (ArtistFrame) comp);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (!compMap.isEmpty()) connectAllResources(g);
    }

    void openNewInternalFrame(JInternalFrame frame)
    {
        int pos;
        add(frame);
        pos = FRAME_OFFSET * (getComponentCount() - 1);
        frame.setLocation(pos, pos);
        frame.show();
        try {
            frame.setSelected(true);
        } catch (PropertyVetoException ignored) {
        }
    }

    void showInternalFrame(JInternalFrame frame)
    {
        if (frame.isClosed()) {
            add(frame);
            frame.show();
        }
        try {
            frame.setMaximum(false);
            frame.setSelected(true);
        } catch (PropertyVetoException ignored) {
        }
    }

    private class ComponentNode
            extends WeakReference<ResourceComponent>
    {
        private final LibraryResource key;
        private ComponentNode prev;
        private ComponentNode next;

        private ComponentNode(ResourceComponent referent, LibraryResource key)
        {
            super(referent, rq);
            this.key = key;
        }

        private void remove()
        {
            if (next == null && prev == null) {
                compMap.remove(key);
            } else {
                if (next != null) next.prev = prev;
                if (prev == null) compMap.put(key, next);
                else prev.next = next;
                prev = next = null;
            }
        }
    }

    private class DisconnectComponentAdapter
            extends InternalFrameAdapter
    {
        @Override
        public void internalFrameClosing(InternalFrameEvent e)
        {
            ResourceDesktopPane.this.repaint();
        }
    }

    private class ConnectComponentAdapter
            extends ComponentAdapter
    {
        @Override
        public void componentMoved(ComponentEvent e)
        {
            ResourceDesktopPane.this.repaint();
        }
    }
}
