package com.telenav.osv.common.structure;

import java.util.LinkedList;

/**
 * Custom implementation for the linked list which would contain a specific number of elements and act as a FIFO.
 * @param <T> the type for the list
 */
public class CircularQueue<T> extends LinkedList<T> {
    /**
     * The capacity for the queue after which it will automatically remove before adding the first added element.
     */
    private int capacity;

    /**
     * Default constructor for the current class.
     */
    public CircularQueue(int capacity) {
        this.capacity = capacity;
    }

    /**
     * If the size is equal with the {@link #capacity} it will automatically remove the first element by using {@link #removeFirst()}.
     * @param e the type of the object to be added.
     * @return {@code true} if the add was successful, {@code false} otherwise.
     */
    @Override
    public boolean add(T e) {
        if (size() >= capacity) {
            removeFirst();
        }
        return super.add(e);
    }
}
