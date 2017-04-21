package com.skobbler.ngx.map;


import java.util.Arrays;
import java.util.List;
import com.skobbler.ngx.SKCoordinate;


/**
 * Stores information related to a polyline. This object is used as an input
 * parameter.
 * @version // [apiVersion]
 */
public class SKPolyline {
    
    
    /**
     * The unique identifier for the overlay.
     */
    private int identifier;
    
    /**
     * The list of polyline nodes
     */
    private List<SKCoordinate> nodes;
    
    /**
     * The polyline's color (RGBA array of values between 0 and 1)
     */
    private float[] color = new float[] { 0, 0, 0, 1 };
    
    /**
     * The polyline's outline color (RGBA array of values between 0 and 1)
     */
    private float[] outlineColor = new float[] { 0, 0, 0, 1 };
    
    /**
     * The size of the polyline. Should be a value in [0, 100] interval. If
     * using dotted lines, use 0.
     */
    private int lineSize = 9;
    
    /**
     * The outline size.Should be a value in [0, 100] interval. If using dotted
     * lines, this controls the dotted line's width.
     */
    private int outlineSize = 4;
    
    /**
     * If outline is present and it's dotted (it's dotted when
     * outlineDottedPixelsSkip is not 0), this is the number of pixels to
     * represent the solid dotted line
     */
    private int outlineDottedPixelsSolid = 6;
    
    /**
     * If outline is present and this param is not zero than the outline is
     * dotted and this param is the number of pixels between the dotted parts
     */
    private int outlineDottedPixelsSkip = 0;
    
    /**
     * Gets the unique identifier for the overlay.
     * @return
     */
    public int getIdentifier() {
        return identifier;
    }
    
    /**
     * Sets the unique identifier for the overlay.
     * @param identifier
     */
    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }
    
    /**
     * Gets the list of polyline nodes
     * @return
     */
    public List<SKCoordinate> getNodes() {
        return nodes;
    }
    
    /**
     * Sets the list of polyline nodes
     * @param points
     */
    public void setNodes(List<SKCoordinate> points) {
        this.nodes = points;
    }
    
    /**
     * Gets the polyline color (RGBA array of values between 0 and 1)
     * @return
     */
    public float[] getColor() {
        return color;
    }
    
    /**
     * Sets the polyline color (RGBA array of values between 0 and 1)
     * @param color
     */
    public void setColor(float[] color) {
        if (color != null) {
            this.color = Arrays.copyOf(color, color.length);
        }
    }
    
    /**
     * Gets the polyline outline color (RGBA array of values between 0 and 1)
     * @return
     */
    public float[] getOutlineColor() {
        return outlineColor;
    }
    
    /**
     * Sets the polyline outline color (RGBA array of values between 0 and 1)
     * @param outlineColor
     */
    public void setOutlineColor(float[] outlineColor) {
        if (outlineColor != null) {
            this.outlineColor = Arrays.copyOf(outlineColor, outlineColor.length);
        }
    }
    
    /**
     * Gets the line size
     * @return
     */
    public int getLineSize() {
        return lineSize;
    }
    
    /**
     * Sets the line size. Should be a value in [0, 100] interval. If using
     * dotted lines, use 0.
     * @param lineSize
     */
    public void setLineSize(int lineSize) {
        this.lineSize = lineSize;
    }
    
    /**
     * Gets the outline size
     * @return
     */
    public int getOutlineSize() {
        return outlineSize;
    }
    
    /**
     * Sets the outline size. Should be a value in [0, 100] interval. If using
     * dotted lines, this controls the dotted line's width.
     * @param outlineSize
     */
    public void setOutlineSize(int outlineSize) {
        this.outlineSize = outlineSize;
    }
    
    /**
     * Gets the number of pixels to represent the solid dotted line
     * @return
     */
    public int getOutlineDottedPixelsSolid() {
        return outlineDottedPixelsSolid;
    }
    
    /**
     * Sets the number of pixels to represent the solid dotted line
     * @param outlineDottedPixelsSolid
     */
    public void setOutlineDottedPixelsSolid(int outlineDottedPixelsSolid) {
        this.outlineDottedPixelsSolid = outlineDottedPixelsSolid;
    }
    
    /**
     * Gets the number of pixels forming the gap between the solid parts
     * @return
     */
    public int getOutlineDottedPixelsSkip() {
        return outlineDottedPixelsSkip;
    }
    
    /**
     * Sets the number of pixels forming the gap between the solid parts
     * @param outlineDottedPixelsSkip
     */
    public void setOutlineDottedPixelsSkip(int outlineDottedPixelsSkip) {
        this.outlineDottedPixelsSkip = outlineDottedPixelsSkip;
    }
    
    
}
