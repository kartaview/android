package com.telenav.osv.recorder.shutter;

import java.util.concurrent.TimeUnit;
import com.telenav.osv.item.SpeedData;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

/**
 * Component handling quality changes in the obd data
 */
class ObdQualityChecker {

    private Completable completable;

    private Disposable disposable;

    private Action action;

    ObdQualityChecker() {
        completable = Completable.create(emitter -> emitter.onComplete())
                .delay(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    void setActionForTimeOut(Action actionForTimeOut) {
        action = actionForTimeOut;
    }

    void onSpeedObtained(SpeedData speedData) {
        if (speedData.getSpeed() != -1) {
            if (disposable != null) {
                disposable.dispose();
            }
            disposable = completable.subscribe(action);
        }
    }
}


