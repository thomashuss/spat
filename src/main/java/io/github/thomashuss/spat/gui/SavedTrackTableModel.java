package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;
import io.github.thomashuss.spat.tracker.Edit;
import io.github.thomashuss.spat.tracker.IllegalEditException;
import io.github.thomashuss.spat.tracker.ResourceFilter;
import io.github.thomashuss.spat.tracker.SaveTracks;
import io.github.thomashuss.spat.tracker.SavedTrackFilter;
import io.github.thomashuss.spat.tracker.TrackInsertion;
import io.github.thomashuss.spat.tracker.TrackRemoval;
import io.github.thomashuss.spat.tracker.UnsaveTracks;

import javax.annotation.Nonnull;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

class SavedTrackTableModel
        extends SavedResourceTableModel<Track>
{
    protected static final String[] COL_NAMES = {SavedResourceTableModel.COL_NAMES[0], "Artist", "Album",
            SavedResourceTableModel.COL_NAMES[1]};
    private static final DataFlavor TRACK_FLAVOR =
            new DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, "Track Range");
    private static final DataFlavor[] FLAVORS = {TRACK_FLAVOR};

    public SavedTrackTableModel(MainGUI main, SavedResourceCollection<Track> collection)
    {
        super(main, collection);
    }

    private static void fireRemoval(boolean isSequential, List<Integer> indices, BiConsumer<Integer, Integer> updater)
    {
        if (isSequential) {
            updater.accept(indices.get(indices.size() - 1), indices.get(0));
        } else {
            for (int i : indices) {
                updater.accept(i, i);
            }
        }
    }

    TrackTransferHandler getTransferHandler()
    {
        return new TrackTransferHandler();
    }

    @Override
    public void populate()
    {
        prePopulate();
        new APILongSupplierWorker(main, main.client::populateSavedTracks)
        {
            @Override
            protected void onTaskSuccess(Void v)
            {
                SavedTrackTableModel.this.onTaskSuccess();
            }
        }.execute();
    }

    @Override
    protected ResourceFilter<Track> getResourceFilter()
    {
        return new SavedTrackFilter(main.library, collection);
    }

    @Override
    public String getColumnName(int col)
    {
        return COL_NAMES[col];
    }

    @Override
    public int getColumnCount()
    {
        return COL_NAMES.length;
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        return switch (col) {
            case 1 -> {
                Artist[] artists = get(row).getResource().getArtists();
                yield switch (artists.length) {
                    case 4 -> artists[0].getName() + ", " + artists[1].getName() + ", " + artists[2].getName()
                            + ", " + artists[3].getName();
                    case 3 -> artists[0].getName() + ", " + artists[1].getName() + ", " + artists[2].getName();
                    case 2 -> artists[0].getName() + ", " + artists[1].getName();
                    case 1 -> artists[0].getName();
                    case 0 -> "";
                    default -> Arrays.stream(artists).map(Artist::getName)
                        .collect(Collectors.joining(", "));
                };
            }
            case 2 -> get(row).getResource().getAlbum().getName();
            default -> super.getValueAt(row, col);
        };
    }

    private void fireUpdate(TrackInsertion edit, boolean isCommit)
    {
        if (isCommit) {
            fireTableRowsInserted(edit.index(), edit.index() + edit.tracks().size() - 1);
        } else {
            fireTableRowsDeleted(edit.index(), edit.index() + edit.tracks().size() - 1);
        }
    }

    private void fireUpdate(TrackRemoval edit, boolean isCommit)
    {
        fireRemoval(edit.isSequential(), edit.indices(),
                isCommit ? this::fireTableRowsDeleted : this::fireTableRowsInserted);
    }

    boolean fireUpdate(Edit edit, boolean isCommit)
    {
        if (edit instanceof TrackInsertion e) {
            fireUpdate(e, isCommit);
            return true;
        } else if (edit instanceof TrackRemoval e) {
            fireUpdate(e, isCommit);
            return true;
        } else if (edit instanceof ResourceFilter<?> f) {
            f.forEach((e) -> fireUpdate(e, isCommit), isCommit);
            return true;
        }
        return false;
    }

    void deleteEntries(int startIndex, int numEntries)
    {
        if (numEntries > 0 && startIndex >= 0) {
            main.commitEdit(UnsaveTracks.of(main.library, startIndex, numEntries));
        }
    }

    class TrackTransferHandler
            extends TransferHandler
    {
        @Override
        protected Transferable createTransferable(JComponent c)
        {
            JTable table = (JTable) c;
            return new TrackTransferable(table.getSelectedRow(), table.getSelectedRowCount());
        }

        @Override
        public int getSourceActions(JComponent c)
        {
            return COPY_OR_MOVE;
        }

        @Override
        public boolean canImport(TransferSupport support)
        {
            return support.isDataFlavorSupported(TRACK_FLAVOR);
        }

        protected boolean commitDrop(boolean isIntraModel, int targetRow, TrackTransferable transferable)
        {
            if (!isIntraModel) {
                try {
                    main.commitEdit(SaveTracks.of(main.library, transferable.getTracks()));
                    return true;
                } catch (IllegalEditException e) {
                    JOptionPane.showInternalMessageDialog(main.desktopPane, e.getReason(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            return false;
        }

        @Override
        public boolean importData(TransferSupport support)
        {
            if (!support.isDataFlavorSupported(TRACK_FLAVOR)) return false;

            int targetRow = ((JTable.DropLocation) support.getDropLocation()).getRow();
            if (targetRow == -1) return false;

            Transferable t = support.getTransferable();
            TrackTransferable transferable;
            try {
                transferable = (TrackTransferable) t.getTransferData(TRACK_FLAVOR);
            } catch (UnsupportedFlavorException | IOException e) {
                throw new RuntimeException(e);
            }
            if (targetRow == transferable.rangeStart) return false;
            return commitDrop(((JTable) support.getComponent()).getModel() == transferable.getModel(),
                    targetRow, transferable);
        }
    }

    final class TrackTransferable
            implements Transferable
    {
        final int rangeStart;
        final int rangeLength;

        private TrackTransferable(int rangeStart, int rangeLength)
        {
            this.rangeStart = rangeStart;
            this.rangeLength = rangeLength;
        }

        private SavedTrackTableModel getModel()
        {
            return SavedTrackTableModel.this;
        }

        List<Track> getTracks()
        {
            return collection.getRange(rangeStart, rangeStart + rangeLength);
        }

        @Override
        public DataFlavor[] getTransferDataFlavors()
        {
            return FLAVORS;
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor dataFlavor)
        {
            return TRACK_FLAVOR.equals(dataFlavor);
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
}
