package gui.visualization;

import core.model.Setting;
import core.simulation.Simulation;
import core.util.Pair;
import gui.SettingPanel;
import gui.Util;
import gui.WrapLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import plotting.Plotter;

/**
 *
 * @author Martin
 */
public class SimulationComparison extends JFrame {

    private final ChartPanel panelChart;
    private final JButton buttonHideAll, buttonShowAll, buttonShowSetting;
    private final JCheckBox checkBoxLevels, checkBoxForceOutputSpecies;
    private final JComboBox comboBoxSpecies, comboBoxSetting;

    private final Setting open_setting;
    private final Pair<Setting, Simulation>[] results;

    private final double[] plotted_times;
    private final boolean[] hidden;

    private final Thread autoUpdater;

    public SimulationComparison(Setting initial_setting, Pair<Setting, Simulation>[] results) {
        super("Simulation Comparison");

        setIconImage(Util.getIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.open_setting = initial_setting.copy();
        this.results = results;
        this.plotted_times = new double[results.length];

        hidden = new boolean[results.length];

        buttonHideAll = new JButton("Hide All");
        buttonHideAll.addActionListener((ActionEvent e) -> {
            for (int result_i = 0; result_i < results.length; result_i++) {
                hidden[result_i] = true;
            }
            updatePlot();
        });
        buttonShowAll = new JButton("Show All");
        buttonShowAll.addActionListener((ActionEvent e) -> {
            for (int result_i = 0; result_i < results.length; result_i++) {
                hidden[result_i] = false;
            }
            updatePlot();
        });

        comboBoxSpecies = new JComboBox(getSelectedSetting().crn.speciesNames);
        comboBoxSpecies.addActionListener((e) -> {
            updatePlot();
        });
        
        String[] settingNames = new String[results.length+1];
        for (int result_i = 0; result_i < results.length; result_i++) {
            settingNames[result_i] = results[result_i].right.name;
            if (settingNames[result_i].length() > 50) {
                settingNames[result_i] = settingNames[result_i].substring(0, 35) + "..." + settingNames[result_i].substring(settingNames[result_i].length() - 12);
            }
        }
        settingNames[results.length] = "Current Setting";
        comboBoxSetting = new JComboBox(settingNames);
        comboBoxSetting.setSelectedIndex(results.length);
        comboBoxSetting.addActionListener((e) -> {
            Object previousSelection = comboBoxSpecies.getSelectedItem();
            comboBoxSpecies.setModel(new DefaultComboBoxModel(getSelectedSetting().crn.speciesNames));
            comboBoxSpecies.setSelectedItem(previousSelection);
            updatePlot();
        });
        
        buttonShowSetting = new JButton("Show Setting");
        buttonShowSetting.addActionListener((ActionEvent e) -> {
            SettingPanel panelSetting = new SettingPanel(getSelectedSetting());
            panelSetting.setEnabled(false);
            JOptionPane.showMessageDialog(this, panelSetting, "Setting used for levels / bounds.", JOptionPane.PLAIN_MESSAGE);
        });

        checkBoxLevels = new JCheckBox("Show Levels");
        checkBoxLevels.setSelected(false);
        checkBoxLevels.addActionListener((e) -> {
            updatePlot();
        });
        checkBoxForceOutputSpecies = new JCheckBox("Force Levels for Output Species");
        checkBoxForceOutputSpecies.setSelected(false);
        checkBoxForceOutputSpecies.addActionListener((e) -> {
            updatePlot();
        });

        panelChart = new ChartPanel(updatePlot(), false);
        panelChart.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                ChartEntity entity = chartMouseEvent.getEntity();
                if (chartMouseEvent.getEntity() instanceof LegendItemEntity) {
                    LegendItemEntity itemEntity = (LegendItemEntity) entity;
                    Comparable seriesKey = itemEntity.getSeriesKey();

                    for (int result_i = 0; result_i < results.length; result_i++) {
                        if (results[result_i].right.name.equals(seriesKey)) {
                            hidden[result_i] = !hidden[result_i];
                        }
                    }
                    updatePlot();
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });

        // auto update plots while simulation not done
        autoUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                }
                boolean changed = false;
                boolean done = true;
                for (int result_i = 0; result_i < results.length; result_i++) {
                    double t = results[result_i].right.getTime();
                    if (plotted_times[result_i] != t) {
                        changed = true;
                    }
                    if (t < results[result_i].left.end_time) {
                        done = false;
                    }
                }
                if (!changed) {
                    if (done) break;
                    continue;
                }
                SwingUtilities.invokeLater(() -> {
                    updatePlot();
                });
            }
        });
        autoUpdater.start();

        // layout
        JPanel panelSetting = new JPanel();
        panelSetting.add(new JLabel("Use levels of:"));
        panelSetting.add(comboBoxSetting);
        panelSetting.add(buttonShowSetting);
        
        JPanel panelBottom = new JPanel(new WrapLayout());
        panelBottom.add(buttonHideAll);
        panelBottom.add(buttonShowAll);
        panelBottom.add(panelSetting);
        panelBottom.add(comboBoxSpecies);
        panelBottom.add(checkBoxLevels);
        panelBottom.add(checkBoxForceOutputSpecies);

        JPanel panelAll = new JPanel(new BorderLayout());
        panelAll.add(panelChart, BorderLayout.CENTER);
        panelAll.add(panelBottom, BorderLayout.SOUTH);

        add(panelAll);

        pack();
        setVisible(true);
    }

    @Override
    public void dispose() {
        autoUpdater.interrupt();
        super.dispose();
    }
    
    private Setting getSelectedSetting() {
        if (comboBoxSetting == null) return open_setting;
        int index = comboBoxSetting.getSelectedIndex();
        return index < results.length ? results[index].left : open_setting;
    }

    private JFreeChart updatePlot() {
        for (int result_i = 0; result_i < results.length; result_i++) {
            plotted_times[result_i] = results[result_i].right.getTime();
        }
        
        Setting setting = getSelectedSetting();

        setting.recomputeIntervals(checkBoxForceOutputSpecies == null ? false : checkBoxForceOutputSpecies.isSelected());

        JFreeChart newChart = Plotter.plotSimulations(
                results, 
                setting, 
                comboBoxSpecies == null ? 0 : comboBoxSpecies.getSelectedIndex(), 
                checkBoxLevels == null ? true : !checkBoxLevels.isSelected()
        );
        updateVisibility(newChart);
        if (panelChart != null) {
            if (!panelChart.getChart().getXYPlot().getDomainAxis().isAutoRange()) {
                newChart.getXYPlot().getDomainAxis().setRange(panelChart.getChart().getXYPlot().getDomainAxis().getRange());
            }
            panelChart.setChart(newChart);
        }

        return newChart;
    }

    private void updateVisibility(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
            XYDataset dataset = plot.getDataset(dataset_i);
            XYItemRenderer renderer = plot.getRenderer(dataset_i);
            for (int series_i = 0; series_i < dataset.getSeriesCount(); series_i++) {
                Comparable seriesKey = dataset.getSeriesKey(series_i);
                for (int result_i = 0; result_i < results.length; result_i++) {
                    if (results[result_i].right.name.equals(seriesKey)) {
                        renderer.setSeriesVisible(series_i, !hidden[result_i], true);
                    }
                }
            }
        }
    }
}
