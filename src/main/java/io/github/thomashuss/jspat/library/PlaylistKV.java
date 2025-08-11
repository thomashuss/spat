package io.github.thomashuss.jspat.library;

class PlaylistKV
        extends SimpleResourceKV<Playlist>
{
    PlaylistKV(Library library)
    {
        super(library, Playlist.class, "playlist", false);
    }
}
