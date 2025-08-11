package io.github.thomashuss.jspat.gui;

import io.github.thomashuss.jspat.library.AbstractSpotifyResource;
import io.github.thomashuss.jspat.library.SavedAlbumCollection;
import io.github.thomashuss.jspat.library.SavedResourceCollection;
import io.github.thomashuss.jspat.library.SavedTrackCollection;
import io.github.thomashuss.jspat.library.Track;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

class SavedResourceCollectionExportHandler<T extends AbstractSpotifyResource>
        extends TransferHandler
{
    static final DataFlavor[] STC_FLAVORS =
            {new DataFlavor(SavedTrackCollection.class, "Saved Track Collection")};
    static final DataFlavor[] SAC_FLAVORS =
            {new DataFlavor(SavedAlbumCollection.class, "Saved Album Collection")};

    final ListModel<SavedResourceCollection<T>> model;
    final DataFlavor[] flavors;

    SavedResourceCollectionExportHandler(ListModel<SavedResourceCollection<T>> model,
                                         DataFlavor[] flavors)
    {
        this.model = model;
        this.flavors = flavors;
    }

    static SavedResourceCollectionExportHandler<Track> forTrackModel(
            ListModel<SavedResourceCollection<Track>> model)
    {
        return new SavedResourceCollectionExportHandler<>(model, STC_FLAVORS);
    }

    @Override
    protected Transferable createTransferable(JComponent c)
    {
        @SuppressWarnings("unchecked")
        JList<SavedResourceCollection<T>> list = (JList<SavedResourceCollection<T>>) c;
        return new SavedResourceCollectionTransferable<>(flavors, list.getSelectedIndex(), list.getSelectedValue(),
                list.getModel());
    }

    @Override
    public int getSourceActions(JComponent c)
    {
        return COPY_OR_MOVE;
    }

    @Override
    public boolean canImport(TransferSupport support)
    {
        return support.isDataFlavorSupported(flavors[0]);
    }

    @Override
    public boolean importData(TransferSupport support)
    {
        return false;
    }
}
