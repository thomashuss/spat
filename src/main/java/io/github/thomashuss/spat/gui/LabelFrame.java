package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Label;
import io.github.thomashuss.spat.library.LibraryResource;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;

class LabelFrame
        extends NodeFrame
{
    private static final Dimension DIMENSION = new Dimension(350, 70);
    private final Label label;

    public LabelFrame(MainGUI main, Label label)
    {
        super(main, label.getName());
        this.label = label;

        final JPanel headerPanel = new JPanel();
        headerPanel.setBorder(HEADER_BORDER);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        nameLabel = new JLabel(label.getName());
        nameLabel.setFont(new Font(nameLabel.getFont().getName(), Font.BOLD, 18));
        headerPanel.add(getLeftAligned(nameLabel));

        Container contentPane = getContentPane();
        contentPane.add(headerPanel, BorderLayout.PAGE_START);

        pack();
        setVisible(true);
        setSize(DIMENSION);
    }

    @Override
    public void update()
    {
    }

    @Override
    public LibraryResource getResource()
    {
        return label;
    }
}
