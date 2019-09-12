package com.telenav.osv.common.event;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class SimpleEventBus {
    private final PublishSubject<Object> mBusSubject;

    public SimpleEventBus() {
        mBusSubject = PublishSubject.create();
    }

    public void post(Object event) {
        mBusSubject.onNext(event);
    }

    public Observable<Object> observable() {
        return mBusSubject;
    }

    @SuppressWarnings("unchecked")
    public <T> Observable<T> filteredObservable(final Class<T> eventClass) {
        return mBusSubject.filter(eventClass::isInstance).map(event -> (T) event);
    }
}
