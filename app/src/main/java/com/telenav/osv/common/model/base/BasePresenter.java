package com.telenav.osv.common.model.base;

/**
 * Base interface for all presenter interfaces. It will impose the usage of the {@link #start()} to have a mechanism of initialisation of the presenter.
 * @author horatiuf
 */
public interface BasePresenter {
    /**
     * Initialisation method for each presenter implementation.
     */
    void start();
}
