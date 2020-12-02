package com.telenav.osv.data.score.database.entity;

import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;
import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity class for persistence for the {@code Score} data source. This will use {@link androidx.room.Room} annotation to show the column and their specific name.
 * @author horatiuf
 */
@Entity(tableName = "score",
        foreignKeys = {
                @ForeignKey(entity = SequenceEntity.class,
                        parentColumns = "id",
                        childColumns = "sequence_id",
                        onDelete = CASCADE)})
public class ScoreEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String scoreId;

    /**
     * The number of frames for the current coverage which have obd.
     */
    @ColumnInfo(name = "obd_frame_count")
    @Nullable
    private Integer obdFrameCount;

    /**
     * The number of frames for the current coverage which do not have obd.
     */
    @ColumnInfo(name = "frame_count")
    @Nullable
    private Integer frameCount;

    /**
     * The number representing the coverage for the specified score.
     */
    @ColumnInfo(name = "coverage")
    @NonNull
    private Integer coverage;

    /**
     * Foreign key in order to tie a video to a sequence.
     */
    @ColumnInfo(name = "sequence_id")
    @NonNull
    private String sequenceID;

    /**
     * Default constructor for the current class.
     */
    public ScoreEntity(@NonNull String scoreId, @Nullable Integer obdFrameCount, @Nullable Integer frameCount, @NonNull Integer coverage, @NonNull String sequenceID) {
        this.scoreId = scoreId;
        this.obdFrameCount = obdFrameCount;
        this.frameCount = frameCount;
        this.coverage = coverage;
        this.sequenceID = sequenceID;
    }

    @NonNull
    public String getScoreId() {
        return scoreId;
    }

    @Nullable
    public Integer getObdFrameCount() {
        return obdFrameCount;
    }

    @Nullable
    public Integer getFrameCount() {
        return frameCount;
    }

    @NonNull
    public Integer getCoverage() {
        return coverage;
    }

    @NonNull
    public String getSequenceID() {
        return sequenceID;
    }
}
