package com.telenav.osv.data.sequence.database.entity;

import org.joda.time.DateTime;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity class for persistence for the {@code Sequence} data source. This will use {@link androidx.room.Room} annotation to show the column and their specific name.
 * @author horatiuf
 */
@Entity(tableName = "sequence")
public class SequenceEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @NonNull
    private String sequenceId;

    /**
     * Flag which show if the current sequence has been used with OBD feature.
     */
    @ColumnInfo(name = "obd")
    @Nullable
    private Boolean obd;

    /**
     * The latitude coordinate for the initial location of the sequence.
     */
    @ColumnInfo(name = "lat")
    @NonNull
    private Double latitude;

    /**
     * The longitude coordinates for the initial location of the sequence.
     */
    @ColumnInfo(name = "lon")
    @NonNull
    private Double longitude;

    /**
     * The address name of the initial location. Can be null for local sequences where initial location have not been processed.
     */
    @ColumnInfo(name = "address_name")
    @Nullable
    private String addressName;

    /**
     * The total distance of the sequence for which data was taken.
     */
    @ColumnInfo(name = "distance")
    @NonNull
    private Double distance;

    /**
     * The version of the app on which the sequence was created.
     */
    @ColumnInfo(name = "app_version")
    @NonNull
    private String appVersion;

    /**
     * The timestamp representing creation time for the sequence.
     */
    @ColumnInfo(name = "timestamp")
    @NonNull
    private DateTime creationTime;

    /**
     * The number of frames encoded.
     */
    @ColumnInfo(name = "frame_count")
    @NonNull
    private Integer locationsCount;

    /**
     * The number of videos encoded.
     */
    @ColumnInfo(name = "video_count")
    @Nullable
    private Integer videoCount;

    /**
     * The disk size of the sequence on the device.
     */
    @ColumnInfo(name = "disk_size")
    @NonNull
    private Long diskSize;

    /**
     * The path to the physical sequence on the device.
     */
    @ColumnInfo(name = "file_path")
    @NonNull
    private String filePath;

    /**
     * The identifier on the sequence remotely. This is required for all types of sequences.
     */
    @ColumnInfo(name = "online_id")
    @Nullable
    private Long onlineID;

    /**
     * The north latitude for the bounding box in order to display the coordinates for the sequence.
     */
    @ColumnInfo(name = "bounding_north_lat")
    @Nullable
    private Double boundingNorthLat;

    /**
     * The south latitude for the bounding box in order to display the coordinates for the sequence.
     */
    @ColumnInfo(name = "bounding_south_lat")
    @Nullable
    private Double boundingSouthLat;

    /**
     * The west longitude for the bounding box in order to display the coordinates for the sequence.
     */
    @ColumnInfo(name = "bounding_west_lon")
    @Nullable
    private Double boundingWestLon;

    /**
     * The east longitude for the bounding box in order to display the coordinates for the sequence.
     */
    @ColumnInfo(name = "bounding_east_lon")
    @Nullable
    private Double boundingEastLon;

    /**
     * The flag representing the consistency status of the
     */
    @ColumnInfo(name = "consistency_status")
    @NonNull
    private Integer consistencyStatus;

    /**
     * Default constructor for the current class.
     */
    public SequenceEntity(@NonNull String sequenceId,
                          @Nullable Boolean obd,
                          @NonNull Double latitude,
                          @NonNull Double longitude,
                          @Nullable String addressName,
                          @NonNull Double distance,
                          @NonNull String appVersion,
                          @NonNull DateTime creationTime,
                          @NonNull Integer locationsCount,
                          @Nullable Integer videoCount,
                          @NonNull Long diskSize,
                          @NonNull String filePath,
                          @Nullable Long onlineID,
                          @NonNull Integer consistencyStatus,
                          @Nullable Double boundingNorthLat,
                          @Nullable Double boundingSouthLat,
                          @Nullable Double boundingWestLon,
                          @Nullable Double boundingEastLon) {

        this.sequenceId = sequenceId;
        this.obd = obd;
        this.latitude = latitude;
        this.longitude = longitude;
        this.addressName = addressName;
        this.distance = distance;
        this.appVersion = appVersion;
        this.creationTime = creationTime;
        this.locationsCount = locationsCount;
        this.videoCount = videoCount;
        this.diskSize = diskSize;
        this.filePath = filePath;
        this.onlineID = onlineID;
        this.consistencyStatus = consistencyStatus;
        this.boundingNorthLat = boundingNorthLat;
        this.boundingSouthLat = boundingSouthLat;
        this.boundingWestLon = boundingWestLon;
        this.boundingEastLon = boundingEastLon;
    }

    @NonNull
    public Integer getConsistencyStatus() {
        return consistencyStatus;
    }

    @NonNull
    public String getSequenceId() {
        return sequenceId;
    }

    @Nullable
    public Boolean isObd() {
        return obd;
    }

    @NonNull
    public Double getLatitude() {
        return latitude;
    }

    @NonNull
    public Double getLongitude() {
        return longitude;
    }

    @Nullable
    public String getAddressName() {
        return addressName;
    }

    @NonNull
    public Double getDistance() {
        return distance;
    }

    @NonNull
    public String getAppVersion() {
        return appVersion;
    }

    @NonNull
    public DateTime getCreationTime() {
        return creationTime;
    }

    @NonNull
    public Integer getLocationsCount() {
        return locationsCount;
    }

    @Nullable
    public Integer getVideoCount() {
        return videoCount;
    }

    @NonNull
    public Long getDiskSize() {
        return diskSize;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    @Nullable
    public Long getOnlineID() {
        return onlineID;
    }

    @Nullable
    public Double getBoundingNorthLat() {
        return boundingNorthLat;
    }

    @Nullable
    public Double getBoundingSouthLat() {
        return boundingSouthLat;
    }

    @Nullable
    public Double getBoundingWestLon() {
        return boundingWestLon;
    }

    @Nullable
    public Double getBoundingEastLon() {
        return boundingEastLon;
    }
}
