package core.util;

import core.model.Setting;
import java.io.File;

/**
 *
 * @author Martin
 */
public class Examples {
    
    public static Setting getSettingFromFile(File file) {
        //Try open file
        String content = IO.getContentOfFile(file);
        //Try converting
        try {
            return Setting.fromJson(content);
        } catch (Exception e) {
            System.err.println(e);
            System.out.println(file + " does not contain a vlaid setting!");
        }
        return null;
    }
    
    public static Setting getSettingByName(String name) {
        Setting res = getSettingFromFile(new File(IO.EXAMPLE_FOLDER, name));
        if (res == null) res = getSettingFromFile(new File(IO.SETTINGS_FOLDER, name));
        return res;
    }
    
    public static void repairSettings(){
        IO.EXAMPLE_FOLDER.mkdirs();
        
        File[] directoryListing = IO.EXAMPLE_FOLDER.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (!file.isFile()) continue;
                Setting setting = getSettingFromFile(file);
//                setting.extraLevels = null;
                boolean first = true;
                for(int i = 0; i < setting.dim(); i++) {
                    if (! PopulationLevelHelper.checkIntervals(setting.intervals[i], setting.population_level_growth_factor)) {
                        if (first) {
                            System.out.println();
                            System.out.println("Problems with " + file.getName() + ", c=" + setting.population_level_growth_factor);
                            first = false;
                        }
                        System.out.println("In dimension " + i + "(" + setting.crn.speciesNames[i] + "): " + PopulationLevelHelper.problemsWithIntervals(setting.intervals[i], setting.population_level_growth_factor));
                    }
                }
                setting.recomputeIntervals(false);
                IO.writeObjectToJsonFile(file, setting);
            }
        }
    }
    
    public static void main(String[] args) {
        repairSettings();
        
//        IO.writeObjectToJsonFile(new File(EXAMPLE_FOLDER, "viral_withVlimit.json"), IO.fromClassicDSD(new File(EXAMPLE_DSD_FOLDER, "viral_withVlimit_DSD.txt")));
//        IO.writeStringToFile(new File(EXAMPLE_DSD_FOLDER, "viral_withVlimit_canonical_DSD.txt"), IO.toClassicDSD(Examples.getSettingFromFile(new File(EXAMPLE_FOLDER, "viral_withVlimit_canonical.json")), 2000));
    }
}
