package com.telenav.osv.item.network;

import java.util.ArrayList;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;

/**
 * Class holding payment objects
 * Created by kalmanb on 7/12/17.
 */
public class PhotoCollection extends ApiResponse {

    private ArrayList<ImageFile> nodes = new ArrayList<>();

    private ArrayList<ImageCoordinate> track = new ArrayList<>();

    public ArrayList<ImageFile> getNodes() {
        return nodes;
    }

    public ArrayList<ImageCoordinate> getTrack() {
        return track;
    }
}
