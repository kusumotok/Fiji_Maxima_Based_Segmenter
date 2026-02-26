package jp.yourorg.fiji_maxima_based_segmenter.util;

import ij.IJ;

public class IJLog {
    public static void info(String msg) { IJ.log("[Maxima_Based_Segmenter] " + msg); }
    public static void warn(String msg) { IJ.log("[Maxima_Based_Segmenter][WARN] " + msg); }
}
