package core.util;

import core.model.CRN;
import core.model.Reaction;
import core.model.Setting;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.StringCharacterIterator;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.stream.Stream;

public class IO {

    public final static DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public final static DateTimeFormatter dateTimeFormatterForFileNames = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm.ss.SSS");

    public static final File CODE_FOLDER = new File("").getAbsoluteFile();
    public static final File BASE_FOLDER = CODE_FOLDER.getParentFile();
    public static final File RESULTS_FOLDER = new File(BASE_FOLDER, "results");
    public static final File DATA_FOLDER = new File(BASE_FOLDER, "data");
    public static final File SETTINGS_FOLDER = new File(DATA_FOLDER, "settings");
    public static final File EXAMPLE_FOLDER = new File(SETTINGS_FOLDER, "examples");
    public static final File EXAMPLE_DSD_FOLDER = new File(EXAMPLE_FOLDER, "DSD");
    public static final File BENCHMARK_FOLDER = new File(RESULTS_FOLDER, "benchmark");
    public static File CURRENT_BENCHMARK_FOLDER = new File(BENCHMARK_FOLDER, IO.getCurTimeStringForFileNames());
//    public static File CURRENT_BENCHMARK_FOLDER = new File(BENCHMARK_FOLDER, "20230116_sequence_speed_singletask3");

    public static final File DEFAULT_BENCHMARK_FILE = new File(IO.DATA_FOLDER, "benchmarks.json");

    public static File getMostRecentBenchmarkOutputFolder() {
        File res = null;
        long lastMod = -1;
        for (File f : BENCHMARK_FOLDER.listFiles()) {
            if (f.isDirectory() && f.lastModified() > lastMod) {
                res = f;
                lastMod = f.lastModified();
            }
        }
        return res;
    }

    public static String getCurTimeString() {
        return dateTimeFormatter.format(LocalDateTime.now());
    }

    public static String getCurTimeStringForFileNames() {
        String res = dateTimeFormatterForFileNames.format(LocalDateTime.now());
        return res.replace(" ", "_");
    }

    public static String significantFigures(double d) {
        return significantFigures(d, 4);
    }

    public static String significantFigures(double d, int sign_figures) {
        return significantFigures(d, sign_figures, false);
    }

    public static String significantFigures(double d, int sign_figures, boolean forceScientific) {
        String res = String.format(Locale.ENGLISH, "%1." + (sign_figures - 1) + "e", d);
        if (!forceScientific) {
            res = "" + Double.valueOf(res);
        }
        // cut ending '.0'
        if (res.length() > 2 && res.substring(res.length() - 2).equals(".0")) {
            res = res.substring(0, res.length() - 2);
        }
        return res;
    }

    public static String toString(Double d) {
        String res = d + "";
        if (res.endsWith(".0")) {
            res = res.substring(0, res.length() - 2);
        }
        return res;
    }

