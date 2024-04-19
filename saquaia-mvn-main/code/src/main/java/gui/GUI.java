package gui;

import benchmarking.simulatorconfiguration.HybridConfig;
import benchmarking.simulatorconfiguration.ODEConfig;
import benchmarking.simulatorconfiguration.SSAConfig;
import benchmarking.simulatorconfiguration.SegmentalConfig;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import benchmarking.simulatorconfiguration.TAUConfig;
import com.google.gson.reflect.TypeToken;
import core.analysis.TransientAnalysis;
import core.analysis.TransientAnalysisResult;
import core.model.Reaction;
import core.model.Setting;
import core.simulation.Simulation;
import core.simulation.simulators.SegmentalSimulator;
import core.simulation.simulators.Simulator;
import core.util.Examples;
import core.util.IO;
import core.util.JSON;
import core.util.Pair;
import core.util.Progressable;
import core.util.Vector;
import gui.simulator.SimulatorConfigEditor;
import gui.visualization.SimulationComparision;
import gui.visualization.SimulationVisualization;
import gui.visualization.SpeedVisualization;
import gui.visualization.TransientAnalysisVisualization;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.openjdk.jol.info.ClassLayout;

/**
 *
 * @author Martin
 */
public class GUI extends JFrame {

    private final SettingPanel panelSetting;
    private final DefaultListModel<String> messages = new DefaultListModel<>();
    private final JPopupMenu listMessagesPopupMenu;
    private final JList listMessages;
    private final ArrayList<Pair<Setting, Simulation>> simulations = new ArrayList<>();
    private final ListPanel listSimulations;
    private final ArrayList<Pair<Setting, TransientAnalysisResult>> transients = new ArrayList<>();
    private final ListPanel listTransients;

    private final ArrayList<Pair<String, SimulatorConfig>> simulatorConfigs;
    private Simulator simulator;
    private SimulatorConfig simulatorConfig;
    private final JComboBox comboBoxSimulator;
    private final JButton buttonSimulate, buttonTransient, buttonCancel;
    private final JSpinner spinnerTransientNrOfSims;
    private final JProgressBar progressBar;

    private final JMenuBar menuBar;

    private File curFile = null;

    private Progressable progressable;
    private boolean working;

