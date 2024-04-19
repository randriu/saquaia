package gui.visualization;

import core.analysis.TransientAnalysisResult;
import core.model.Setting;
import core.util.Pair;
import gui.SettingPanel;
import gui.Util;
import gui.WrapLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
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
public class TransientAnalysisVisualization extends JFrame {

    private final Setting setting;
    private final Pair<Setting, TransientAnalysisResult>[] results;

    private final ChartPanel panelChart;

    private final JComboBox comboBoxSpecies, comboBoxResults;
    private final JCheckBox checkBoxLevels, checkBoxRelative, checkBoxShowEMDs;
    private final JButton buttonShowAll, buttonHideAll, buttonShowSetting;

    private boolean[] hidden;
    private final int[] plotted_nr_of_results;

    private final Thread autoUpdater;

    public TransientAnalysisVisualization(Setting initial_setting, TransientAnalysisResult result) {
        this(initial_setting, new Pair[]{new Pair(initial_setting, result)});
    }

    public TransientAnalysisVisualization(Setting initial_setting, Pair<Setting, TransientAnalysisResult>[] results) {
        super("Transient Distribution of " + initial_setting.name);

        this.setting = initial_setting.copy();
        this.results = results;
        this.hidden = new boolean[results.length];
        this.plotted_nr_of_results = new int[results.length];   // to update plot if more data was added

        setIconImage(Util.getIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        comboBoxSpecies = new JComboBox(setting.crn.speciesNames);
        checkBoxLevels = new JCheckBox("Show Levels");
        checkBoxLevels.setSelected(true);
        checkBoxRelative = new JCheckBox("Relative");
        checkBoxRelative.setSelected(true);
        checkBoxShowEMDs = new JCheckBox("Show EMD to");
        checkBoxShowEMDs.setSelected(false);
        String[] result_names = new String[results.length];
        for (int result_i = 0; result_i < results.length; result_i++) result_names[result_i] = results[result_i].right.name;
        comboBoxResults = new JComboBox(result_names);

        buttonHideAll = new JButton("Hide All");
        buttonHideAll.addActionListener((ActionEvent e) -> {
            for (int res_i = 0; res_i < results.length; res_i++) {
                hidden[res_i] = true;
            }
            updatePlot();
        });
        buttonShowAll = new JButton("Show All");
        buttonShowAll.addActionListener((ActionEvent e) -> {
            for (int res_i = 0; res_i < results.length; res_i++) {
                hidden[res_i] = false;
            }
            updatePlot();
        });
        buttonShowSetting = new JButton("Show Setting");
        buttonShowSetting.addActionListener((e) -> {
            SettingPanel panelSetting = new SettingPanel(this.setting);
            panelSetting.setEnabled(false);
            JOptionPane.showMessageDialog(this, panelSetting, "Setting used for levels / bounds.", JOptionPane.PLAIN_MESSAGE);
        });

        ActionListener plotUpdater = (e) -> {
            updatePlot();
        };

        comboBoxSpecies.addActionListener(plotUpdater);
        checkBoxLevels.addActionListener(plotUpdater);
        checkBoxRelative.addActionListener(plotUpdater);
        checkBoxShowEMDs.addActionListener(plotUpdater);
        comboBoxResults.addActionListener(plotUpdater);

        panelChart = new ChartPanel(updatePlot(), false);
        panelChart.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                ChartEntity entity = chartMouseEvent.getEntity();
                if (chartMouseEvent.getEntity() instanceof LegendItemEntity) {
                    LegendItemEntity itemEntity = (LegendItemEntity) entity;
                    Comparable seriesKey = itemEntity.getSeriesKey();

                    for (int res_i = 0; res_i < results.length; res_i++) {
                        if (results[res_i].right.name.equals(seriesKey)) {
                            hidden[res_i] = !hidden[res_i];
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
                boolean new_result = false;
                for (int result_i = 0; result_i < results.length; result_i++) {
                    if (results[result_i].right.results > plotted_nr_of_results[result_i]) {
                        new_result = true;
                        break;
                    }
                }
                if (!new_result) {
                    continue;
                }
                SwingUtilities.invokeLater(() -> {
                    updatePlot();
                });
            }
        });
        autoUpdater.start();

        // layout 
        JPanel panelEMD = new JPanel();
        panelEMD.add(checkBoxShowEMDs);
        panelEMD.add(comboBoxResults);
        
        JPanel panelBottom = new JPanel(new WrapLayout());
        if (results.length > 1) {
            panelBottom.add(buttonHideAll);
            panelBottom.add(buttonShowAll);
        }
        panelBottom.add(buttonShowSetting);
        panelBottom.add(comboBoxSpecies);
        panelBottom.add(checkBoxLevels);
        panelBottom.add(checkBoxRelative);
        if (results.length > 1) {
            panelBottom.add(panelEMD);
        }
        panelBottom.setMaximumSize(new Dimension(500, 100));

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
        for (int result_i = 0; result_i < results.length; result_i++) {
            plotted_nr_of_results[result_i] = results[result_i].right.results;
        }
        JFreeChart newChart = Plotter.plotTransientAnalysisDistribution(
                results, 
                comboBoxResults.getSelectedIndex(),
                setting, 
                comboBoxSpecies == null ? 0 : comboBoxSpecies.getSelectedIndex(), 
                checkBoxLevels == null ? true : !checkBoxLevels.isSelected(), 
                checkBoxRelative == null ? true : checkBoxRelative.isSelected(), 
                checkBoxShowEMDs == null ? false : checkBoxShowEMDs.isSelected()
        );
        if (results.length > 1) {
            updateVisibility(newChart);
        }
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
        LegendItemCollection legendItems = plot.getLegendItems();
        plot.setFixedLegendItems(legendItems);
        for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
            XYDataset dataset = plot.getDataset(dataset_i);
            XYItemRenderer renderer = plot.getRenderer(dataset_i);
            for (int series_i = 0; series_i < dataset.getSeriesCount(); series_i++) {
                Comparable seriesKey = dataset.getSeriesKey(series_i);
                for (int res_i = 0; res_i < results.length; res_i++) {
                    if (results[res_i].right.name.equals(seriesKey)) {
                        renderer.setSeriesVisible(series_i, !hidden[res_i], true);
                    }
                }
            }
        }
    }
}
