package gui.visualization;

import core.analysis.TransientAnalysisResult;
import gui.Util;
import gui.WrapLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
public class SpeedVisualization extends JFrame {

    private final TransientAnalysisResult[] results;

    private final ChartPanel panelChart;

    private final JComboBox comboBoxBase;
    private final JButton buttonShowAll, buttonHideAll;

    private final boolean[] hidden;
    private final int[] plotted_nr_of_results;

    private final Thread autoUpdater;

    public SpeedVisualization(TransientAnalysisResult[] results) {
        super("Speedup comparision");
        this.results = results;
        this.hidden = new boolean[results.length + 1];    // last entry for reference line
        this.plotted_nr_of_results = new int[results.length];   // to update plot if more data was added

        setIconImage(Util.getIcon());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        panelChart = new ChartPanel(updatePlot(), false);
        panelChart.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                ChartEntity entity = chartMouseEvent.getEntity();
                if (chartMouseEvent.getEntity() instanceof LegendItemEntity) {
                    LegendItemEntity itemEntity = (LegendItemEntity) entity;
                    Comparable seriesKey = itemEntity.getSeriesKey();
                    boolean found = false;
                    for (int res_i = 0; res_i < results.length; res_i++) {
                        if (results[res_i].name.equals(seriesKey)) {
                            hidden[res_i] = !hidden[res_i];
                            found = true;
                        }
                    }
                    if (!found) {
                        hidden[hidden.length - 1] = !hidden[hidden.length - 1];
                    }
                    updatePlot();
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });

        String[] result_names = new String[results.length];
        for (int res_i = 0; res_i < results.length; res_i++) {
            result_names[res_i] = results[res_i].name;
        }

        comboBoxBase = new JComboBox(result_names);

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

        comboBoxBase.addActionListener((e) -> {
            updatePlot();
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
                    if (results[result_i].results > plotted_nr_of_results[result_i]) {
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
        JPanel panelBottom = new JPanel(new WrapLayout());
        if (results.length > 1) {
            panelBottom.add(new JLabel("Reference: "));
            panelBottom.add(comboBoxBase);
            panelBottom.add(buttonHideAll);
            panelBottom.add(buttonShowAll);
        }

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
            plotted_nr_of_results[result_i] = results[result_i].results;
        }
        JFreeChart newChart = Plotter.plotSpeedup(
                results,
                comboBoxBase == null ? 0 : comboBoxBase.getSelectedIndex()
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
                boolean found = false;
                for (int res_i = 0; res_i < results.length; res_i++) {
                    if (results[res_i].name.equals(seriesKey)) {
                        renderer.setSeriesVisible(series_i, !hidden[res_i], true);
                        found = true;
                    }
                }
                if (!found) {
                    renderer.setSeriesVisible(series_i, !hidden[hidden.length - 1], true);
                }
            }
        }
    }
}
