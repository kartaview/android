package com.telenav.osv.data.user.model;

import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.OSVBaseModel;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.driver.DriverDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;
import androidx.annotation.Nullable;

/**
 * User model class for data related to the user.
 * @author horatiuf
 */

public class User extends OSVBaseModel {

    /**
     * Access token required for each online request.
     */
    private String accessToken = "";

    /**
     * Display name for the user in the Profile screen.
     */
    private String displayName = "";

    /**
     * The type representing the login authentication method. It can be:
     * <ul>
     * <li>Facebook</li>
     * <li>Google</li>
     * <li>OSM</li>
     * </ul>
     */
    private String loginType;

    /**
     * The username of the account.
     */
    private String userName;

    /**
     * The type of the user. The value can be:
     * <ul>
     * <li>{@link PreferenceTypes#USER_TYPE_UNKNOWN}</li>
     * <li>{@link PreferenceTypes#USER_TYPE_CONTRIBUTOR}</li>
     * <li>{@link PreferenceTypes#USER_TYPE_QA}</li>
     * <li>{@link PreferenceTypes#USER_TYPE_DEDICATED}</li>
     * <li>{@link PreferenceTypes#USER_TYPE_BYOD}</li>
     * <li>{@link PreferenceTypes#USER_TYPE_BAU}</li>
     * </ul>
     */
    private int userType;

    /**
     * The details of the account which can be either:
     * <ul>
     * <li>{@link DriverDetails}</li>
     * <li>{@link GamificationDetails}</li>
     * </ul>
     * In order to check the type, the value from {@link BaseUserDetails#getType()} can be used.
     */
    @Nullable
    private BaseUserDetails details;

    /**
     * Default constructor for the base model class.
     * @param ID {@code String} representing {@link #ID}.
     * @param accessToken {@code String} representing {@link #accessToken}.
     * @param displayName {@code String} representing {@link #displayName}.
     * @param loginType {@code String} representing {@link #loginType}.
     * @param userName {@code String} representing {@link #userName}.
     * @param userType {@code int} representing {@link #userType}.
     * @param details {@code BaseUserDetails} representing {@link #details}.
     */
    public User(String ID, String accessToken, String displayName, String loginType, String userName, int userType, @Nullable BaseUserDetails details) {
        super(ID);
        this.accessToken = accessToken;
        this.displayName = displayName;
        this.loginType = loginType;
        this.userName = userName;
        this.userType = userType;
        this.details = details;
    }

    /**
     * @return {@code String} representing {@link #accessToken}.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * @return {@code String} representing {@link #displayName}.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return {@code String} representing {@link #loginType}.
     */
    public String getLoginType() {
        return loginType;
    }

    /**
     * @return {@code String} representing {@link #userName}.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return {@code int} representing {@link #userType}.
     */
    public int getUserType() {
        return userType;
    }

    /**
     * @return {@code BaseUserDetails} representing {@link #details}.
     */
    @Nullable
    public BaseUserDetails getDetails() {
        return details;
    }

    /**
     * @param details the new {@code BaseUserDetails} to be set.
     */
    public void setDetails(@Nullable BaseUserDetails details) {
        this.details = details;
    }
}
