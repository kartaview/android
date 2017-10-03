package com.telenav.osv.item.view.profile;

/**
 * Data presented on the profile screen extended appbar
 * Created by kalmanb on 8/30/17.
 */
public class ProfileData {

    private String name;

    private String username;

    private String photoUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
