package io.github.thomashuss.spat.client;

@FunctionalInterface
public interface ProgressTracker
{
    void updateProgress(int progress);
}
