package com.telenav.osv.recorder.tagging.logger;

import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.item.OSVFile;
import androidx.annotation.Nullable;

/**
 * The recording tagging logger. This will logs any events of type:
 * <ul>
 * <li>road closed - {@link #onRoadClosed(String)}</li>
 * <li>rode narrow - {@link #onNarrowRoad(String)} (String)}</li>
 * <li>note persist - {@link #dropNote(String)}</li>
 * <li>road way - {@link #onRoadWay(boolean)}</li>
 * </ul>
 * The whole process needs to be initialised before signaling events by using {@link #initTagging(OSVFile, String)} and also needs be released of resources by using
 * {@link #finish()}.
 */
public interface RecordingTaggingLogger {

    /**
     * Signals road closed event with {@code nullable} note parameter.
     * @param note the note which can be null.
     */
    void onRoadClosed(@Nullable String note);

    /**
     * Signals road narrow event with {@code nullable} note parameter.
     * @param note the note which can be null.
     */
    void onNarrowRoad(@Nullable String note);

    /**
     * Signals a change in the road from two way to one way, or vice versa.
     * @param isOneWay {@code true} will signal that the road is one way, {@code false} otherwise.
     */
    void onRoadWay(boolean isOneWay);

    /**
     * Initialises the tagging process.
     * @param sequenceFolder the sequence folder required in order to the file of tagging to be persisted there.
     * @return {@code Action} which represent the conversion of the file from {@code .txt} format in {@code .geoJon} format.
     */
    void initTagging(OSVFile sequenceFolder);

    /**
     * Signals a note event with {@code nullable} note parameter.
     * @param note the note which can be null.
     */
    void dropNote(@Nullable String note);

    /**
     * @return the current status of the tagging for the road, which is {@code true} if it is one way, {@code false} otherwise.
     */
    boolean isOneWay();

    /**
     * This will release resource of the tagging and convert the file from {@code .txt} format in {@code .geoJon} format.
     * @param sequenceId the identifier for the sequence, required for disk size updates.
     * @param sequenceDetailsLocal the {@code SequenceDetailsLocal} for the sequence, required for disk size updates.
     */
    void finish(String sequenceId, SequenceDetailsLocal sequenceDetailsLocal);
}