package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Playlist;

class PlaylistTrackTableModel
        extends SavedTrackTableModel
{
    public PlaylistTrackTableModel(MainGUI main, Playlist playlist)
    {
        super(main, playlist);
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
}
