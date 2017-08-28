package com.telenav.osv.item.network;

import com.telenav.osv.item.Polyline;
import java.util.ArrayList;

/**
 * Created by kalmanb on 7/5/17.
 */
public class GeometryCollection extends ApiResponse {

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
