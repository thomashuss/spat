package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;

abstract class SavedResourceCollectionFrame<T extends AbstractSpotifyResource>
        extends ResourceFrame
{
    public SavedResourceCollectionFrame(MainGUI main, String title)
    {
        super(main, title);
    }

    abstract SavedResourceTableModel<T> getModel();
}
