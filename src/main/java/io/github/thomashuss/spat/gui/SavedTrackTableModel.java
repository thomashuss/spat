package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import java.util.Arrays;
import java.util.stream.Collectors;

class SavedTrackTableModel
        extends SavedResourceTableModel<Track>
{
    public final String[] COL_NAMES = {super.COL_NAMES[0], "Artist", "Album", super.COL_NAMES[1]};

    public SavedTrackTableModel(MainGUI main, SavedResourceCollection<Track> collection)
    {
        super(main, collection);
    }

    public void populate()
    {
        super.populate(main.client::populateSavedTrackCollection);
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
            case 1 ->
                    Arrays.stream(get(row).getResource().getArtists()).map(Artist::getName).collect(Collectors.joining(", "));
            case 2 -> get(row).getResource().getAlbum().getName();
            default -> super.getValueAt(row, col);
        };
    }
}
