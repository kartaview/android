package com.telenav.osv.recorder.camera.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.telenav.osv.utils.Size;
import androidx.collection.ArrayMap;

/**
 * A collection class that automatically groups {@link Size}s by their {@link AspectRatio}s.
 */
public class SizeMap {

    /**
     * The ratios map containing sizes grouped by their aspect ratio.
     */
    private final ArrayMap<AspectRatio, SortedSet<Size>> ratios = new ArrayMap<>();

    /**
     * Add a new {@link Size} to this collection.
     * @param size The size to be added.
     * @return {@code true} if the size was successfully added, {@code false} if the size already exists.
     */
    public boolean add(Size size) {
        for (AspectRatio ratio : ratios.keySet()) {
            if (ratio.matches(size)) {
                final SortedSet<Size> sizes = ratios.get(ratio);
                if (sizes.contains(size)) {
                    return false;
                } else {
                    sizes.add(size);
                    return true;
                }
            }
        }
        // Add a new key when no ratio matches with the given size.
        SortedSet<Size> sizes = new TreeSet<>();
        sizes.add(size);
        ratios.put(AspectRatio.createAspectRatio(size.getWidth(), size.getHeight()), sizes);
        return true;
    }

    /**
     * Removes the specified aspect ratio and all sizes associated with it.
     * @param ratio The aspect ratio to be removed.
     */
    public void remove(AspectRatio ratio) {
        ratios.remove(ratio);
    }

    /**
     * @return a list with all the available aspect ratios.
     */
    public Set<AspectRatio> ratios() {
        return ratios.keySet();
    }

    /**
     * @param ratio the ratio key from which the sizes should be return.
     * @return a list with all the sizes from the given ratio.
     */
    public SortedSet<Size> sizes(AspectRatio ratio) {
        return ratios.get(ratio);
    }

    /**
     * @return a list with all the sizes from each aspect ratio.
     */
    public List<Size> allSizes() {
        List<Size> sizeList = new ArrayList<>();
        for (SortedSet<Size> sizes : ratios.values()) {
            sizeList.addAll(sizes);
        }
        return sizeList;
    }

    /**
     * Clears the map of (aspect ratio, sizes list).
     */
    public void clear() {
        ratios.clear();
    }
}
