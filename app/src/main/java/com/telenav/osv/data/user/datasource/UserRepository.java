package com.telenav.osv.data.user.datasource;

import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.utils.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author horatiuf
 */

public class UserRepository implements UserDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = UserRepository.class.getSimpleName();

    /**
     * Instance for the current class.
     */
    private static UserRepository INSTANCE;

    private final UserDataSource userLocalDataSource;

    private final UserDataSource userRemoteDataSource;

    /**
     * Cached user for faster processing after local/remote loading of data.
     */
    private User cachedUser;

    private boolean cacheIsDirty = false;

    /**
     * Default constructor for the current class. Private to prevent instantiation outside the class scope.
     * @param userLocalDataSource the {@code UserDataSource} representing the local data source.
     * @param userRemoteDataSource the {@code UserDataSource} representing the remote data source.
     */
    private UserRepository(@NonNull UserDataSource userLocalDataSource, @NonNull UserDataSource userRemoteDataSource) {
        this.userLocalDataSource = checkNotNull(userLocalDataSource);
        this.userRemoteDataSource = checkNotNull(userRemoteDataSource);
    }

    /**
     * @param userLocalDataSource the {@code UserDataSource} representing the local data source.
     * @param userRemoteDataSource the {@code UserDataSource} representing the remote data source.
     * @return {@code UserRepository} representing {@link #INSTANCE}.
     */
    public static UserRepository getInstance(UserDataSource userLocalDataSource, UserDataSource userRemoteDataSource) {
        if (INSTANCE == null) {
            INSTANCE = new UserRepository(userLocalDataSource, userRemoteDataSource);
        }
        return INSTANCE;
    }

    @Override
    public void updateCacheUserDetails(BaseUserDetails userDetails) {
        if (cachedUser != null) {
            Log.d(TAG, String.format("Cached user updated details. User name: %s. Details type: %s", cachedUser.getUserName(), userDetails.getType()));
            cachedUser.setDetails(userDetails);
        } else {
            Log.d(TAG, "Cached user details null");
        }
    }

    @Override
    public Maybe<User> getUser() {
        //returns the cache if there is one and there isn't any corruption
        if (cachedUser != null && !cacheIsDirty) {
            Log.d(TAG, "getUser. Status: success. Message: getting user from cache.");
            return Maybe.create(emitter -> emitter.onSuccess(cachedUser));
        }

        //There is not get for the API therefore there is no
        Log.d(TAG, "Fetching user from local data source");
        return userLocalDataSource
                .getUser()
                .doOnSuccess(user -> {
                    Log.d(TAG, "getUser. Status: success. Message: persisting user in cache.");
                    persistCache(user);
                    cacheIsDirty = false;
                });
    }

    @Override
    public Completable deleteUser() {
        return userRemoteDataSource
                .deleteUser()
                .concatWith(userLocalDataSource.deleteUser())
                .doOnComplete(() -> persistCache(null));
    }

    @Override
    public Completable saveUser(User user) {
        return userRemoteDataSource
                .saveUser(user)
                .concatWith(userLocalDataSource.saveUser(user))
                .doOnComplete(() -> {
                    cachedUser = user;
                    Log.d(TAG, String.format("Save user: user persisted in cache. Id: %s. Username: %s. Login type: %s",
                            user.getID(),
                            user.getUserName(),
                            user.getLoginType()));
                });
    }

    @Override
    public void refreshUser() {
        cacheIsDirty = true;
    }

    /**
     * Saves a user in the local cache.
     * @param user the user to be persisted in the cache.
     */
    private void persistCache(@Nullable User user) {
        cachedUser = user;
        Log.d(TAG, String.format("User persisted in cache. Name: %s. ", user != null ? user.getUserName() : null));
    }
}
