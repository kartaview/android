package com.telenav.osv.ui.list;

import java.util.ArrayList;
import java.util.Collection;
import android.databinding.ListChangeRegistry;
import android.databinding.ObservableList;

/**
 * Observable array list that behaves like a set, no duplicate items allowed
 * Created by kalmanb on 9/15/17.
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ObservableOrderedSet<T> extends ArrayList<T> implements ObservableList<T> {

    private transient ListChangeRegistry mListeners = new ListChangeRegistry();

    @Override
    public void addOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners == null) {
            mListeners = new ListChangeRegistry();
        }
        mListeners.add(listener);
    }

    @Override
    public void removeOnListChangedCallback(OnListChangedCallback listener) {
        if (mListeners != null) {
            mListeners.remove(listener);
        }
    }

    @Override
    public T set(int index, T object) {
        T val = super.set(index, object);
        if (mListeners != null) {
            mListeners.notifyChanged(this, index, 1);
        }
        return val;
    }

    @Override
    public void add(int index, T object) {
        super.add(index, object);
        notifyAdd(index, 1);
    }

    @Override
    public T remove(int index) {
        T val = super.remove(index);
        notifyRemove(index, 1);
        return val;
    }

    @Override
    public boolean remove(Object object) {
        int index = indexOf(object);
        if (index >= 0) {
            remove(index);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        int oldSize = size();
        super.clear();
        if (oldSize != 0) {
            notifyRemove(0, oldSize);
        }
    }

    @Override
    protected void removeRange(int fromIndex, int toIndex) {
        super.removeRange(fromIndex, toIndex);
        notifyRemove(fromIndex, toIndex - fromIndex);
    }

    public ObservableOrderedSet<T> insertItem(T object) {
        if (!contains(object)) {
            super.add(object);
        }
        return this;
    }

    public ObservableOrderedSet<T> insertItem(int index, T object) {
        if (!contains(object)) {
            super.add(index, object);
        }
        return this;
    }

    public ObservableOrderedSet<T> insertCollection(Collection<? extends T> collection) {
        int oldSize = size();
        boolean added = false;
        for (T e : collection) {
            boolean res = addSilent(e);
            added = added || res;
        }
        if (added) {
            notifyAdd(oldSize, size() - oldSize);
        }
        return this;
    }

    public ObservableOrderedSet<T> removeCollection(Collection<? extends T> collection) {
        int oldSize = size();
        boolean removed = false;
        for (T e : collection) {
            boolean res = removeSilent(e);
            removed = removed || res;
        }
        if (removed) {
            notifyRemove(oldSize, size() - oldSize);
        }
        return this;
    }

    private boolean addSilent(T object) {
        return !contains(object) && super.add(object);
    }

    private boolean removeSilent(T object) {
        int index = indexOf(object);
        if (index >= 0) {
            remove(index);
            return true;
        } else {
            return false;
        }
    }

    private void notifyAdd(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyInserted(this, start, count);
        }
    }

    private void notifyRemove(int start, int count) {
        if (mListeners != null) {
            mListeners.notifyRemoved(this, start, count);
        }
    }
}
