package com.telenav.osv.item.network;

import java.util.ArrayList;
import com.telenav.osv.item.Polyline;

/**
 * Created by kalmanb on 7/5/17.
 */
public class GeometryCollection extends ApiResponse {

    /**
     * total number of segments in the response
     */
    private int totalFilteredItems;

    private ArrayList<Polyline> segmentList = new ArrayList<>();

    public int getTotalFilteredItems() {
        return totalFilteredItems;
    }

    public void setTotalFilteredItems(int totalFilteredItems) {
        this.totalFilteredItems = totalFilteredItems;
    }

    public ArrayList<Polyline> getSegmentList() {
        return segmentList;
    }

    public void setSegmentList(ArrayList<Polyline> trackList) {
        this.segmentList = trackList;
    }
}
