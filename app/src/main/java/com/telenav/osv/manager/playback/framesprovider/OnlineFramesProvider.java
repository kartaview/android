package com.telenav.osv.manager.playback.framesprovider;

import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.UserDataManager;
import com.telenav.osv.manager.playback.framesprovider.data.TrackInfo;
import com.telenav.osv.utils.Log;

/**
 * Concrete implementation of a {@link AbstractFramesProvider} which loads a JPEG sequence from the online storage.
 * <p>
 * Created by catalinj on 9/28/17.
 */
public class OnlineFramesProvider extends AbstractFramesProvider {

    private static final String TAG = OnlineFramesProvider.class.getSimpleName();

    private final UserDataManager mUserDataManager;

    public OnlineFramesProvider(UserDataManager mUserDataManager) {
        this.mUserDataManager = mUserDataManager;
    }

    @Override
    public void fetchFrames(int sequenceId) {
        mUserDataManager.listImages(sequenceId, new NetworkResponseDataListener<PhotoCollection>() {

            @Override
            public void requestFailed(int status, PhotoCollection details) {
                Log.w(TAG, "displaySequence: failed, " + details);
                notifyListenerOfFailure();
            }

            @Override
            public void requestFinished(int status, final PhotoCollection collectionData) {
                notifyListenerOfLoadComplete(new TrackInfo(collectionData));
            }
        });
    }
}
