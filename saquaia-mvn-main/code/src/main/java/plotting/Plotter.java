/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package plotting;

import com.google.gson.JsonSyntaxException;
import core.analysis.TransientAnalysisResult;
import core.model.Setting;
import core.util.IO;
import core.util.JSON;
import core.util.Pair;
import core.util.Stochastics;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.LegendItemEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.Dataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import core.simulation.Simulation;
import core.util.EMD_Helper;
import gui.Util;
import org.jfree.chart.annotations.XYTitleAnnotation;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.VerticalAlignment;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYBarDataset;

/**
 *
 * @author Martin
 */
public class Plotter {

    public static final Color[] my_colors = new Color[]{
        // kelly colors
        new Color(255, 179, 0), // vivid_yellow
        new Color(128, 62, 117), // strong_purple
        new Color(255, 104, 0), // vivid_orange
        new Color(166, 189, 215), // very_light_blue
        new Color(193, 0, 32), // vivid_red
        new Color(206, 162, 98), // grayish_yellow
        new Color(129, 112, 102), // medium_gray
        new Color(0, 125, 52), // vivid_green
        new Color(246, 118, 142), // strong_purplish_pink
        new Color(0, 83, 138), // strong_blue
        new Color(255, 122, 92), // strong_yellowish_pink
        new Color(83, 55, 122), // strong_violet
        new Color(255, 142, 0), // vivid_orange_yellow
        new Color(179, 40, 81), // strong_purplish_red
        new Color(244, 200, 0), // vivid_greenish_yellow
        new Color(127, 24, 13), // strong_reddish_brown
        new Color(147, 170, 0), // vivid_yellowish_green
        new Color(89, 51, 21), // deep_yellowish_brown
        new Color(241, 58, 19), // vivid_reddish_orange
        new Color(35, 44, 22), // dark_olive_green

        // boynto colors
        new Color(0, 0, 255), // Blue
        new Color(255, 0, 0), // Red
        new Color(0, 255, 0), // Green
        new Color(255, 255, 0), // Yellow
        new Color(255, 0, 255), // Magenta
        new Color(255, 128, 128), // Pink
        new Color(128, 128, 128), // Gray
        new Color(128, 0, 0), // Brown
        new Color(255, 128, 0), // Orange
    };

    public static final BasicStroke STROKE_NORMAL = new BasicStroke(2.0f);
    public static final BasicStroke STROKE_DASHED = new BasicStroke(
            2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            1.0f, new float[]{6.0f, 6.0f}, 0.0f
    );
    public static final BasicStroke STROKE_DOTTED = new BasicStroke(
            2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
            1.0f, new float[]{2.0f, 2.0f}, 0.0f
    );

    public static BasicStroke randomStroke() {
        BasicStroke[] strokes = new BasicStroke[]{STROKE_NORMAL, STROKE_DASHED, STROKE_DOTTED};
        return Stochastics.choose(new Random(), strokes);
    }

    public static JFreeChart plotSimulation(Simulation simulation, Setting setting) {
        return plotSimulation(simulation, setting, true);
    }

    public static JFreeChart plotSimulation(Simulation simulation, Setting setting, boolean concrete) {
        // Prepare series for selected simulation
        XYSeries[] series = new XYSeries[simulation.dim()];
        XYSeriesCollection data = new XYSeriesCollection();
        for (int speciesIndex = 0; speciesIndex < setting.dim(); speciesIndex++) {
            series[speciesIndex] = new XYSeries(setting.crn.speciesNames[speciesIndex]);
            data.addSeries(series[speciesIndex]);
        }

        for (Pair<double[], Double> pair : simulation.getHistory()) {
            double[] state = pair.left;
            double time = pair.right;
            for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
                double x = state[dim_i];
                if (concrete) {
                    series[dim_i].add(time, x);
                } else {
                    series[dim_i].add(time, setting.intervalIndexFor(dim_i, x));
                }
            }
        }

        JFreeChart chart = ChartFactory.createXYLineChart(simulation.name, "Time (in s)", concrete ? "Copy Number" : "Level", data);

