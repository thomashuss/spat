package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.Track;
import io.github.thomashuss.spat.tracker.AddTracks;
import io.github.thomashuss.spat.tracker.Edit;
import io.github.thomashuss.spat.tracker.IllegalEditException;
import io.github.thomashuss.spat.tracker.MoveTracks;
import io.github.thomashuss.spat.tracker.PlaylistFilter;
import io.github.thomashuss.spat.tracker.RemoveTracks;
import io.github.thomashuss.spat.tracker.ResourceFilter;

import javax.swing.JOptionPane;

class PlaylistTrackTableModel
        extends SavedTrackTableModel
{
    public PlaylistTrackTableModel(MainGUI main, Playlist playlist)
    {
        super(main, playlist);
    }

    @Override
    protected ResourceFilter<Track> getResourceFilter()
    {
        return new PlaylistFilter(main.library, (Playlist) collection);
    }

    @Override
    TrackTransferHandler getTransferHandler()
    {
        return new PlaylistTrackTransferHandler();
    }

    private void fireUpdate(MoveTracks edit, boolean isCommit)
    {
        if (isCommit) {
            fireTableRowsDeleted(edit.rangeStart, edit.rangeStart + edit.rangeLength - 1);
            fireTableRowsInserted(edit.insertBefore, edit.insertBefore + edit.rangeLength - 1);
        } else {
            fireTableRowsDeleted(edit.insertBefore, edit.insertBefore + edit.rangeLength - 1);
            fireTableRowsInserted(edit.rangeStart, edit.rangeStart + edit.rangeLength - 1);
        }
    }

    @Override
    boolean fireUpdate(Edit edit, boolean isCommit)
    {
        if (!super.fireUpdate(edit, isCommit) && edit instanceof MoveTracks e) {
            fireUpdate(e, isCommit);
            return true;
        }
        return false;
    }

    @Override
    void deleteEntries(int startIndex, int numEntries)
    {
        if (numEntries > 0) {
            main.commitEdit(RemoveTracks.of((Playlist) collection, startIndex, numEntries));
        }
    }

    @Override
    public void populate()
    {
        prePopulate();
        new APILongFunctionWorker<>(main, main.client::populatePlaylist, (Playlist) collection)
        {
            @Override
            protected void onTaskSuccess(Void v)
            {
                PlaylistTrackTableModel.this.onTaskSuccess();
            }
        }.execute();
    }

    class PlaylistTrackTransferHandler
            extends TrackTransferHandler
    {
        @Override
        protected boolean commitDrop(boolean isIntraModel, int targetRow, TrackTransferable transferable)
        {
            if (isIntraModel) {
                if (targetRow > transferable.rangeStart + transferable.rangeLength || targetRow < transferable.rangeStart) {
                    main.commitEdit(MoveTracks.of((Playlist) collection, targetRow,
                            transferable.rangeStart, transferable.rangeLength));
                    return true;
                }
            } else {
                try {
                    main.commitEdit(AddTracks.of((Playlist) collection, transferable.getTracks(), targetRow));
                    return true;
                } catch (IllegalEditException e) {
                    JOptionPane.showInternalMessageDialog(main.desktopPane, e.getReason(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            return false;
        }
    }
}
