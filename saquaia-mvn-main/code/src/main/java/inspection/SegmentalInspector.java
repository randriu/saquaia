package inspection;

import benchmarking.simulatorconfiguration.SSAConfig;
import benchmarking.simulatorconfiguration.SegmentalConfig;
import benchmarking.simulatorconfiguration.SimulatorConfig;
import core.model.Interval;
import core.model.Setting;
import core.util.Examples;
import core.util.IO;
import core.util.IntArrayWrapper;
import core.util.Pair;
import core.util.Progressable;
import core.util.Vector;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import core.simulation.Simulation;
import core.simulation.events.IntervalStateChangeEndCondition;
import core.simulation.events.IntervalStateChangeListener;
import core.simulation.simulators.AbstractSimulator;
import core.simulation.simulators.segmental.GrowingMemoryFunction;

/**
 *
 * @author helfrich
 */
public class SegmentalInspector {

    public static void main(String[] args) {
        JFileChooser chooser = new JFileChooser(IO.EXAMPLE_FOLDER);
        chooser.setSelectedFile(new File(IO.EXAMPLE_FOLDER, "brusselator_canonical_dense.json"));
        int answer = chooser.showDialog(null, "Inspekt!");
        if (answer != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Setting setting = Examples.getSettingFromFile(chooser.getSelectedFile());

        if (setting.dim() < 2) {
            return;
        }

        JFrame frame = new JFrame("ABS-Inspector: " + setting.name);
        frame.setLayout(new BorderLayout());

        //2. Optional: What happens when the frame closes?
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final MyPanel myPanel = new MyPanel(setting, 0, 1);
        frame.add(myPanel);

        int[] initial_abs = setting.intervalStateForState(setting.initial_state);

        JPanel abs_panel_outer = new JPanel();
        abs_panel_outer.setLayout(new BorderLayout());
        JPanel abs_panel = new JPanel();
        abs_panel_outer.add(abs_panel, BorderLayout.NORTH);
//        abs_panel.setLayout(new BoxLayout(abs_panel, BoxLayout.Y_AXIS));
        abs_panel.setLayout(new GridLayout(0, 1));
        frame.add(abs_panel_outer, BorderLayout.WEST);
        JSpinner[] abs_spinners = new JSpinner[setting.dim()];
        final Button[] abs_buttons_x = new Button[setting.dim()];
        final Button[] abs_buttons_y = new Button[setting.dim()];
        SpinnerNumberModel[] abs_spinner_models = new SpinnerNumberModel[setting.dim()];

        ChangeListener l = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int[] new_abs = new int[setting.dim()];
                for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
                    new_abs[dim_i] = (int) abs_spinner_models[dim_i].getNumber();
                }
//                System.out.println(Arrays.toString(new_abs));
                myPanel.setAbs(new_abs);
//                System.out.println(Arrays.toString(new_abs));
//                System.out.println("");
            }
        };

        for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
            JPanel p = new JPanel();
//            p.setLayout(new BoxLayout(p , BoxLayout.X_AXIS));
            abs_spinner_models[dim_i] = new SpinnerNumberModel(initial_abs[dim_i], 0, setting.intervals[dim_i].length - 1, 1);
            abs_spinners[dim_i] = new JSpinner(abs_spinner_models[dim_i]);
            p.add(abs_spinners[dim_i]);
            p.add(new JLabel(setting.crn.speciesNames[dim_i]));
            int cur_dim = dim_i;
            abs_buttons_x[dim_i] = new Button("x");
            abs_buttons_x[dim_i].addActionListener((e) -> {
                abs_buttons_x[myPanel.dim_y].setEnabled(true);
                abs_buttons_y[myPanel.dim_x].setEnabled(true);
                myPanel.setXDim(cur_dim);
                abs_buttons_x[myPanel.dim_y].setEnabled(false);
                abs_buttons_y[myPanel.dim_x].setEnabled(false);
            });
            p.add(abs_buttons_x[dim_i]);
            abs_buttons_y[dim_i] = new Button("y");
            abs_buttons_y[dim_i].addActionListener((e) -> {
                abs_buttons_x[myPanel.dim_y].setEnabled(true);
                abs_buttons_y[myPanel.dim_x].setEnabled(true);
                myPanel.setYDim(cur_dim);
                abs_buttons_x[myPanel.dim_y].setEnabled(false);
                abs_buttons_y[myPanel.dim_x].setEnabled(false);
            });
            p.add(abs_buttons_y[dim_i]);
            abs_panel.add(p);
            abs_spinners[dim_i].addChangeListener(l);
        }
        abs_buttons_x[myPanel.dim_y].setEnabled(false);
        abs_buttons_y[myPanel.dim_x].setEnabled(false);

        // build menu bar
        JMenu exampleMenu = new JMenu("Example");
        JMenuItem exampleSSA = new JMenuItem();
        exampleSSA.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myPanel.exampleConfig = 0;
                myPanel.repaint();
