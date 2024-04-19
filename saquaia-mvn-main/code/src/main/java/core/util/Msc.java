/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.util;

import java.io.File;

/**
 *
 * @author Martin
 */
public class Msc {

    public static boolean isPowerOf10(long value) {
        return isPowerOf(value, 10);
    }

    public static boolean isPowerOf2(long value) {
        return isPowerOf(value, 2);
    }

    public static boolean isPowerOf(long value, int c) {
        while (value >= c && value % c == 0) {
            value /= c;
        }
        return value == 1;
    }

    public static void deleteDirectory(File directory) {

        // if the file is directory or not
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();

            // if the directory contains any file
            if (files != null) {
                for (File file : files) {

                    // recursive call if the subdirectory is non-empty
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
}
