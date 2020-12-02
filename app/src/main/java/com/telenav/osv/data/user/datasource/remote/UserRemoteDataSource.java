package com.telenav.osv.data.user.datasource.remote;

import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.utils.Log;
import io.reactivex.Completable;
import io.reactivex.CompletableEmitter;
import io.reactivex.Maybe;

/**
 * @author horatiuf
 */

public class UserRemoteDataSource implements UserDataSource {

    /**
     * The {@code String} representing the TAG of the current class.
     */
    public static final String TAG = UserRemoteDataSource.class.getSimpleName();

    /**
     * Instance for the current class.
     */
    private static UserRemoteDataSource INSTANCE;

    /**
     * Default constructor for the current class. Private to prevent instantiation outside the class scope.
     */
    private UserRemoteDataSource() {

    }

    /**
     * @return {@code UserLocalDataSource} representing {@link #INSTANCE}.
     */
    public static UserRemoteDataSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UserRemoteDataSource();
        }
        return INSTANCE;
    }

    @Override
    public Maybe<User> getUser() {
        //returns empty since there is not API call available to get the user
        return Maybe.empty();
    }

    @Override
    public Completable deleteUser() {
        //Empty completable for now
        return Completable
                .create(CompletableEmitter::onComplete)
                .doOnComplete(() -> Log.d(TAG, "DeleteUser complete"));
    }

    @Override
    public Completable saveUser(User user) {
        return Completable
                .create(CompletableEmitter::onComplete)
                .doOnComplete(() -> Log.d(TAG, "SaveUser complete"));
    }
}
