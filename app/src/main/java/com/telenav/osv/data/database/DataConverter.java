package com.telenav.osv.data.database;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.telenav.osv.data.frame.database.entity.FrameEntity;
import com.telenav.osv.data.frame.database.entity.FrameWithLocationEntity;
import com.telenav.osv.data.frame.model.Frame;
import com.telenav.osv.data.location.database.entity.LocationEntity;
import com.telenav.osv.data.location.model.KVLocation;
import com.telenav.osv.data.score.database.entity.ScoreEntity;
import com.telenav.osv.data.score.datasource.ScoreDataSource;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.data.sequence.database.entity.SequenceEntity;
import com.telenav.osv.data.sequence.database.entity.SequenceWithRewardEntity;
import com.telenav.osv.data.sequence.model.LocalSequence;
import com.telenav.osv.data.sequence.model.details.SequenceDetails;
import com.telenav.osv.data.sequence.model.details.SequenceDetailsLocal;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionBase;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionJpeg;
import com.telenav.osv.data.sequence.model.details.compression.SequenceDetailsCompressionVideo;
import com.telenav.osv.data.sequence.model.details.reward.SequenceDetailsRewardPoints;
import com.telenav.osv.data.video.database.entity.VideoEntity;
import com.telenav.osv.data.video.model.Video;
import com.telenav.osv.item.KVFile;
import com.telenav.osv.utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The converter used in order to transform from {@code entity} objects to data {@code models} and vice-versa.
 * @author horatiuf
 */
public class DataConverter {

    /**
     * Default value for the sequence online id.
     */
    public static final int DEFAULT_SEQUENCE_ONLINE_ID = -1;

    /**
     * @param scoreHistory the {@code ScoreHistory} to be translated into a {@code ScoreEntity}.
     * @param sequenceID the {@code identifier} for which the score will correspond too.
     * @return {@code ScoreEntity} model with all the data from the given params.
     */
    public static ScoreEntity toScoreEntity(@NonNull ScoreHistory scoreHistory, @NonNull String sequenceID) {
        return new ScoreEntity(
                scoreHistory.getID(),
                scoreHistory.getObdPhotoCount(),
                scoreHistory.getPhotoCount(),
                scoreHistory.getCoverage(),
                sequenceID);
    }

    /**
     * @param scoreEntity the {@code ScoreEntity} to be translated into a {@code ScoreHistory}.
     * @return {@code ScoreHistory} model with all the data from the given params.
     */
    public static ScoreHistory toScoreHistory(@NonNull ScoreEntity scoreEntity) {
        return new ScoreHistory(
                scoreEntity.getScoreId(),
                scoreEntity.getCoverage(),
                scoreEntity.getFrameCount() != null ? scoreEntity.getFrameCount() : 0,
                scoreEntity.getObdFrameCount() != null ? scoreEntity.getObdFrameCount() : 0);
    }

    /**
     * @param frame the {@code Frame} to be translated into a {@code FrameEntity}.
     * @param sequenceID the {@code identifier} for which the frame will correspond too.
     * @return {@code FrameEntity} model with all the data from the given params.
     */
    public static FrameEntity toFrameEntity(@NonNull Frame frame, @NonNull String sequenceID) {
        return new FrameEntity(
                frame.getID(),
                frame.getDateTime(),
                frame.getFilePath(),
                frame.getIndex(),
                sequenceID);
    }

    /**
     * @param video the {@code Video} to be translated into a {@code VideoEntity}.
     * @param sequenceID the {@code identifier} for which the frame will correspond too.
     * @return {@code VideoEntity} model with all the data from the given params.
     */
    public static VideoEntity toVideoEntity(@NonNull Video video, @NonNull String sequenceID) {
        return new VideoEntity(
                video.getID(),
                video.getIndex(),
                video.getPath(),
                video.getLocationsCount(),
                sequenceID);
    }

