package com.telenav.osv.data.video.database.entity;

import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity class for persistence for the {@code Video} data source. This will use {@link androidx.room.Room} annotation to show the column and their specific name.
 * @author horatiuf
 */
@Entity(tableName = "video",
        foreignKeys = {
                @ForeignKey(entity = SequenceEntity.class,
                        parentColumns = "id",
                        childColumns = "sequence_id",
                        onDelete = CASCADE)})
public class VideoEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String videoId;

    /**
     * The index representing the position of the current video in the sequence.
     */
    @ColumnInfo(name = "index")
    @NonNull
    private Integer index;

    /**
     * The path to the physical video file on the device.
     */
    @ColumnInfo(name = "file_path")
    @NonNull
    private String filePath;

    /**
     * The number of frames compressed in the current video.
     */
    @ColumnInfo(name = "frame_count")
    @NonNull
    private Integer frameCount;

    /**
     * Foreign key in order to tie a video to a sequence.
     */
    @ColumnInfo(name = "sequence_id")
    @NonNull
    private String sequenceID;

    /**
     * Default constructor for the current class.
     */
    public VideoEntity(@NonNull String videoId, @NonNull Integer index, @NonNull String filePath, @NonNull Integer frameCount, @NonNull String sequenceID) {
        this.videoId = videoId;
        this.index = index;
        this.filePath = filePath;
        this.frameCount = frameCount;
        this.sequenceID = sequenceID;
    }

    @NonNull
    public String getVideoId() {
        return videoId;
    }

    @NonNull
    public Integer getIndex() {
        return index;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public Integer getFrameCount() {
        return frameCount;
    }

    @NonNull
    public String getSequenceID() {
        return sequenceID;
    }
}
