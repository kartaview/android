package com.telenav.osv.data.location.datasource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.telenav.osv.data.location.model.KVLocation;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Single;

/**
 * Data Source interface for the {@code Location} persistence.
 * <p> The interface provides all the available operations for the data source such as:
 * <ul>
 * <li>{@link #saveLocation(KVLocation, String, String)}</li>
 * <li>{@link #delete(String)}</li>
 * <li>{@link #getLocationsBySequenceId(String)}</li>
 * <li>{@link #getLocationsByVideoId(String)}</li>
 * <li>{@link #getLocationById(String)}</li>
 * <li>{@link #getLocationByFrameId(String)}</li>
 * <li>{@link #getLocationsCountBySequenceId(String)}</li>
 * </ul>
 *
 * @author horatiuf
 */
public interface LocationLocalDataSource {

    /**
     * This method uses parameters instead of model due to the fact that it would create ambiguity between android {@code location} and {@code app} location which from the point
     * of view of logic is basically the same thing with ID in for persistence relationship.
     *
     * @param kvLocation the custom {@code location} representing the data to be persisted.
     * @param videoID    the {@code identifier} representing the video to which the location <i>may</i> correspond to.
     * @param frameID    the {@code identifier} representing the frame to which the location <i>may</i> correspond to.
     * @return {@code true} if the location was persisted successful, {@code false} otherwise.
     */
    boolean saveLocation(@NonNull KVLocation kvLocation, @Nullable String videoID, @Nullable String frameID);

    /**
     * @param locationID the {@code identifier} of the location.
     * @return {@code true} if the removal was successful, {@code false} otherwise.
     */
    boolean delete(@NonNull String locationID);

    /**
     * @return {@code Single} representing a rx stream representing the list of {@code Location} in the
     */
    Single<List<KVLocation>> getLocations();

    /**
     * @param sequenceID the {@code identifier} representing the sequence to which the location correspond to.
     * @return {@code collection} of location corresponding to given param sequence identifier.
     */
    Maybe<List<KVLocation>> getLocationsBySequenceId(@NonNull String sequenceID);

    /**
     * @param videoID the {@code identifier} representing the video to which the location correspond to.
     * @return {@code collection} of location corresponding to given param video identifier.
     */
    Maybe<List<KVLocation>> getLocationsByVideoId(@NonNull String videoID);

    /**
     * @param locationID the {@code identifier} of the location.
     * @return {@code location} identified by given parameter.
     */
    Maybe<KVLocation> getLocationById(@NonNull String locationID);

    /**
     * @param frameID the {@code identifier} representing the frame to which the location correspond to.
     * @return {@code location} identified by given parameter.
     */
    Maybe<KVLocation> getLocationByFrameId(@NonNull String frameID);

    /**
     * @param sequenceId the sequence
     * @return {@code number} representing how many location are persisted for the sequence identified by the given id.
     */
    int getLocationsCountBySequenceId(@NonNull String sequenceId);
}
