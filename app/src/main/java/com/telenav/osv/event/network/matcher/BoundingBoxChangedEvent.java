package com.telenav.osv.event.network.matcher;

import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKBoundingBox;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kalman on 03/02/2017.
 */
public class BoundingBoxChangedEvent extends OSVEvent {

  public static final String TAG = "BoundingBoxChangedEvent";

  public Polyline lastBB;

  public Polyline requestedBB;

  public Polyline smallBB;

  public BoundingBoxChangedEvent(SKBoundingBox lastBB, SKBoundingBox requestedBB, SKBoundingBox smallBB) {
    this.lastBB = new Polyline(113);
    this.requestedBB = new Polyline(114);
    this.smallBB = new Polyline(115);
    this.lastBB.setColor(new float[] {0f, 0f, 1f, 1f});
    this.lastBB.setOutlineColor(new float[] {0f, 0f, 1f, 1f});
    this.requestedBB.setColor(new float[] {0f, 1f, 0f, 1f});
    this.requestedBB.setOutlineColor(new float[] {0f, 1f, 0f, 1f});
    this.smallBB.setColor(new float[] {1f, 0f, 0f, 1f});
    this.smallBB.setOutlineColor(new float[] {1f, 0f, 0f, 1f});

    if (lastBB != null) {
      List<SKCoordinate> nodes = new ArrayList<>();
      nodes.add(new SKCoordinate(lastBB.getBottomRight().getLatitude(), lastBB.getBottomRight().getLongitude()));
      nodes.add(new SKCoordinate(lastBB.getTopLeft().getLatitude(), lastBB.getBottomRight().getLongitude()));
      nodes.add(new SKCoordinate(lastBB.getTopLeft().getLatitude(), lastBB.getTopLeft().getLongitude()));
      nodes.add(new SKCoordinate(lastBB.getBottomRight().getLatitude(), lastBB.getTopLeft().getLongitude()));
      nodes.add(new SKCoordinate(lastBB.getBottomRight().getLatitude(), lastBB.getBottomRight().getLongitude()));
      this.lastBB.setNodes(nodes);
    }

    if (requestedBB != null) {
      List<SKCoordinate> nodes2 = new ArrayList<>();
      nodes2.add(new SKCoordinate(requestedBB.getBottomRight().getLatitude(), requestedBB.getBottomRight().getLongitude()));
      nodes2.add(new SKCoordinate(requestedBB.getTopLeft().getLatitude(), requestedBB.getBottomRight().getLongitude()));
      nodes2.add(new SKCoordinate(requestedBB.getTopLeft().getLatitude(), requestedBB.getTopLeft().getLongitude()));
      nodes2.add(new SKCoordinate(requestedBB.getBottomRight().getLatitude(), requestedBB.getTopLeft().getLongitude()));
      nodes2.add(new SKCoordinate(requestedBB.getBottomRight().getLatitude(), requestedBB.getBottomRight().getLongitude()));
      this.requestedBB.setNodes(nodes2);
    }

    if (smallBB != null) {
      List<SKCoordinate> nodes3 = new ArrayList<>();
      nodes3.add(new SKCoordinate(smallBB.getBottomRight().getLatitude(), smallBB.getBottomRight().getLongitude()));
      nodes3.add(new SKCoordinate(smallBB.getTopLeft().getLatitude(), smallBB.getBottomRight().getLongitude()));
      nodes3.add(new SKCoordinate(smallBB.getTopLeft().getLatitude(), smallBB.getTopLeft().getLongitude()));
      nodes3.add(new SKCoordinate(smallBB.getBottomRight().getLatitude(), smallBB.getTopLeft().getLongitude()));
      nodes3.add(new SKCoordinate(smallBB.getBottomRight().getLatitude(), smallBB.getBottomRight().getLongitude()));
      this.smallBB.setNodes(nodes3);
    }
  }
}
