package io.github.thomashuss.jspat.client;

@FunctionalInterface
public interface ProgressTracker
{
    void updateProgress(int progress);
}
