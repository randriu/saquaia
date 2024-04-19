package gui;

import core.model.Interval;
import core.model.Reaction;
import core.model.Setting;
import core.util.Examples;
import core.util.IO;
import core.util.PopulationLevelHelper;
import core.util.Vector;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

/**
 *
 * @author helfrich
 */
public class SettingPanel extends JPanel {
    
    private boolean enabled = true;

    private Setting setting;
    private EditableText editableTextCrnName;
    private EditableText editableTextSettingName;
    private EditableText editableTextInitialState;
    private EditableText editableTextEndTime;
    private ListPanel listSpecies;
    private ListPanel listReactions;
    private JSpinner spinnerPopulationGrowthFactor;
    private JTable tableIntervals;
    private DefaultTableModel tableIntervalsModel;

    private final double POPULATION_LEVEL_GROWTH_FACTOR_MIN = 1.05;
    private final double POPULATION_LEVEL_GROWTH_FACTOR_MAX = 4;
    private SettingChangeListener listener;

    public SettingPanel(Setting initial_setting) {
        super(new BorderLayout());
        
        SettingPanel this_ref = this;

        editableTextCrnName = new EditableText("Name:", "");
        editableTextCrnName.setChangeListener((newValue) -> {
            Setting copy = getSetting().copy();
            copy.crn.name = newValue;
            setSetting(copy);
        });

        listSpecies = new ListPanel("Species:", 6);
        listSpecies.setCellFont(Util.FONT_MONO);
        listSpecies.setListener(new ListPanel.ListChangeListener() {
            @Override
            public Object parse(String s) {
                return s.strip();
            }

            @Override
            public void onAdd(String s, int index) {
                s = IO.speciesNamePreprocessed(s);
                if (!IO.isValidNewSpeciesName(s, getSetting().crn)) {
                    return;
                }

                Setting copy = getSetting().copy();
                copy.addDim(s);
                notifyListener(copy);
            }

            @Override
            public void onMove(int from, int to) {
                Setting copy = getSetting().copy();
                copy.moveDim(from, to);
                notifyListener(copy);
            }

            @Override
            public void onDelete(int index) {
                Setting copy = getSetting().copy();
                copy.deleteDim(index);
                notifyListener(copy);
            }

            @Override
            public void onClear() {
                Setting copy = getSetting().copy();
                copy.changeDim(0);
                notifyListener(copy);
            }

            @Override
            public void onEdit(String s, int index) {
                s = IO.speciesNamePreprocessed(s);
                if (!IO.isValidNewSpeciesName(s, getSetting().crn)) {
                    return;
                }

                Setting copy = getSetting().copy();
                copy.crn.speciesNames[index] = s;
                notifyListener(copy);
            }
        });

        listReactions = new ListPanel("Reactions:", 6);
        listReactions.setCellFont(Util.FONT_MONO);
        listReactions.setListener(new ListPanel.ListChangeListener() {
            @Override
            public Object parse(String s) {
                return Reaction.fromString(getSetting().crn, s);
            }

            @Override
            public String render(Object o) {
                Reaction r = (Reaction) o;
                return r.toString(getSetting().crn);
            }

            @Override
            public void onAdd(String s, int index) {
                Reaction r = Reaction.fromString(getSetting().crn, s);
                if (r == null) {
                    return;
                }
                Setting copy = getSetting().copy();
                copy.crn.reactions = Vector.addDim(copy.crn.reactions, r, index);
                notifyListener(copy);
            }

            @Override
            public void onMove(int from, int to) {
                Setting copy = getSetting().copy();
                Vector.moveDim(copy.crn.reactions, from, to);
                notifyListener(copy);
            }

            @Override
            public void onDelete(int index) {
                Setting copy = getSetting().copy();
                copy.crn.reactions = Vector.removeDim(copy.crn.reactions, index);
                notifyListener(copy);
            }

            @Override
            public void onClear() {
                Setting copy = getSetting().copy();
                copy.crn.reactions = Vector.clear(copy.crn.reactions);
                notifyListener(copy);
            }

            @Override
            public void onEdit(String s, int index) {
                Reaction r = Reaction.fromString(getSetting().crn, s);
                if (r == null) {
                    return;
                }
                Setting copy = getSetting().copy();
                copy.crn.reactions[index] = r;
                notifyListener(copy);
            }
        });

        editableTextSettingName = new EditableText("Name:", "");
        editableTextSettingName.setChangeListener((newValue) -> {
            Setting copy = getSetting().copy();
            copy.name = newValue;
            notifyListener(copy);
        });

        editableTextInitialState = new EditableText("Initial state:", "");
        editableTextInitialState.setChangeListener((newValue) -> {
            int[] new_intial_state = Vector.intVectorFromString(newValue, getSetting().crn.speciesNames);
            if (new_intial_state == null) {
                JOptionPane.showMessageDialog(editableTextInitialState, "Could not parse initial state.");
                return;
            }
            if (!setting.isWithinBounds(new_intial_state)){
                JOptionPane.showMessageDialog(editableTextInitialState, "CARE: Your initial state is not within bounds of \nyour setting. Any simulation starting from the \nnew initial state will stop immidiately. You will \nneed to modify the species bounds!");
            }
            Setting copy = getSetting().copy();
            copy.initial_state = new_intial_state;
            notifyListener(copy);
        });

        editableTextEndTime = new EditableText("End time:", "");
        editableTextEndTime.setChangeListener((newValue) -> {
            double new_end_time = IO.parseDouble(newValue);
            if (new_end_time >= 0) {
                Setting copy = getSetting().copy();
                copy.end_time = new_end_time;
                notifyListener(copy);
            }
        });
        
        spinnerPopulationGrowthFactor = new JSpinner(new SpinnerNumberModel(1.5, POPULATION_LEVEL_GROWTH_FACTOR_MIN, POPULATION_LEVEL_GROWTH_FACTOR_MAX, 0.05));
//        ((DefaultEditor) spinnerPopulationGrowthFactor.getEditor()).getTextField().setEditable(false);
        ChangeListener abstractionChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Setting copy = getSetting().copy();
                double new_population_level_growth_factor = (double) spinnerPopulationGrowthFactor.getValue();
                new_population_level_growth_factor = Math.min(new_population_level_growth_factor, POPULATION_LEVEL_GROWTH_FACTOR_MAX);
                new_population_level_growth_factor = Math.max(new_population_level_growth_factor, POPULATION_LEVEL_GROWTH_FACTOR_MIN);
                if (copy.population_level_growth_factor == new_population_level_growth_factor) return;
                copy.population_level_growth_factor = new_population_level_growth_factor;
                notifyListener(copy);
            }
        };
        spinnerPopulationGrowthFactor.addChangeListener(abstractionChangeListener);

        tableIntervalsModel = new DefaultTableModel(initial_setting.intervals, initial_setting.crn.speciesNames);
        tableIntervals = new JTable(tableIntervalsModel);
        tableIntervals.setPreferredScrollableViewportSize(new Dimension(300, 300));
        tableIntervals.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableIntervals.setDefaultEditor(Object.class, null);
        tableIntervals.getTableHeader().setReorderingAllowed(false);
        tableIntervals.setCellSelectionEnabled(true);
        tableIntervals.setRowSelectionAllowed(true);
        tableIntervals.setColumnSelectionAllowed(true);
        tableIntervals.setDefaultRenderer(Object.class, new TableCellRenderer() {
            DefaultTableCellRenderer base = new DefaultTableCellRenderer();
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object o, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel cell = (JLabel)base.getTableCellRendererComponent(jtable, o, isSelected, hasFocus, row, col);
                Setting setting = getSetting();
                cell.setToolTipText(null);
                if (row >= 0 && row < setting.intervals[col].length) cell.setToolTipText(setting.crn.speciesNames[col] + ": Level " + row);
                return cell;
            }
        });
        JScrollPane tableIntervalsScrollPane = new JScrollPane(tableIntervals, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableIntervalsScrollPane.setMinimumSize(new Dimension(100, 100));
        MouseAdapter tableMouseAdapter = new MouseAdapter() {
            private void updatePreviewModel(Collection<Integer> forced, Collection<Integer> extra, int bound, double growth_factor, DefaultListModel previewModel) {
                TreeSet<Integer> combined = new TreeSet<>();
                combined.addAll(forced);
                combined.addAll(extra);
                Interval[] intervals = PopulationLevelHelper.intervalsFor(0, bound, growth_factor, combined);
                previewModel.clear();
                for (int i = 0; i < intervals.length; i++) {
                    previewModel.addElement(i + ": "  + intervals[i]);
                }
            }
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int col = tableIntervals.columnAtPoint(evt.getPoint());
                int row = tableIntervals.rowAtPoint(evt.getPoint());
                Setting setting = getSetting();
                
                // info popup
                if (0 <= row && row < setting.intervals[col].length && (!enabled && evt.getButton() == evt.BUTTON1 || evt.getButton() == evt.BUTTON3)) {
                    Interval interval = setting.intervals[col][row];
                    JOptionPane.showMessageDialog(this_ref, "Level " + row + ":\nmin=" + interval.min + "\nrep=" + interval.rep + "\nmax=" + interval.max);
                }
                
                if (!enabled || evt.getClickCount() != 2 || col < 0 || col > setting.dim()) {
                    return;
                }
                
                int[] bound = new int[]{getSetting().bounds[col]};
                TreeSet<Integer> forced = getSetting().forcedByReactions(col);
                TreeSet<Integer> extra = new TreeSet<>();
                if (getSetting().extraLevels != null && getSetting().extraLevels[col] != null) {
                    extra.addAll(getSetting().extraLevels[col]);
                }
                DefaultListModel listPreviewModel = new DefaultListModel();
                updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);

                JList listPreview = new JList();
                listPreview.setEnabled(false);
                listPreview.setVisibleRowCount(15);
                listPreview.setModel(listPreviewModel);

                DefaultListModel listForcedModel = new DefaultListModel();
                listForcedModel.addAll(forced);
                JList listForced = new JList(listForcedModel);
                listForced.setEnabled(false);
                listForced.setBackground(getBackground());
                listForced.setVisibleRowCount(3);

                ListPanel listExtra = new ListPanel("Extra levels:", 6, extra.toArray());
                listExtra.setPreferredSize(new Dimension(150, 100));
                listExtra.setListener(new ListPanel.ListChangeListener() {
                    @Override
                    public Object parse(String s) {
                        try {
                            int v = Integer.parseInt(s);
                            if (v < 1 || v > bound[0]) {
                                return null;
                            }
                            return v;
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }

                    @Override
                    public void onAdd(String s, int index) {
                        Integer integer = (Integer) parse(s);
                        if (integer != null && !forced.contains(integer)) {
                            extra.add(integer);
                        }
                        listExtra.setValues(extra.toArray());
                        updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                    }

                    @Override
                    public void onEdit(String s, int index) {
                        Integer integer = (Integer) parse(s);
                        if (integer == null) {
                            return;
                        }
                        Object o = extra.toArray()[index];
                        extra.remove(o);
                        if (!forced.contains(integer)) {
                            extra.add(integer);
                        }
                        listExtra.setValues(extra.toArray());
                        updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                    }

                    @Override
                    public void onMove(int from, int to) {
                    }

                    @Override
                    public void onDelete(int index) {
                        Object o = extra.toArray()[index];
                        extra.remove(o);
                        listExtra.setValues(extra.toArray());
                        updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                    }

                    @Override
                    public void onClear() {
                        extra.clear();
                        listExtra.setValues(extra.toArray());
                        updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                    }
                });
                JButton buttonReset = new JButton("Clear extra levels");
                buttonReset.addActionListener((e) -> {
                    extra.clear();
                    listExtra.setValues(extra.toArray());
                    updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                });
                EditableText editableTextBound = new EditableText("Bound:", bound[0] + "");
                editableTextBound.setChangeListener((newValue) -> {
                    try {
                        int v = Integer.parseInt(newValue);
                        if (v >= 0) {
                            bound[0] = v;
                            editableTextBound.setValue(bound[0] + "");
                            Iterator<Integer> iterator = extra.iterator();
                            while (iterator.hasNext()) {
                                int i = iterator.next();
                                if (i > bound[0]) {
                                    iterator.remove();
                                }
                            }
                            listExtra.setValues(extra.toArray());
                            updatePreviewModel(forced, extra, bound[0], getSetting().population_level_growth_factor, listPreviewModel);
                        }
                    } catch (NumberFormatException e) {
                    }
                });

                final JComponent[] diaContents = new JComponent[]{
                    editableTextBound,
                    new JLabel("Forced levels (by reactions)"),
                    new JScrollPane(listForced),
                    listExtra,
                    buttonReset,
                    new JLabel("Preview"),
                    new JScrollPane(listPreview)
                };
                int result = JOptionPane.showConfirmDialog(this_ref, diaContents, "Custom Levels for " + getSetting().crn.speciesNames[col], JOptionPane.PLAIN_MESSAGE);
                if (enabled && result == JOptionPane.OK_OPTION) {
                    Setting copy = getSetting().copy();
                    copy.bounds[col] = bound[0];
                    if (!extra.isEmpty()) {
                        if (copy.extraLevels == null) {
                            copy.extraLevels = new TreeSet[copy.dim()];
                        }
                        copy.extraLevels[col] = extra;
                    } else {
                        if (copy.extraLevels != null) {
                            copy.extraLevels[col] = null;
                            boolean allEmpty = true;
                            for (int dim_i = 0; dim_i < copy.dim(); dim_i++) {
                                if (copy.extraLevels[dim_i] != null) {
                                    allEmpty = false;
                                    break;
                                }
                            }
                            if (allEmpty) {
                                copy.extraLevels = null;
                            }
                        }
                    }
                    notifyListener(copy);
                }
            }
        };
        tableIntervals.addMouseListener(tableMouseAdapter);
        tableIntervals.getTableHeader().addMouseListener(tableMouseAdapter);

        // layout
        JPanel panelAbstractionSettings = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelAbstractionSettings.add(new JLabel("Abstraction:       Growth Factor:"));
        panelAbstractionSettings.add(spinnerPopulationGrowthFactor);

        JPanel panelAbstraction = new JPanel(new BorderLayout());
        panelAbstraction.add(panelAbstractionSettings, BorderLayout.NORTH);
        panelAbstraction.add(tableIntervalsScrollPane, BorderLayout.CENTER);
        
        JPanel panelInitAndTime = new JPanel();
        BoxLayout panelInitAndTimeBoxLayout = new BoxLayout(panelInitAndTime, BoxLayout.Y_AXIS);
        panelInitAndTime.setLayout(panelInitAndTimeBoxLayout);
        panelInitAndTime.add(editableTextInitialState);
        panelInitAndTime.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL)));
        panelInitAndTime.add(editableTextEndTime);
        panelInitAndTime.add(Box.createVerticalGlue());

        JPanel panelSetting = new JPanel(new BorderLayout(3, Util.GAP_VERTICAL));
        panelSetting.add(editableTextSettingName, BorderLayout.NORTH);
        panelSetting.add(panelAbstraction, BorderLayout.CENTER);
        panelSetting.add(panelInitAndTime, BorderLayout.SOUTH);
        panelSetting.setBorder(Util.getTitledBorder("Setting"));

        JSplitPane paneReactionsAndSpecies = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listSpecies, listReactions);
        paneReactionsAndSpecies.setResizeWeight(0.5);

        JPanel panelCrn = new JPanel(new BorderLayout(3, Util.GAP_VERTICAL));
        panelCrn.add(editableTextCrnName, BorderLayout.NORTH);
        panelCrn.add(paneReactionsAndSpecies, BorderLayout.CENTER);
        panelCrn.setBorder(Util.getTitledBorder("CRN"));

        JSplitPane paneCRNandOther = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelCrn, panelSetting);
        paneCRNandOther.setResizeWeight(1.0);
        add(paneCRNandOther, BorderLayout.CENTER);
