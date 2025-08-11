package io.github.thomashuss.jspat.gui;

import io.github.thomashuss.jspat.library.LibraryResource;

interface ResourceComponent
{
    void update();

    LibraryResource getResource();
}