        // integer values for copy numbers / levels
        chart.getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        betterColors(chart);
        improve(chart);
        return chart;
    }

    public static JFreeChart plotSimulations(Pair<Setting, Simulation>[] results, Setting setting, int dim_i, boolean concrete) {
        String species = setting.crn.speciesNames[dim_i];

        XYSeriesCollection data = new XYSeriesCollection();
        for (Pair<Setting, Simulation> p : results) {
            Setting setting_i = p.left;
            Simulation sim = p.right;
            int dim_j = setting_i.crn.getDimensionForSpeciesName(species);
            if (dim_j == -1) {
                continue;
            }
            XYSeries series = new XYSeries(sim.name);
            for (Pair<double[], Double> pair : sim.getHistory()) {
                double[] state = pair.left;
                double time = pair.right;
                double x = state[dim_j];
                if (concrete) {
                    series.add(time, x);
                } else {
                    series.add(time, setting.intervalIndexFor(dim_i, x));
                }
            }
            data.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Evolutions of " + species,
                "Time (in s)",
                concrete ? "Copy Number" : "Level",
                data
        );

        // integer values for copy numbers / levels
        chart.getXYPlot().getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        betterColors(chart);
        improve(chart);
        return chart;
    }

    public static JFreeChart plotTransientAnalysisDistribution(TransientAnalysisResult result, Setting setting, int dim_i, boolean conrete, boolean relative, boolean showEMDs) {
        return plotTransientAnalysisDistribution(new Pair[]{new Pair<>(setting, result)}, 0, setting, dim_i, conrete, relative, showEMDs);
    }

    public static JFreeChart plotTransientAnalysisDistribution(Pair<Setting, TransientAnalysisResult>[] results, int baseline, Setting setting, int dim_i, boolean concrete, boolean relative, boolean showEMDs) {
        setting = setting.copy();
        setting.recomputeIntervals(true);
        String species = setting.crn.speciesNames[dim_i];

        // prepare data
        HashMap<Integer, Integer>[] data = new HashMap[results.length];
        for (int result_i = 0; result_i < results.length; result_i++) {
            Setting setting_i = results[result_i].left;
            TransientAnalysisResult result = results[result_i].right;
            int datapoints = result.results;
            int dim_j = setting_i.crn.getDimensionForSpeciesName(species);
            if (dim_j == -1) {
                continue;
            }
            data[result_i] = new HashMap<>();
            for (int data_i = 0; data_i < datapoints; data_i++) {
                double state = result.states.get(data_i)[dim_j];
                int v = concrete ? ((int) Math.floor(state)) : setting.intervalIndexFor(dim_i, state);
                data[result_i].put(v, data[result_i].getOrDefault(v, 0) + 1);
            }
        }

        // draw
        DefaultXYDataset dataset = new DefaultXYDataset();
        for (int result_i = 0; result_i < results.length; result_i++) {
            double[][] vs = new double[2][data[result_i].size()];
            int vs_i = 0;
            for (Integer x : data[result_i].keySet()) {
                vs[0][vs_i] = x;
                vs[1][vs_i] = relative ? (1.0 * data[result_i].get(x) / results[result_i].right.results) : data[result_i].get(x);
                vs_i++;
            }
            String series_name = results[result_i].right.name;
            dataset.addSeries(series_name, vs);
        }
        JFreeChart chart = ChartFactory.createXYBarChart(
                (relative ? "Transient Distribution" : "Histogram") + " at t=" + setting.end_time + " for " + species,
                concrete ? "Copy-Number" : "Level",
                false,
                relative ? "Mass" : "#",
                new XYBarDataset(dataset, 1)
        );

        betterColors(chart);

        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setForegroundAlpha(0.7f);
        if (!relative) {
            plot.getRangeAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        }
        plot.getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // flat bars look best...
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);

        // add EMDs as annotation (if relative)
        if (showEMDs && relative) {
            String[] EMD_annotation_strings = new String[results.length];
            for (int result_i = 0; result_i < results.length; result_i++) {
                double EMD = result_i == baseline ? 0 : EMD_Helper.EMD(data[baseline], data[result_i]);
                EMD_annotation_strings[result_i] = IO.significantFigures(EMD);
                if (EMD_annotation_strings[result_i].length() < 8) EMD_annotation_strings[result_i] += " ".repeat(8- EMD_annotation_strings[result_i].length());
                EMD_annotation_strings[result_i] += results[result_i].right.name;
            }
            TextTitle textTitle = new TextTitle(
                    "EMD     Distribution\n" + String.join("\n", EMD_annotation_strings),
                    Util.FONT_MONO,
                    Color.BLACK,
                    RectangleEdge.TOP,
                    HorizontalAlignment.LEFT,
                    VerticalAlignment.TOP,
                    new RectangleInsets(2, 4, 2, 4)
            );
            XYTitleAnnotation annotation = new XYTitleAnnotation(
                    0, 
                    1, 
                    textTitle, 
                    RectangleAnchor.TOP_LEFT
            );
            annotation.setToolTipText(
                    "<html><body><h3>Earth Mover Distance (EMD)</h3><ul><li>Messures the distance between two distributions.</li><li>Can be understood as work of turning a distribution into another distribution where work is probability mass times distance.</li><li>Reference distribution: " + results[baseline].right.name + "</li><li>More information: <a href=\"https://en.wikipedia.org/wiki/Earth_mover%27s_distance\"></a></li></ul></body></html>");
            plot.addAnnotation(annotation);
        }

        return chart;
    }

    public static JFreeChart plotSpeedup(TransientAnalysisResult[] results, int reference) {
        double reference_speed = results[reference].avg_comp_time();
        // Prepare data
        XYSeriesCollection data = new XYSeriesCollection();
        XYSeries base = new XYSeries("Reference");
        data.addSeries(base);
        base.add(1, 1);
        int max_nr_of_results = 0;
        for (int res_i = 0; res_i < results.length; res_i++) {
            TransientAnalysisResult result = results[res_i];
            XYSeries series = new XYSeries(result.name);
            for (int i = 0; i < result.results; i++) {
                series.add(i + 1, reference_speed / result.avg_comp_time(i));
            }
            data.addSeries(series);
            max_nr_of_results = Math.max(max_nr_of_results, result.results);
        }
        base.add(max_nr_of_results, 1);

        JFreeChart chart = ChartFactory.createXYLineChart("Speedup vs " + results[reference].name, "Number of Simulations", "Speedup Factor", data);

        chart.getXYPlot().getDomainAxis().setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        // make reference dashed
        changeStroke(chart, 0, STROKE_DASHED);

        betterColors(chart);
        improve(chart);

        return chart;
    }

    public static void improve(JFreeChart chart) {
        try {
            XYPlot plot = (XYPlot) chart.getPlot();
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setDefaultStroke(STROKE_NORMAL);
            renderer.setAutoPopulateSeriesStroke(false);
            LegendItemCollection legendItems = plot.getLegendItems();
            plot.setFixedLegendItems(legendItems);
        } catch (ClassCastException e) {
        }
    }

    public static void varyStroke(JFreeChart chart) {
        try {
            XYPlot plot = (XYPlot) chart.getPlot();
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setDefaultStroke(STROKE_NORMAL);
            renderer.setAutoPopulateSeriesStroke(false);
            BasicStroke[] strokes = new BasicStroke[]{STROKE_NORMAL, STROKE_DASHED, STROKE_DOTTED};
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                renderer.setSeriesStroke(i, strokes[i % strokes.length]);
            }
        } catch (ClassCastException e) {
        }
    }

    public static void changeStroke(JFreeChart chart, int series, BasicStroke stroke) {
        try {
            XYPlot plot = (XYPlot) chart.getPlot();
            if (series < 0 || series >= plot.getSeriesCount()) {
                return;
            }
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setDefaultStroke(new BasicStroke(2));
            renderer.setAutoPopulateSeriesStroke(false);
            renderer.setSeriesStroke(series, stroke);
        } catch (ClassCastException e) {
        }
    }

    public static void changeStroke(JFreeChart chart, String series_name, BasicStroke stroke) {
        try {
            XYPlot plot = (XYPlot) chart.getPlot();
            XYSeriesCollection col = (XYSeriesCollection) plot.getDataset();
            int series = col.getSeriesIndex(series_name);
            changeStroke(chart, series, stroke);
        } catch (ClassCastException e) {
        }
    }

    public static void betterColors(JFreeChart chart) {
        try {
            XYPlot plot = (XYPlot) chart.getPlot();
            Paint backgroundPaint = plot.getBackgroundPaint();
            plot.setBackgroundPaint(Color.white);
            plot.setDomainGridlinePaint(backgroundPaint);
            plot.setRangeGridlinePaint(backgroundPaint);
            AbstractXYItemRenderer renderer = (AbstractXYItemRenderer) plot.getRenderer();
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                renderer.setSeriesPaint(i, my_colors[i % my_colors.length]);
            }
        } catch (ClassCastException e) {
        }
    }

    public static void saveAsPNG(JFreeChart chart, File file) {
        saveAsPNG(chart, file, 800, 600, 2, true, true);
    }

    public static void saveAsPNG(JFreeChart chart, File file, int width, int height, int scale, boolean improve, boolean save_data) {
        if (improve) {
            improve(chart);
        }
        if (save_data) {
            File dataDir = new File(file.getParentFile(), "plotting_data");
            dataDir.mkdirs();
            storeDataSet(chart, new File(dataDir, file.getName() + ".csv"));
            storeMetaData(chart, new File(dataDir, file.getName() + ".csv.meta"));
        }
        try {
            ChartUtils.writeScaledChartAsPNG(
                    new FileOutputStream(file),
                    chart,
                    width,
                    height,
                    scale,
                    scale
            );
        } catch (IOException ex) {
            System.out.println("Could save chart...");
            System.out.println(ex.getLocalizedMessage());
        }
    }

    // https://stackoverflow.com/a/58530238
    public static void storeDataSet(JFreeChart chart, File file) {
        java.util.List<String> csv = new ArrayList<>();
        if (chart.getPlot() instanceof XYPlot) {
            Dataset dataset = chart.getXYPlot().getDataset();
            XYDataset xyDataset = (XYDataset) dataset;
            int seriesCount = xyDataset.getSeriesCount();
            for (int i = 0; i < seriesCount; i++) {
                int itemCount = xyDataset.getItemCount(i);
                for (int j = 0; j < itemCount; j++) {
                    Comparable key = xyDataset.getSeriesKey(i);
                    Number x = xyDataset.getX(i, j);
                    Number y = xyDataset.getY(i, j);
                    csv.add(String.format("%s, %s, %s", key, x, y));
                }
            }

        } else if (chart.getPlot() instanceof CategoryPlot) {
            Dataset dataset = chart.getCategoryPlot().getDataset();
            CategoryDataset categoryDataset = (CategoryDataset) dataset;
            int columnCount = categoryDataset.getColumnCount();
            int rowCount = categoryDataset.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                for (int j = 0; j < columnCount; j++) {
                    Comparable key = categoryDataset.getRowKey(i);
                    Number n = categoryDataset.getValue(i, j);
                    csv.add(String.format("%s, %s", key, n));
                }
            }
        } else {
            System.out.println("Unknown dataset");
        }
        try ( BufferedWriter writer = new BufferedWriter(new FileWriter(file));) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Cannot write dataset");
            System.out.println(e.getLocalizedMessage());
        }
    }

    public static class Metadata {

        public String titel;
        public String x_axis_label;
        public boolean x_axis_is_log;
        public String y_axis_label;
        public boolean y_axis_is_log;
        public HashMap<String, int[]> colors = new HashMap<>();
    }

    public static Metadata getMetadata(JFreeChart chart) {
        Metadata res = new Metadata();
        res.titel = chart.getTitle() == null ? "" : chart.getTitle().getText();
        res.x_axis_label = chart.getXYPlot().getDomainAxis().getLabel();
        res.x_axis_is_log = (chart.getXYPlot().getDomainAxis()) instanceof LogarithmicAxis;
        res.y_axis_label = chart.getXYPlot().getRangeAxis().getLabel();
        res.y_axis_is_log = (chart.getXYPlot().getRangeAxis()) instanceof LogarithmicAxis;

        try {
            XYPlot plot = chart.getXYPlot();
            XYItemRenderer renderer = plot.getRenderer();
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                String label = plot.getDataset().getSeriesKey(i).toString();
                Paint seriesPaint = renderer.getSeriesPaint(i);
                try {
                    Color color = (Color) seriesPaint;
                    if (color != null) {
                        res.colors.put(label, new int[]{color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()});
                    }
                } catch (ClassCastException e) {
                }
            }
        } catch (ClassCastException e) {
        }

        return res;
    }

    public static void applyMetadata(JFreeChart chart, Metadata metadata) {
        try {
            chart.setTitle(metadata.titel);
            XYPlot plot = chart.getXYPlot();

            if (metadata.x_axis_is_log) {
                plot.setDomainAxis(new LogarithmicAxis(metadata.x_axis_label));
            } else {
                plot.setDomainAxis(new NumberAxis(metadata.x_axis_label));
            }

            if (metadata.y_axis_is_log) {
                plot.setRangeAxis(new LogarithmicAxis(metadata.y_axis_label));
            } else {
                plot.setRangeAxis(new NumberAxis(metadata.y_axis_label));
            }

            XYItemRenderer renderer = plot.getRenderer();
            for (int i = 0; i < plot.getSeriesCount(); i++) {
                String label = plot.getDataset().getSeriesKey(i).toString();
                int[] color_values = metadata.colors.get(label);
                Color c = new Color(color_values[0], color_values[1], color_values[2], color_values[3]);
                renderer.setSeriesPaint(i, c);
            }
        } catch (Exception e) {
        }
    }

    public static void storeMetaData(JFreeChart chart, File file) {
        IO.writeObjectToJsonFile(file, getMetadata(chart));
    }

    public static void showStoredDataSet(File file) {
        BufferedReader reader;
        TreeMap<String, XYSeries> series = new TreeMap<>();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                String[] splitted = line.split(",");
                for (int i = 0; i < splitted.length; i++) {
                    splitted[i] = splitted[i].strip();
                }
                if (splitted.length == 3) {
                    if (!series.containsKey(splitted[0])) {
                        series.put(splitted[0], new XYSeries(splitted[0]));
                    }
                    XYSeries s = series.get(splitted[0]);
                    try {
                        s.add(Double.valueOf(splitted[1]), Double.valueOf(splitted[2]));
                    } catch (NumberFormatException e) {
                    }
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        XYSeriesCollection dataset = new XYSeriesCollection();
        for (XYSeries s : series.values()) {
            dataset.addSeries(s);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(file.getName(), "x", "y", dataset);
        betterColors(chart);
        improve(chart);

        File metaFile = new File(file.getParent(), file.getName() + ".meta");
        if (metaFile.exists()) {
            try {
                Metadata metadata = JSON.getGson().fromJson(IO.getContentOfFile(metaFile), Metadata.class);
                applyMetadata(chart, metadata);
            } catch (JsonSyntaxException e) {
                System.out.println(e.getMessage());
            }
        }

        improve(chart);

        show(chart);
    }

    public static void show(JFreeChart chart) {
        ChartPanel figure = new ChartPanel(chart, 1040, 640, ChartPanel.DEFAULT_MINIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MINIMUM_DRAW_HEIGHT, ChartPanel.DEFAULT_MAXIMUM_DRAW_WIDTH, ChartPanel.DEFAULT_MAXIMUM_DRAW_HEIGHT, true, true, true, true, true, true, true);
        XYPlot plot = chart.getXYPlot();
        LegendItemCollection legendItems = plot.getLegendItems();
        plot.setFixedLegendItems(legendItems);
        figure.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent chartMouseEvent) {
                ChartEntity entity = chartMouseEvent.getEntity();
                if (chartMouseEvent.getEntity() instanceof LegendItemEntity) {
                    LegendItemEntity itemEntity = (LegendItemEntity) entity;
                    if (SwingUtilities.isMiddleMouseButton(chartMouseEvent.getTrigger())) {
                        changeStroke(chart, itemEntity.getSeriesKey().toString(), randomStroke());
                    } else {
                        for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
                            XYDataset dataset = plot.getDataset(dataset_i);
                            XYItemRenderer renderer = plot.getRenderer(dataset_i);
                            int index = dataset.indexOf(itemEntity.getSeriesKey());
                            if (index >= 0) {
                                renderer.setSeriesVisible(index, !renderer.isSeriesVisible(index), true);
                            }
                        }
                    }
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {
            }
        });
        JFrame frame = new JFrame(chart.getTitle() == null ? "Simulation" : chart.getTitle().getText());
        frame.setIconImage(Util.getIcon());
        frame.add(figure);

        frame.setLayout(new BorderLayout());
        frame.add(figure);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton hideAll = new JButton("Hide All");
        hideAll.addActionListener((ActionEvent e) -> {
            for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
                XYDataset dataset = plot.getDataset(dataset_i);
                XYItemRenderer renderer = plot.getRenderer(dataset_i);
                for (int series_i = 0; series_i < dataset.getSeriesCount(); series_i++) {
                    renderer.setSeriesVisible(series_i, false, true);
                }
            }
        });
        buttons.add(hideAll);

        JButton showAll = new JButton("Show All");
        showAll.addActionListener((ActionEvent e) -> {
            for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
                XYDataset dataset = plot.getDataset(dataset_i);
                XYItemRenderer renderer = plot.getRenderer(dataset_i);
                for (int series_i = 0; series_i < dataset.getSeriesCount(); series_i++) {
                    renderer.setSeriesVisible(series_i, true, true);
                }
            }
        });
        buttons.add(showAll);

        JButton varry = new JButton("Varry Strokes");
        varry.addActionListener((ActionEvent e) -> {
            varyStroke(chart);
        });
        buttons.add(varry);

        JButton open = new JButton("Open Dataset");
        open.addActionListener((ActionEvent e) -> {
            frame.dispose();
            main(new String[]{});
        });
        buttons.add(open);

        JButton yscale_log_toogle = new JButton("Y-Scale Log");
        yscale_log_toogle.addActionListener((ActionEvent e) -> {
            for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
                ValueAxis rangeAxis = plot.getRangeAxis();
                if (!(rangeAxis instanceof LogarithmicAxis || rangeAxis instanceof LogAxis)) {
//                    LogAxis axis = new LogAxis(rangeAxis.getLabel());
//                    axis.setBase(10);
//                    LogFormat format = new LogFormat(axis.getBase(), "", "", true);
//                    axis.setNumberFormatOverride(format);
                    LogarithmicAxis axis = new LogarithmicAxis(rangeAxis.getLabel());
//                    axis.setTickUnit(new NumberTickUnit(0.1));
//                    axis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                    plot.setRangeAxis(axis);
                } else {
                    plot.setRangeAxis(new NumberAxis(rangeAxis.getLabel()));
                }
            }
        });
        buttons.add(yscale_log_toogle);

        JButton xscale_log_toogle = new JButton("X-Scale Log");
        xscale_log_toogle.addActionListener((ActionEvent e) -> {
            for (int dataset_i = 0; dataset_i < plot.getDatasetCount(); dataset_i++) {
                ValueAxis domainAxis = plot.getDomainAxis();
                if (!(domainAxis instanceof LogarithmicAxis || domainAxis instanceof LogAxis)) {
//                    LogAxis axis = new LogAxis(domainAxis.getLabel());
                    LogarithmicAxis axis = new LogarithmicAxis(domainAxis.getLabel());
                    plot.setDomainAxis(axis);
                } else {
                    plot.setDomainAxis(new NumberAxis(domainAxis.getLabel()));
                }
            }
        });
        buttons.add(xscale_log_toogle);

        frame.add(buttons, BorderLayout.PAGE_END);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    public static void main(String[] args) {
        if (args.length > 0) {
            showStoredDataSet(new File(args[0]));
        } else {
//            showStoredDataSet(new File(new File(new File(IO.BENCHMARK_FOLDER, "20221121_all"), "PP_Speed"), "comparison_log.png.csv"));
            JFileChooser chooser = new JFileChooser(IO.BENCHMARK_FOLDER);
            chooser.addChoosableFileFilter(new FileNameExtensionFilter("csv file", "csv"));
            chooser.setAcceptAllFileFilterUsed(false);
            int answer = chooser.showDialog(null, "Plot!");
            if (answer == JFileChooser.APPROVE_OPTION) {
                showStoredDataSet(chooser.getSelectedFile());
            }
        }
    }
}
