package com.telenav.osv.data.user.model;

import androidx.annotation.Nullable;

import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.KVBaseModel;
import com.telenav.osv.data.user.model.details.BaseUserDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;

/**
 * User model class for data related to the user.
 *
 * @author horatiuf
 */

public class User extends KVBaseModel {

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
     * </ul>
     */
    private int userType;

    /**
     * The details of the account which can be either:
     * <ul>
     * <li>{@link GamificationDetails}</li>
     * </ul>
     * In order to check the type, the value from {@link BaseUserDetails#getType()} can be used.
     */
    @Nullable
    private BaseUserDetails details;

    /**
     * User Id for Jarvis User
     */
    private int jarvisUserId;

    /**
     * User Name for Jarvis User
     */
    private String jarvisUserName;

    /**
     * Access token required for Jarvis API requests
     */
    private String jarvisAccessToken;

    /**
     * Refresh token for Jarvis
     */
    private String jarvisRefreshToken;

    /**
     * Default constructor for the base model class.
     * @param ID {@code String} representing User ID.
     * @param accessToken {@code String} representing {@link #accessToken}.
     * @param displayName {@code String} representing {@link #displayName}.
     * @param loginType {@code String} representing {@link #loginType}.
     * @param userName {@code String} representing {@link #userName}.
     * @param userType {@code int} representing {@link #userType}.
     * @param jarvisUserId {@code int} representing {@link #jarvisUserId}.
     * @param jarvisUserName {@code int} representing {@link #jarvisUserName}.
     * @param jarvisAccessToken {@code int} representing {@link #jarvisAccessToken}.
     * @param details {@code BaseUserDetails} representing {@link #details}.
     */
    public User(
            String ID,
            String accessToken,
            String displayName,
            String loginType,
            String userName,
            int userType,
            int jarvisUserId,
            @Nullable String jarvisUserName,
            @Nullable String jarvisAccessToken,
            @Nullable String jarvisRefreshToken,
            @Nullable BaseUserDetails details
    ) {
        super(ID);
        this.accessToken = accessToken;
        this.displayName = displayName;
        this.loginType = loginType;
        this.userName = userName;
        this.userType = userType;
        this.jarvisUserId = jarvisUserId;
        this.jarvisUserName = jarvisUserName;
        this.jarvisAccessToken = jarvisAccessToken;
        this.jarvisRefreshToken = jarvisRefreshToken;
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

    public int getJarvisUserId() {
        return jarvisUserId;
    }

    public String getJarvisUserName() {
        return jarvisUserName;
    }

    public String getJarvisAccessToken() {
        return jarvisAccessToken;
    }

    public String getJarvisRefreshToken() {
        return jarvisRefreshToken;
    }
}
