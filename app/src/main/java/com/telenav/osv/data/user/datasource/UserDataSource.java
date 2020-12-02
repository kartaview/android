package com.telenav.osv.data.user.datasource;

import com.google.common.base.Optional;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import io.reactivex.Completable;
import io.reactivex.Maybe;

/**
 * Interface which represents all the Repository Pattern operations, such as:
 * <ul>
 * <li>{@link #getUser()}</li>
 * <li>{@link #saveUser(User)}</li>
 * <li>{@link #deleteUser()}</li>
 * There are also not required default methods representing specific functionality
 * <li>{@link #updateCacheUserDetails(BaseUserDetails)}</li>
 * <li>{@link #refreshUser()}</li>
 * </ul>
 * @author horatiuf
 */

public interface UserDataSource {

    /**
     * @return {@code Single} stream of one user.  The value for the user is wrapped in an {@link Optional} class due to null avoidance.
     * <p>
     * The value with have {@link Optional#absent()} in case there is invalid/missing data.
     * </p>
     */
    Maybe<User> getUser();

    /**
     * @return {@code Completable} stream representing delete operation with a completable status.
     */
    Completable deleteUser();

    /**
     * @param user the {@code User} which will be persisted.
     * @return {@code Completable} stream representing the save operation with a completable status.
     */
    Completable saveUser(User user);

    /**
     * Updates the {@link User#details} for an existing user.
     * @param userDetails the {@code BaseUserDetails}.
     */
    default void updateCacheUserDetails(BaseUserDetails userDetails) {
        // Empty body since there is no required by all user data sources
        // ToDo: addChild the functionality in get user for remote user data source
    }

    /**
     * Flag the user cache to be refreshed by setting a anti-corruption flag.
     */
    default void refreshUser() {}
}
