package com.telenav.osv.data.location.datasource;

import android.content.Context;

import androidx.annotation.NonNull;

import com.telenav.osv.common.Injection;
import com.telenav.osv.data.database.DataConverter;
import com.telenav.osv.data.location.database.dao.LocationDao;
import com.telenav.osv.data.location.model.KVLocation;
import com.telenav.osv.utils.Log;

import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;

/**
 * The concrete implementation of the {@code LocationLocalDataSource} which offers all the details of implementation for the available methods in the interface.
 * @author horatiuf
 */
public class LocationLocalDataSourceImpl implements LocationLocalDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = LocationLocalDataSourceImpl.class.getSimpleName();

    /**
     * The instance of the current class.
     */
    private static LocationLocalDataSourceImpl instance;

    /**
     * Instance of the {@code database} DAO for the location data.
     */
    private LocationDao locationDao;

    /**
     * Default constructor for the current class. Private to prevent instantiation from external sources.
     * @param context the {@code Context} used to instantiate the local persistence.
     */
    private LocationLocalDataSourceImpl(@NonNull Context context) {
        locationDao = Injection.provideKVDatabase(context).locationDao();
    }

    /**
     * @param context the {@code Context} used to instantiate the local persistence.
     * @return {@code VideoLocalDataSource} representing {@link #instance}.
     */
    public static LocationLocalDataSource getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new LocationLocalDataSourceImpl(context);
        }
        return instance;
    }

    @Override
    public boolean saveLocation(@NonNull KVLocation kvLocation, String videoID, String frameID) {
        boolean locationSaved = locationDao.insert(DataConverter.toLocationEntity(kvLocation, videoID, frameID)) != 0;
        Log.d(TAG, String.format("Location persisted. Status: %s. Id: %s", locationSaved, kvLocation.getID()));
        return locationSaved;
    }

    @Override
    public boolean delete(String locationID) {
        boolean locationRemoved = locationDao.deleteById(locationID) != 0;
        Log.d(TAG, String.format("Location deleted. Status: %s. Id: %s", locationRemoved, locationID));
        return locationRemoved;
    }

    @Override
    public Maybe<List<KVLocation>> getLocationsBySequenceId(String sequenceID) {
        return locationDao
                .findAllBySequenceID(sequenceID)
                .doOnSuccess(items -> Log.d(TAG, String.format("getLocationsBySequenceId. Status: success. Sequence id: %s. Message: Locations found.", sequenceID)))
                .doOnComplete(() -> Log.d(TAG, String.format("getLocationsBySequenceId. Status: complete. Sequence id: %s. Message: Locations not found.", sequenceID)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getLocationsBySequenceId. Status: error. Sequence id: %s. Message: %s.", sequenceID, throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toKVLocation)
                        .toList()
                        .toMaybe());
    }

    @Override
    public Single<List<KVLocation>> getLocations() {
        return locationDao
                .findAll()
                .doOnSuccess(items -> Log.d(TAG, "getLocations. Status: success. Message: Locations found."))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getLocations. Status: error. Message: %s.", throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toKVLocation)
                        .toList());
    }

    @Override
    public Maybe<List<KVLocation>> getLocationsByVideoId(String videoID) {
        return locationDao
                .findByVideoID(videoID)
                .doOnSuccess(items -> Log.d(TAG, String.format("getLocationsByVideoId. Status: success. Video id: %s. Message: Locations found.", videoID)))
                .doOnComplete(() -> Log.d(TAG, String.format("getLocationsByVideoId. Status: complete. Video id: %s. Message: Locations not found.", videoID)))
                .doOnError(throwable -> Log.d(TAG,
                        String.format("getLocationsByVideoId. Status: error. Video id: %s. Message: %s.", videoID, throwable.getLocalizedMessage())))
                .flatMap(entities -> Observable
                        .fromIterable(entities)
                        .map(DataConverter::toKVLocation)
                        .toList()
                        .toMaybe());
    }

    @Override
    public Maybe<KVLocation> getLocationById(String locationID) {
        return locationDao
                .findByID(locationID)
                .doOnSuccess(items -> Log.d(TAG, String.format("getLocationById. Status: success. Location id: %s. Message: Location found.", locationID)))
                .doOnComplete(() -> Log.d(TAG, String.format("getLocationById. Status: complete. Location id: %s. Message: Location not found.", locationID)))
                .doOnError(throwable -> Log.d(TAG, String.format("getLocationById. Status: error. Location id: %s. Message: %s.", locationID, throwable.getLocalizedMessage())))
                .map(DataConverter::toKVLocation);
    }

    @Override
    public Maybe<KVLocation> getLocationByFrameId(String frameID) {
        return locationDao
                .findByFrameID(frameID)
                .doOnSuccess(items -> Log.d(TAG, String.format("getLocationByFrameId. Status: success. Frame id: %s. Message: Location found.", frameID)))
                .doOnComplete(() -> Log.d(TAG, String.format("getLocationByFrameId. Status: complete. Frame id: %s. Message: Location not found.", frameID)))
                .doOnError(throwable -> Log.d(TAG, String.format("getLocationByFrameId. Status: error. Frame id: %s. Message: %s.", frameID, throwable.getLocalizedMessage())))
                .map(DataConverter::toKVLocation);
    }

    @Override
    public int getLocationsCountBySequenceId(@NonNull String sequenceId) {
        int locationsCountBySeqId = locationDao.findNumberOfRows(sequenceId);
        Log.d(TAG,
                String.format(
                        "getLocationsCountBySequenceId. Status: success. Seq id: %s. Message: Found %s rows with the given sequence id.",
                        sequenceId,
                        locationsCountBySeqId));
        return locationsCountBySeqId;
    }
}
