package gui.visualization;

import core.model.Setting;
import core.simulation.Simulation;
import gui.SettingPanel;
import gui.Util;
import gui.WrapLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
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
public class SimulationVisualization extends JFrame {

    private final ChartPanel panelChart;
    private final JButton buttonHideAll, buttonShowAll, buttonShowSetting;
    private final JCheckBox checkBoxLevels, checkBoxForceOutputSpecies;

    private final Setting setting;
    private final Simulation simulation;
    private double shown_time;

    private final boolean[] hidden;
    
    private final Thread autoUpdater;

    public SimulationVisualization(Setting setting, Simulation simulation) {
        super("Simulation " + simulation.name + " of " + setting.name);
        
        setIconImage(Util.getIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        this.setting = setting.copy();
        this.simulation = simulation;
        this.shown_time = simulation.getTime();

        hidden = new boolean[setting.dim()];

        buttonHideAll = new JButton("Hide All");
        buttonHideAll.addActionListener((ActionEvent e) -> {
            for (int dim_i = 0; dim_i < this.setting.dim(); dim_i++) hidden[dim_i] = true;
            updatePlot();
        });
        buttonShowAll = new JButton("Show All");
        buttonShowAll.addActionListener((ActionEvent e) -> {
            for (int dim_i = 0; dim_i < this.setting.dim(); dim_i++) hidden[dim_i] = false;
            updatePlot();
        });
        buttonShowSetting = new JButton("Show Setting");
        buttonShowSetting.addActionListener((ActionEvent e) -> {
            SettingPanel panelSetting = new SettingPanel(this.setting);
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

                    for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
                        if (setting.crn.speciesNames[dim_i].equals(seriesKey)) {
                            hidden[dim_i] = !hidden[dim_i];
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
            while (simulation.getTime() < this.setting.end_time) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    break;
                }
                if (simulation.getTime() == shown_time) continue;
                SwingUtilities.invokeLater(() -> {
                    updatePlot();
                });
            }
        });
        autoUpdater.start();
        
        // layout
        JPanel panelBottom = new JPanel(new WrapLayout());
        panelBottom.add(buttonHideAll);
        panelBottom.add(buttonShowAll);
        panelBottom.add(buttonShowSetting);
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
    
    private JFreeChart updatePlot() {
        shown_time = simulation.getTime();
        
        setting.recomputeIntervals(checkBoxForceOutputSpecies == null ? false : checkBoxForceOutputSpecies.isSelected());
        
        JFreeChart newChart = Plotter.plotSimulation(simulation, setting, checkBoxLevels == null ? true : !checkBoxLevels.isSelected());
        updateVisibility(newChart);
        if (panelChart != null) {
            if (!panelChart.getChart().getXYPlot().getDomainAxis().isAutoRange()) newChart.getXYPlot().getDomainAxis().setRange(panelChart.getChart().getXYPlot().getDomainAxis().getRange());
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
                for (int dim_i = 0; dim_i < this.setting.dim(); dim_i++) {
                    if (this.setting.crn.speciesNames[dim_i].equals(seriesKey)) {
                        renderer.setSeriesVisible(series_i, !hidden[dim_i], true);
                    }
                }
            }
        }
    }
}
