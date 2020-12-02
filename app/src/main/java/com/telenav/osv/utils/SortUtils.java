package com.telenav.osv.utils;

import java.util.Collections;
import java.util.List;
import com.telenav.osv.data.sequence.model.LocalSequence;

/**
 * Helper class for sorting utils such as:
 * <ul>
 * <li>{@link #sortLocalSequences(List)}</li>
 * </ul>
 * @author horatiuf
 */
public class SortUtils {

    /**
     * Sorts the {@code LocalSequence} based on the creation time of the sequence. This will be in descended order from newest to oldest.
     * @param localSequences the {@code List<LocalSequence>}
     */
    public static void sortLocalSequences(List<LocalSequence> localSequences) {
        Collections.sort(localSequences, (lhs, rhs) -> rhs.getDetails().getDateTime().compareTo(lhs.getDetails().getDateTime()));
    }
}

