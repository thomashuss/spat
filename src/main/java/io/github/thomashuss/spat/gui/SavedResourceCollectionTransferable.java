package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;

import javax.annotation.Nonnull;
import javax.swing.ListModel;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

record SavedResourceCollectionTransferable<T extends AbstractSpotifyResource>(DataFlavor[] flavors, int startIndex,
                                                                              SavedResourceCollection<T> src,
                                                                              ListModel<SavedResourceCollection<T>> model
)
        implements Transferable
{
    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return flavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor dataFlavor)
    {
        return flavors[0].equals(dataFlavor);
    }

    @Nonnull
    @Override
    public Object getTransferData(DataFlavor dataFlavor)
    throws UnsupportedFlavorException
    {
        if (!isDataFlavorSupported(dataFlavor)) {
            throw new UnsupportedFlavorException(dataFlavor);
        }
        return this;
    }
}