    public GUI(Setting setting) {
        super("SeQuaiA");
        setIconImage(Util.getIcon());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        simulatorConfigs = new ArrayList<>();
        simulatorConfigs.add(new Pair("SSA", new SSAConfig()));
        simulatorConfigs.add(new Pair("TAU", new TAUConfig()));
        simulatorConfigs.add(new Pair("HYB", new HybridConfig()));
        simulatorConfigs.add(new Pair("ODE", new ODEConfig()));
        simulatorConfigs.add(new Pair("Segmental SSA", new SegmentalConfig().setMaxMemory(2_000_000_000l).setBaseConfig(new SSAConfig())));
        simulatorConfigs.add(new Pair("Segmental HYB", new SegmentalConfig().setMaxMemory(2_000_000_000l).setBaseConfig(new HybridConfig())));

        panelSetting = new SettingPanel(setting);
        panelSetting.setListener((newSetting) -> {
            panelSetting.setSetting(newSetting);
        });

        listMessages = new JList(messages);
        listMessages.setDragEnabled(true);
        listMessagesPopupMenu = new JPopupMenu();
        JMenuItem listMessagesPopupMenuClear = new JMenuItem("Clear");
        listMessagesPopupMenuClear.addActionListener((e) -> {
            messages.clear();
        });
        listMessagesPopupMenu.add(listMessagesPopupMenuClear);
        listMessages.setComponentPopupMenu(listMessagesPopupMenu);

        listSimulations = new ListPanel("Simulations:", 10, simulations.toArray());
        listSimulations.setListener(new ListPanel.ListChangeListener() {
            public static final String ENTRY_COMPARE = "Compare";
            public static final String ENTRY_SETTING = "Load Setting";

            @Override
            public Object parse(String s) {
                Pair<Setting, Simulation> p = JSON.getGson().fromJson(s, new TypeToken<Pair<Setting, Simulation>>() {
                }.getType());
                return p;
            }

            @Override
            public String render(Object o) {
                Pair<Setting, Simulation> p = (Pair<Setting, Simulation>) o;
                return p.right.name;
            }

            @Override
            public String export(Object label, int index) {
                Pair<Setting, Simulation> p = simulations.get(index);
                return JSON.getGson().toJson(p);
            }

            @Override
            public void onAdd(String s, int index) {
                Pair<Setting, Simulation> p = (Pair<Setting, Simulation>) parse(s);
                if (p != null) {
                    simulations.add(index, p);
                }
                listSimulations.setValues(simulations.toArray());
            }

            @Override
            public void onEdit(String s, int index) {
                simulations.get(index).right.name = s;
                listSimulations.setValues(simulations.toArray());
            }

            @Override
            public void onMove(int from, int to) {
                Pair<Setting, Simulation> removed = simulations.remove(from);
                simulations.add(to, removed);
                listSimulations.setValues(simulations.toArray());
            }

            @Override
            public void onDelete(int index) {
                simulations.remove(index);
                listSimulations.setValues(simulations.toArray());
            }

            @Override
            public void onClear() {
                simulations.clear();
                listSimulations.setValues(simulations.toArray());
            }

            @Override
            public boolean onDoubleClick(int index) {
                new SimulationVisualization(simulations.get(index).left, simulations.get(index).right);
                return false;
            }

            @Override
            public String[] getAdditionalPopopMenuEntries() {
                return new String[]{ENTRY_COMPARE, ENTRY_SETTING};
            }

            @Override
            public void onAdditionalPopopMenuEntry(String entry, int[] indices) {
                if (entry.equals(ENTRY_COMPARE)) {
                    compareSimulations();
                } else if (entry.equals(ENTRY_SETTING)) {
                    if (indices.length != 1) {
                        JOptionPane.showMessageDialog(listSimulations, "Please select exactly one result to load its setting.");
                    } else {
                        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                                listSimulations,
                                "Are you sure you want to load the setting of\n" + simulations.get(indices[0]).right.name + "?\nAll unsaved changes to your current setting will be lost.",
                                "Load setting?",
                                JOptionPane.YES_NO_OPTION
                        )) {
                            panelSetting.setSetting(simulations.get(indices[0]).left);
                        }
                    }
                }
            }
        });

        progressBar = new JProgressBar();

        listTransients = new ListPanel("Transient Analysis:", 10, transients.toArray());
        listTransients.setListener(new ListPanel.ListChangeListener() {
            public final static String ENTRY_TRANSIENT_DISTRIBUTION = "Compare Transient Distributions";
            public final static String ENTRY_SPEED = "Compare Speeds";
            public static final String ENTRY_SETTING = "Load Setting";

            @Override
            public Object parse(String s) {
                Pair<Setting, TransientAnalysisResult> p = JSON.getGson().fromJson(s, new TypeToken<Pair<Setting, TransientAnalysisResult>>() {
                }.getType());
                return p;
            }

            @Override
            public String render(Object o) {
                Pair<Setting, TransientAnalysisResult> p = (Pair<Setting, TransientAnalysisResult>) o;
                return p.right.name;
            }

            @Override
            public String export(Object label, int index) {
                Pair<Setting, TransientAnalysisResult> p = transients.get(index);
                return JSON.getGson().toJson(p);
            }

            @Override
            public void onAdd(String s, int index) {
                Pair<Setting, Simulation> p = (Pair<Setting, Simulation>) parse(s);
                if (p != null) {
                    simulations.add(index, p);
                }
                listTransients.setValues(transients.toArray());
            }

            @Override
            public void onEdit(String s, int index) {
                transients.get(index).right.name = s;
                listTransients.setValues(transients.toArray());
            }

            @Override
            public void onMove(int from, int to) {
                Pair<Setting, TransientAnalysisResult> removed = transients.remove(from);
                transients.add(to, removed);
                listTransients.setValues(transients.toArray());
            }

            @Override
            public void onDelete(int index) {
                transients.remove(index);
                listTransients.setValues(transients.toArray());
            }

            @Override
            public void onClear() {
                transients.clear();
                listTransients.setValues(transients.toArray());
            }

            @Override
            public boolean onDoubleClick(int index) {
                new TransientAnalysisVisualization(transients.get(index).left, transients.get(index).right);
                return false;
            }

            @Override
            public String[] getAdditionalPopopMenuEntries() {
                return new String[]{ENTRY_TRANSIENT_DISTRIBUTION, ENTRY_SPEED, ENTRY_SETTING};
            }

            @Override
            public void onAdditionalPopopMenuEntry(String entry, int[] indices) {
                if (entry.equals(ENTRY_TRANSIENT_DISTRIBUTION)) {
                    compareTransientDistributions();
                } else if (entry.equals(ENTRY_SPEED)) {
                    compareSpeed();
                } else if (entry.equals(ENTRY_SETTING)) {
                    if (indices.length != 1) {
                        JOptionPane.showMessageDialog(listTransients, "Please select exactly one result to load its setting.");
                    } else {
                        if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                                listTransients,
                                "Are you sure you want to load the setting of\n" + simulations.get(indices[0]).right.name + "?\nAll unsaved changes to your current setting will be lost.",
                                "Load setting?",
                                JOptionPane.YES_NO_OPTION
                        )) {
                            panelSetting.setSetting(transients.get(indices[0]).left);
                        }
                    }
                }
            }
        });

        comboBoxSimulator = new JComboBox<>(Pair.getFirst(simulatorConfigs).toArray());
        comboBoxSimulator.setSelectedIndex(0);

        buttonSimulate = new JButton("Simulate!");
        buttonSimulate.addActionListener((e) -> {
            simulate(1);
        });
        buttonTransient = new JButton("Transient Analysis!");
        buttonTransient.addActionListener((e) -> {
            transientAnalysis();
        });

        spinnerTransientNrOfSims = new JSpinner(new SpinnerNumberModel(1000, 1, 1000000, 100));

        buttonCancel = new JButton("Cancel!");
        buttonCancel.addActionListener((e) -> {
            if (progressable != null && !progressable.isStopped()) {
                progressable.stop();
                addMessage("Cancelling task...", 0);
            }
        });

        setWorking(false);

        // menu
        menuBar = new JMenuBar();
        JMenu menuSetting = new JMenu("Setting");
        menuSetting.setMnemonic(KeyEvent.VK_S);
        menuSetting.setToolTipText("Load / Save the setting.");
        menuBar.add(menuSetting);

        JMenuItem menuItemSettingNew = new JMenuItem("New");
        menuItemSettingNew.setToolTipText("Create a new setting.");
        menuItemSettingNew.setMnemonic(KeyEvent.VK_N);
        menuItemSettingNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingNew.addActionListener((e) -> {
            curFile = null;
            Setting newSetting = new Setting(2);
            newSetting.initial_state = new int[]{100, 100};
            newSetting.crn.reactions = Vector.addDim(newSetting.crn.reactions, new Reaction(new int[]{1, 1}, new int[]{2, 0}, 1.5, "example"), 0);
            panelSetting.setSetting(newSetting);
        });
        menuSetting.add(menuItemSettingNew);

        JMenuItem menuItemSettingOpen = new JMenuItem("Open");
        menuItemSettingOpen.setToolTipText("Open a setting file.");
        menuItemSettingOpen.setMnemonic(KeyEvent.VK_O);
        menuItemSettingOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingOpen.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser(curFile == null ? IO.SETTINGS_FOLDER : curFile.getParentFile());
            if (curFile != null) {
                fileChooser.setSelectedFile(curFile);
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                curFile = fileChooser.getSelectedFile();
                Setting newSetting = Examples.getSettingFromFile(curFile);
                newSetting.recomputeIntervals(false);
                panelSetting.setSetting(newSetting);
            }
        });
        menuSetting.add(menuItemSettingOpen);

        JMenuItem menuItemSettingSave = new JMenuItem("Save");
        menuItemSettingSave.setToolTipText("Save a setting file.");
        menuItemSettingSave.setMnemonic(KeyEvent.VK_S);
        menuItemSettingSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingSave.addActionListener((e) -> {
            if (curFile == null) {
                JFileChooser fileChooser = new JFileChooser(curFile == null ? IO.SETTINGS_FOLDER : curFile.getParentFile());
                fileChooser.setApproveButtonText("Save");
                if (curFile != null) {
                    fileChooser.setSelectedFile(curFile);
                }
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int returnVal = fileChooser.showOpenDialog(this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    curFile = fileChooser.getSelectedFile();
                }
            }
            if (curFile != null) {
                IO.writeStringToFile(curFile, panelSetting.getSetting().toJson());
            }
        });
        menuSetting.add(menuItemSettingSave);

        JMenuItem menuItemSettingSaveAs = new JMenuItem("Save As");
        menuItemSettingSaveAs.setToolTipText("Save a setting file to a new file.");
        menuItemSettingSaveAs.setMnemonic(KeyEvent.VK_A);
        menuItemSettingSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Util.KEYBOARD_SHORTCUR_KEY_MASK | ActionEvent.SHIFT_MASK));
        menuItemSettingSaveAs.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser(curFile == null ? IO.SETTINGS_FOLDER : curFile.getParentFile());
            fileChooser.setApproveButtonText("Save");
            if (curFile != null) {
                fileChooser.setSelectedFile(curFile);
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                curFile = fileChooser.getSelectedFile();
                IO.writeStringToFile(curFile, panelSetting.getSetting().toJson());
            }
        });
        menuSetting.add(menuItemSettingSaveAs);

        menuSetting.addSeparator();

        JMenuItem menuItemSettingExport = new JMenuItem("Export to DSD");
        menuItemSettingExport.setToolTipText("Export to Classic DSD format.");
        menuItemSettingExport.setMnemonic(KeyEvent.VK_E);
        menuItemSettingExport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingExport.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setApproveButtonText("Export");
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                IO.writeStringToFile(fileChooser.getSelectedFile(), IO.toClassicDSD(setting));
            }
        });
        menuSetting.add(menuItemSettingExport);
        
        JMenuItem menuItemSettingImport = new JMenuItem("Import from DSD");
        menuItemSettingImport.setToolTipText("Import from Classic DSD format.");
        menuItemSettingImport.setMnemonic(KeyEvent.VK_I);
        menuItemSettingImport.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingImport.addActionListener((e) -> {
            JFileChooser fileChooser = new JFileChooser(IO.EXAMPLE_DSD_FOLDER);
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setApproveButtonText("Import");
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                curFile = fileChooser.getSelectedFile();
                Setting newSetting = IO.fromClassicDSD(curFile);
                newSetting.recomputeIntervals(true);
                panelSetting.setSetting(newSetting);
            }
        });
        menuSetting.add(menuItemSettingImport);
        
        menuSetting.addSeparator();
        
        JMenuItem menuItemSettingChangeAllBounds = new JMenuItem("Change All Bounds");
        menuItemSettingChangeAllBounds.setToolTipText("Change the bounds for all species.");
        menuItemSettingChangeAllBounds.setMnemonic(KeyEvent.VK_B);
        menuItemSettingChangeAllBounds.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSettingChangeAllBounds.addActionListener((e) -> {
            String newBoundString = JOptionPane.showInputDialog(this, "Change the bound for ALL species to: ", 20000);
            try {
                int newBound = Integer.parseInt(newBoundString);
                panelSetting.changeAllBounds(newBound);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Could not parse number: '" + newBoundString + "'.");
            }
        });
        
        menuSetting.add(menuItemSettingChangeAllBounds);

        JMenu menuSimulator = new JMenu("Simulator");
        menuSimulator.setMnemonic(KeyEvent.VK_M);
        menuSimulator.setToolTipText("Options for the simulator.");

        JMenuItem menuItemSimulatorReset = new JMenuItem("Reset memory");
        menuItemSimulatorReset.setToolTipText("Reset the memory of current simulator.");
        menuItemSimulatorReset.setMnemonic(KeyEvent.VK_R);
        menuItemSimulatorReset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSimulatorReset.addActionListener((e) -> {
            resetSimulator();
        });
        menuSimulator.add(menuItemSimulatorReset);

        JMenuItem menuItemSimulatorInfo = new JMenuItem("Memory Info");
        menuItemSimulatorInfo.setToolTipText("Details about memory of current simulator.");
        menuItemSimulatorInfo.setMnemonic(KeyEvent.VK_M);
        menuItemSimulatorInfo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSimulatorInfo.addActionListener((e) -> {
            if (simulator == null) {
                JOptionPane.showMessageDialog(this, "Simulator is not initilized.");
                return;
            }
            ArrayList<Pair<String, Number>> stats = simulator.getStatistics();
            if (stats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Simulator does not provide memory stats.");
                return;
            }
            messageNewParagraph();
            addMessage("Memory stats of simulator:", 0);
            stats.forEach(pair -> {
                addMessage(pair.left + ": " + pair.right, 1);
            });
        });
        menuSimulator.add(menuItemSimulatorInfo);

        menuSimulator.addSeparator();
        JMenuItem menuItemSimulatorNew = new JMenuItem("New Simulator");
        menuItemSimulatorNew.setToolTipText("Add a new simulator by setting choosing all parameters.");
        menuItemSimulatorNew.setMnemonic(KeyEvent.VK_N);
        menuItemSimulatorNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemSimulatorNew.addActionListener((e) -> {
            Pair<String, SimulatorConfig> template = new Pair<>("Segmental HYB", new SegmentalConfig());
            if (!simulatorConfigs.isEmpty()) {
                String[] options = new String[simulatorConfigs.size()];
                for (int i = 0; i < simulatorConfigs.size(); i++) options[i] = simulatorConfigs.get(i).left;
                String selected = (String) JOptionPane.showInputDialog(null,
                        "Chose an existing simulator as starting point.",
                        "Simulator Template",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        options,
                        simulatorConfigs.get(simulatorConfigs.size() - 1).left
                );
                for (Pair<String, SimulatorConfig> pair : simulatorConfigs) {
                    if (pair.left.equals(selected)) template = pair;
                }
            }
            SimulatorConfig newConfig = SimulatorConfigEditor.edit(template.right);
            if (newConfig == null) return;
            String newName = JOptionPane.showInputDialog(null, "Name the new simulator:", template.left);
            if (newName != null) {
                simulatorConfigs.add(new Pair<>(newName, newConfig));
                comboBoxSimulator.setModel(new DefaultComboBoxModel(Pair.getFirst(simulatorConfigs).toArray()));
                comboBoxSimulator.setSelectedIndex(simulatorConfigs.size()-1);
                System.out.println(newConfig.toJson());
            }
        });
        menuSimulator.add(menuItemSimulatorNew);

        menuBar.add(menuSimulator);

        JMenu menuRun = new JMenu("Run");
        menuRun.setMnemonic(KeyEvent.VK_R);
        menuRun.setToolTipText("Run simulation / transient analysis.");

        JMenuItem menuItemRunSimulation = new JMenuItem("Simulation");
        menuItemRunSimulation.setToolTipText("Run a simulation.");
        menuItemRunSimulation.setMnemonic(KeyEvent.VK_S);
        menuItemRunSimulation.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        menuItemRunSimulation.addActionListener((e) -> {
            simulate(1);
        });
        menuRun.add(menuItemRunSimulation);

        JMenuItem menuItemRunSimulations = new JMenuItem("Multiple Simulations");
        menuItemRunSimulations.setToolTipText("Multiple simulations.");
        menuItemRunSimulations.setMnemonic(KeyEvent.VK_M);
        menuItemRunSimulations.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, Util.KEYBOARD_SHORTCUR_KEY_MASK));
        menuItemRunSimulations.addActionListener((e) -> {
            String nr_of_simulation_string = JOptionPane.showInputDialog(this, "Who many simulations do you want to run?", "5");
            try {
                simulate(Integer.parseInt(nr_of_simulation_string));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Could not parse number: '" + nr_of_simulation_string + "'");
            }
        });
        menuRun.add(menuItemRunSimulations);

        menuRun.addSeparator();

        JMenuItem menuItemRunTransient = new JMenuItem("Transient Analysis");
        menuItemRunTransient.setToolTipText("Run a transient analysis.");
        menuItemRunTransient.setMnemonic(KeyEvent.VK_T);
        menuItemRunTransient.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0));
        menuItemRunTransient.addActionListener((e) -> {
            transientAnalysis();
        });
        menuRun.add(menuItemRunTransient);

        menuBar.add(menuRun);

        JMenu menuCompare = new JMenu("Compare");
        menuCompare.setMnemonic(KeyEvent.VK_C);
        JMenuItem menuItemCompareSimulations = new JMenuItem("Simulations");
        menuItemCompareSimulations.setToolTipText("Compare multiple simulations.");
        menuItemCompareSimulations.setMnemonic(KeyEvent.VK_S);
        menuItemCompareSimulations.addActionListener((e) -> {
            compareSimulations();
        });
        menuCompare.add(menuItemCompareSimulations);
        menuCompare.addSeparator();

        JMenuItem menuItemCompareTransient = new JMenuItem("Transient Analysis Distribution");
        menuItemCompareTransient.setToolTipText("Compare transient distributions of multiple analysis.");
        menuItemCompareTransient.setMnemonic(KeyEvent.VK_T);
        menuItemCompareTransient.addActionListener((e) -> {
            compareTransientDistributions();
        });
        menuCompare.add(menuItemCompareTransient);

        JMenuItem menuItemCompareSpeed = new JMenuItem("Transient Analysis Speed");
        menuItemCompareSpeed.setToolTipText("Compare speeds evolution of multiple transient analysis.");
        menuItemCompareSpeed.setMnemonic(KeyEvent.VK_P);
        menuItemCompareSpeed.addActionListener((e) -> {
            compareSpeed();
        });
        menuCompare.add(menuItemCompareSpeed);

        menuBar.add(menuCompare);

        setJMenuBar(menuBar);

        // layout
        JSplitPane panelResults = new JSplitPane(JSplitPane.VERTICAL_SPLIT, listSimulations, listTransients);
        panelResults.setResizeWeight(0.5);
        panelResults.setBorder(Util.getTitledBorder("Results"));

        JPanel panelSimulators = new JPanel();
        panelSimulators.setLayout(new BoxLayout(panelSimulators, BoxLayout.Y_AXIS));
        panelSimulators.add(comboBoxSimulator);
        panelSimulators.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL * 3)));
        panelSimulators.add(new JSeparator());
        panelSimulators.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL * 3)));
        buttonSimulate.setAlignmentX(CENTER_ALIGNMENT);
        panelSimulators.add(buttonSimulate);
        panelSimulators.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL * 3)));
        panelSimulators.add(new JSeparator());
        panelSimulators.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL * 2)));

        JPanel panelTransientButton = new JPanel();
        panelTransientButton.setLayout(new BoxLayout(panelTransientButton, BoxLayout.Y_AXIS));
        buttonTransient.setAlignmentX(CENTER_ALIGNMENT);
        panelTransientButton.add(buttonTransient);
        JPanel panelTransientSettings = new JPanel();
        panelTransientSettings.setAlignmentX(CENTER_ALIGNMENT);
        panelTransientSettings.add(new JLabel("(by running"));
        panelTransientSettings.add(spinnerTransientNrOfSims);
        panelTransientSettings.add(new JLabel("simulations)"));
        panelTransientButton.add(panelTransientSettings);
        panelSimulators.add(panelTransientButton);
        panelSimulators.setBorder(Util.getTitledBorder("Simulator"));

        JPanel panelRight = new JPanel(new BorderLayout());
        panelRight.add(panelSimulators, BorderLayout.NORTH);
        panelRight.add(panelResults, BorderLayout.CENTER);
        JSplitPane panelMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelSetting, panelRight);
        panelMain.setResizeWeight(0.9);

        JPanel panelProgressBar = new JPanel();
        panelProgressBar.setLayout(new BoxLayout(panelProgressBar, BoxLayout.X_AXIS));
        panelProgressBar.add(new JLabel("Progress:"));
        panelProgressBar.add(Box.createRigidArea(new Dimension(Util.GAP_HORIZONTAL, 0)));
        panelProgressBar.add(progressBar);

        JPanel panelProgress = new JPanel();
        panelProgress.setLayout(new BoxLayout(panelProgress, BoxLayout.Y_AXIS));
        panelProgress.add(panelProgressBar);
        panelProgress.add(Box.createRigidArea(new Dimension(0, Util.GAP_VERTICAL)));
        panelProgress.add(buttonCancel);
        panelProgress.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel panelBottom = new JPanel(new BorderLayout());
        panelBottom.add(panelProgress, BorderLayout.WEST);
        panelBottom.add(new JScrollPane(listMessages));

        JSplitPane panelAll = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelMain, panelBottom);
        panelAll.setResizeWeight(0.9);
        panelAll.setBorder(new EmptyBorder(10, 10, 10, 10));

        add(panelAll);

        pack();
    }

    private void initProgressable() {
        progressBar.setValue(progressBar.getMinimum());
        progressable = new Progressable();
        messageNewParagraph();
        progressable.addMessageListener((message) -> {
            addMessage(message, progressable.getCurrentLevel());
        });
        progressable.addProgressListener((progress) -> {
            int progressAsInt = (int) ((progressBar.getMaximum() - progressBar.getMinimum()) * progress);
            if (progressAsInt != progressBar.getValue()) {
                progressBar.setValue(progressAsInt);
            }
        });
        progressable.enableSystemOutput();
    }

    private void messageNewParagraph() {
        if (!messages.isEmpty()) {
            messages.addElement(" ");
        }
    }

    private void prepareSimulator() {
        SimulatorConfig nextConfig = simulatorConfigs.get(comboBoxSimulator.getSelectedIndex()).right;
        Setting cur_setting = panelSetting.getSetting();
        if (simulatorConfig != null && simulatorConfig == nextConfig && simulator.getMemory() != null) {
            if (simulatorConfig.type == SimulatorConfig.Type.SEG) {
                SegmentalSimulator segmental_simulator = (SegmentalSimulator) simulator;
                if (segmental_simulator.isCompatibleWith(cur_setting)) {
                    addMessage("Setting is compatible: Reusing artifact...", 0);
                    return;
                }
            }
        }
        resetSimulator();
    }

    private void resetSimulator() {
        SimulatorConfig nextConfig = simulatorConfigs.get(comboBoxSimulator.getSelectedIndex()).right;
        Setting cur_setting = panelSetting.getSetting();
        simulator = simulatorConfigs.get(comboBoxSimulator.getSelectedIndex()).right.createSimulator(cur_setting);
        simulatorConfig = nextConfig;
    }

    private synchronized void simulate(int nr_of_simulations) {
        if (working) {
            return;
        }
        setWorking(true);
        new Thread(() -> {
            long total_time = 0;
            int simulated = 0;
            
            try {
                initProgressable();
                Setting cur_setting = panelSetting.getSetting();
                prepareSimulator();

                // create simulations
                Simulation[] sims = new Simulation[nr_of_simulations];
                String time_string = IO.getCurTimeString();
                for (int i = 0; i < nr_of_simulations; i++) {
                    sims[i] = cur_setting.createSimulation();
                    sims[i].setKeepHistory(true);
                    sims[i].name = cur_setting.name + " " + comboBoxSimulator.getSelectedItem() + " " + time_string + (nr_of_simulations > 1 ? (" (" + (i + 1) + ")") : "");
                    simulations.add(new Pair<>(cur_setting.copy(), sims[i]));
                }
                listSimulations.setValues(simulations.toArray());
                addMessage("Starting simulation" + (nr_of_simulations > 1 ? "s" : "") + " of " + cur_setting.name + " with simulator " + comboBoxSimulator.getSelectedItem(), 0);

                for (int i = 0; i < nr_of_simulations; i++) {
                    if (progressable.isStopped()) {
                        // remove simulations that did not start
                        for (int j = i; j < nr_of_simulations; j++) {
                            simulations.remove(simulations.size() - 1);
                        }
                        listSimulations.setValues(simulations.toArray());
                        break;
                    }
                    if (nr_of_simulations > 1) {
                        addMessage((i + 1) + "/" + nr_of_simulations, 0);
                    }
                    addMessage("Simulation name: " + sims[i].name, nr_of_simulations > 1 ? 1 : 0);
                    if (nr_of_simulations > 1) {
                        progressable.start_subroutine(1.0 / nr_of_simulations);
                    }
                    long start = System.nanoTime();
                    progressable.start_subroutine(1.0);
                    simulator.simulate(sims[i], cur_setting.end_time, progressable);
                    progressable.end_subroutine();
                    simulated++;
                    long took = System.nanoTime() - start;
                    if (nr_of_simulations > 1) {
                        progressable.end_subroutine();
                    }
                    addMessage("Simulation stopped at t=" + sims[i].getTime() + " in " + Arrays.toString(sims[i].getState()), nr_of_simulations > 1 ? 1 : 0);
                    addMessage("took " + IO.humanReadableDuration(took), nr_of_simulations > 1 ? 1 : 0);
                    total_time += took;
                }
            } catch (Exception e)
            {
                e.printStackTrace();
                addMessage("Error: " + e.getMessage(), 0);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    setWorking(false);
                });
                if (simulated > 1) {
                    addMessage("average computation time per simulation: " + IO.humanReadableDuration(total_time / simulated), 0);
                }
            }
        }).start();
    }

    private synchronized void transientAnalysis() {
        if (working) {
            return;
        }
        setWorking(true);
        new Thread(() -> {
            TransientAnalysisResult res = new TransientAnalysisResult();
            
            try{
                initProgressable();
                Setting cur_setting = panelSetting.getSetting();
                prepareSimulator();
                res.name = cur_setting.name + " " + comboBoxSimulator.getSelectedItem() + " " + IO.getCurTimeString();
                addMessage("Starting transient analysis of " + cur_setting.name + " with simulator " + comboBoxSimulator.getSelectedItem(), 0);
                addMessage("Transient anaylsis name: " + res.name, 0);

                transients.add(new Pair<>(cur_setting, res));
                listTransients.setValues(transients.toArray());
                int sims = (int) spinnerTransientNrOfSims.getModel().getValue();

                TransientAnalysis.analyize(res, cur_setting, simulator, sims, progressable.start_subroutine(1.0), null);
                progressable.end_subroutine();
            } catch (Exception e) {
                e.printStackTrace();
                addMessage("Error: " + e.getMessage(), 0);
            } finally {
                if (res.results > 0) {
                    addMessage("average computation time per simulation: " + IO.humanReadableDuration(res.getTotalCompTime() / res.results), 0);
                }
                SwingUtilities.invokeLater(() -> {
                    setWorking(false);
                });
            }
            
            
        }).start();
    }

    private void compareSimulations() {
        int[] selectedIndices = listSimulations.getSelectedIndices();
        if (selectedIndices.length < 2) {
            JOptionPane.showMessageDialog(this, "Please select at least 2 simulation results in the list.");
            return;
        }
        Pair<Setting, Simulation>[] toCompare = new Pair[selectedIndices.length];
        for (int i = 0; i < selectedIndices.length; i++) {
            toCompare[i] = simulations.get(selectedIndices[i]);
        }
        new SimulationComparision(panelSetting.getSetting(), toCompare);
    }

    private void compareTransientDistributions() {
        int[] selectedIndices = listTransients.getSelectedIndices();
        if (selectedIndices.length < 2) {
            JOptionPane.showMessageDialog(this, "Please select at least 2 transient analysis results in the list.");
            return;
        }
        Pair<Setting, TransientAnalysisResult>[] toCompare = new Pair[selectedIndices.length];
        for (int i = 0; i < selectedIndices.length; i++) {
            toCompare[i] = transients.get(selectedIndices[i]);
        }
        new TransientAnalysisVisualization(panelSetting.getSetting(), toCompare);
    }

    private void compareSpeed() {
        int[] selectedIndices = listTransients.getSelectedIndices();
        if (selectedIndices.length < 2) {
            JOptionPane.showMessageDialog(this, "Please select at least 2 transient analysis results in the list.");
            return;
        }
        TransientAnalysisResult[] toCompare = new TransientAnalysisResult[selectedIndices.length];
        for (int i = 0; i < selectedIndices.length; i++) {
            toCompare[i] = transients.get(selectedIndices[i]).right;
        }
        new SpeedVisualization(toCompare);
    }

    private void setWorking(boolean b) {
        working = b;
        buttonSimulate.setEnabled(!b);
        buttonTransient.setEnabled(!b);
        buttonCancel.setEnabled(b);
    }

    private void addMessage(String message, int level) {
        SwingUtilities.invokeLater(() -> {
            messages.addElement("<" + IO.getCurTimeString() + "> " + "    ".repeat(level + 1) + message);
            listMessages.ensureIndexIsVisible(messages.size() - 1);
        });
    }

    public static void main(String[] args) {
        Setting setting = Examples.getSettingByName("predator_prey_canonical_dense.json");
        setting.recomputeIntervals(false);
        GUI gui = new GUI(setting);
        gui.setVisible(true);

        // initialize memory computations
        new Thread(() -> {
            ClassLayout.parseClass(GUI.class).instanceSize();
        }).start();
    }
}