    // source: https://stackoverflow.com/questions/3758606/how-can-i-convert-byte-size-into-a-human-readable-format-in-java
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + "B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format(Locale.ENGLISH, "%.1f%cB", bytes / 1000.0, ci.current());
    }

    public static String humanReadableDuration(long nano) {
        double dur = nano;
        String[] units = new String[]{"ns", "micros", "ms", "s", "min", "h", "d", "y"};
        double[] conversions = new double[]{1000, 1000, 1000, 60, 60, 24, 365.24};

        int i = 0;
        for (; i < conversions.length; i++) {
            if (-conversions[i] < dur && dur < conversions[i]) {
                break;
            }
            dur /= conversions[i];
        }

        if (-10 < dur && dur < 10) {
            return String.format(Locale.ENGLISH, "%.1f", dur) + units[i];
        }
        return ((int) Math.round(dur)) + units[i];
    }

    public static String getContentOfFile(File file) {
        if (!file.exists()) {
            return null;
        }
        Path filePath = file.toPath();
        StringBuilder contentBuilder = new StringBuilder();

        try ( Stream<String> stream = Files.lines(filePath, StandardCharsets.UTF_8)) {
            //Read the content with Stream
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String fileContent = contentBuilder.toString();
        return fileContent;
    }

    public static long writeStringToFile(File file, String s) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            writer.write(s);
            writer.flush();
            writer.close();
            return file.length();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return 0;
    }

    public static long writeObjectToJsonFile(File file, Object o) {
        try {
            FileWriter f = new FileWriter(file);
            JSON.getGson().toJson(o, f);
            f.flush();
            f.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return file.length();
    }

    public static long writeObjectToJsonFile(File file, Object o, Type t) {
        try {
            FileWriter f = new FileWriter(file);
            JSON.getGson().toJson(o, t, f);
            f.flush();
            f.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return file.length();
    }

    public static PrintStream tryToGetFileStream(File f) {
        try {
            return new PrintStream(f);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not create file stream " + f.getPath() + " : " + ex.getMessage());
        }
        return System.out;
    }

    public static class DublicateToFilePrintStream extends PrintStream {

        public DublicateToFilePrintStream(File file) throws FileNotFoundException {
            super(file);
        }

        @Override
        public void println() {
            System.out.println();
            super.println();
        }

        @Override
        public void println(final int output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public void println(final double output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public void println(final float output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public void println(final long output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public void println(final String output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public void println(final Object output) {
            System.out.println(output);
            super.println(output);
        }

        @Override
        public PrintStream printf(final String output, final Object... variables) {
            System.out.printf(output, variables);
            super.printf(output, variables);
            return this;
        }

        public static PrintStream tryWith(File file) {
            try {
                return new DublicateToFilePrintStream(file);
            } catch (FileNotFoundException ex) {
                System.out.println("can only print to System.out: " + ex.getMessage());
                return System.out;
            }
        }
    }

    public static Setting fromClassicDSD(File file) {
        HashMap<String, Integer> names = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            boolean commentBlock = false;
            while (line != null) {
                if (line.contains("(\\*")) {
                    commentBlock = true;
                }
                if (line.contains("\\*)")) {
                    commentBlock = false;
                }
                if (commentBlock) {
                    continue;
                }

                line = line.replaceAll("\\s+", "");
                line = line.replaceAll("//.*", "");
                if (line.length() > 0 && line.charAt(0) == '|') {
                    line = line.substring(1);
                    if (line.contains("->")) {
                        String[] splitted = line.split("->");
                        String[] reactants = splitted[0].split("\\+");
                        for (String reactant : reactants) {
                            if (!names.containsKey(reactant)) {
                                names.put(reactant, names.size());
                            }
                        }
                        if (splitted.length >= 1) {
                            splitted = splitted[1].substring(1).split("}");
                        }
                        if (splitted.length > 1) {
                            String[] products = splitted[1].split("\\+");
                            for (String product : products) {
                                if (!names.containsKey(product)) {
                                    names.put(product, names.size());
                                }
                            }
                        }
                    }
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int dim = names.size();

        ArrayList<Reaction> rs = new ArrayList<>();
        int[] init = new int[dim];
        int[] bounds = new int[dim];
        for (int dim_i = 0; dim_i < dim; dim_i++) {
            bounds[dim_i] = 20000;
        }

        try {
            reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            boolean commentBlock = false;
            while (line != null) {
                if (line.contains("(\\*")) {
                    commentBlock = true;
                }
                if (line.contains("\\*)")) {
                    commentBlock = false;
                }
                if (commentBlock) {
                    continue;
                }
                if (line.length() > 0 && line.charAt(0) == '|') {
                    line = line.substring(1);
                    if (line.contains("->")) {
                        line = line.replaceAll("\\s+", "");
                        line = line.replaceAll("//.*", "");
                        String[] splitted = line.split("->");
                        String[] reactants = splitted[0].split("\\+");
                        int[] reactants2 = new int[dim];
                        for (String reactant : reactants) {
                            reactants2[names.get(reactant)]++;
                        }
                        double rate_contant = 1.0;
                        if (splitted.length >= 1) {
                            splitted = splitted[1].substring(1).split("}");
                            rate_contant = Double.parseDouble(splitted[0]);
                        }
                        int[] products2 = new int[dim];
                        if (splitted.length > 1) {
                            String[] products = splitted[1].split("\\+");
                            for (String product : products) {
                                products2[names.get(product)]++;
                            }
                        }
                        rs.add(new Reaction(reactants2, products2, rate_contant, "r" + rs.size()));
                    } else {
                        line = line.trim();
                        String[] splitted = line.split(" ");
                        System.out.println(names.get(splitted[1]));
                        init[names.get(splitted[1])] = Integer.parseInt(splitted[0]);
                    }
                }
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] species_names = new String[names.size()];
        for (String name : names.keySet()) {
            species_names[names.get(name)] = name;
        }

        return new Setting(
                file.getName(),
                new CRN(file.getName(), species_names, rs.toArray(new Reaction[rs.size()])),
                init,
                bounds,
                null,
                2.0,
                100
        );
    }

    public static String toClassicDSD(Setting setting) {
        String res = "// SeQuaiA export of setting " + setting.name + " \n";
        res += "// date: " + getCurTimeString() + " \n\n";

        res += "directive simulation {final=" + setting.end_time + "; points=10000}\n";
        res += "directive simulator stochastic\n\n";
        res += "// initial state \n";
        for (int dim_i = 0; dim_i < setting.dim(); dim_i++) {
            res += "| " + setting.initial_state[dim_i] + " " + setting.crn.speciesNames[dim_i] + "\n";
        }
        res += "\n";
        res += "// reactions \n";
        for (Reaction r : setting.crn.reactions) {
            res += "| " + r.toString(setting.crn) + "\n";
        }
        res += "\n";

        res += "\n\n\n\n(*\nJSON export of setting: \n\n" + setting.toJson() + "\n*)\n";
        return res;
    }
    
    public static String removeWhiteSpaces(String s) {
        return s.replaceAll("\\s+", "");
    }

    public static String speciesNamePreprocessed(String s) {
        return removeWhiteSpaces(s);
    }

    public static boolean isValidSpeciesName(String s) {
        return !(s.length() == 0
                || !speciesNamePreprocessed(s).equals(s)
                || !Character.isAlphabetic(s.charAt(0))
                || s.contains("/")
                || s.contains("{")
                || s.contains("}")
                || s.contains("-")
                || s.contains(","));
    }

    public static boolean isValidNewSpeciesName(String s, CRN crn) {
        if (!isValidSpeciesName(s)) {
            return false;
        }
        for (int dim_i = 0; dim_i < crn.dim(); dim_i++) {
            if (crn.speciesNames[dim_i].strip().equals(s)) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
//        writeStringToFile(new File("repressilator_DSD.txt"), toClassicDSD(Examples.getSettingByName("repressilator_canonical.json"), 50000));
//        writeStringToFile(new File("toggle_switch_advanced_canonical_DSD.txt"), toClassicDSD(Examples.getSettingByName("toggle_switch_advanced_canonical.json"), 50000));
//        writeStringToFile(new File("viral_withV_canonical_DSD.txt"), toClassicDSD(Examples.getSettingByName("viral_withV_canonical.json"), 200));
        System.out.println("BASE_FOLDER: " + BASE_FOLDER.getAbsolutePath());
        System.out.println("CODE_FOLDER: " + CODE_FOLDER.getAbsolutePath());
        System.out.println("DATA_FOLDER: " + DATA_FOLDER.getAbsolutePath());
        System.out.println("RESULTS_FOLDER: " + RESULTS_FOLDER.getAbsolutePath());
        System.out.println("SETTINGS_FOLDER: " + SETTINGS_FOLDER.getAbsolutePath());
        System.out.println("EXAMPLE_FOLDER: " + EXAMPLE_FOLDER.getAbsolutePath());
        System.out.println("EXAMPLE_DSD_FOLDER: " + EXAMPLE_DSD_FOLDER.getAbsolutePath());
        System.out.println("BENCHMARK_FOLDER: " + BENCHMARK_FOLDER.getAbsolutePath());
        System.out.println("CURRENT_BENCHMARK_FOLDER: " + CURRENT_BENCHMARK_FOLDER.getAbsolutePath());
        System.out.println("DEFAULT_BENCHMARK_FILE: " + DEFAULT_BENCHMARK_FILE.getAbsolutePath());
    }
    
    public static double parseDouble(String s) {
        s = removeWhiteSpaces(s);
        if (s.equalsIgnoreCase("inf") || s.equalsIgnoreCase("infty") || s.equalsIgnoreCase("infinity")) return Double.POSITIVE_INFINITY;
        if (s.equalsIgnoreCase("-inf") || s.equalsIgnoreCase("-infty") || s.equalsIgnoreCase("-infinity")) return Double.NEGATIVE_INFINITY;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {}
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.getDefault());
            return format.parse(s).doubleValue();
        } catch (ParseException e) {}
        return Double.NaN;
    }
}
