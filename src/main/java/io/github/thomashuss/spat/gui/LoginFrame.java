package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.SpotifyClientException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

class LoginFrame
        extends JInternalFrame
{
    private final MainGUI main;
    private final JTextField callbackUriField;
    private final JButton goBtn;

    LoginFrame(MainGUI main)
    {
        super("Authenticate with Spotify", false, true, false, false);
        setFrameIcon(null);
        this.main = main;

        URI loginRedirect;
        synchronized (main.client) {
            loginRedirect = main.client.getLoginRedirect();
        }

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JPanel loginPane = new JPanel();
        loginPane.setLayout(new BorderLayout());
        loginPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel loginLabel = new JLabel("<HTML><FONT COLOR=\"#2D61BB\">" +
                "<U>Click here</U></FONT> to log in to your Spotify account.  After granting<BR/>" +
                "permissions, copy and paste the URL in your browser's<BR/>" +
                "address bar into the text box.</HTML>");
        loginLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                main.openUri(loginRedirect);
            }
        });
        loginPane.add(loginLabel, BorderLayout.PAGE_START);

        JPanel callbackPane = new JPanel();

        callbackUriField = new JTextField(20);
        loginLabel.setLabelFor(callbackUriField);
        callbackPane.add(callbackUriField);

        goBtn = new JButton("Authenticate");
        goBtn.addActionListener(actionEvent -> {
            try {
                new AuthWorker(new URI(callbackUriField.getText())).execute();
                goBtn.setEnabled(false);
            } catch (URISyntaxException e) {
                JOptionPane.showMessageDialog(this.main, "Improperly formatted callback URI.",
                        "Authentication error", JOptionPane.ERROR_MESSAGE);
            }
        });
        callbackUriField.addActionListener(actionEvent -> goBtn.doClick());
        callbackPane.add(goBtn);

        loginPane.add(callbackPane, BorderLayout.CENTER);
        setContentPane(loginPane);
        pack();
        setVisible(true);
    }

    private class AuthWorker
            extends SwingWorker<Void, Void>
    {
        private final URI callbackUri;

        AuthWorker(URI callbackUri)
        {
            this.callbackUri = callbackUri;
        }

        @Override
        protected Void doInBackground()
        throws IOException, SpotifyClientException
        {
            synchronized (main.client) {
                main.client.loginCallback(callbackUri);
            }
            return null;
        }

        @Override
        protected void done()
        {
            try {
                get();
                LoginFrame.this.doDefaultCloseAction();
            } catch (ExecutionException e) {
                goBtn.setEnabled(true);
                JOptionPane.showMessageDialog(main, "There was a problem authenticating with Spotify:\n\n"
                        + e.getCause().getMessage(), "Authentication error", JOptionPane.ERROR_MESSAGE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
