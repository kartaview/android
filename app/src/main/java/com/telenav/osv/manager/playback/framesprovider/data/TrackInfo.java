package com.telenav.osv.manager.playback.framesprovider.data;

import android.support.annotation.NonNull;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.network.PhotoCollection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by catalinj on 9/28/17.
 */

public class TrackInfo {

  private ArrayList<ImageFile> nodes = new ArrayList<>();

  private ArrayList<ImageCoordinate> track = new ArrayList<>();

  public TrackInfo(@NonNull final PhotoCollection photoCollection) {
    nodes.addAll(photoCollection.getNodes());
    track.addAll(photoCollection.getTrack());
  }

  public TrackInfo(@NonNull final ArrayList<ImageFile> nodes, final ArrayList<ImageCoordinate> track) {
    this.nodes.addAll(nodes);
    this.track.addAll(track);
  }

  public List<ImageFile> getNodes() {
    return nodes;
  }

  public List<ImageCoordinate> getTrack() {
    return track;
  }
}