//        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // insert data
        setSetting(initial_setting);
    }
    
    public interface SettingChangeListener {
        public void settingChanged(Setting newSetting);
    }
    
    private void notifyListener(Setting newSetting) {
        if (listener != null) {
            newSetting.recomputeIntervals(false);
            listener.settingChanged(newSetting);
        }
    }
    
    public void setListener(SettingChangeListener l) {
        this.listener = l;
    }

    public Setting getSetting() {
        return setting;
    }

    public final void setSetting(Setting setting) {
        this.setting = setting;
        editableTextCrnName.setValue(setting.crn.name);
        listSpecies.setValues((Object[]) setting.crn.speciesNames);
        listReactions.setValues((Object[]) setting.crn.reactions);
        editableTextSettingName.setValue(setting.name);
        editableTextInitialState.setValue(Vector.toString(setting.initial_state, setting.crn.speciesNames));
        editableTextEndTime.setValue(setting.end_time + "");
        spinnerPopulationGrowthFactor.setValue(setting.population_level_growth_factor);

        // intervals
        tableIntervalsModel.setColumnCount(setting.dim());
        tableIntervalsModel.setColumnIdentifiers(setting.crn.speciesNames);
        int max_nr_of_intervals = 0;
        for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
            int nr_of_intervals = setting.intervals[dim_i].length;
            max_nr_of_intervals = Math.max(max_nr_of_intervals, nr_of_intervals);
        }
        tableIntervalsModel.setRowCount(max_nr_of_intervals);
        TableColumnModel tableIntervalsColumnModel = tableIntervals.getColumnModel();
        TableCellRenderer tableIntervalsRenderer = setting.dim() > 0 ? tableIntervals.getCellRenderer(0, 0) : null;
        for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
            int nr_of_intervals = setting.intervals[dim_i].length;
            int width = 50;
            int header_width = tableIntervalsRenderer.getTableCellRendererComponent(tableIntervals, setting.crn.speciesNames[dim_i], false, false, 0, dim_i).getPreferredSize().width;
            width = Math.max(width, header_width);
            if (tableIntervalsModel.getRowCount() < nr_of_intervals) {
                tableIntervalsModel.setRowCount(nr_of_intervals);
            }
            for (int interval_i = 0; interval_i < tableIntervalsModel.getRowCount(); interval_i++) {
                if (interval_i < nr_of_intervals) {
                    Interval interval = setting.intervals[dim_i][interval_i];
                    tableIntervalsModel.setValueAt(interval, interval_i, dim_i);
                    int cell_width = tableIntervalsRenderer.getTableCellRendererComponent(tableIntervals, interval, false, false, interval_i, dim_i).getPreferredSize().width;
                    width = Math.max(width, cell_width);
                } else {
                    tableIntervalsModel.setValueAt(null, interval_i, dim_i);
                }
            }
            width += 5;
            width = Math.min(width, 200);
            tableIntervalsColumnModel.getColumn(dim_i).setPreferredWidth(width);
        }
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.enabled = enabled;
        editableTextCrnName.setEnabled(enabled);
        listSpecies.setEnabled(enabled);
        listReactions.setEnabled(enabled);
        editableTextSettingName.setEnabled(enabled);
        spinnerPopulationGrowthFactor.setEnabled(enabled);
        editableTextInitialState.setEnabled(enabled);
        editableTextEndTime.setEnabled(enabled);
    }
    
    public void changeAllBounds(int newBound) {
        if (!enabled) return;
        if (newBound < 0) newBound = 0;
        Setting copy = getSetting().copy();
        for (int dim_i = 0; dim_i < copy.dim(); dim_i++) {
            copy.bounds[dim_i] = newBound;
            if (copy.extraLevels == null || copy.extraLevels[dim_i] == null) continue;
            Iterator<Integer> iterator = copy.extraLevels[dim_i].iterator();
            while (iterator.hasNext()) {
                int i = iterator.next();
                if (i > newBound) {
                    iterator.remove();
                }
            }
        }
        copy.recomputeIntervals(false);
        setSetting(copy);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("SettingPanelDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());
        SettingPanel sp = new SettingPanel(Examples.getSettingByName("ecoli_canonical.json"));
//        SettingPanel sp = new SettingPanel(Examples.getSettingByName("predator_prey_canonical.json"));
        sp.setListener((newSetting) -> {
            sp.setSetting(newSetting);
        });
        frame.add(sp, BorderLayout.CENTER);

        frame.setIconImage(Util.getIcon());
        
//        new Thread(() -> {
//            boolean next = false;
//            while(true) {
//                try {
//                    Thread.sleep(5000);
//                } catch (InterruptedException ex) {}
//                sp.setEnabled(next);
//                next = !next;
//            }
//        }).start();

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
}
