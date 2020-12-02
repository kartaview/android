package com.telenav.osv.data.frame.database.entity;

import com.telenav.osv.data.location.database.entity.LocationEntity;
import androidx.room.Embedded;

/**
 * @author horatiuf
 */
public class FrameWithLocationEntity {

    @Embedded
    private FrameEntity frameEntity;

    @Embedded(prefix = "loc_")
    private LocationEntity locationEntity;

    public FrameEntity getFrameEntity() {
        return frameEntity;
    }

    public void setFrameEntity(FrameEntity frameEntity) {
        this.frameEntity = frameEntity;
    }

    public LocationEntity getLocationEntity() {
        return locationEntity;
    }

    public void setLocationEntity(LocationEntity locationEntity) {
        this.locationEntity = locationEntity;
    }
}
