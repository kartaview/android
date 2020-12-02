package com.telenav.osv.event.network.matcher;

import com.telenav.osv.common.model.KVLatLng;
import com.telenav.osv.event.OSVEvent;
import com.telenav.osv.item.Polyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kalman on 03/02/2017.
 */
public class BoundingBoxChangedEvent extends OSVEvent {

    public final static String TAG = "BoundingBoxChangedEvent";

    public Polyline lastBB;

    public Polyline requestedBB;

    public Polyline smallBB;

    public BoundingBoxChangedEvent(KVBoundingBox lastBB, KVBoundingBox requestedBB, KVBoundingBox smallBB) {
        this.lastBB = new Polyline(113);
        this.requestedBB = new Polyline(114);
        this.smallBB = new Polyline(115);
        this.lastBB.setColor(new float[]{0f, 0f, 1f, 1f});
        this.lastBB.setOutlineColor(new float[]{0f, 0f, 1f, 1f});
        this.requestedBB.setColor(new float[]{0f, 1f, 0f, 1f});
        this.requestedBB.setOutlineColor(new float[]{0f, 1f, 0f, 1f});
        this.smallBB.setColor(new float[]{1f, 0f, 0f, 1f});
        this.smallBB.setOutlineColor(new float[]{1f, 0f, 0f, 1f});

        if (lastBB != null) {
            List<KVLatLng> nodes = new ArrayList<>();
            nodes.add(new KVLatLng(lastBB.getBottomRight().getLat(), lastBB.getBottomRight().getLon(), 0));
            nodes.add(new KVLatLng(lastBB.getTopLeft().getLat(), lastBB.getBottomRight().getLon(), 0));
            nodes.add(new KVLatLng(lastBB.getTopLeft().getLat(), lastBB.getTopLeft().getLon(), 0));
            nodes.add(new KVLatLng(lastBB.getBottomRight().getLat(), lastBB.getTopLeft().getLon(), 0));
            nodes.add(new KVLatLng(lastBB.getBottomRight().getLat(), lastBB.getBottomRight().getLon(), 0));
            this.lastBB.setNodes(nodes);
        }

        if (requestedBB != null) {
            List<KVLatLng> nodes2 = new ArrayList<>();
            nodes2.add(new KVLatLng(requestedBB.getBottomRight().getLat(), requestedBB.getBottomRight().getLon(), 0));
            nodes2.add(new KVLatLng(requestedBB.getTopLeft().getLat(), requestedBB.getBottomRight().getLon(), 0));
            nodes2.add(new KVLatLng(requestedBB.getTopLeft().getLat(), requestedBB.getTopLeft().getLon(), 0));
            nodes2.add(new KVLatLng(requestedBB.getBottomRight().getLat(), requestedBB.getTopLeft().getLon(), 0));
            nodes2.add(new KVLatLng(requestedBB.getBottomRight().getLat(), requestedBB.getBottomRight().getLon(), 0));
            this.requestedBB.setNodes(nodes2);
        }

        if (smallBB != null) {
            List<KVLatLng> nodes3 = new ArrayList<>();
            nodes3.add(
                    new KVLatLng(smallBB.getBottomRight().getLat(), smallBB.getBottomRight().getLon(), 0));
            nodes3.add(new KVLatLng(smallBB.getTopLeft().getLat(),
                    smallBB.getBottomRight().getLon(), 0));
            nodes3.add(new KVLatLng(smallBB.getTopLeft().getLat(),
                    smallBB.getTopLeft().getLon(), 0));
            nodes3.add(new KVLatLng(smallBB.getBottomRight().getLat(),
                    smallBB.getTopLeft().getLon(), 0));
            nodes3.add(new KVLatLng(smallBB.getBottomRight().getLat(),
                    smallBB.getBottomRight().getLon(), 0));
            this.smallBB.setNodes(nodes3);
        }
    }
}
