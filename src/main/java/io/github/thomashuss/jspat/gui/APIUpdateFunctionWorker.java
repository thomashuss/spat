package io.github.thomashuss.jspat.gui;

import io.github.thomashuss.jspat.client.APIFunction;
import io.github.thomashuss.jspat.library.LibraryResource;

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
