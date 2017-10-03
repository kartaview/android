package com.telenav.osv.di;

import javax.inject.Singleton;
import android.content.Context;
import com.telenav.osv.data.RecordingPreferences;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.manager.capture.Camera2Manager;
import com.telenav.osv.manager.capture.CameraManager;
import com.telenav.osv.manager.capture.CameraManagerOld;
import com.telenav.osv.manager.location.PositionMatcher;
import com.telenav.osv.manager.location.ScoreManager;
import com.telenav.osv.manager.location.SensorManager;
import com.telenav.osv.manager.network.GeometryRetriever;
import dagger.Module;
import dagger.Provides;

/**
 * Created by kalmanb on 9/26/17.
 */
@Module
class RecordModule {

    @Singleton
    @Provides
    ScoreManager provideScoreManager(SequenceDB db, PositionMatcher matcher) {
        return new ScoreManager(matcher, db);
    }

    @Singleton
    @Provides
    PositionMatcher providePositionMatcher(GeometryRetriever retriever) {
        return new PositionMatcher(retriever);
    }

    @Singleton
    @Provides
    SensorManager provideSensorManager(Context context) {
        return new SensorManager(context);
    }

    @Singleton
    @Provides
    CameraManager provideCameraManager(Context context, RecordingPreferences prefs) {
        CameraManager manager;
        if (prefs.isNewCameraApi()) {
            manager = new Camera2Manager(context, prefs);
        } else {
            manager = new CameraManagerOld(context, prefs);
        }
        return manager;
    }
}
