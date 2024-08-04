package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AudioFeatures;
import io.github.thomashuss.spat.library.LibraryResource;
import io.github.thomashuss.spat.library.Track;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;

class TrackFrame
        extends NodeFrame
{
    private static final String PLAY = "⏵";
    private static final String STOP = "⏹";
    private static final Dimension DIMENSION = new Dimension(500, 200);
    private final Track track;
    private final ResourceLabel albumLabel;
    private final Box artistBox;
    private final FeaturesTableModel tableModel;

    public TrackFrame(MainGUI main, Track track)
    {
        super(main, track.getName());
        this.track = track;

        final JPanel headerPanel = new JPanel();
        headerPanel.setBorder(HEADER_BORDER);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        Box box = Box.createHorizontalBox();
        nameLabel = new JLabel('"' + track.getName() + '"');
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.BOLD, 18));
        box.add(nameLabel);
        box.add(Box.createHorizontalStrut(HEADER_MARGIN));
        box.add(new PreviewButton());
        box.add(Box.createHorizontalGlue());
        headerPanel.add(box);
        headerPanel.add(Box.createVerticalStrut(5));

        artistBox = Box.createHorizontalBox();
        addResourceLabelsToBox(track.getArtists(), artistBox);
        headerPanel.add(artistBox);
        headerPanel.add(Box.createVerticalStrut(5));

        albumLabel = new ResourceLabel(track.getAlbum());
        final Font defaultFont = albumLabel.getFont();
        albumLabel.setFont(new Font(defaultFont.getName(), Font.ITALIC, defaultFont.getSize()));
        headerPanel.add(getLeftAligned(albumLabel));

        headerPanel.add(new JSeparator());

        JTabbedPane tabbedPane = new JTabbedPane();
        JPanel metadataPanel = new JPanel();
        metadataPanel.setLayout(new BorderLayout());
        box = Box.createHorizontalBox();

        JButton updateMetadataButton = new APIButton("Update Metadata");
        updateMetadataButton.setToolTipText("Update name, artists, album, duration and popularity.");
        updateMetadataButton.addActionListener(actionEvent -> new APIUpdateFunctionWorker<>(main, main.client::updateTrack, track).execute());
        box.add(updateMetadataButton);

        JButton updateFeaturesButton = new APIButton("Update Features");
        updateFeaturesButton.addActionListener(actionEvent -> new APIUpdateFunctionWorker<>(main, main.client::updateAudioFeaturesForTrack, track).execute());
        updateFeaturesButton.setToolTipText("Update Spotify's generated audio features.");
        box.add(updateFeaturesButton);

        box.add(createOpenButton());
        box.add(Box.createHorizontalGlue());
        metadataPanel.add(box, BorderLayout.PAGE_START);

        tableModel = new FeaturesTableModel();
        JTable table = new JTable(tableModel);
        table.setTableHeader(null);
        table.setPreferredScrollableViewportSize(new Dimension(500, 20));
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        metadataPanel.add(scrollPane, BorderLayout.CENTER);

        tabbedPane.addTab("Metadata", metadataPanel);

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
        setTitle(track.getName());
        nameLabel.setText('"' + track.getName() + '"');
        updateResourceLabelsInBox(track.getArtists(), artistBox);
        albumLabel.setResource(track.getAlbum());
        tableModel.fireTableDataChanged();
    }

    @Override
    public LibraryResource getResource()
    {
        return track;
    }

    private class PreviewButton
            extends APIButton
    {
        protected PreviewButton()
        {
            super(PLAY);
            addActionListener(actionEvent -> playPause());
            addInternalFrameListener(new InternalFrameAdapter()
            {
                @Override
                public void internalFrameClosed(InternalFrameEvent e)
                {
                    stopPreview();
                    setText(PLAY);
                }
            });
        }

        private void stopPreview()
        {
            main.stopPreview(track);
        }

        private void startPreview()
        {
            main.previewTrack(track);
        }

        private void playPause()
        {
            if (main.getPreviewingTrack() == track) {
                stopPreview();
            } else {
                startPreview();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent event)
        {
            super.propertyChange(event);
            if (MainGUI.PREVIEWING_TRACK_KEY.equals(event.getPropertyName())) {
                if (event.getOldValue() == track) {
                    setText(PLAY);
                } else if (event.getNewValue() == track) {
                    setText(STOP);
                }
            }
        }
    }

    private class FeaturesTableModel
            extends AbstractTableModel
    {
        private static final String[] VALUE_NAMES = new String[]{
                "Duration",
                "Popularity",
                "Explicit",
                "Playable",
                "Acousticness",
                "Danceability",
                "Energy",
                "Instrumentalness",
                "Key",
                "Liveness",
                "Loudness",
                "Mode",
                "Speechiness",
                "Tempo",
                "Time Signature",
                "Valence"
        };

        @Override
        public int getRowCount()
        {
            return track.getFeatures() == null ? 4 : 16;
        }

        @Override
        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public Object getValueAt(int i, int i1)
        {
            if (i1 == 0) {
                return VALUE_NAMES[i];
            } else {
                return switch (i) {
                    case 0 -> {
                        int duration = track.getDuration();
                        yield (duration / 60000) + ":" + (((float) duration / 1000) % 60);
                    }
                    case 1 -> track.getPopularity();
                    case 2 -> track.isExplicit();
                    case 3 -> track.isPlayable();
                    default -> {
                        AudioFeatures features = track.getFeatures();
                        if (features == null)
                            yield null;
                        yield switch (i) {
                            case 4 -> features.getAcousticness();
                            case 5 -> features.getDanceability();
                            case 6 -> features.getEnergy();
                            case 7 -> features.getInstrumentalness();
                            case 8 -> features.getKey();
                            case 9 -> features.getLiveness();
                            case 10 -> features.getLoudness();
                            case 11 -> features.isMajor() ? "Major" : "Minor";
                            case 12 -> features.getSpeechiness();
                            case 13 -> features.getTempo();
                            case 14 -> features.getTimeSignature() + "/4";
                            case 15 -> features.getValence();
                            default -> null;
                        };
                    }
                };
            }
        }
    }
}
