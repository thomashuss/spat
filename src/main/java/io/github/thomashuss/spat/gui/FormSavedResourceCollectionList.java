package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.library.AbstractSpotifyResource;
import io.github.thomashuss.spat.library.SavedResourceCollection;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

class FormSavedResourceCollectionList<T extends AbstractSpotifyResource>
        extends JPanel
{
    private static final Dimension LIST_SIZE = new Dimension(200, 80);
    final DefaultListModel<SavedResourceCollection<T>> listModel;

    FormSavedResourceCollectionList(String name, SavedResourceCollection<T> initial)
    {
        super();
        listModel = new DefaultListModel<>();
        listModel.addElement(initial);
        final JList<SavedResourceCollection<T>> srcList = new JList<>(listModel);
        srcList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        srcList.setLayoutOrientation(JList.VERTICAL);
        srcList.setVisibleRowCount(-1);
        srcList.setDragEnabled(true);
        srcList.setDropMode(DropMode.INSERT);
        srcList.setTransferHandler(SavedResourceCollectionTransferHandler.of(listModel, initial));
        final JScrollPane srcListScrollPane = new JScrollPane(srcList);
        srcListScrollPane.setPreferredSize(LIST_SIZE);
        srcListScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        setLayout(new BorderLayout());

        final JPanel srcListButtonPane = new JPanel();
        srcListButtonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        JButton inputRemoveButton = new JButton("Remove");
        srcListButtonPane.add(inputRemoveButton);

        add(srcListScrollPane, BorderLayout.CENTER);
        add(srcListButtonPane, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createTitledBorder(name));
    }
}
