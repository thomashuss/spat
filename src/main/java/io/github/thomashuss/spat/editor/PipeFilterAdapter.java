package io.github.thomashuss.spat.editor;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.export.ExportWriters;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PipeFilterAdapter
{
    private final ProcessBuilder processBuilder;
    private final boolean isJson;

    public PipeFilterAdapter(String[] cmd, boolean isJson)
    {
        processBuilder = new ProcessBuilder(cmd);
        this.isJson = isJson;
    }

    public <T extends AbstractSpotifyResource> void filter(Library library, List<SavedResourceCollection<T>> sources,
                                                           List<SavedResourceCollection<T>> targets, Editor ed)
    throws IOException, InterruptedException
    {
        if (sources.isEmpty() || targets.isEmpty()) return;
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        ExportWriters.writeAll(sources, isJson,
                new BufferedWriter(new OutputStreamWriter(process.getOutputStream())));
        Iterator<SavedResourceCollection<T>> it = targets.iterator();
        ResourceFilter<T> resourceFilter = it.next().getResourceFilter(library);
        List<T> filtered = reader.lines()
                .map(resourceFilter::getByKey)
                .filter(Objects::nonNull)
                .toList();
        process.waitFor();
        reader.close();
        while (true) {
            resourceFilter.filter(filtered);
            ed.commit(resourceFilter);
            if (!it.hasNext()) break;
            resourceFilter = it.next().getResourceFilter(library);
        }
    }
}
