package com.telenav.osv.data.frame.database.entity;

import org.joda.time.DateTime;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity class for persistence for the {@code Frame} data source. This will use {@link androidx.room.Room} annotation to show the column and their specific name.
 * @author horatiuf
 */
@Entity(tableName = "frame",
        foreignKeys = {
                @ForeignKey(entity = SequenceEntity.class,
                        parentColumns = "id",
                        childColumns = "sequence_id",
                        onDelete = CASCADE)})
public class FrameEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String frameId;

    /**
     * The date and time representing the creation time of the frame. In the database this will be persisted as a {@code long} data type and converted to/from at runtime.
     */
    @ColumnInfo(name = "timestamp")
    @Nullable
    private DateTime dateTime;

    /**
     * The path toward the device file.
     */
    @ColumnInfo(name = "file_path")
    @NonNull
    private String filePath;

    /**
     * The position of the frame in the sequence.
     */
    @ColumnInfo(name = "index")
    @NonNull
    private Integer index;

    /**
     * Foreign key in order to tie a video to a sequence.
     */
    @ColumnInfo(name = "sequence_id")
    @NonNull
    private String sequenceID;

    /**
     * Default constructor for the current class.
     */
    public FrameEntity(@NonNull String frameId, @Nullable DateTime dateTime, @NonNull String filePath, @NonNull Integer index, @NonNull String sequenceID) {
        this.frameId = frameId;
        this.dateTime = dateTime;
        this.filePath = filePath;
        this.index = index;
        this.sequenceID = sequenceID;
    }

    @NonNull
    public String getFrameId() {
        return frameId;
    }

    @Nullable
    public DateTime getDateTime() {
        return dateTime;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @NonNull
    public Integer getIndex() {
        return index;
    }

    @NonNull
    public String getSequenceID() {
        return sequenceID;
    }
}
