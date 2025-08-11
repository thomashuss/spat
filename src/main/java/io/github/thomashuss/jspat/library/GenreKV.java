package io.github.thomashuss.jspat.library;

class GenreKV
        extends SimpleResourceKV<Genre>
{
    GenreKV(Library library)
    {
        super(library, Genre.class, "genre", true);
    }
}
