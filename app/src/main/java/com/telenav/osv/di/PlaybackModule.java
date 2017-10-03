package com.telenav.osv.di;

import javax.inject.Named;
import javax.inject.Singleton;
import android.content.Context;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.manager.playback.JpegPlaybackManager;
import com.telenav.osv.manager.playback.LocalPlaybackManager;
import com.telenav.osv.manager.playback.PlaybackManager;
import com.telenav.osv.manager.playback.framesprovider.LocalStorageFramesProvider;
import com.telenav.osv.manager.playback.framesprovider.OnlineFramesProvider;
import dagger.Module;
import dagger.Provides;

/**
 * Module providing the playback functionality's dependencies
 * Created by kalmanb on 9/26/17.
 */
@Module
public class PlaybackModule {

    public static final String SCOPE_MP4_LOCAL = "scope_mp4_local";

    public static final String SCOPE_JPEG_ONLINE = "jpeg_online";

    public static final String SCOPE_JPEG_LOCAL = "jpeg_local";

    @Singleton
    @Provides
    @Named(PlaybackModule.SCOPE_MP4_LOCAL)
    PlaybackManager provideLocalPlaybackManager(Context context, SequenceDB db) {
        return new LocalPlaybackManager(context, db);
    }

    @Singleton
    @Provides
    @Named(PlaybackModule.SCOPE_JPEG_ONLINE)
    PlaybackManager provideJpegOnlinePlaybackManager(Context context, OnlineFramesProvider onlineFramesProvider) {
        return new JpegPlaybackManager(context, onlineFramesProvider);
    }

    @Singleton
    @Provides
    @Named(PlaybackModule.SCOPE_JPEG_LOCAL)
    PlaybackManager provideJpegLocalPlaybackManager(Context context, LocalStorageFramesProvider localStorageFramesProvider) {
        return new JpegPlaybackManager(context, localStorageFramesProvider);
    }

    @Singleton
    @Provides
    LocalStorageFramesProvider provideLocalStorageFramesProvider(SequenceDB db) {
        return new LocalStorageFramesProvider(db);
    }

    @Singleton
    @Provides
    OnlineFramesProvider provideOnlineFramesProvider(UserDataManager userDataManager) {
        return new OnlineFramesProvider(userDataManager);
    }
}
