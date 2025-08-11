package io.github.thomashuss.jspat.library;

class SavedTrackSerializer
        extends SavedResourceSerializer<SavedTrack, Track>
{
    SavedTrackSerializer(Library library)
    {
        super(library, SavedTrack.class, SavedTrack::new, library.trackDb);
    }
}
