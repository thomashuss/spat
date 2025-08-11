package io.github.thomashuss.jspat.gui;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;

abstract class SavedResourceCollectionFrame<T extends AbstractSpotifyResource>
        extends ResourceFrame
{
    public SavedResourceCollectionFrame(MainGUI main, String title)
    {
        super(main, title);
    }

    abstract SavedResourceTableModel<T> getModel();
}