    /**
     * @param kvLocation the custom {@code location} model to be translated into a {@code LocationEntity}.
     * @param videoID    the {@code identifier} for video if the location corresponds to one.
     * @param frameID    the {@code identifier} for frame if the location corresponds to one.
     * @return {@code LocationEntity} model with all the data from the given params.
     */
    public static LocationEntity toLocationEntity(KVLocation kvLocation, @Nullable String videoID, @Nullable String frameID) {
        Location location = kvLocation.getLocation();
        return new LocationEntity(
                kvLocation.getID(),
                location.getLatitude(),
                location.getLongitude(),
                kvLocation.getSequenceId(),
                videoID,
                frameID);
    }

    /**
     * @param frameEntity the {@code FrameEntity} to be translated into a {@code Frame}.
     * @return {@code Frame} model with all the data from the given params.
     */
    public static Frame toFrame(@NonNull FrameEntity frameEntity) {
        return new Frame(
                frameEntity.getFrameId(),
                frameEntity.getFilePath(),
                frameEntity.getDateTime(),
                frameEntity.getIndex());
    }

    /**
     * @param frameWithLocationEntity the {@code FrameWithLocationEntity} to be translated into a {@code Frame}.
     * @return {@code Frame} model with all the data from the given params. This will include the location into the model also.
     */
    public static Frame toFrame(@NonNull FrameWithLocationEntity frameWithLocationEntity) {
        FrameEntity frameEntity = frameWithLocationEntity.getFrameEntity();
        LocationEntity locationEntity = frameWithLocationEntity.getLocationEntity();
        Location location = new Location(StringUtils.EMPTY_STRING);
        location.setLongitude(locationEntity.getLongitude());
        location.setLatitude(locationEntity.getLatitude());
        return new Frame(
                frameEntity.getFrameId(),
                frameEntity.getFilePath(),
                frameEntity.getDateTime(),
                frameEntity.getIndex(),
                location);
    }

    /**
     * @param videoEntity the {@code VideoEntity} to be translated into a {@code Video}.
     * @return {@code Video} model with all the data from the given params.
     */
    public static Video toVideo(@NonNull VideoEntity videoEntity) {
        return new Video(
                videoEntity.getVideoId(),
                videoEntity.getFrameCount(),
                videoEntity.getIndex(),
                videoEntity.getFilePath());
    }

    /**
     * @param kvLocations {@code collection} of {@code KVLocation} objects.
     * @return {@code collection} of {@code Android} locations translated from {@code collection} of {@code KVLocation} objects.
     */
    public static List<Location> toLocations(@NonNull List<KVLocation> kvLocations) {
        List<Location> locations = new ArrayList<>();
        if (!kvLocations.isEmpty()) {
            for (KVLocation kvLocation : kvLocations) {
                locations.add(kvLocation.getLocation());
            }
        }
        return locations;
    }

    /**
     * @param locationEntity the {@code LocationEntity} to be translated into an {@code KVLocation}.
     * @return {@code KVLocation} model with all the data from the given params.
     */
    public static KVLocation toKVLocation(@NonNull LocationEntity locationEntity) {
        Location location = new Location(StringUtils.EMPTY_STRING);
        location.setLatitude(locationEntity.getLatitude());
        location.setLongitude(locationEntity.getLongitude());
        return new KVLocation(locationEntity.getLocationId(), location, locationEntity.getSequenceID());
    }

    /**
     * @param sequence the {@code LocalSequence} to be translated into a {@code SequenceEntity}.
     * //ToDo: add bounding box implementation to the model used in app
     * @return {@code SequenceEntity} model with all the data from the given params.
     */
    public static SequenceEntity toSequenceEntity(LocalSequence sequence) {
        SequenceDetails sequenceDetails = sequence.getDetails();
        Location location = sequenceDetails.getInitialLocation();
        SequenceDetailsLocal sequenceDetailsLocal = sequence.getLocalDetails();
        SequenceDetailsCompressionBase compressionBase = sequence.getCompressionDetails();
        int videoCount = compressionBase instanceof SequenceDetailsCompressionVideo ? compressionBase.getLength() : 0;
        return new SequenceEntity(
                sequence.getID(),
                sequenceDetails.isObd(),
                location.getLatitude(),
                location.getLongitude(),
                sequenceDetails.getAddressName(),
                sequenceDetails.getDistance(),
                sequenceDetails.getAppVersion(),
                sequenceDetails.getDateTime(),
                compressionBase.getLocationsCount(),
                videoCount,
                sequenceDetailsLocal.getDiskSize(),
                sequenceDetailsLocal.getFolder().getPath(),
                sequenceDetails.getOnlineId(),
                sequence.getLocalDetails().getConsistencyStatus(),
                0.0,
                0.0,
                0.0,
                0.0
        );
    }

