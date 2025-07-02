package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.tracker.IllegalEditException;
import io.github.thomashuss.spat.tracker.PipeFilterAdapter;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class FilterFrame<T extends AbstractSpotifyResource>
        extends JInternalFrame
{
    private static final JFileChooser CHOOSER = new JFileChooser();
    private final MainGUI main;
    private final FormSavedResourceCollectionList<T> inputs;
    private final FormSavedResourceCollectionList<T> outputs;

    FilterFrame(MainGUI main, SavedResourceCollection<T> initial)
    {
        super("Filter", false, true, false, false);
        this.main = main;

        final JPanel pane = new JPanel();

        pane.add(inputs = new FormSavedResourceCollectionList<>("Inputs", initial));
        pane.add(outputs = new FormSavedResourceCollectionList<>("Outputs", initial));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionEvent -> doDefaultCloseAction());
        JButton filterButton = new JButton("Filter (CSV)");
        filterButton.addActionListener(actionEvent -> chooseFilter(false));
        JButton filterJsonButton = new JButton("Filter (JSON)");
        filterJsonButton.addActionListener(actionEvent -> chooseFilter(true));
        buttonPane.add(cancelButton);
        buttonPane.add(filterJsonButton);
        buttonPane.add(filterButton);

        final Container contentPane = getContentPane();
        contentPane.add(pane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();
    }

    private static <T> List<T> modelToList(DefaultListModel<T> model)
    {
        final int size = model.size();
        final List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(model.get(i));
        }
        return list;
    }

    private void chooseFilter(boolean isJson)
    {
        if (CHOOSER.showOpenDialog(main) == JFileChooser.APPROVE_OPTION) {
            List<SavedResourceCollection<T>> inp = modelToList(inputs.listModel);
            List<SavedResourceCollection<T>> out = modelToList(outputs.listModel);
            Set<SavedResourceTableModel<T>> models = Stream.concat(inp.stream(), out.stream())
                    .map((src) -> {
                        // Note: this won't open the Saved Tracks frame, but currently Saved Tracks
                        // can be dropped into an SRC list only from its FilterFrame (which requires
                        // the frame to be open).
                        @SuppressWarnings("unchecked")
                        SavedResourceCollectionFrame<T> rf = (SavedResourceCollectionFrame<T>)
                                main.desktopPane.openFrameForResource(src, this);
                        SavedResourceTableModel<T> model = rf.getModel();
                        model.updating = true;
                        return model;
                    }).collect(Collectors.toUnmodifiableSet());
            File exe = CHOOSER.getSelectedFile();
            new SwingWorker<Void, Void>()
            {
                @Override
                protected Void doInBackground()
                throws IllegalEditException, IOException, InterruptedException
                {
                    new PipeFilterAdapter(new String[]{exe.toString()}, isJson)
                            .filter(main.library, inp, out, main.editTracker);
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
                        e.getCause().printStackTrace();
                    }
                    for (SavedResourceTableModel<T> model : models) {
                        model.updating = false;
                        model.fireTableDataChanged();
                    }
                    main.updateEditControls();
                }
            }.execute();
        }
    }
}
