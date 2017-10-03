package com.telenav.osv.data;

import java.util.List;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import com.telenav.osv.utils.Size;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
public interface RecordingPreferences {

    LiveData<Size> getResolutionLive();

    Size getResolution();

    void setResolution(Size resolution);

    LiveData<Size> getPreviewResolutionLive();

    Size getPreviewResolution();

    void setPreviewResolution(Size resolution);

    MutableLiveData<Boolean> getSafeModeLive();

    boolean isSafeMode();

    void setSafeMode(boolean value);

    MutableLiveData<Boolean> getStaticFocusLive();

    boolean isStaticFocus();

    void setStaticFocus(boolean value);

    MutableLiveData<Boolean> getNewCameraApiLive();

    boolean isNewCameraApi();

    void setNewCameraApi(boolean value);

    List<Size> getSupportedResolutions();

    void setSupportedResolutions(List<Size> list);
}
