package com.telenav.osv.item.network;

/**
 * Created by kalmanb on 7/5/17.
 */
public class IssueData extends ApiResponse {

    /**
     * like sequence id, only for issues
     */
    private int onlineID;

    public int getOnlineID() {
        return onlineID;
    }

    public void setOnlineID(int onlineID) {
        this.onlineID = onlineID;
    }
}
