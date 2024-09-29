package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.tracker.IllegalEditException;
import io.github.thomashuss.spat.tracker.PipeFilterAdapter;
import io.github.thomashuss.spat.tracker.ResourceFilter;

import javax.swing.JOptionPane;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

abstract class SavedResourceTableModel<T extends AbstractSpotifyResource>
        extends AbstractTableModel
{
    protected static final String[] COL_NAMES = {"Name", "Saved At"};
    public final int NAME_COL = 0;
    protected final MainGUI main;
    protected final SavedResourceCollection<T> collection;
    protected boolean updating = false;

    public SavedResourceTableModel(MainGUI main, SavedResourceCollection<T> collection)
    {
        this.main = main;
        this.collection = collection;
    }

    @Override
    public final int getRowCount()
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

    public final SavedResource<T> get(int i)
    {
        return collection.getSavedResourceAt(i);
    }

    @Override
    public String getColumnName(int col)
    {
        return COL_NAMES[col];
    }

    public int findByName(String pattern, int startAt)
    {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        int idx = findByName(p, startAt, collection.getNumResources());
        if (idx == -1) {
            idx = findByName(p, 0, startAt);
        }
        return idx;
    }

    public int findByName(Pattern pattern, int startAt, int until)
    {
        for (int i = startAt; i < until; i++) {
            if (pattern.matcher(collection.getSavedResourceAt(i).getResource().getName()).find()) {
                return i;
            }
        }
        return -1;
    }

    abstract protected void populate();

    protected final void prePopulate()
    {
        if (!collection.isEmpty())
            fireTableRowsDeleted(0, collection.getNumResources() - 1);
        main.abandonEditsFor(collection);
        updating = true;
    }

    protected final void onTaskSuccess()
    {
        updating = false;
        synchronized (main.client) {
            fireTableDataChanged();
        }
    }

    abstract protected ResourceFilter<T> getResourceFilter();

    protected void filter(File exe)
    {
        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground()
            throws IllegalEditException, IOException, InterruptedException
            {
                updating = true;
                ResourceFilter<T> filter = getResourceFilter();
                new PipeFilterAdapter(new String[]{exe.toString()})
                        .filter(filter, true);
                main.editTracker.commit(filter);
                return null;
            }

            @Override
            protected void done()
            {
                try {
                    get();
                } catch (Exception e) {
                    JOptionPane.showInternalMessageDialog(main.desktopPane, e.getMessage(),
                            "Filter error", JOptionPane.ERROR_MESSAGE);
                }
                updating = false;
                fireTableDataChanged();
                main.updateEditControls();
            }
        }.execute();
    }
}
