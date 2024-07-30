package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Artist;
import io.github.thomashuss.spat.library.LibraryResource;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

class ArtistFrame
        extends NodeFrame
{
    private static final Dimension DIMENSION = new Dimension(400, 170);
    private final Artist artist;
    private final Box genresBox;

    public ArtistFrame(MainGUI main, Artist artist)
    {
        super(main, artist.getName());
        this.artist = artist;

        final JPanel headerPanel = new JPanel();
        headerPanel.setBorder(HEADER_BORDER);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        nameLabel = new JLabel(artist.getName());
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.BOLD, 18));
        headerPanel.add(getLeftAligned(nameLabel));
        headerPanel.add(Box.createVerticalStrut(5));

        genresBox = Box.createHorizontalBox();
        addResourceLabelsToBox(artist.getGenres(), genresBox);
        headerPanel.add(genresBox);
        headerPanel.add(Box.createVerticalStrut(5));

        headerPanel.add(new JSeparator());

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Statistics", new SpotifyMetadataPanel());
        ImagePanel imagePanel = new ImagePanel();
        tabbedPane.addTab("Photo", imagePanel);
        tabbedPane.addChangeListener(changeEvent -> {
            if (imagePanel.equals(tabbedPane.getSelectedComponent())) {
                imagePanel.loadImage(artist.getImages());
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
        setTitle(artist.getName());
        nameLabel.setText(artist.getName());
        genresBox.removeAll();
        updateResourceLabelsInBox(artist.getGenres(), genresBox);
    }

    @Override
    public LibraryResource getResource()
    {
        return artist;
    }

    private class SpotifyMetadataPanel
            extends JPanel
            implements ResourceComponent
    {
        private final JLabel popularityLabel;
        private final JLabel followersLabel;

        private SpotifyMetadataPanel()
        {
            super();
            main.desktopPane.trackComponentForResource(this, artist);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            Box box = Box.createHorizontalBox();
            JButton updateButton = new APIButton("Update");
            updateButton.addActionListener(actionEvent -> new APIUpdateFunctionWorker<>(main, main.client::updateArtist, artist).execute());
            updateButton.setToolTipText("Update name, genres, popularity, follower count and photo.");
            box.add(updateButton);
            box.add(createOpenButton());
            box.add(Box.createHorizontalGlue());
            add(box);

            box = Box.createHorizontalBox();
            box.add(new JLabel("Popularity: "));
            box.add(popularityLabel = new JLabel(String.valueOf(artist.getPopularity())));
            box.add(Box.createHorizontalGlue());
            add(box);

            box = Box.createHorizontalBox();
            box.add(new JLabel("Followers: "));
            box.add(followersLabel = new JLabel(String.valueOf(artist.getFollowers())));
            box.add(Box.createHorizontalGlue());
            add(box);
        }

        @Override
        public void update()
        {
            popularityLabel.setText(String.valueOf(artist.getPopularity()));
            followersLabel.setText(String.valueOf(artist.getFollowers()));
        }

        @Override
        public LibraryResource getResource()
        {
            return artist;
        }
    }
}
