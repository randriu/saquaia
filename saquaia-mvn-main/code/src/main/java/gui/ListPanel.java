package gui;

import core.util.Vector;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;

/**
 * A panel containing a labeled list of elements that can be reordered.
 *
 * @author helfrich
 */
public class ListPanel extends JPanel {

    private boolean enabled = true;

    private final JScrollPane scoll_pane;
    private final JList list;
    private final DefaultListModel listModel;
    private final JPopupMenu popupMenu;
    private final ActionListener deleteListener, editListener, addListener, clearListener;
    private final JMenuItem deleteItem, editItem, addItem, clearItem;

    private ListChangeListener listener;

    private Font cellFont = null;
    private ListCellRenderer renderer;

    /**
     *
     * @param label The label for the list. If null, there will be no label.
     * @param rows The number of rows to be displayed.
     * @param values The elements of the list.
     */
    public ListPanel(String label, int rows, Object... values) {
        super();
        this.listModel = new DefaultListModel();
        setValues(values);

        list = new JList(listModel);
        list.setDragEnabled(true);
        list.setDropMode(DropMode.ON);
        list.setTransferHandler(new ListTransferHandler());
        list.setVisibleRowCount(rows);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        renderer = new ListCellRenderer() {
            ListCellRenderer original = list.getCellRenderer();

            @Override
            public Component getListCellRendererComponent(JList jlist, Object e, int i, boolean bln, boolean bln1) {
                Component res = original.getListCellRendererComponent(jlist, e, i, bln, bln1);
                if (cellFont != null) {
                    try {
                        JLabel x = (JLabel) res;
                        x.setFont(cellFont);
                    } catch (ClassCastException ex) {
                        ex.printStackTrace();
                    }
                }
                return res;
            }
        };
        list.setCellRenderer(renderer);

        scoll_pane = new JScrollPane(list);
//        Dimension prefSize = list.getPreferredScrollableViewportSize();
//        prefSize.width = list.getPreferredSize().width;
//        scoll_pane.setPreferredSize(prefSize);

        deleteItem = new JMenuItem("Delete");
        deleteListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ev) {
                startDeleteDialog();
            }
        };
        deleteItem.addActionListener(deleteListener);
        addItem = new JMenuItem("Add");
        addListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startAddDialog();
            }
        };
        addItem.addActionListener(addListener);
        editItem = new JMenuItem("Edit");
        editListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startEditDialog();
            }
        };
        editItem.addActionListener(editListener);
        clearItem = new JMenuItem("Clear");
        clearListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startClearDialog();
            }
        };
        clearItem.addActionListener(clearListener);

        // popup menu
        popupMenu = new JPopupMenu();
        // double click
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (list.getSelectedIndex() == -1) {
                    return;
                }
                if (evt.getClickCount() == 2) {
                    boolean continueWithEdit = listener.onDoubleClick(list.getSelectedIndex());
                    if (continueWithEdit) startEditDialog();
                }
            }
        });

        setLayout(new BorderLayout());
        add(scoll_pane, BorderLayout.CENTER);
        if (label != null) {
            add(new JLabel(label), BorderLayout.NORTH);
        }
    }

    public final void setValues(Object... values) {
        listModel.removeAllElements();
        for (Object v : values) {
            if (listener != null) {
                v = listener.render(v);
            }
            listModel.addElement(v);
        }
    }

    public Object[] getValues() {
        return listModel.toArray();
    }
    
    public int[] getSelectedIndices() {
        return list.getSelectedIndices();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        list.setEnabled(enabled);
        list.setComponentPopupMenu(enabled ? popupMenu : null);
    }

    public void setListener(ListChangeListener listener) {
        this.listener = listener;
        popupMenu.removeAll();
        list.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
        list.unregisterKeyboardAction(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0));

        if (listener.canAdd()) {
            popupMenu.add(addItem);
        }
        if (listener.canEdit()) {
            popupMenu.add(editItem);
        }
        if (listener.canDelete()) {
            popupMenu.add(deleteItem);
        }
        if (listener.canDelete()) {
            popupMenu.add(clearItem);
            // delete key
            list.registerKeyboardAction(deleteListener, KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), JComponent.WHEN_FOCUSED);
            // back space key
            list.registerKeyboardAction(deleteListener, KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), JComponent.WHEN_FOCUSED);
        }
        String[] additionalPopopMenuEntries = listener.getAdditionalPopopMenuEntries();
        for (String entry : additionalPopopMenuEntries) {
            JMenuItem item = new JMenuItem(entry);
            item.addActionListener((e) -> {
                listener.onAdditionalPopopMenuEntry(entry, list.getSelectedIndices());
            });
            popupMenu.add(item);
        }
        list.setComponentPopupMenu(popupMenu);
    }

    public void setCellFont(Font f) {
        this.cellFont = f;
    }

    public void startAddDialog() {
        if (!enabled || listener == null || !listener.canAdd()) {
            return;
        }
        String newValue = JOptionPane.showInputDialog(list, "New value: ");
        if (enabled && newValue != null) {
            listener.onAdd(newValue, list.getSelectedIndex() + 1);
        }
    }

    public void startClearDialog() {
        if (!enabled || listener == null || !listener.canDelete()) {
            return;
        }
        int dia_res = JOptionPane.showConfirmDialog(list, "Are you sure you want to delete all values?");
        if (enabled && dia_res == JOptionPane.YES_OPTION) {
            listener.onClear();
        }
    }

    public void startEditDialog() {
        if (!enabled || listener == null || list.getSelectedIndices().length != 1) {
            return;
        }
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex == -1) {
            return;
        }
        String newValue = JOptionPane.showInputDialog(list, "Edit value: ", list.getSelectedValue());
        if (enabled && newValue != null) {
            listener.onEdit(newValue, selectedIndex);
        }
    }

    public void startDeleteDialog() {
        int[] selectedIndices = list.getSelectedIndices();
        if (!enabled || listener == null || !listener.canDelete() || selectedIndices.length <= 0) {
            return;
        }
        String question = "Are you sure you want to delete '" + list.getSelectedValue() + "'?";
        if (list.getSelectedIndices().length > 1) {
            question = "Are you sure you want to delete " + list.getSelectedIndices().length  + " elements?";
        }
        int dia_res = JOptionPane.showConfirmDialog(list, question);
        if (enabled && dia_res == JOptionPane.YES_OPTION) {
            listener.onDelete(selectedIndices);
        }
    }

    public static interface ListChangeListener {

        public Object parse(String s);

        public default String render(Object o) {
            return o.toString();
        }

        public default String export(Object label, int index) {
            return label.toString();
        }

        public default boolean canAdd() {
            return true;
        }

        public void onAdd(String s, int index);

        public default boolean canEdit() {
            return true;
        }

        public void onEdit(String s, int index);

        public default boolean canMove() {
            return true;
        }
        
        public default void onMove(int[] from, int to) {
            for (int i = 0; i < from.length; i++) {
                if (from[i] <= to) {
                    onMove(from[i]-i, to);
                }
                else onMove(from[i], to+i);
            }
        }

        public void onMove(int from, int to);

        public default boolean canDelete() {
            return true;
        }

        public void onDelete(int index);
        
        public default void onDelete(int[] indices) {
            for (int i = indices.length-1; i >= 0; i--) onDelete(indices[i]);
        }

        public void onClear();

        public default boolean onDoubleClick(int index) {
            return true;   // continue with 
        }
        
        public default String[] getAdditionalPopopMenuEntries() {
            return new String[0];
        }

        public default void onAdditionalPopopMenuEntry(String entry, int[] indices) {}
    }

    private class ListTransferHandler extends TransferHandler {

        private int[] lastDragIndices = new int[0];
        private int lastDropIndex = -1;

        /**
         * We only support importing strings. The listener can reject if the
         * string cannot be converted to the right object.
         */
        @Override
        public boolean canImport(TransferHandler.TransferSupport info) {
            if (!enabled || listener == null) {
                return false;
            }
            // Check for String flavor
            if (!info.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            String data;
            try {
                data = (String) info.getTransferable().getTransferData(
                        DataFlavor.stringFlavor);
                return listener.parse(data) != null;
            } catch (UnsupportedFlavorException ufe) {
                return false; 
            } catch (InvalidDnDOperationException e) {
                return false;
            } catch (Exception ioe) {
                return false;
            }
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            lastDragIndices = list.getSelectedIndices();
            lastDropIndex = -1;
            int[] selected = list.getSelectedIndices();
            String[] exported = new String[selected.length];
            for (int i = 0; i < selected.length; i++) {
                if (listener != null) {
                    try {
                        exported[i] = listener.export(listModel.get(selected[i]), selected[i]);
                    } catch (Exception e) {}
                } else {
                    exported[i] = listModel.get(selected[i]).toString();
                }
            }
            return new StringSelection(String.join("\n", exported));
        }

        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport info) {
            if (!enabled || listener == null || !listener.canAdd()) {
                return false;
            }
            String data;
            try {
                data = (String) info.getTransferable().getTransferData(DataFlavor.stringFlavor);
                if (listener.parse(data) == null) {
                    return false;
                }
            } catch (Exception ioe) {
                return false;
            }

            if (!info.isDrop()) {
                // adding (e.g. with CTRL + V)
                int index = list.getSelectedIndex() + 1; // after selection
                listener.onAdd(data, index);
                lastDragIndices = new int[0];
                lastDropIndex = -1;
            } else {
                // move: drag and drop reordering
                if (lastDragIndices.length == 0) {
                    return false;  // drag from exernal (not supported)
                }
                JList.DropLocation dl = (JList.DropLocation) info.getDropLocation();
                lastDropIndex = dl.getIndex();  // only mark location, exectue atomically in exportData
            }

            return true;
        }

        @Override
        protected void exportDone(JComponent c, Transferable data, int action) {
            if (!enabled || listener == null || !listener.canMove() || lastDropIndex == -1) {
                return;
            }
            if (action == TransferHandler.MOVE) {
                listener.onMove(lastDragIndices, lastDropIndex);
            }
            lastDragIndices = new int[0];
            lastDropIndex = -1;
        }
    }

    public static void main(String[] args) {
        Object[] values = new Object[]{1, 2, 3, 4, 5, 6, "123", 7, 8};
        ListPanel p = new ListPanel("Values:", 3, values);
        ListChangeListener listChangeListener = new ListChangeListener() {
            @Override
            public Object parse(String s) {
                return s;
            }

            @Override
            public void onAdd(String s, int index) {
                Object[] new_values = Vector.addDim(p.getValues(), s, index);
                p.setValues(new_values);
            }

            @Override
            public void onEdit(String s, int index) {
                Object[] new_values = Vector.copy(p.getValues());
                new_values[index] = s;
                p.setValues(new_values);
            }

            @Override
            public void onMove(int from, int to) {
                Object[] new_values = p.getValues();
                Vector.moveDim(new_values, from, to);
                p.setValues(new_values);
            }

            @Override
            public void onDelete(int index) {
                Object[] new_values = Vector.removeDim(p.getValues(), index);
                p.setValues(new_values);
            }

            @Override
            public void onClear() {
                Object[] new_values = Vector.clear(p.getValues());
                p.setValues(new_values);
            }
        };
        p.setListener(listChangeListener);
        JFrame frame = new JFrame("ListDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        frame.add(p, BorderLayout.CENTER);

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
