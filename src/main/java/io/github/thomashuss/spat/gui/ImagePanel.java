package io.github.thomashuss.spat.gui;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.net.URL;

class ImagePanel
        extends JPanel
{
    private final JLabel imageLabel;
    private URL currentUrl;

    ImagePanel()
    {
        super();
        setLayout(new BorderLayout());
        add(imageLabel = new JLabel(), BorderLayout.CENTER);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setHorizontalTextPosition(JLabel.CENTER);
        imageLabel.setVerticalTextPosition(JLabel.CENTER);
    }

    void loadImage(URL[] images)
    {
        if (images != null) {
            URL imageUrl = images[0];
            if (currentUrl != imageUrl) {
                imageLabel.setIcon(new ImageIcon(currentUrl = imageUrl));
            }
        }
    }
}

