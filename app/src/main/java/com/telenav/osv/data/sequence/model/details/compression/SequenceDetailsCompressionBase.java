package com.telenav.osv.data.sequence.model.details.compression;

import java.util.List;
import javax.annotation.Nullable;
import android.location.Location;
import com.telenav.osv.data.sequence.model.Sequence;

/**
 * Base class which holds information related to the sequence frames such as:
 * Related to the frames size, {@link #length}, and to details related to each frame we hold an instance to the {@link #polyline} for that.
 * <p>
 * The current display for the frame in previews and the thumbnail link will be shown by using both {@link #frameIndex} and {@link #thumbnailLink}.
 * @author horatiuf
 */
public abstract class SequenceDetailsCompressionBase {

    /**
     * The details related too all
     */
    @Nullable
    private List<Location> coordinates;

    /**
     * The number of frames or videos in a sequence.
     */
    private int length;

    /**
     * The frame thumbnail link used for frame display. Based on the sequence type, the link can be either local or remote.
     * ToDo: Remove this once the jpeg format is no longer supported.
     */
    private String thumbnailLink;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsCompressionBase(int length, String thumbnailLink) {
        this.length = length;
        this.thumbnailLink = thumbnailLink;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    @Nullable
    public List<Location> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(@Nullable List<Location> coordinates) {
        this.coordinates = coordinates;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    @Sequence.SequenceTypes
    public abstract int getCompressionType();

    public abstract int getLocationsCount();

    public abstract void setLocationsCount(int locationsCount);

    @SequenceFilesExtensions
    public abstract String getFileExtensions();

    /**
     * Interface representing the compression format for the frames in a local sequence. The values can be:
     * <ul>
     * <li>{@link #VIDEO}</li>
     * <li>{@link #JPEG}</li>
     * </ul>
     */
    public @interface SequenceDetailsCompressionFormat {
        /**
         * This value represents the h264 compression for the sequence frames.
         */
        int VIDEO = 0;

        /**
         * This value represents the jpeg compression for the sequence frames.
         */
        int JPEG = 1;
    }

    /**
     * Interface representing the compression extensions for the files in the sequence. The values can be:
     * <ul>
     * <li>{@link #VIDEO}</li>
     * <li>{@link #JPEG}</li>
     * <li>{@link #METADATA_TXT}</li>
     * <li>{@link #METADATA_DEFAULT}</li>
     * </ul>
     */
    public @interface SequenceFilesExtensions {
        /**
         * Video extension.
         */
        String VIDEO = ".mp4";

        /**
         * Photo format extension.
         */
        String JPEG = ".jpg";

        /**
         * Metadata in special cases will be text extension.
         */
        String METADATA_TXT = ".txt";

        /**
         * The default extension of compressed metadata.s
         */
        String METADATA_DEFAULT = ".txt.gz";
    }
}
