package com.telenav.osv.item.network;

import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.item.ImageFile;

import java.util.ArrayList;

/**
 * Class holding photo objects
 * Created by kalmanb on 7/12/17.
 */
public class PhotoCollection extends ApiResponse {

    private ArrayList<ImageFile> nodes = new ArrayList<>();

    private ArrayList<KVLatLng> track = new ArrayList<>();

    public ArrayList<ImageFile> getNodes() {
        return nodes;
    }

    public ArrayList<KVLatLng> getTrack() {
        return track;
    }
}
