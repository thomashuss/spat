package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

class SavedTrackCollectionFrame
        extends ResourceFrame
{
    private static final Dimension DIMENSION = new Dimension(600, 300);
    private final SavedResourceCollection<Track> collection;
    private final SavedTrackTableModel model;
    private final JTable table;

    public SavedTrackCollectionFrame(MainGUI main, SavedResourceCollection<Track> collection)
    {
        super(main, collection instanceof Playlist ? collection.getName() : "Saved Tracks");
        main.library.populateSavedResources(collection);
        this.collection = collection;
        if (collection instanceof Playlist p) {
            model = new PlaylistTrackTableModel(main, p);
        } else {
            model = new SavedTrackTableModel(main, collection);
        }
        table = new JTable(model);
        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "navigateList");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "navigateList");
        getActionMap().put("navigateList", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (!table.hasFocus()) {
                    table.grabFocus();
                    if (table.getSelectedRow() == -1) table.setRowSelectionInterval(0, 0);
                }
            }
        });
        final JButton updateButton = new APIButton("Refresh");
        updateButton.addActionListener(actionEvent -> model.populate());
        final JButton openButton = new JButton("Open");
        openButton.addActionListener(actionEvent ->
                main.desktopPane.openFrameForResource(model.get(table.getSelectedRow()).getResource(), this));

        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.addMouseListener(new OpenClickAdapter(openButton));
        table.addKeyListener(new OpenKeyAdapter(openButton));

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BoxLayout(tablePane, BoxLayout.PAGE_AXIS));
        tablePane.add(scrollPane);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (collection instanceof Playlist)
            buttonPane.add(new JLabel("Playlist"));
        buttonPane.add(updateButton);
        buttonPane.add(openButton);

        Container contentPane = getContentPane();
        contentPane.add(tablePane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_START);

        pack();
        setVisible(true);
        setSize(DIMENSION);
    }

    @Override
    public void update()
    {
        model.fireTableDataChanged();
    }

    @Override
    public LibraryResource getResource()
    {
        return collection;
    }
}
