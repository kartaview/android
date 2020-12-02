package com.telenav.osv.data.location.database.entity;

import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import static androidx.room.ForeignKey.CASCADE;

/**
 * Entity class for persistence for the {@code Location} data source. This will use {@link androidx.room.Room} annotation to show the column and their specific name.
 * @author horatiuf
 */
@Entity(tableName = "location",
        indices = {
                @Index(value = "video_id"),
                @Index(value = "frame_id")
        },
        foreignKeys = {
                @ForeignKey(entity = VideoEntity.class,
                        parentColumns = "id",
                        childColumns = "video_id",
                        onDelete = CASCADE),
                @ForeignKey(entity = FrameEntity.class,
                        parentColumns = "id",
                        childColumns = "frame_id",
                        onDelete = CASCADE)}
)
public class LocationEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String locationId;

    /**
     * The latitude coordinate for the location persisted.
     */
    @ColumnInfo(name = "lat")
    private Double latitude;

    /**
     * The longitude coordinates for the location persisted.
     */
    @ColumnInfo(name = "lon")
    private Double longitude;

    /**
     * The sequence id for which the location corresponds too. This is mostly used in order to not ad a foreign key based connection in sequences which would seem redundant due
     * to the fact that the {@code Video} and {@code Frame} already posses such connection.
     */
    @ColumnInfo(name = "sequence_id")
    @NonNull
    private String sequenceID;

    /**
     * Foreign key in order to tie a location to a video. This will be a 1 to many relationship between Video and Location models.
     */
    @ColumnInfo(name = "video_id")
    @Nullable
    private String videoID;

    /**
     * Foreign key in order to tie a location to a frame. This will be a 1 to 1 relationship between Frame and Location models.
     */
    @ColumnInfo(name = "frame_id")
    @Nullable
    private String frameID;

    /**
     * Default constructor for the current class.
     */
    public LocationEntity(@NonNull String locationId,
                          @NonNull Double latitude,
                          @NonNull Double longitude,
                          @NonNull String sequenceID,
                          @Nullable String videoID,
                          @Nullable String frameID) {
        this.locationId = locationId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sequenceID = sequenceID;
        this.videoID = videoID;
        this.frameID = frameID;
    }

    @NonNull
    public String getLocationId() {
        return locationId;
    }

    @NonNull
    public Double getLatitude() {
        return latitude;
    }

    @NonNull
    public Double getLongitude() {
        return longitude;
    }

    @NonNull
    public String getSequenceID() {
        return sequenceID;
    }

    @Nullable
    public String getVideoID() {
        return videoID;
    }

    @Nullable
    public String getFrameID() {
        return frameID;
    }
}
