package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Album;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Track;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

class AlbumFrame
        extends NodeFrame
{
    private static final Dimension DIMENSION = new Dimension(400, 270);
    private final DefaultListModel<Track> trackModel;
    private final Album album;
    private final Box artistBox;
    private final Box genresBox;
    private final ResourceLabel labelLabel;

    public AlbumFrame(MainGUI main, Album album)
    {
        super(main, album.getName());
        this.album = album;

        final JPanel headerPanel = new JPanel();
        headerPanel.setBorder(HEADER_BORDER);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        nameLabel = new JLabel(album.getName());
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.ITALIC, 18));
        headerPanel.add(getLeftAligned(nameLabel));
        headerPanel.add(Box.createVerticalStrut(5));

        artistBox = Box.createHorizontalBox();
        addResourceLabelsToBox(album.getArtists(), artistBox);
        headerPanel.add(artistBox);
        headerPanel.add(Box.createVerticalStrut(5));

        genresBox = Box.createHorizontalBox();
        addResourceLabelsToBox(album.getGenres(), genresBox);
        headerPanel.add(genresBox);
        headerPanel.add(Box.createVerticalStrut(5));

        if (album.getLabel() == null) labelLabel = new ResourceLabel();
        else labelLabel = new ResourceLabel(album.getLabel());
        headerPanel.add(getLeftAligned(labelLabel));

        headerPanel.add(new JSeparator());

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel tracksPanel = new JPanel(new BorderLayout());
        trackModel = new DefaultListModel<>();
        Track[] tracks = album.getTracks();
        if (tracks != null) trackModel.addAll(List.of(tracks));
        JList<Track> trackList = new JList<>(trackModel);
        JScrollPane trackListScrollPane = new JScrollPane(trackList);

        JButton updateButton = new APIButton("Update");
        updateButton.addActionListener(actionEvent -> new APIUpdateFunctionWorker<>(main, main.client::updateAlbum, album).execute());
        JButton openTrackButton = new JButton("Open Track");
        openTrackButton.addActionListener(actionEvent -> main.desktopPane.openFrameForResource(trackList.getSelectedValue(), this));
        trackList.addKeyListener(new OpenKeyAdapter(openTrackButton));
        trackList.addMouseListener(new OpenClickAdapter(openTrackButton));

        Box box = Box.createHorizontalBox();
        box.add(updateButton);
        box.add(createOpenButton());
        box.add(openTrackButton);
        box.add(Box.createHorizontalGlue());
        tracksPanel.add(box, BorderLayout.PAGE_START);
        tracksPanel.add(trackListScrollPane, BorderLayout.CENTER);
        tabbedPane.addTab("Tracks", tracksPanel);

        tabbedPane.addTab("Metadata", new SpotifyMetadataPanel());

        ImagePanel imagePanel = new ImagePanel();
        tabbedPane.addTab("Photo", imagePanel);
        tabbedPane.addChangeListener(changeEvent -> {
            if (imagePanel.equals(tabbedPane.getSelectedComponent())) {
                imagePanel.loadImage(album.getImages());
            }
        });

        Container contentPane = getContentPane();
        contentPane.add(headerPanel, BorderLayout.PAGE_START);
        contentPane.add(tabbedPane, BorderLayout.CENTER);

        pack();
        setVisible(true);
        setSize(DIMENSION);
    }

    @Override
    public void update()
    {
        setTitle(album.getName());
        nameLabel.setText(album.getName());
        updateResourceLabelsInBox(album.getArtists(), artistBox);
        updateResourceLabelsInBox(album.getGenres(), genresBox);
        labelLabel.setResource(album.getLabel());
        trackModel.clear();
        Track[] tracks = album.getTracks();
        if (tracks != null) trackModel.addAll(List.of(tracks));
    }

    @Override
    public LibraryResource getResource()
    {
        return album;
    }

    private class SpotifyMetadataPanel
            extends JPanel
            implements ResourceComponent
    {
        private final JLabel popularityLabel;
        private final JLabel releaseDateLabel;

        private SpotifyMetadataPanel()
        {
            super();
            main.desktopPane.trackComponentForResource(this, album);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            Box box = Box.createHorizontalBox();
            box.add(new JLabel("Release Date: "));
            box.add(releaseDateLabel = new JLabel(String.valueOf(album.getReleaseDate())));
            box.add(Box.createHorizontalGlue());
            add(box);

            box = Box.createHorizontalBox();
            box.add(new JLabel("Popularity: "));
            box.add(popularityLabel = new JLabel(String.valueOf(album.getPopularity())));
            box.add(Box.createHorizontalGlue());
            add(box);
        }

        @Override
        public void update()
        {
            popularityLabel.setText(String.valueOf(album.getPopularity()));
            releaseDateLabel.setText(String.valueOf(album.getReleaseDate()));
        }

        @Override
        public LibraryResource getResource()
        {
            return album;
        }
    }
}
