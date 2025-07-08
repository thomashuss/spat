package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedAlbumCollection;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.SavedTrackCollection;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

class SavedResourceCollectionTransferHandler<T extends AbstractSpotifyResource>
        extends SavedResourceCollectionExportHandler<T>
{
    private SavedResourceCollectionTransferHandler(DefaultListModel<SavedResourceCollection<T>> model,
                                                   DataFlavor[] flavors)
    {
        super(model, flavors);
    }

    static <T extends AbstractSpotifyResource> SavedResourceCollectionTransferHandler<T> of(
            DefaultListModel<SavedResourceCollection<T>> model, SavedResourceCollection<T> collection)
    {
        if (collection instanceof SavedTrackCollection) {
            return new SavedResourceCollectionTransferHandler<>(model, STC_FLAVORS);
        } else if (collection instanceof SavedAlbumCollection) {
            return new SavedResourceCollectionTransferHandler<>(model, SAC_FLAVORS);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean importData(TransferSupport support)
    {
        if (!support.isDataFlavorSupported(flavors[0])) return false;

        int target = ((JList.DropLocation) support.getDropLocation()).getIndex();
        if (target == -1) return false;

        Transferable t = support.getTransferable();
        SavedResourceCollectionTransferable<T> transferable;
        DefaultListModel<SavedResourceCollection<T>> model = (DefaultListModel<SavedResourceCollection<T>>) this.model;
        try {
            @SuppressWarnings("unchecked")
            SavedResourceCollectionTransferable<T> tr = (SavedResourceCollectionTransferable<T>) t.getTransferData(flavors[0]);
            transferable = tr;
        } catch (UnsupportedFlavorException | IOException e) {
            throw new RuntimeException(e);
        }
        if (model.equals(transferable.model())) {
            int source = transferable.startIndex();
            if (source != target) {
                model.add(target > source ? target - 1 : target, model.remove(source));
                return true;
            }
        } else if (!model.contains(transferable.src())) {
            model.add(target, transferable.src());
            return true;
        }
        return false;
    }
}
