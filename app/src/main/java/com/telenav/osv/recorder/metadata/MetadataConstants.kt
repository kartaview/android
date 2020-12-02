package com.telenav.osv.recorder.metadata

enum class TemplateID(val value: String, val version: Int, val minimumCompatibleVersion: Int) {
    /**
     * Represents the id of the template that will have static information about the device.
     */
    DEVICE("d", 1, 1),

    /**
     * Represents the id of the template that will have GPS information.
     */
    GPS("g", 1, 1),

    /**
     * Represents the id of the template that will have OBD speed information.
     */
    OBD("o", 1, 1),

    /**
     * Represents the id of the template that will have camera paramters information.
     */
    CAMERA("cam", 2, 2),

    /**
     * Represents the id of the template that will have camera paramters information.
     */
    EXIF("exif", 2, 2),

    /**
     * Represents the id of the template that will have pressure information.
     */
    PRESSURE("p", 1, 1),

    /**
     * Represents the id of the template that will have compass information.
     */
    COMPASS("c", 1, 1),

    /**
     * Represents the id of the template that will have photo and video indexes information.
     */
    PHOTO("f", 1, 1),

    /**
     *  Represents the id of the template that will have photo and video indexes information.
     */
    ACCELERATION("a", 1, 1),

    /**
     * Represents the id of the template that will have attitude.
     */
    ATTITUDE("y", 1, 1),

    /**
     * Represents the id of the template that will have gravity.
     */
    GRAVITY("x", 1, 1)
}