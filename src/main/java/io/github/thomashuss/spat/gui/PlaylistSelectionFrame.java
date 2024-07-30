package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Playlist;

import javax.swing.AbstractListModel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Set;

class PlaylistSelectionFrame
        extends SubFrame
{
    private static final Dimension DIMENSION = new Dimension(400, 200);
    private final PlaylistListListModel model;
    private final JList<Playlist> list;

    public PlaylistSelectionFrame(MainGUI main)
    {
        super(main, "Playlists");
        setFrameIcon(UIManager.getIcon("FileChooser.listViewIcon"));

        model = new PlaylistListListModel();
        list = new JList<>(model);
        new NavigateListAction<>(list, getInputMap(), getActionMap());

        JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JButton updateButton = new APIButton("Refresh");
        updateButton.addActionListener(actionEvent -> model.populate());
        buttonPane.add(updateButton);
        final JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(actionEvent -> removePlaylist(list.getSelectedIndex()));
        buttonPane.add(removeButton);
        final JButton openButton = new JButton("Open");
        openButton.addActionListener(actionEvent -> main.desktopPane.openFrameForResource(list.getSelectedValue(), this));
        buttonPane.add(openButton);

        list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.addMouseListener(new OpenClickAdapter(openButton));
        list.addKeyListener(new OpenKeyAdapter(openButton));
        JScrollPane listScrollPane = new JScrollPane(list);
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(listScrollPane);

        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_START);

        pack();
        setVisible(true);
        setSize(DIMENSION);
    }

    private void removePlaylist(int i)
    {
        if (i == -1) return;
        int response = JOptionPane.showInternalConfirmDialog(this,
                "Also remove the playlist from your Spotify library?",
                "Remove playlist",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (response != JOptionPane.CANCEL_OPTION) {
            model.clearPlaylistAt(i);
        }
    }

    /**
     * A list model for lists of lists.  Lists listing lists listed in this list model will always list the lists that
     * are listed in the backing list of lists, but the listings of those listed lists will not be listed.
     */
    private class PlaylistListListModel
            extends AbstractListModel<Playlist>
    {
        private List<Playlist> playlists;
        private boolean updating = false;

        private PlaylistListListModel()
        {
            playlists = main.library.getPlaylists();
        }

        @Override
        public int getSize()
        {
            return updating ? 0 : playlists.size();
        }

        @Override
        public Playlist getElementAt(int i)
        {
            return playlists.get(i);
        }

        private void populate()
        {
            if (!playlists.isEmpty())
                fireIntervalRemoved(this, 0, playlists.size() - 1);
            updating = true;
            new PlaylistDownloaderWorker().execute();
        }

        private void updateOtherModel(Playlist p)
        {
            main.desktopPane.updateComponentsForResource(p);
        }

        private void clearPlaylistAt(int i)
        {
            Playlist p = playlists.get(i);
            synchronized (main.client) {
                p.clearResources();
                main.library.markModified(p);
            }
            updateOtherModel(p);
        }

        private class PlaylistDownloaderWorker
                extends APICollectionMutatorWorker<Playlist>
        {
            public PlaylistDownloaderWorker()
            {
                super(PlaylistSelectionFrame.this.main, PlaylistSelectionFrame.this.main.client::updateMyPlaylists);
            }

            @Override
            protected void onTaskSuccess(Set<Playlist> deleted)
            {
                updating = false;
                playlists = main.library.getPlaylists();
                fireIntervalAdded(PlaylistListListModel.this, 0, playlists.size() - 1);
                ResourceFrame frame;
                for (Playlist d : deleted) {
                    frame = main.desktopPane.getFrameForResource(d);
                    if (frame != null) frame.doDefaultCloseAction();
                }
            }
        }
    }
}
