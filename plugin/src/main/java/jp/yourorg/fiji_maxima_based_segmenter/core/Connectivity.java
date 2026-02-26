package jp.yourorg.fiji_maxima_based_segmenter.core;

public enum Connectivity {
    C4, C8, C6;

    /**
     * Convert to MorphoLibJ 3D connectivity constant.
     * C6 = 6-connectivity (face neighbors only).
     */
    public int to3D() {
        return this == C6 ? 6 : (this == C8 ? 26 : 6);
    }
}
