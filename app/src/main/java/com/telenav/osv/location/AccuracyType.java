package com.telenav.osv.location;

public @interface AccuracyType {
    /**
     * Accuracy type is good when: accuracy value <= 15
     */
    int ACCURACY_GOOD = 15;

    /**
     * Accuracy type is medium when: 15 < accuracy value <= 40
     */
    int ACCURACY_MEDIUM = 40;

    /**
     * Accuracy type is bad when: accuracy value >= 41
     */
    int ACCURACY_BAD = 41;
}