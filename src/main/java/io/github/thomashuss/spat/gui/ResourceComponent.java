package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.LibraryResource;

interface ResourceComponent
{
    void update();

    LibraryResource getResource();
}
