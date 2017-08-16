package com.telenav.spherical.network;

import java.io.IOException;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import android.util.Xml;

/**
 * XMP data class
 */
public class XMP {
    private final static String XMP_START_ELEMENT = "<x:xmpmeta";

    private final static String XMP_END_ELEMENT = "</x:xmpmeta>";

    private final static String XMP_TAG_NAME_PITCH = "PosePitchDegrees";

    private final static String XMP_TAG_NAME_ROLL = "PoseRollDegrees";

    private Double mPosePitchDegrees;

    private Double mPoseRollDegrees;

    /**
     * Constructor
     * @param original Raw data of image
     */
    public XMP(byte[] original) {
        int startXmpIndex = indexOf(original, XMP_START_ELEMENT.getBytes(), 0);
        int endXmpIndex = indexOf(original, XMP_END_ELEMENT.getBytes(), startXmpIndex);
        String xmpData = new String(original, startXmpIndex, endXmpIndex - startXmpIndex + XMP_END_ELEMENT.length());

        XmlPullParser parser = Xml.newPullParser();
        try {
            parser.setInput(new StringReader(xmpData));
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        String tagName = parser.getName();
                        if (tagName.equals(XMP_TAG_NAME_PITCH)) {
                            String pitchInXml = parser.nextText();
                            mPosePitchDegrees = Double.valueOf(pitchInXml);
                        } else if (tagName.equals(XMP_TAG_NAME_ROLL)) {
                            String rollInXml = parser.nextText();
                            mPoseRollDegrees = Double.valueOf(rollInXml);
                        }
                        break;
                    case XmlPullParser.START_DOCUMENT:
                    case XmlPullParser.END_TAG:
                    case XmlPullParser.TEXT:
                        // do nothing
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Acquire pitch angle set for XMP
     * @return Pitch angle
     */
    public Double getPosePitchDegrees() {
        return mPosePitchDegrees;
    }

    /**
     * Acquire roll angle set for XMP
     * @return Roll angle
     */
    public Double getPoseRollDegrees() {
        return mPoseRollDegrees;
    }

    /**
     * Search position of specific data pattern
     * @param original Search target data
     * @param sub Searched data
     * @param startIndex Search start position
     * @return The position where the searched data starts. "-1" is returned if there are no hits.
     */
    private int indexOf(byte[] original, byte[] sub, int startIndex) {
        int subIndex = 0;
        for (int originalIndex = startIndex; originalIndex < original.length; originalIndex++) {
            if (original[originalIndex] == sub[subIndex]) {
                if (subIndex == sub.length - 1) {
                    return originalIndex - subIndex;
                }
                subIndex++;
            } else {
                subIndex = 0;
            }
        }

        return -1;
    }
}