//                System.out.println("example_simulator_SSA");
            }
        });
        exampleSSA.setText("SSA");
        exampleSSA.setAccelerator(KeyStroke.getKeyStroke("F1"));
        exampleMenu.add(exampleSSA);
        JMenuItem exampleSEG = new JMenuItem();
        exampleSEG.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myPanel.exampleConfig = 1;
                myPanel.repaint();
//                System.out.println("example_simulator_SEG_summary");
            }
        });
        exampleSEG.setText("SEG (with summaries)");
        exampleSEG.setAccelerator(KeyStroke.getKeyStroke("F2"));
        exampleMenu.add(exampleSEG);
        JMenuItem exampleSEG2 = new JMenuItem();
        exampleSEG2.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myPanel.exampleConfig = 2;
                myPanel.repaint();
//                System.out.println("example_simulator_SEG");
            }
        });
        exampleSEG2.setText("SEG (without summaries)");
        exampleSEG2.setAccelerator(KeyStroke.getKeyStroke("F3"));
        exampleMenu.add(exampleSEG2);
        exampleMenu.addSeparator();
        JMenuItem exampleLength = new JMenuItem();
        exampleLength.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    myPanel.example_length = Integer.parseInt(JOptionPane.showInputDialog("Example Length", myPanel.example_length + ""));
                } catch (NumberFormatException ex) {
                    myPanel.example_length = 10;
                }
                exampleLength.setText("Change Example Length (current: " + myPanel.example_length + ")");
                myPanel.repaint();
//                System.out.println("Change Example Length");
            }
        });
        exampleLength.setText("Change Example Length (current: " + myPanel.example_length + ")");
        exampleLength.setAccelerator(KeyStroke.getKeyStroke("C"));
        exampleMenu.add(exampleLength);

        JMenu absMenu = new JMenu("Abstract State");
        JMenuItem moveUp = new JMenuItem();
        moveUp.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("up");
                if (abs_spinner_models[myPanel.dim_y].getNextValue() != null) {
                    abs_spinner_models[myPanel.dim_y].setValue(abs_spinner_models[myPanel.dim_y].getNextValue());
                }
            }
        });
        moveUp.setText("Y++");
        moveUp.setAccelerator(KeyStroke.getKeyStroke("W"));
        absMenu.add(moveUp);
        JMenuItem moveDown = new JMenuItem();
        moveDown.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("down");
                if (abs_spinner_models[myPanel.dim_y].getPreviousValue() != null) {
                    abs_spinner_models[myPanel.dim_y].setValue(abs_spinner_models[myPanel.dim_y].getPreviousValue());
                }
            }
        });
        moveDown.setText("Y--");
        moveDown.setAccelerator(KeyStroke.getKeyStroke("S"));
        absMenu.add(moveDown);
        JMenuItem moveLeft = new JMenuItem();
        moveLeft.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("left");
                if (abs_spinner_models[myPanel.dim_x].getNextValue() != null) {
                    abs_spinner_models[myPanel.dim_x].setValue(abs_spinner_models[myPanel.dim_x].getNextValue());
                }
            }
        });
        moveLeft.setText("X++");
        moveLeft.setAccelerator(KeyStroke.getKeyStroke("D"));
        absMenu.add(moveLeft);
        JMenuItem moveRight = new JMenuItem();
        moveRight.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("right");
                if (abs_spinner_models[myPanel.dim_x].getPreviousValue() != null) {
                    abs_spinner_models[myPanel.dim_x].setValue(abs_spinner_models[myPanel.dim_x].getPreviousValue());
                }
            }
        });
        moveRight.setText("X--");
        moveRight.setAccelerator(KeyStroke.getKeyStroke("A"));
        absMenu.add(moveRight);
        absMenu.addSeparator();
        JMenuItem lessAbs = new JMenuItem();
        lessAbs.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("choose nr of abs states");
                try {
                    myPanel.shown_x_neg = Integer.parseInt(JOptionPane.showInputDialog("shown_x_neg", myPanel.shown_x_neg + ""));
                } catch (NumberFormatException ex) {
                    myPanel.shown_x_neg = 1;
                }
                try {
                    myPanel.shown_x_pos = Integer.parseInt(JOptionPane.showInputDialog("shown_x_pos", myPanel.shown_x_pos + ""));
                } catch (NumberFormatException ex) {
                    myPanel.shown_x_pos = 1;
                }
                try {
                    myPanel.shown_y_neg = Integer.parseInt(JOptionPane.showInputDialog("shown_y_neg", myPanel.shown_y_neg + ""));
                } catch (NumberFormatException ex) {
                    myPanel.shown_y_neg = 1;
                }
                try {
                    myPanel.shown_y_pos = Integer.parseInt(JOptionPane.showInputDialog("shown_y_pos", myPanel.shown_y_pos + ""));
                } catch (NumberFormatException ex) {
                    myPanel.shown_y_pos = 1;
                }
                myPanel.repaint();
            }
        });
        lessAbs.setText("Choose Number of Drawn Abstract States");
        lessAbs.setAccelerator(KeyStroke.getKeyStroke("Q"));
        absMenu.add(lessAbs);
        JMenuItem allAbs = new JMenuItem();
        allAbs.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("allAbs");
                myPanel.shown_x_neg = 100;
                myPanel.shown_x_pos = 100;
                myPanel.shown_y_neg = 100;
                myPanel.shown_y_pos = 100;
                myPanel.repaint();
            }
        });
        allAbs.setText("Show Full");
        allAbs.setAccelerator(KeyStroke.getKeyStroke("F"));
        absMenu.add(allAbs);
        JMenuItem reset = new JMenuItem();
        reset.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("reset");
                myPanel.shown_x_neg = 1;
                myPanel.shown_x_pos = 1;
                myPanel.shown_y_neg = 1;
                myPanel.shown_y_neg = 1;
                myPanel.repaint();
            }
        });
        reset.setText("Show Default");
        reset.setAccelerator(KeyStroke.getKeyStroke("X"));
        absMenu.add(reset);
        absMenu.addSeparator();
        JMenuItem resample = new JMenuItem();
        resample.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                myPanel.resample(myPanel.abs);
