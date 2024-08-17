package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.Library;
import io.github.thomashuss.spat.library.LibraryResource;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Iterator;

class LibraryCleanupFrame
        extends JInternalFrame
{
    private static final Dimension SPACER = new Dimension(0, 5);
    private static final Dimension LIST_SIZE = new Dimension(250, 120);
    private final CleanupListModel toRemoveModel;
    private final DefaultListModel<LibraryResource> recoveredModel;
    private final JList<LibraryResource> toRemoveList;
    private final Library.Cleanup cleanup;
    private final MainGUI main;

    public LibraryCleanupFrame(MainGUI main)
    {
        super("Library Cleanup", true, true, true, false);
        this.main = main;
        synchronized (main.client) {
            this.cleanup = main.library.cleanUnusedResources();
        }

        toRemoveModel = new CleanupListModel();
        toRemoveList = new JList<>(toRemoveModel);
        toRemoveList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        toRemoveList.setLayoutOrientation(JList.VERTICAL);
        toRemoveList.setVisibleRowCount(-1);
        JScrollPane listScrollPane = new JScrollPane(toRemoveList);
        listScrollPane.setPreferredSize(LIST_SIZE);
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        recoveredModel = new DefaultListModel<>();
        JList<LibraryResource> recoveredList = new JList<>(recoveredModel);
        recoveredList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        recoveredList.setLayoutOrientation(JList.VERTICAL);
        recoveredList.setVisibleRowCount(-1);
        JScrollPane recoveredListScrollPane = new JScrollPane(recoveredList);
        recoveredListScrollPane.setPreferredSize(LIST_SIZE);
        recoveredListScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        final JButton updateButton = new JButton("Clean");
        updateButton.addActionListener(actionEvent -> cleanAndClose());
        final JButton closeButton = new JButton("Cancel");
        closeButton.addActionListener(actionEvent -> doDefaultCloseAction());
        final JButton keepButton = new JButton("Keep");
        keepButton.addActionListener(actionEvent -> toRemoveModel.removeElementAt(toRemoveList.getSelectedIndex()));

        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(new JLabel("The following resources are no longer needed and slated for removal from the library:"));
        listPane.add(Box.createRigidArea(SPACER));
        listPane.add(listScrollPane);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        listPane.add(new JLabel("The following resources are no longer needed, but will be kept:"));
        listPane.add(Box.createRigidArea(SPACER));
        listPane.add(recoveredListScrollPane);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BorderLayout());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel actionButtonPane = new JPanel();
        actionButtonPane.setLayout(new BoxLayout(actionButtonPane, BoxLayout.PAGE_AXIS));
        actionButtonPane.add(updateButton);
        actionButtonPane.add(Box.createRigidArea(SPACER));
        actionButtonPane.add(closeButton);
        JPanel resourcePane = new JPanel();
        resourcePane.setLayout(new BoxLayout(resourcePane, BoxLayout.PAGE_AXIS));
        resourcePane.add(keepButton);
        buttonPane.add(actionButtonPane, BorderLayout.PAGE_END);
        buttonPane.add(resourcePane, BorderLayout.PAGE_START);

        Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.LINE_END);

        pack();
        setVisible(true);
    }

    private void cleanAndClose()
    {
        synchronized (main.client) {
            cleanup.clean();
        }
        ResourceFrame frame;
        for (int i = 0; i < toRemoveModel.size(); i++) {
            frame = main.desktopPane.getFrameForResource(toRemoveModel.get(i));
            if (frame != null) frame.doDefaultCloseAction();
        }
        doDefaultCloseAction();
    }

    private class CleanupListModel
            extends DefaultListModel<LibraryResource>
    {
        public CleanupListModel()
        {
            super();
            cleanup.forEachResource(this::addElement);
        }

        @Override
        public void removeElementAt(int i)
        {
            LibraryResource resource = remove(i);
            Iterator<LibraryResource> recovered = cleanup.keep(resource).iterator();
            recoveredModel.addElement(resource);
            if (recovered.hasNext()) recovered.next();
            while (recovered.hasNext()) {
                resource = recovered.next();
                removeElement(resource);
                recoveredModel.addElement(resource);
            }
        }
    }
}
