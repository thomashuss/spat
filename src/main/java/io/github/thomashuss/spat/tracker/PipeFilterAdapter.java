package io.github.thomashuss.spat.tracker;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.export.ExportWriters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Objects;

public class PipeFilterAdapter
{
    private final ProcessBuilder processBuilder;

    public PipeFilterAdapter(String[] cmd)
    {
        processBuilder = new ProcessBuilder(cmd);
    }

    public <T extends AbstractSpotifyResource> void filter(ResourceFilter<T> resourceFilter,
                                                           boolean shouldFailOnDuplicates)
    throws IOException, InterruptedException, IllegalEditException
    {
        filter(List.of(resourceFilter.getTarget()), resourceFilter, shouldFailOnDuplicates);
    }

    public <T extends AbstractSpotifyResource> void filter(List<SavedResourceCollection<T>> source,
                                                           ResourceFilter<T> resourceFilter,
                                                           boolean shouldFailOnDuplicates)
    throws IOException, InterruptedException, IllegalEditException
    {
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        ExportWriters.writeAll(source, true,
                new BufferedWriter(new OutputStreamWriter(process.getOutputStream())));
        List<T> filtered = reader.lines()
                .map(resourceFilter::getByKey)
                .filter(Objects::nonNull)
                .toList();
        process.waitFor();
        reader.close();
        resourceFilter.filter(filtered, shouldFailOnDuplicates);
    }
}
