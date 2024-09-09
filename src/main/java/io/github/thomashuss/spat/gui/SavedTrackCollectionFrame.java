package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Playlist;
import io.github.thomashuss.spat.library.SavedResourceCollection;
import io.github.thomashuss.spat.library.Track;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DropMode;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

class SavedTrackCollectionFrame
        extends ResourceFrame
{
    private static final Object NAVIGATE_LIST = new Object();
    private static final Object DELETE_ENTRIES = new Object();
    private static final JFileChooser CHOOSER = new JFileChooser();
    final SavedTrackTableModel model;
    private final JTable table;
    private final SavedResourceCollection<Track> collection;
    private static final Dimension DIMENSION = new Dimension(600, 300);

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
        table.setDragEnabled(true);
        table.setDropMode(DropMode.INSERT_ROWS);
        table.setTransferHandler(model.getTransferHandler());

        InputMap inputMap = getInputMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), NAVIGATE_LIST);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), NAVIGATE_LIST);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), NAVIGATE_LIST);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), NAVIGATE_LIST);
        ActionMap actionMap = getActionMap();
        actionMap.put(NAVIGATE_LIST, new AbstractAction()
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
        InputMap tableInputMap = table.getInputMap();
        tableInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE_ENTRIES);
        ActionMap tableActionMap = table.getActionMap();
        tableActionMap.put(DELETE_ENTRIES, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                model.deleteEntries(table.getSelectedRow(), table.getSelectedRowCount());
            }
        });

        final JButton updateButton = new APIButton("Refresh");
        updateButton.addActionListener(actionEvent -> model.populate());
        final JButton openButton = new JButton("Open");
        openButton.addActionListener(actionEvent ->
                main.desktopPane.openFrameForResource(model.get(table.getSelectedRow()).getResource(), this));
        final JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(actionEvent -> promptFilter());

        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        table.setShowHorizontalLines(false);
        table.setShowVerticalLines(false);
        table.addMouseListener(new OpenClickAdapter(openButton));
        table.addKeyListener(new OpenKeyAdapter(openButton));

        JTextField searchField = new JTextField();
        searchField.addActionListener(actionEvent -> search(searchField.getText()));
        table.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_SLASH) {
                    searchField.requestFocusInWindow();
                    e.consume();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);

        JPanel tablePane = new JPanel();
        tablePane.setLayout(new BorderLayout());
        tablePane.add(scrollPane, BorderLayout.CENTER);
        tablePane.add(searchField, BorderLayout.PAGE_END);

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (collection instanceof Playlist)
            buttonPane.add(new JLabel("Playlist"));
        buttonPane.add(updateButton);
        buttonPane.add(openButton);
        buttonPane.add(filterButton);

        Container contentPane = getContentPane();
        contentPane.add(tablePane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_START);

        pack();
        setVisible(true);
        setSize(DIMENSION);
    }

    private void promptFilter()
    {
        if (CHOOSER.showOpenDialog(main) == JFileChooser.APPROVE_OPTION) {
            model.filter(CHOOSER.getSelectedFile());
        }
    }

    private void search(String pattern)
    {
        int index = model.findByName(pattern, table.getSelectedRow() + 1);
        if (index != -1) {
            table.setRowSelectionInterval(index, index);
            table.scrollRectToVisible(table.getCellRect(index, 0, true));
        }
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
