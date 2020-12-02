package com.telenav.osv.item;

import com.telenav.osv.common.model.KVLatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Polyline class extension used to denote that if the polyline is used
 */
public class Polyline {

    public int coverage = 0;

    private int identifier;

    private List<KVLatLng> nodes;

    private float[] color;

    private float[] outlineColor;

    public Polyline(int identifier) {
        this.identifier = identifier;
        this.nodes = new ArrayList<>();
    }

    public List<KVLatLng> getNodes() {
        return nodes;
    }

    public void setNodes(List<KVLatLng> nodes) {
        this.nodes = nodes;
    }

    public boolean isLocal() {
        return true;
    }

    public int getCoverage() {
        return coverage;
    }

    public int getIdentifier() {
        return identifier;
    }

    public float[] getColor() {
        return color;
    }

    public void setColor(float[] color) {
        this.color = color;
    }

    public float[] getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(float[] outlineColor) {
        this.outlineColor = outlineColor;
    }
}