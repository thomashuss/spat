package io.github.thomashuss.spat.gui;

import io.github.thomashuss.spat.client.SpotifyClientException;

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
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.List;

class EditPushFrame
        extends JInternalFrame
{
    private static final Dimension LIST_SIZE = new Dimension(250, 120);
    private final MainGUI main;
    private final DefaultListModel<String> listModel;
    private final JButton cancelButton;
    private final JButton pushButton;

    EditPushFrame(MainGUI main)
    {
        super("Synchronize changes", false, true, false, false);
        this.main = main;
        listModel = new DefaultListModel<>();
        final JList<String> list = new JList<>(listModel);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        final JScrollPane listScrollPane = new JScrollPane(list);
        listScrollPane.setPreferredSize(LIST_SIZE);
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);

        final JPanel listPane = new JPanel();
        listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
        listPane.add(new JLabel("Push these changes?"));
        listPane.add(Box.createRigidArea(MainGUI.SPACER));
        listPane.add(listScrollPane);
        listPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        cancelButton = new JButton("No");
        cancelButton.addActionListener(actionEvent -> doDefaultCloseAction());
        pushButton = new JButton("Yes");
        pushButton.addActionListener(actionEvent -> doPush());

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        buttonPane.add(cancelButton);
        buttonPane.add(pushButton);

        final Container contentPane = getContentPane();
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.PAGE_END);

        pack();
    }

    void prompt()
    {
        main.editTracker.forEach((e) -> listModel.addElement(e.toString()));
        setState(true);
        setLocation((main.desktopPane.getWidth() - getWidth()) / 2,
                (main.desktopPane.getHeight() - getHeight()) / 2);
        setVisible(true);
    }

    private void setState(boolean b)
    {
        cancelButton.setEnabled(b);
        pushButton.setEnabled(b);
    }

    private void doPush()
    {
        setState(false);
        new EditPushWorker().execute();
    }

    private class EditPushWorker
            extends APIWorker<Void, Integer>
    {
        private final int last = listModel.size() - 1;

        private EditPushWorker()
        {
            super(EditPushFrame.this.main);
        }

        @Override
        protected void done()
        {
            try {
                super.done();
            } finally {
                main.updateEditControls();
            }
        }

        @Override
        protected void process(List<Integer> chunks)
        {
            for (int i : chunks) {
                if (i == last) {
                    listModel.clear();
                    doDefaultCloseAction();
                    break;
                } else {
                    listModel.setElementAt(listModel.get(i) + " [done]", i);
                }
            }
        }

        @Override
        protected Void doTask()
        throws SpotifyClientException, IOException, InterruptedException
        {
            synchronized (main.client) {
                main.editTracker.pushAll(main.client, this, this::publish);
            }
            return null;
        }
    }
}
