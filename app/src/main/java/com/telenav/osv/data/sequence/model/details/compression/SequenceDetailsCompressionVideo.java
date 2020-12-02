package com.telenav.osv.data.sequence.model.details.compression;

import java.util.List;
import com.telenav.osv.data.video.model.Video;
import androidx.annotation.Nullable;

/**
 * @author horatiuf
 */
public class SequenceDetailsCompressionVideo extends SequenceDetailsCompressionBase {

    /**
     * Collection of videos for the current sequence.
     */
    @Nullable
    private List<Video> videos;

    /**
     * The count for the frames.
     */
    private int locationsCount;

    /**
     * Default constructor for the current class.
     */
    public SequenceDetailsCompressionVideo(int length, String thumbnailLink, int locationsCount) {
        super(length, thumbnailLink);
        this.locationsCount = locationsCount;
    }

    public int getLocationsCount() {
        return locationsCount;
    }

    @Override
    public void setLocationsCount(int locationsCount) {
        this.locationsCount = locationsCount;
    }

    @Override
    public String getFileExtensions() {
        return SequenceFilesExtensions.VIDEO;
    }

    @Override
    public int getCompressionType() {
        return SequenceDetailsCompressionFormat.VIDEO;
    }

    @Nullable
    public List<Video> getVideos() {
        return videos;
    }

    public void setVideos(List<Video> videos) {
        this.videos = videos;
    }
}
