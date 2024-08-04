package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.Spat;
import io.github.thomashuss.spat.client.SpotifyClient;
import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.SaveDirectory;
import io.github.thomashuss.spat.library.SaveFileException;
import io.github.thomashuss.spat.library.Track;
import javazoom.jl.player.Player;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.Box;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;

public class MainGUI
        extends JFrame
        implements PropertyChangeListener
{
    public static final String HAS_AUTH_KEY = "hasAuth";
    public static final String HAS_LIBRARY_KEY = "hasLibrary";
    public static final String PREVIEWING_TRACK_KEY = "previewingTrack";
    private static final Dimension DESKTOP_DIMENSION = new Dimension(1000, 800);
    final ResourceDesktopPane desktopPane;
    /**
     * Synchronize library operations on this object.
     */
    final SpotifyClient client;
    private final Desktop desktop;
    private final JProgressBar statusProgressBar;
    private final PropertyChangeSupport statePcs;
    Library library;
    private File saveDirectory;
    private FrameCheckbox playlistsCheckbox;
    private JFileChooser chooser;
    private LoginFrame loginFrame;
    private PreviewWorker previewWorker;

    public MainGUI()
    {
        super(Spat.PROGRAM_NAME);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        client = new SpotifyClient();
        statePcs = new PropertyChangeSupport(this);
        desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;

        desktopPane = new ResourceDesktopPane(this);
        desktopPane.putClientProperty("JDesktopPane.dragMode", "outline");
        desktopPane.setPreferredSize(DESKTOP_DIMENSION);
        desktopPane.setBackground(Color.DARK_GRAY);
        setContentPane(desktopPane);
        setJMenuBar(createMenuBar());

        addWindowListener(new MainWindowAdapter());

        JInternalFrame statusFrame = new JInternalFrame("Status", false, false, false, true);
        statusFrame.setFrameIcon(null);
        statusProgressBar = new JProgressBar();
        statusFrame.add(statusProgressBar);
        statusFrame.pack();
        statusFrame.show();
        desktopPane.add(statusFrame);
    }

    public static void createAndShowGUI()
    {
        MainGUI main = new MainGUI();
        main.pack();
        main.setVisible(true);
        main.loadDataDefault();
        main.initialSetup(false);
    }

    private void uriOpenFailed(URI uri)
    {
        JOptionPane.showInputDialog(this, "Copy and paste the link below into your browser:", uri);
    }

    public void openUri(URI uri)
    {
        if (desktop == null) uriOpenFailed(uri);
        else new URIOpenWorker(uri).execute();
    }

    public void addStatePropertyChangeListener(PropertyChangeListener listener)
    {
        statePcs.addPropertyChangeListener(listener);
    }

    public void removeStatePropertyChangeListener(PropertyChangeListener listener)
    {
        statePcs.removePropertyChangeListener(listener);
    }

    private void setLibrary(Library library)
    {
        statePcs.firePropertyChange(HAS_LIBRARY_KEY, this.library != null, library != null);
        client.setLibrary(this.library = library);
    }

    private void showLogin()
    {
        if (loginFrame == null) {
            desktopPane.openNewInternalFrame(loginFrame = new LoginFrame(this));
            loginFrame.addInternalFrameListener(new InternalFrameAdapter()
            {
                @Override
                public void internalFrameClosed(InternalFrameEvent e)
                {
                    firePropertyChange(HAS_AUTH_KEY, false, client.getToken() != null);
                    if (library == null)
                        openLibrary();
                }
            });
        } else if (!loginFrame.isVisible()) {
            loginFrame.show();
            desktopPane.add(loginFrame);
        }
    }

    private void close()
    {
        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    private void initialSetup(boolean shouldForce)
    {
        String clientId = shouldForce ? null : Spat.preferences.get(Spat.P_CLIENT_ID, null);
        if (clientId == null) {
            while (clientId == null) {
                clientId = JOptionPane.showInternalInputDialog(desktopPane,
                        "Create an app on the Spotify developer website, and enter the client ID here:",
                        "Initial setup", JOptionPane.QUESTION_MESSAGE);
                if (clientId == null) {
                    if (shouldForce)
                        return;
                    else
                        close();
                }
            }
        }

        URI redirectUri = null;
        String redirectUriString = shouldForce ? null : Spat.preferences.get(Spat.P_REDIRECT_URI, null);
        if (redirectUriString == null) {
            while (redirectUri == null) {
                try {
                    redirectUri = new URI(JOptionPane.showInternalInputDialog(desktopPane,
                            "Enter the redirect URI for the Spotify client.  This can just be a placeholder,\n" +
                                    "but it should be a valid URI that a web browser can redirect to.", "Initial setup", JOptionPane.QUESTION_MESSAGE));
                } catch (URISyntaxException ignored) {
                    JOptionPane.showInternalMessageDialog(desktopPane, "Enter a correct URI.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (NullPointerException ignored) {
                }
                if (redirectUri == null) {
                    if (shouldForce)
                        return;
                    else
                        close();
                }
            }
        } else {
            try {
                redirectUri = new URI(redirectUriString);
            } catch (URISyntaxException ignored) {
            }
        }

        synchronized (client) {
            client.setClientId(clientId);
            client.setRedirectUri(redirectUri);
        }
        Spat.preferences.put(Spat.P_CLIENT_ID, clientId);
        if (redirectUri != null) Spat.preferences.put(Spat.P_REDIRECT_URI, redirectUri.toString());

        if (shouldForce || client.getToken() == null) {
            statePcs.firePropertyChange(HAS_AUTH_KEY, true, false);
            showLogin();
        } else {
            statePcs.firePropertyChange(HAS_AUTH_KEY, false, true);
        }
    }

    public boolean hasAuth()
    {
        return client.getToken() != null;
    }

    private void loadDataDefault()
    {
        String path = Spat.preferences.get(Spat.P_FILE_PATH, null);
        if (path != null) {
            saveDirectory = new File(path);
            try {
                setLibrary(SaveDirectory.loadData(saveDirectory, client));
            } catch (SaveFileException | IOException ignored) {
            }
        }
    }

    private void saveDataDefault()
    {
        if (saveDirectory != null && library != null) {
            try {
                SaveDirectory.saveData(saveDirectory, client);
            } catch (SaveFileException | IOException ignored) {
            }
            if (library.hasModified()) {
                library.saveModified();
            }
        }
    }

    private void openLibrary()
    {
        JOptionPane.showMessageDialog(this,
                "Please select a new or existing folder for the library.");
        if (chooser == null) {
            chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                saveDirectory = chooser.getSelectedFile();
                Spat.preferences.put(Spat.P_FILE_PATH, saveDirectory.getAbsolutePath());
                Library lib = SaveDirectory.loadData(saveDirectory, client);
                if (lib == null) {
                    lib = SaveDirectory.createNewLibrary(saveDirectory);
                }
                setLibrary(lib);
            } catch (SaveFileException | IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Couldn't create new library.\n\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JMenuBar createMenuBar()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);
        JMenuItem openItem = new JMenuItem("Open", KeyEvent.VK_N);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        openItem.addActionListener(actionEvent -> openLibrary());
        openItem.setEnabled(false);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        JMenuItem quitItem = new JMenuItem("Quit", KeyEvent.VK_Q);
        quitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        quitItem.addActionListener(actionEvent -> close());
        fileMenu.add(quitItem);

        JMenu libraryMenu = new JMenu("Library");
        libraryMenu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(libraryMenu);
        JMenuItem cleanItem = new JMenuItem("Clean");
        cleanItem.addActionListener(actionEvent -> desktopPane.openNewInternalFrame(new LibraryCleanupFrame(this)));
        cleanItem.setEnabled(false);
        libraryMenu.add(cleanItem);

        JMenu spotifyMenu = new JMenu("Spotify");
        spotifyMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(spotifyMenu);
        JMenuItem authItem = new JMenuItem("Authenticate...", KeyEvent.VK_A);
        authItem.addActionListener(actionEvent -> showLogin());
        spotifyMenu.add(authItem);
        JMenuItem resetItem = new JMenuItem("Change client properties");
        resetItem.addActionListener(actionEvent -> initialSetup(true));
        spotifyMenu.add(resetItem);
        JMenuItem syncItem = new JMenuItem("Synchronize changes", KeyEvent.VK_R);
        syncItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        syncItem.setEnabled(false);
        spotifyMenu.add(syncItem);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        menuBar.add(viewMenu);
        playlistsCheckbox = new FrameCheckbox("Playlists")
        {
            @Override
            protected JInternalFrame createNewFrame()
            {
                return new PlaylistSelectionFrame(MainGUI.this);
            }
        };
        playlistsCheckbox.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(playlistsCheckbox);
        JCheckBoxMenuItem savedTracksCheckbox = new FrameCheckbox("Saved Tracks")
        {
            @Override
            protected JInternalFrame createNewFrame()
            {
                return new SavedTrackCollectionFrame(MainGUI.this, library.getLikedSongs());
            }
        };
        savedTracksCheckbox.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(savedTracksCheckbox);
        viewMenu.addSeparator();
        JCheckBoxMenuItem nodeCheckbox = new JCheckBoxMenuItem("Node view");
        nodeCheckbox.addItemListener(itemEvent -> {
            if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                desktopPane.enableNodeView();
            } else {
                desktopPane.disableNodeView();
            }
        });
        nodeCheckbox.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        viewMenu.add(nodeCheckbox);

        addStatePropertyChangeListener(event -> {
            String prop = event.getPropertyName();
            if (HAS_LIBRARY_KEY.equals(prop)) {
                boolean state = (Boolean) event.getNewValue();
                cleanItem.setEnabled(state);
                syncItem.setEnabled(state);
                playlistsCheckbox.setEnabled(state);
                savedTracksCheckbox.setEnabled(state);
            } else if (HAS_AUTH_KEY.equals(prop)) {
                boolean state = (Boolean) event.getNewValue();
                openItem.setEnabled(state);
            }
        });

        return menuBar;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if ("progress".equals(evt.getPropertyName())) {
            int progress = (Integer) evt.getNewValue();
            if (progress == 100) progress = 0;
            statusProgressBar.setValue(progress);
        }
    }

    Track getPreviewingTrack()
    {
        if (previewWorker != null && previewWorker.isPlaying) {
            return previewWorker.track;
        }
        return null;
    }

    void stopPreview()
    {
        if (previewWorker != null && previewWorker.isPlaying) {
            previewWorker.stop();
        }
        previewWorker = null;
    }

    void stopPreview(Track track)
    {
        if (previewWorker != null && previewWorker.track == track) {
            if (previewWorker.isPlaying) {
                previewWorker.stop();
            }
            previewWorker = null;
        }
    }

    void previewTrack(Track track)
    {
        stopPreview();
        previewWorker = new PreviewWorker(track);
        previewWorker.execute();
    }

    private abstract class FrameCheckbox
            extends JCheckBoxMenuItem
    {
        private JInternalFrame frame;

        private FrameCheckbox(String text)
        {
            super(text);
            addItemListener(itemEvent -> {
                if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
                    if (frame == null) {
                        frame = createNewFrame();
                        frame.addInternalFrameListener(new InternalFrameAdapter()
                        {
                            @Override
                            public void internalFrameClosed(InternalFrameEvent e)
                            {
                                setSelected(false);
                            }
                        });
                        desktopPane.openNewInternalFrame(frame);
                    } else {
                        desktopPane.showInternalFrame(frame);
                    }
                } else if (frame != null) {
                    frame.doDefaultCloseAction();
                }
            });
            setEnabled(false);
        }

        protected abstract JInternalFrame createNewFrame();
    }

    private class MainWindowAdapter
            extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            for (JInternalFrame frame : desktopPane.getAllFrames()) {
                frame.doDefaultCloseAction();
            }

            final JMenuBar menuBar = getJMenuBar();
            final int menuCount = menuBar.getMenuCount();
            for (int i = 0; i < menuCount; i++) {
                menuBar.getMenu(i).setEnabled(false);
            }

            desktopPane.add(new JInternalFrame("Wait...",
                    false, false, false, false)
            {
                {
                    setLayout(new FlowLayout());
                    Box box = Box.createVerticalBox();
                    box.add(new JLabel("Please wait while the library is saved."));
                    box.add(Box.createVerticalStrut(5));
                    JProgressBar progressBar = new JProgressBar();
                    progressBar.setIndeterminate(true);
                    box.add(progressBar);
                    add(box);
                    pack();
                    setLocation((desktopPane.getWidth() - getWidth()) / 2,
                            (desktopPane.getHeight() - getHeight()) / 2);
                    show();
                }
            });

            new SwingWorker<Void, Void>()
            {
                @Override
                protected Void doInBackground()
                {
                    saveDataDefault();
                    if (library != null) {
                        library.close();
                    }
                    try {
                        Spat.preferences.flush();
                    } catch (BackingStoreException ignored) {
                    }
                    return null;
                }

                @Override
                protected void done()
                {
                    dispose();
                }
            }.execute();
        }
    }

    private class URIOpenWorker
            extends SwingWorker<Void, Void>
    {
        private final URI uri;

        public URIOpenWorker(URI uri)
        {
            this.uri = uri;
        }

        @Override
        protected Void doInBackground()
        throws IOException
        {
            desktop.browse(uri);
            return null;
        }

        @Override
        protected void done()
        {
            try {
                get();
            } catch (ExecutionException | InterruptedException e) {
                uriOpenFailed(uri);
            }
        }
    }

    private class PreviewWorker
            extends SwingWorker<Void, Void>
    {
        private final Track track;
        private boolean isPlaying = false;
        private Player player;

        private PreviewWorker(Track track)
        {
            super();
            this.track = track;
        }

        @Override
        protected Void doInBackground()
        throws Exception
        {
            HttpsURLConnection conn = (HttpsURLConnection) track.getPreviewUrl().openConnection();
            try (InputStream is = conn.getInputStream()) {
                synchronized (this) {
                    player = new Player(is);
                }
                publish((Void) null);
                isPlaying = true;
                player.play();
            }
            return null;
        }

        private void stop()
        {
            synchronized (this) {
                if (player != null) {
                    player.close();
                }
            }
        }

        @Override
        protected void process(List<Void> chunks)
        {
            statePcs.firePropertyChange(PREVIEWING_TRACK_KEY, null, track);
        }

        @Override
        protected void done()
        {
            try {
                get();
            } catch (InterruptedException | CancellationException e) {
                stop();
            } catch (ExecutionException e) {
                JOptionPane.showInternalMessageDialog(MainGUI.this,
                        "There was an error playing the preview:\n\n" + e.getCause().getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            statePcs.firePropertyChange(PREVIEWING_TRACK_KEY, track, null);
            isPlaying = false;
        }
    }
}
