package jp.yourorg.fiji_area_segmentater.util;

import ij.IJ;

public class IJLog {
    public static void info(String msg) { IJ.log("[Area_Segmentater] " + msg); }
    public static void warn(String msg) { IJ.log("[Area_Segmentater][WARN] " + msg); }
}
