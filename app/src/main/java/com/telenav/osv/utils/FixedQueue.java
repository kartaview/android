package com.telenav.osv.utils;

/**
 * fixed size queue
 * Created by Kalman on 17/02/2017.
 */
public class FixedQueue {

    protected int index;

    Integer[] ring;

    /**
     * @param initialValues contains the ring's initial values.
     * The "oldest" value in the queue is expected to reside in
     * position 0, the newest one in position length-1.
     */
    public FixedQueue(Integer[] initialValues) {
        // This is a little ugly, but there are no
        // generic arrays in Java
        ring = new Integer[initialValues.length];

        // We don't want to work on the original data
        System.arraycopy(initialValues, 0, ring, 0, initialValues.length);

        // The next time we add something to the queue,
        // the oldest element should be replaced
        index = 0;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("[");
        for (int i = index, n = 0; n < ring.length; i = nextIndex(i), n++) {
            sb.append(ring[i]);
            if (n + 1 < ring.length) {
                sb.append(", ");
            }
        }
        return sb.append("]").toString();
    }

    public boolean add(Integer newest) {
        return offer(newest);
    }

    public Integer element() {
        return ring[getHeadIndex()];
    }

    public boolean offer(Integer newest) {
        Integer oldest = ring[index];
        ring[index] = newest;
        incrIndex();
        return true;
    }

    public Integer peek() {
        return ring[getHeadIndex()];
    }

    public int size() {
        return ring.length;
    }

    public Integer get(int absIndex) throws IndexOutOfBoundsException {
        if (absIndex >= ring.length) {
            throw new IndexOutOfBoundsException("Invalid index " + absIndex);
        }
        int i = index + absIndex;
        if (i >= ring.length) {
            i -= ring.length;
        }
        return ring[i];
    }

    void incrIndex() {
        index = nextIndex(index);
    }

    int nextIndex(int current) {
        if (current + 1 >= ring.length) {
            return 0;
        } else {
            return current + 1;
        }
    }

    int previousIndex(int current) {
        if (current - 1 < 0) {
            return ring.length - 1;
        } else {
            return current - 1;
        }
    }

    int getHeadIndex() {
        if (index == 0) {
            return ring.length - 1;
        } else {
            return index - 1;
        }
    }
}
