package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.tracker.AddTracks;
import io.github.thomashuss.spat.tracker.Edit;
import io.github.thomashuss.spat.tracker.MoveTracks;
import io.github.thomashuss.spat.tracker.RemoveTracks;

class PlaylistTrackTableModel
        extends SavedTrackTableModel
{
    public PlaylistTrackTableModel(MainGUI main, Playlist playlist)
    {
        super(main, playlist);
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
            main.commitEdit(new RemoveTracks((Playlist) collection, startIndex, numEntries));
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
                main.commitEdit(new MoveTracks((Playlist) collection, targetRow,
                        transferable.rangeStart, transferable.rangeLength));
            } else {
                main.commitEdit(new AddTracks((Playlist) collection, transferable.getTracks(), targetRow));
            }
            return true;
        }
    }
}
