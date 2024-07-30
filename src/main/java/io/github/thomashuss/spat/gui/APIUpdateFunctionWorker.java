package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APIFunction;
import io.github.thomashuss.spat.library.LibraryResource;

class APIUpdateFunctionWorker<T extends LibraryResource>
        extends APIFunctionWorker<T>
{
    public APIUpdateFunctionWorker(MainGUI main, APIFunction<T> task, T t)
    {
        super(main, task, t);
    }

    @Override
    protected void onTaskSuccess(Void unused)
    {
        main.desktopPane.updateComponentsForResource(t);
    }
}
