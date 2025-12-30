package io.github.thomashuss.spat.library;

class PlaylistKV
        extends SimpleResourceKV<Playlist>
{
    PlaylistKV(Library library)
    {
        super(library, Playlist.class, "playlist", false);
    }
}
