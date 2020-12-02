package com.telenav.osv.common.model.base;

/**
 * Base interface for all view interfaces. It's main focus is too impose a {@link #setPresenter(BasePresenter)} method.
 * @author horatiuf
 */
public interface BaseView<T extends BasePresenter> {
    /**
     * Sets the presenter for the current view in order to be able to be provided with business functionality.
     * @param presenter the {@link T} presenter to be set. It must an implementation of the {@link BasePresenter}.
     */
    void setPresenter(T presenter);
}
