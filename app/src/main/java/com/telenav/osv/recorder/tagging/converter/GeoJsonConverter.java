package com.telenav.osv.recorder.tagging.converter;

import android.location.Location;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.telenav.osv.item.KVFile;
import com.telenav.osv.recorder.tagging.converter.model.TaggingBase;
import com.telenav.osv.recorder.tagging.converter.model.TaggingCoordinate;
import com.telenav.osv.recorder.tagging.converter.model.TaggingFeature;
import com.telenav.osv.recorder.tagging.converter.model.TaggingFeatureCollection;
import com.telenav.osv.recorder.tagging.converter.model.TaggingGeometryLineString;
import com.telenav.osv.recorder.tagging.converter.model.TaggingGeomtryPoint;
import com.telenav.osv.recorder.tagging.converter.model.TaggingProperties;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.StringUtils;
import com.telenav.osv.utils.Utils;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GeoJsonConverter {

    public static final String RECORDING_TAGGING_FILE_NAME = "recordingTagging.geoJson";

    public static final int CSV_LINE_MAX_VALUES = 8;

    public static final int INDEX_TIMESTAMP = 0;

    public static final int INDEX_LAT = 1;

    public static final int INDEX_LON = 2;

    public static final int INDEX_ROAD_TYPE = 3;

    public static final int INDEX_ROAD_CLOSED = 4;

    public static final int INDEX_ROAD_NARROW = 5;

    public static final int INDEX_ROAD_NOTE_LENGTH = 6;

    public static final int INDEX_ROAD_NOTE = 7;

    private static final String LINE_SEPARATOR = "\n";

    private static final String LINE_SPLIT = ",";

    /**
     * The pattern of one line in the text file which as stated represents following:
     * <ul>
     * <li>location timestamp</li>
     * <li>latitude value</li>
     * <li>longitude value</li>
     * <li>road symbol - {@link #LOG_SYMBOL_ONE_WAY}/{@link #LOG_SYMBOL_TWO_WAY}</li>
     * <li>{@link #LOG_SYMBOL_ROAD_CLOSED} symbol</li>
     * <li>{@link #LOG_SYMBOL_ROAD_NARROW} symbol</li>
     * <li>base 64 note length</li>
     * <li>base 64 note length</li>
     * </ul>
     */
    private static final String PATTERN = "%s,%s,%s,%s,%s,%s,%s,%s" + LINE_SEPARATOR;

    private static final String LOG_SYMBOL_ONE_WAY = "one_way";

    private static final String LOG_SYMBOL_TWO_WAY = "two_way";

    private static final String LOG_SYMBOL_ROAD_CLOSED = "road_closed";

    private static final String LOG_SYMBOL_ROAD_NARROW = "road_narrow";

    private static final String TAG = GeoJsonConverter.class.getSimpleName();

    private TaggingFeatureCollection featureCollection;

    private String previousRoadType = StringUtils.EMPTY_STRING;

    private TaggingGeometryLineString featureLineString;

    /**
     * Default constructor for the current class.
     */
    public GeoJsonConverter() { }

    /**
     * Converts the given txt file to a geoJson file in the given output folder.
     *
     * @param recordingTaggingTxtFile the {@code .txt} format tagging file to be converted.
     * @param geoJsonOutputFolder     the {@code folder} where the converted file to be persisted.
     * @return the size of the converted file.
     */
    public long convert(KVFile recordingTaggingTxtFile, KVFile geoJsonOutputFolder) {
        featureCollection = new TaggingFeatureCollection();
        try {
            readFromFile(recordingTaggingTxtFile);
            KVFile geoJsonFile = new KVFile(geoJsonOutputFolder, RECORDING_TAGGING_FILE_NAME);
            return writeToFile(featureCollection.toGeoJson(), geoJsonFile);
        } catch (IOException e) {
            Log.w(TAG, String.format("convert. Status: error. Message: %s", e.getLocalizedMessage()));
        }
        //returns nothing if there was problem or error encountered during conversion.
        return 0;
    }

    /**
     * Converts all given parameters to a line in a .txt file.
     * @return {@code String} population {@link #PATTERN} with the given parameters transformed to logging appropriate format.
     */
    public String convertToCsvLine(boolean oneWay, Location lastLocation, boolean closedRoad, boolean narrowRoad, @Nullable String escapeNote) {
        String base64NoteLength = StringUtils.EMPTY_STRING;
        String base64String = StringUtils.EMPTY_STRING;
        if (escapeNote != null && !escapeNote.equals(StringUtils.EMPTY_STRING)) {
            base64String = Base64.encodeToString(escapeNote.getBytes(), Base64.NO_WRAP);
            base64NoteLength = String.valueOf(base64String.length());
        }
        return String.format(PATTERN,
                lastLocation.getTime(),
                lastLocation.getLatitude(),
                lastLocation.getLongitude(),
                oneWay ? LOG_SYMBOL_ONE_WAY : LOG_SYMBOL_TWO_WAY,
                closedRoad ? LOG_SYMBOL_ROAD_CLOSED : StringUtils.EMPTY_STRING,
                narrowRoad ? LOG_SYMBOL_ROAD_NARROW : StringUtils.EMPTY_STRING,
                base64NoteLength,
                base64String);
    }

    private void readFromFile(KVFile recordingTaggingTxtFile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(
                recordingTaggingTxtFile.getAbsoluteFile()));
        String line = reader.readLine();
        while (line != null) {
            // read next line
            line = reader.readLine();
            parseLine(line);
        }
        reader.close();
    }

    private long writeToFile(String data, KVFile outputFile) throws IOException {
        outputFile.createNewFile();
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile, false);
        fileOutputStream.write(data.getBytes());
        fileOutputStream.close();
        return Utils.fileSize(outputFile);
    }

    private void parseLine(String line) {
        if (line == null) {
            return;
        }
        String[] csvValues = line.split(LINE_SPLIT, -1);
        if (csvValues.length != CSV_LINE_MAX_VALUES) {
            Log.w(TAG, "parseLine. Status: error. Message: Wrong number of elements present in a line");
            return;
        }
        double lat;
        double lon;
        long timeStamp;
        try {
            lat = Double.parseDouble(csvValues[INDEX_LAT]);
            lon = Double.parseDouble(csvValues[INDEX_LON]);
            timeStamp = Long.parseLong(csvValues[INDEX_TIMESTAMP]);
        } catch (NumberFormatException e) {
            Log.w(TAG, "parseLine. Status: invalid coordinates. Message: Wrong format for the lat/lon in string");
            return;
        }
        addEventIfRequired(timeStamp,
                lat,
                lon,
                csvValues[INDEX_ROAD_CLOSED],
                csvValues[INDEX_ROAD_NARROW],
                csvValues[INDEX_ROAD_NOTE_LENGTH],
                csvValues[INDEX_ROAD_NOTE]);
        addRoadTypeFeature(lat, lon, csvValues[INDEX_ROAD_TYPE]);
    }

    private void addRoadTypeFeature(double lat, double lon, String roadType) {
        if (!roadType.equals(previousRoadType)) {
            Log.d(TAG, String.format("addRoadTypeFeature. Status: new road type found. Type: %s.", roadType));
            //add to the previous road type the current location
            if (featureLineString != null) {
                featureLineString.addCoordinate(new TaggingCoordinate(lat, lon));
            }
            //create a new road type and add the current coordinate as a start for it
            featureLineString = new TaggingGeometryLineString();
            featureLineString.addCoordinate(new TaggingCoordinate(lat, lon));
            TaggingFeature taggingFeature =
                    new TaggingFeature(
                            new TaggingProperties(roadType.equals(LOG_SYMBOL_ONE_WAY) ? TaggingBase.PROPERTY_VALUE_KV_TAGGING_ONE_WAY :
                                    TaggingBase.PROPERTY_VALUE_KV_TAGGING_TWO_WAY),
                            featureLineString);
            featureCollection.addFeature(taggingFeature);
        } else {
            Log.d(TAG, "addRoadTypeFeature. Status: coordinate add.");
            featureLineString.addCoordinate(new TaggingCoordinate(lat, lon));
        }
        this.previousRoadType = roadType;
    }

    private void addEventIfRequired(long timeStamp, double lat, double lon, String closedRoad, String narrowRoad, String noteLength, String note) {
        String evenType = TaggingBase.PROPERTY_VALUE_KV_TAGGING_NOTE;
        if (!closedRoad.equals(StringUtils.EMPTY_STRING)) {
            evenType = TaggingBase.PROPERTY_VALUE_KV_TAGGING_CLOSED_ROAD;
        }
        if (!narrowRoad.equals(StringUtils.EMPTY_STRING)) {
            evenType = TaggingBase.PROPERTY_VALUE_KV_TAGGING_CLOSED_ROAD;
        }

        boolean isNoteInvalid = noteLength.equals(StringUtils.EMPTY_STRING) || Integer.parseInt(noteLength) != note.length();
        if (evenType.equals(TaggingBase.PROPERTY_VALUE_KV_TAGGING_NOTE)
                && note.equals(StringUtils.EMPTY_STRING)
                && isNoteInvalid) {
            Log.w(TAG, "addEventIfRequired. Status: exit. Message: No event and note found.");
            return;
        }

        if (isNoteInvalid) {
            note = null;
        } else {
            byte[] noteDecodeBytes = Base64.decode(note.getBytes(), Base64.DEFAULT);
            try {
                note = new String(noteDecodeBytes, StandardCharsets.UTF_8);
            } catch (UnsupportedOperationException e) {
                note = null;
                Log.w(TAG, "addEventIfRequired. Status: decode note error. Message: Could not decode the note.");
            }
        }
        Log.w(TAG, String.format("addEventIfRequired. Status: complete. Type: %s. Note: %s. Message: Adding event.", evenType, note));
        featureCollection.addFeature(
                new TaggingFeature(
                        new TaggingProperties(timeStamp, evenType, note),
                        new TaggingGeomtryPoint(new TaggingCoordinate(lat, lon))));
    }
}