//                System.out.println("resample");
            }
        });
        resample.setText("Resample");
        resample.setAccelerator(KeyStroke.getKeyStroke("R"));
        absMenu.add(resample);
        JMenuItem average = new JMenuItem();
        average.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("toggle_average");
                myPanel.show_average = !myPanel.show_average;
                myPanel.repaint();
            }
        });
        average.setText("Toggle Average Effect");
        average.setAccelerator(KeyStroke.getKeyStroke("T"));
        absMenu.add(average);

        JMenu settingMenu = new JMenu("Setting");
        JMenuItem open = new JMenuItem();
        open.setAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
//                System.out.println("open");
                frame.dispose();
                main(new String[]{});
            }
        });
        open.setText("Open");
        open.setAccelerator(KeyStroke.getKeyStroke("O"));
        settingMenu.add(open);

        JMenuBar menubar = new JMenuBar();
        menubar.add(settingMenu);
        menubar.add(absMenu);
        menubar.add(exampleMenu);
        frame.setJMenuBar(menubar);

        //4. Size the frame.
        frame.pack();
//        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        myPanel.requestFocus();

        //5. Show it.
        frame.setVisible(true);
    }

    public static class MyPanel extends JPanel {

        Setting setting;
        int dim_x, dim_y;
        int[] abs;
        int w, h, border_left, border_right, border_top, border_bottom;
        int border_base = 20;
        int nr_of_sims = 100;
        int shown_x_neg = 1;
        int shown_x_pos = 1;
        int shown_y_neg = 1;
        int shown_y_pos = 1;
        boolean show_average = true;
        int example_length = 10;

        final SimulatorConfig[] configs = new SimulatorConfig[]{
            new SSAConfig(),
            new SegmentalConfig().setMemoryFunction(GrowingMemoryFunction.normal().setStartAt(0)),
            new SegmentalConfig().setMemoryFunction(GrowingMemoryFunction.normal().setStartAt(0)).setUseSummaries(false)
        };
        final SimulatorConfig baseConfig = configs[0];
        int exampleConfig = 0;

        HashMap<IntArrayWrapper, ArrayList<Simulation>> sims = new HashMap<>();

        private class SimComparer implements Comparator<Simulation> {

            private final int dim_x, dim_y;

            public SimComparer(int dim_x, int dim_y) {
                this.dim_x = dim_x;
                this.dim_y = dim_y;
            }

            @Override
            public int compare(Simulation o1, Simulation o2) {
                double[] a1 = new double[]{o1.start_state[dim_x], o1.start_state[dim_y], o1.getState()[dim_x], o1.getState()[dim_y]};
                double[] a2 = new double[]{o2.start_state[dim_x], o2.start_state[dim_y], o2.getState()[dim_x], o2.getState()[dim_y]};
                return Arrays.compare(a1, a2);
            }
        }
        int max_amount = 0;

        public MyPanel(Setting setting, int dim_x, int dim_y) {
            super();
            this.setting = setting;
            this.abs = Vector.copy(setting.intervalStateForState(setting.initial_state));
            this.dim_x = dim_x;
            this.dim_y = dim_y;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(500, 500);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(300, 300);
        }

        public boolean setXDim(int dim_x) {
            if (dim_x == this.dim_y) {
                return false;
            }
            this.dim_x = dim_x;
            repaint();
            return true;
        }

        public boolean setYDim(int dim_y) {
            if (dim_y == this.dim_x) {
                return false;
            }
            this.dim_y = dim_y;
            repaint();
            return true;
        }

        private TreeMap<Simulation, Integer> getSimsMultis(ArrayList<Simulation> sims_i) {
            TreeMap<Simulation, Integer> sims_multis = new TreeMap<>(new SimComparer(dim_x, dim_y));
            max_amount = 0;
            for (Simulation sim : sims_i) {
                int amount = sims_multis.getOrDefault(sim, 0) + 1;
                sims_multis.put(sim, amount);
                max_amount = Math.max(max_amount, amount);
            }
            return sims_multis;
        }

        public void setAbs(int[] abs) {
            this.abs = abs;
            repaint();
        }

        private void resample(int[] abs_i) {
            sims.getOrDefault(new IntArrayWrapper(abs_i), new ArrayList<>()).clear();
            repaint();
        }

        public ArrayList<Simulation> getOrGenerateSims(int[] abs_i) {
            AbstractSimulator simulator = baseConfig.createSimulator(setting);
            simulator.addIntervalStateChangeListener(new IntervalStateChangeEndCondition());

            IntArrayWrapper abs_i_wrapped = new IntArrayWrapper(abs_i);
            if (!sims.containsKey(abs_i_wrapped)) {
                sims.put(abs_i_wrapped, new ArrayList<>());
            }
            ArrayList<Simulation> sims_i = sims.get(abs_i_wrapped);
            while (sims_i.size() < nr_of_sims) {
                Simulation sim = new Simulation(Vector.asDoubleArray(setting.representativeForIntervalState(abs_i)));
                simulator.simulate(sim, Double.MAX_VALUE, new Progressable());
                sims_i.add(sim);
            }
            return sims_i;
        }

        public Simulation exampleSim(int crossings) {
            AbstractSimulator simulator = configs[exampleConfig % configs.length].createSimulator(setting);
            simulator.addIntervalStateChangeListener(new IntervalStateChangeListener() {
                int crossed = 0;
                int[] prev = null;

                @Override
                public boolean notify(double time, double[] state, int[] previous_interv_state, int[] new_interv_state, int[] interv_state_offset) {
                    if (prev == null || prev[dim_x] != new_interv_state[dim_x] || prev[dim_y] != new_interv_state[dim_y]) {
                        crossed++;
//                        System.out.println(Arrays.toString(prev) + " --> " + Arrays.toString(new_interv_state));
                    }
//                    System.out.println(crossed + ": " + Arrays.toString(previous_interv_state) + " --> " + Arrays.toString(new_interv_state));
                    prev = Vector.copy(previous_interv_state);
                    return crossed <= crossings
                            && new_interv_state[dim_x] >= abs[dim_x] - shown_x_neg
                            && new_interv_state[dim_x] <= abs[dim_x] + shown_x_pos
                            && new_interv_state[dim_y] >= abs[dim_y] - shown_y_neg
                            && new_interv_state[dim_y] <= abs[dim_y] + shown_y_pos;
                }
            });

            Simulation sim = new Simulation(Vector.asDoubleArray(setting.representativeForIntervalState(abs)));
            sim.setKeepHistory(true);
//            sim.addStepListener(new Simulation.StepListener() {
//                @Override
//                public void onStep(double[] state, double time) {
//                    System.out.println("Step: " + time + " " + Arrays.toString(state));
//                }
//            });
            simulator.simulate(sim, Double.MAX_VALUE, new Progressable());
            return sim;
        }

        private String latexColorString(Color c) {
            return "{rgb,255:red," + c.getRed() + "; green," + c.getGreen() + "; blue," + c.getBlue() + "}";
        }

        private void fillRect(double x, double y, double w, double h, Graphics2D g2, Color c, String classString) {
            g2.setColor(c);
            g2.fillRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
            System.out.println("    \\fill[fill=" + latexColorString(c) + ", " + classString + "] (" + x + "," + -y + ") rectangle (" + (x + w) + "," + -(y + h) + "); ");
        }

        private void drawRect(double x, double y, double w, double h, Graphics2D g2, Color c, String classString, float stroke) {
            g2.setColor(c);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawRect((int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h));
            float latex_stroke = stroke / 5;
            System.out.println("    \\draw[draw=" + latexColorString(c) + ", line width=" + latex_stroke + ", " + classString + "] (" + (x + latex_stroke) + "," + -(y+latex_stroke) + ") rectangle (" + (x + w - 2*latex_stroke) + "," + -(y + h - 2*latex_stroke) + "); ");
        }

        private void drawLine(double x, double y, double x2, double y2, Graphics2D g2, Color c, String classString, float stroke) {
            g2.setColor(c);
            g2.setStroke(new BasicStroke(stroke));
            g2.drawLine((int) Math.round(x), (int) Math.round(y), (int) Math.round(x2), (int) Math.round(y2));
            float latex_stroke = stroke / 5;
            System.out.println("    \\draw[draw=" + latexColorString(c) + ", line width=" + latex_stroke + ", " + classString + "] (" + x + "," + -y + ") -- (" + x2 + "," + -y2 + "); ");
        }

        private void drawString(String s, double x, double y, Graphics2D g2, Color c, String classString, String name, int anchor_h, int anchor_v) {
            Font f = g2.getFont();
            FontMetrics metrics = g2.getFontMetrics(f);
            double latexFontSize = g2.getFont().getSize() / 3;
            String anchor_string = "";
            if (anchor_v > 0) anchor_string += "north";
            else if (anchor_v < 0) anchor_string += "south";
            if (anchor_h > 0) anchor_string += "east";
            else if (anchor_h < 0) anchor_string += "west";
            if (anchor_h == 0 && anchor_v == 0) anchor_string = "center";
            
            double x_java = x;
            if (anchor_h > 0) x_java -= metrics.stringWidth(s);
            else if (anchor_h == 0) x_java -= metrics.stringWidth(s) / 2.0;
            double y_java = y;
            if (anchor_v > 0) y_java += metrics.getMaxAscent();
            else if (anchor_v == 0) y_java += metrics.getMaxAscent() / 2.0;
            
            
            g2.setColor(c);
            g2.drawString(s, (int) Math.round(x_java), (int) Math.round(y_java));
            
            System.out.println("    \\node[rectangle, inner sep=0, outer sep=0, text=" + latexColorString(c) + ", anchor=" + anchor_string + ", font={\\fontsize{" + latexFontSize + "}{" +latexFontSize * 1.2 + "}\\selectfont}, " + classString + "] (" + name + ") at (" + x + "," + -y + ") {" + s + "}; ");
        }

        private void fillCircle(double x, double y, double r, Graphics2D g2, Color c, String classString) {
            g2.setColor(c);
            g2.fillOval((int) Math.round(x - r / 2), (int) Math.round(y - r / 2), (int) Math.round(r), (int) Math.round(r));
            System.out.println("    \\fill[fill=" + latexColorString(c) + ", " + classString + "] (" + x + "," + -y + ") circle[radius=" + r/2 + "]; ");
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            System.out.println("");
            System.out.println("");

            Graphics2D g2 = (Graphics2D) g;

            w = getWidth();
            h = getHeight();
            System.out.println("\\begin{tikzpicture}[scale=" + Math.ceil(5.0 / Math.max(w, h) * 100000) / 100000 + ", auto]");
            
            System.out.println("    \\tikzstyle{representative}=[]");
            System.out.println("    \\tikzstyle{example}=[]");
            System.out.println("    \\tikzstyle{avg}=[]");
            System.out.println("    \\tikzstyle{summary}=[]");
            System.out.println("    \\tikzstyle{bg}=[]");
            System.out.println("    \\tikzstyle{abs}=[]");
            System.out.println("    ");

            Font font = g.getFont();
            FontMetrics metrics = g.getFontMetrics(font);
            int font_height = metrics.getAscent();

            String default_label = "999999999";
            String string_x_species = setting.crn.speciesNames[dim_x];
            String string_y_species = setting.crn.speciesNames[dim_y];
            border_left = metrics.stringWidth(default_label);
            border_right = metrics.stringWidth(default_label);
            border_top = metrics.getHeight() + border_base;
            border_bottom = metrics.getHeight() + border_base;
            for (int x = Math.max(0, abs[dim_x] - shown_x_neg); x < Math.min(setting.intervals[dim_x].length, abs[dim_x] + shown_x_pos + 1); x++) {
                for (int y = Math.max(0, abs[dim_y] - shown_y_neg); y < Math.min(setting.intervals[dim_y].length, abs[dim_y] + shown_y_pos + 1); y++) {
                    int[] abs_i = Vector.copy(abs);
                    abs_i[dim_x] = x;
                    abs_i[dim_y] = y;

                    Interval interval_x = setting.intervals[dim_x][abs_i[dim_x]];
                    Interval interval_y = setting.intervals[dim_y][abs_i[dim_y]];
                    String string_x_max = "" + interval_x.max;
                    String string_y_min = "" + interval_y.min;
                    String string_y_rep = "" + interval_y.rep;
                    String string_y_max = "" + interval_y.max;
                    border_left = Math.max(border_left, metrics.stringWidth(string_y_min));
                    border_left = Math.max(border_left, metrics.stringWidth(string_y_max));
                    border_left = Math.max(border_left, metrics.stringWidth(string_y_rep));
                    border_right = Math.max(border_right, metrics.stringWidth(string_x_max) / 2);
                    border_right = Math.max(border_right, metrics.stringWidth(string_y_species));
                }
            }
            border_left += border_base;
            border_right += border_base;

            for (int x = Math.max(0, abs[dim_x] - shown_x_neg); x < Math.min(setting.intervals[dim_x].length, abs[dim_x] + shown_x_pos + 1); x++) {
                for (int y = Math.max(0, abs[dim_y] - shown_y_neg); y < Math.min(setting.intervals[dim_y].length, abs[dim_y] + shown_y_pos + 1); y++) {
                    System.out.println("    % ABS + " + x + "," + y);
                    int[] abs_i = Vector.copy(abs);
                    abs_i[dim_x] = x;
                    abs_i[dim_y] = y;

                    Interval interval_x = setting.intervals[dim_x][abs_i[dim_x]];
                    Interval interval_y = setting.intervals[dim_y][abs_i[dim_y]];

                    Color bg = Color.WHITE;
                    if (x == abs[dim_x] && y == abs[dim_y]) {
                        bg = new Color(200, 200, 200, 255);
                    }
                    fillRect(getX(interval_x.min), getY(interval_y.max), getX(interval_x.max) - getX(interval_x.min), getY(interval_y.min) - getY(interval_y.max), g2, bg, "bg");
                    drawRect(getX(interval_x.min), getY(interval_y.max), getX(interval_x.max) - getX(interval_x.min), getY(interval_y.min) - getY(interval_y.max), g2, Color.BLACK, "abs", 2.0f);

                    ArrayList<Simulation> sims_i = getOrGenerateSims(abs_i);
                    TreeMap<Simulation, Integer> sims_i_multis = getSimsMultis(sims_i);

                    // draw simulations
//                    System.out.println("Drawing sims for " + Arrays.toString(abs_i));
                    int[] representative = setting.representativeForIntervalState(abs_i);
                    double x_start = getX(representative[dim_x]);
                    double y_start = getY(representative[dim_y]);
                    for (Simulation sim : sims_i_multis.keySet()) {
//                        System.out.println(Arrays.toString(sim.start_state) + " --> " + Arrays.toString(sim.getState()));

                        double x_end = getX(sim.getState()[dim_x]);
                        double y_end = getY(sim.getState()[dim_y]);
                        int amount = sims_i_multis.get(sim);
//                        System.out.println(x_start + "," + y_start + " --> " + x_end + "," + y_end + "  x" + amount);
//                        System.out.println("");

                        Color color_start = new Color(112, 117, 250, 255);
                        Color color_end = new Color(1, 4, 99, 255);
                        Color color = new Color(
                                color_start.getRed() + (max_amount > 1 ? (color_end.getRed() - color_start.getRed()) * (amount - 1) / (max_amount - 1) : 0),
                                color_start.getGreen() + (max_amount > 1 ? (color_end.getGreen() - color_start.getGreen()) * (amount - 1) / (max_amount - 1) : 0),
                                color_start.getBlue() + (max_amount > 1 ? (color_end.getBlue() - color_start.getBlue()) * (amount - 1) / (max_amount - 1) : 0),
                                color_start.getAlpha() + (max_amount > 1 ? (color_end.getAlpha() - color_start.getAlpha()) * (amount - 1) / (max_amount - 1) : 0));
                        drawLine(x_start, y_start, x_end, y_end, g2, color, "summary", 15f * amount / nr_of_sims);
                    }
                    
                    fillCircle(x_start, y_start, 10, g2, Color.BLACK, "representative");

                    if (show_average) {
                        double[] avg_change = new double[setting.dim()];
                        for (int r_i = 0; r_i < setting.crn.reactions.length; r_i++) {
                            setting.crn.reactions[r_i].applyTo(avg_change, setting.crn.reactions[r_i].propensity(representative));
                        }
                        //                    System.out.println(Arrays.toString(avg_change));
                        if (!Vector.isNull(avg_change)) {
                            double[] null_vector = new double[setting.dim()];
                            int max_interval_size = Math.max(setting.intervals[dim_x][abs[dim_x]].size(), setting.intervals[dim_y][abs[dim_y]].size()) / 2;
                            Vector.times(avg_change, max_interval_size / Vector.distance(avg_change, null_vector));
                            //                        System.out.println(Arrays.toString(avg_change));
                            Vector.plus(avg_change, Vector.asDoubleArray(representative));
                            //                        System.out.println(Arrays.toString(avg_change));
                            fillCircle(x_start, y_start, 10, g2, Color.GREEN, "representative");
                            drawLine(x_start, y_start, getX(avg_change[dim_x]), getY(avg_change[dim_y]), g2, Color.GREEN, "avg", 1.0f);
                        }
                    }
                    System.out.println("    ");
                }
            }

            // Draw X Values
            System.out.println("    % X Values (aka levels)");
            for (int x = Math.max(0, abs[dim_x] - shown_x_neg); x < Math.min(setting.intervals[dim_x].length, abs[dim_x] + shown_x_pos + 1); x++) {
                int[] abs_i = Vector.copy(abs);
                abs_i[dim_x] = x;
                Interval interval_x = setting.intervals[dim_x][abs_i[dim_x]];
                String string_x_min = "" + interval_x.min;
                String string_x_rep = "" + interval_x.rep;
                String string_x_max = "" + interval_x.max;

                drawString(string_x_min,
//                        getX(interval_x.min) - metrics.stringWidth(string_x_min) / 2,
                        getX(interval_x.min),
                        border_top - border_base / 2,
                        g2,
                        Color.BLACK,
                        "label",
                        "lx" + x + "_min",
                        0,
                        -1);
                drawString(string_x_rep,
//                        getX(interval_x.rep) - metrics.stringWidth(string_x_rep) / 2,
                        getX(interval_x.rep),
                        border_top - border_base / 2,
                        g2,
                        Color.BLACK,
                        "label",
                        "lx" + x + "_rep",
                        0,
                        -1);
                if (x == Math.min(setting.intervals[dim_x].length - 1, abs[dim_x] + shown_x_pos)) {
                    drawString(string_x_max,
//                            getX(interval_x.max) - metrics.stringWidth(string_x_max) / 2,
                            getX(interval_x.max),
                            border_top - border_base / 2,
                            g2,
                            Color.BLACK,
                            "label",
                            "lx" + x + "_max",
                        0,
                        -1);
                }
            }
            System.out.println("    ");

            // Draw Y Values
            System.out.println("    % Y Values (aka levels)");
            for (int y = Math.max(0, abs[dim_y] - shown_y_neg); y < Math.min(setting.intervals[dim_y].length, abs[dim_y] + shown_y_pos + 1); y++) {
                int[] abs_i = Vector.copy(abs);
                abs_i[dim_y] = y;
                Interval interval_y = setting.intervals[dim_y][abs_i[dim_y]];
                String string_y_min = "" + interval_y.min;
                String string_y_rep = "" + interval_y.rep;
                String string_y_max = "" + interval_y.max;

                drawString(string_y_min,
//                        border_left - border_base / 2 - metrics.stringWidth(string_y_min),
                        border_left - border_base / 2,
//                        getY(interval_y.min) + font_height / 2,
                        getY(interval_y.min),
                        g2,
                        Color.BLACK,
                        "label",
                        "ly" + y + "_min",
                        1,
                        0);
                drawString(string_y_rep,
//                        border_left - border_base / 2 - metrics.stringWidth(string_y_rep),
                        border_left - border_base / 2,
//                        getY(interval_y.rep) + font_height / 2,
                        getY(interval_y.rep),
                        g2,
                        Color.BLACK,
                        "label",
                        "ly" + y + "_rep",
                        1,
                        0);
                if (y == Math.min(setting.intervals[dim_y].length - 1, abs[dim_y] + shown_y_pos)) {
                    drawString(string_y_max,
//                            border_left - border_base / 2 - metrics.stringWidth(string_y_max),
                            border_left - border_base / 2,
//                            getY(interval_y.max) + font_height / 2,
                            getY(interval_y.max),
                            g2,
                            Color.BLACK,
                            "label",
                            "ly" + y + "_max",
                        1,
                        0);
                }
            }
            System.out.println("    ");

            System.out.println("    % Species Labels");
            // Draw Dimensions
            drawString(string_x_species,
//                    (border_left + w - border_right) / 2 - metrics.stringWidth(string_x_species) / 2,
                    (border_left + w - border_right) / 2,
                    h - border_bottom + border_base / 2,
                    g2,
                    Color.BLACK,
                    "label",
                    "l_x",
                    0,
                    1);
            drawString(string_y_species,
                    w - border_right + border_base / 2,
//                    (border_top + h - border_bottom) / 2 + font_height / 2,
                    (border_top + h - border_bottom) / 2,
                    g2,
                    Color.BLACK,
                    "label",
                    "l_y",
                    -1,
                    0);
            System.out.println("    ");

            // draw example sim
            
            System.out.println("    % Simulation");
            Simulation sim = exampleSim(example_length);
            double[] cur_state = null;
            for (Pair<double[], Double> p : sim.getHistory()) {
                if (cur_state != null) {
                    Color color_start = new Color(220, 20, 20, 255);
                    Color color_end = new Color(220, 220, 20, 255);
                    Color color = new Color(
                            (int) (color_start.getRed() + (sim.getTime() > 0 ? (color_end.getRed() - color_start.getRed()) * (p.right) / sim.getTime() : 0)),
                            (int) (color_start.getGreen() + (sim.getTime() > 0 ? (color_end.getGreen() - color_start.getGreen()) * (p.right) / sim.getTime() : 0)),
                            (int) (color_start.getBlue() + (sim.getTime() > 0 ? (color_end.getBlue() - color_start.getBlue()) * (p.right) / sim.getTime() : 0)),
                            (int) (color_start.getAlpha() + (sim.getTime() > 0 ? (color_end.getAlpha() - color_start.getAlpha()) * (p.right) / sim.getTime() : 0)));
                    drawLine(getX(cur_state[dim_x]), getY(cur_state[dim_y]), getX(p.left[dim_x]), getY(p.left[dim_y]), g2, color, "example", 2f);
                }
                cur_state = p.left;
            }
            
            System.out.println("\\end{tikzpicture}");
        }

        public double getX(double value) {
            Interval interval_min = setting.intervals[dim_x][Math.max(0, abs[dim_x] - shown_x_neg)];
            Interval interval_max = setting.intervals[dim_x][Math.min(abs[dim_x] + shown_x_pos, setting.intervals[dim_x].length - 1)];
            int drawn_size = interval_max.max - interval_min.min + 2;
            if (value < interval_min.min - 1) {
                value = interval_min.min - 1;
            }
            if (value > interval_max.max + 1) {
                value = interval_max.max + 1;
            }
            return border_left + (value - interval_min.min + 1) * (w - border_left - border_right) / drawn_size;
        }

        public double getY(double value) {
            Interval interval_min = setting.intervals[dim_y][Math.max(0, abs[dim_y] - shown_y_neg)];
            Interval interval_max = setting.intervals[dim_y][Math.min(abs[dim_y] + shown_y_pos, setting.intervals[dim_y].length - 1)];
            int drawn_size = interval_max.max - interval_min.min + 2;
            if (value < interval_min.min - 1) {
                value = interval_min.min - 1;
            }
            if (value > interval_max.max + 1) {
                value = interval_max.max + 1;
            }
            return h - border_bottom - (value - interval_min.min + 1) * (h - border_top - border_bottom) / drawn_size;
        }
    }
}