    /**
     * @param sequenceEntity the {@code SequenceEntity} to be translated into a {@code LocalSequence}.
     * @return {@code LocalSequence} model with all the data from the given params.
     */
    public static LocalSequence toLocalSequence(@NonNull SequenceEntity sequenceEntity) {
        Location initialLocation = new Location(StringUtils.EMPTY_STRING);
        initialLocation.setLatitude(sequenceEntity.getLatitude());
        initialLocation.setLongitude(sequenceEntity.getLongitude());
        SequenceDetails sequenceDetails = new SequenceDetails(
                initialLocation,
                sequenceEntity.getDistance(),
                sequenceEntity.getAppVersion(),
                sequenceEntity.getCreationTime()
        );
        Long onlineId = sequenceEntity.getOnlineID();
        sequenceDetails.setOnlineId(onlineId != null ? onlineId : DEFAULT_SEQUENCE_ONLINE_ID);
        sequenceDetails.setAddressName(sequenceEntity.getAddressName());
        sequenceDetails.setDistance(sequenceEntity.getDistance());
        Boolean obd = sequenceEntity.isObd();
        sequenceDetails.setObd(obd != null ? obd : false);
        SequenceDetailsLocal sequenceDetailsLocal = new SequenceDetailsLocal(
                new KVFile(sequenceEntity.getFilePath()),
                sequenceEntity.getDiskSize(),
                sequenceEntity.getConsistencyStatus());
        SequenceDetailsCompressionBase sequenceDetailsCompressionBase;
        Integer videoCountDB = sequenceEntity.getVideoCount();
        int videoCount = videoCountDB != null ? videoCountDB : 0;
        // instead of checking if it is 0, some version will write -1 to videos to denote that is frame encoding.
        if (videoCount != 0) {
            sequenceDetailsCompressionBase = new SequenceDetailsCompressionVideo(
                    videoCount,
                    null,
                    sequenceEntity.getLocationsCount()
            );
        } else {
            sequenceDetailsCompressionBase = new SequenceDetailsCompressionJpeg(
                    sequenceEntity.getLocationsCount(),
                    null,
                    0
            );
        }
        return new LocalSequence(
                sequenceEntity.getSequenceId(),
                sequenceDetails,
                sequenceDetailsLocal,
                sequenceDetailsCompressionBase
        );
    }

    /**
     * @param scoreDataSource the {@code ScoreDataSource} used to calculated the score for a history.
     * @param sequenceWithRewardEntity the {@code SequenceWithRewardEntity} to be translated into a {@code LocalSequence}.
     * @return {@code LocalSequence} model with all the data from the given params.
     */
    public static LocalSequence toLocalSequence(@NonNull ScoreDataSource scoreDataSource, @NonNull SequenceWithRewardEntity sequenceWithRewardEntity) {
        LocalSequence localSequence = toLocalSequence(sequenceWithRewardEntity.getSequenceEntity());
        HashMap<Integer, ScoreHistory> scoreHistoryHashMap = new HashMap<>();
        for (ScoreEntity scoreEntity : sequenceWithRewardEntity.getScoreEntities()) {
            ScoreHistory scoreHistory = toScoreHistory(scoreEntity);
            scoreHistoryHashMap.put(scoreHistory.getCoverage(), scoreHistory);
        }

        SequenceDetailsRewardPoints sequenceDetailsRewardPoints = new SequenceDetailsRewardPoints(scoreDataSource.getScore(scoreHistoryHashMap), "points", scoreHistoryHashMap);
        localSequence.setRewardDetails(sequenceDetailsRewardPoints);
        return localSequence;
    }
}
