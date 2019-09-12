package com.telenav.osv.item.network;

/**
 * Created by kalmanb on 8/3/17.
 */
public class OsmProfileData extends ApiResponse {

    private String profilePictureUrl;

    public String getProfilePictureUrl() {
        return profilePictureUrl;
    }

    public void setProfilePictureUrl(String profilePictureUrl) {
        this.profilePictureUrl = profilePictureUrl;
    }
}
