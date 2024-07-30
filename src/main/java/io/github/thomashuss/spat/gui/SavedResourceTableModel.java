package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.APILongFunction;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.SpotifyResource;

import javax.swing.table.AbstractTableModel;

class SavedResourceTableModel<T extends SpotifyResource>
        extends AbstractTableModel
{
    public final int NAME_COL = 0;
    public final String[] COL_NAMES = {"Name", "Saved At"};
    protected final MainGUI main;
    protected final SavedResourceCollection<T> collection;
    private boolean updating = false;

    public SavedResourceTableModel(MainGUI main, SavedResourceCollection<T> collection)
    {
        this.main = main;
        this.collection = collection;
    }

    @Override
    public int getRowCount()
    {
        return updating ? 0 : collection.getNumResources();
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public Object getValueAt(int row, int col)
    {
        return col == NAME_COL ? collection.getSavedResourceAt(row).getResource().getName() : collection.getSavedResourceAt(row).addedAt();
    }

    public SavedResource<T> get(int i)
    {
        return collection.getSavedResourceAt(i);
    }

    @Override
    public String getColumnName(int col)
    {
        return COL_NAMES[col];
    }

    protected void populate(APILongFunction<SavedResourceCollection<T>> func)
    {
        if (!collection.isEmpty())
            fireTableRowsDeleted(0, collection.getNumResources() - 1);
        updating = true;
        new APILongFunctionWorker<>(main, func, collection)
        {
            @Override
            protected void onTaskSuccess(Void v)
            {
                super.onTaskSuccess(v);
                updating = false;
                synchronized (main.client) {
                    fireTableDataChanged();
                }
            }
        }.execute();
    }
}
