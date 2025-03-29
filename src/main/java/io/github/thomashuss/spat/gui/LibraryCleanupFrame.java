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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Iterator;

class LibraryCleanupFrame
        extends JInternalFrame
{
    private static final Dimension LIST_SIZE = new Dimension(250, 120);
    private final CleanupListModel toRemoveModel;
    private final DefaultListModel<LibraryResource> recoveredModel;
    private final JList<LibraryResource> toRemoveList;
    private final JProgressBar progressBar;
    private final JButton updateButton;
    private final JButton closeButton;
    private final JButton keepButton;
    private Library.Cleanup cleanup;
    private final MainGUI main;

    public LibraryCleanupFrame(MainGUI main)
    {
        super("Cleanup", true, true, true, false);
        this.main = main;

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
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);

        updateButton = new JButton("Clean");
        updateButton.addActionListener(actionEvent -> cleanAndClose());
        closeButton = new JButton("Cancel");
        closeButton.addActionListener(actionEvent -> doDefaultCloseAction());
        keepButton = new JButton("Keep");
        keepButton.addActionListener(actionEvent -> toRemoveModel.removeElementAt(toRemoveList.getSelectedIndex()));
        updateButton.setEnabled(false);
        closeButton.setEnabled(false);
        keepButton.setEnabled(false);

        JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(new JLabel("The following resources are no longer needed and slated for removal from the library:"));
        listPane.add(Box.createRigidArea(MainGUI.H_SPACER));
        listPane.add(listScrollPane);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        listPane.add(new JLabel("The following resources are no longer needed, but will be kept:"));
        listPane.add(Box.createRigidArea(MainGUI.H_SPACER));
        listPane.add(recoveredListScrollPane);
        listPane.add(Box.createRigidArea(MainGUI.H_SPACER));
        listPane.add(progressBar);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BorderLayout());
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel actionButtonPane = new JPanel();
        actionButtonPane.setLayout(new BoxLayout(actionButtonPane, BoxLayout.PAGE_AXIS));
        actionButtonPane.add(updateButton);
        actionButtonPane.add(Box.createRigidArea(MainGUI.H_SPACER));
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

        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground()
            {
                synchronized (LibraryCleanupFrame.this.main.client) {
                    cleanup = LibraryCleanupFrame.this.main.library.cleanUnusedResources();
                }
                return null;
            }

            @Override
            protected void done()
            {
                toRemoveModel.populate();
                progressBar.setIndeterminate(false);
                if (toRemoveModel.isEmpty()) {
                    JOptionPane.showInternalMessageDialog(LibraryCleanupFrame.this,
                            "Nothing to clean.", "Cleanup", JOptionPane.INFORMATION_MESSAGE);
                    doDefaultCloseAction();
                } else {
                    updateButton.setEnabled(true);
                    closeButton.setEnabled(true);
                    keepButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void cleanAndClose()
    {
        progressBar.setIndeterminate(true);
        updateButton.setEnabled(false);
        closeButton.setEnabled(false);
        keepButton.setEnabled(false);
        new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground()
            {
                synchronized (main.client) {
                    cleanup.clean();
                }
                return null;
            }

            @Override
            protected void done()
            {
                ResourceFrame frame;
                for (int i = 0; i < toRemoveModel.size(); i++) {
                    frame = main.desktopPane.getFrameForResource(toRemoveModel.get(i));
                    if (frame != null) frame.doDefaultCloseAction();
                }
                doDefaultCloseAction();
            }
        }.execute();
    }

    private class CleanupListModel
            extends DefaultListModel<LibraryResource>
    {
        public CleanupListModel()
        {
            super();
        }

        private void populate()
        {
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
