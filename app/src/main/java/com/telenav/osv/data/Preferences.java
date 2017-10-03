package com.telenav.osv.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Transformations;
import android.location.Location;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.utils.Size;
import static com.telenav.osv.item.network.UserData.TYPE_UNKNOWN;

/**
 * Class for handling preferences easily
 * Created by kalmanb on 9/14/17.
 */
@SuppressWarnings({"SameParameterValue", "unused"})
public class Preferences implements PreferenceTypes, VersionPreferences, MapPreferences, DynamicPreferences,
        RecordingPreferences, ProfilePreferences, UIPreferences, RunPreferences {

    public static final String[] URL_ENV =
            {"openstreetcam.org/", "staging.openstreetcam.org/", "testing.openstreetcam.org/", "beta.openstreetcam.org/"};

    private final ApplicationPreferences prefs;

    @Inject
    public Preferences(ApplicationPreferences prefs) {
        this.prefs = prefs;
    }

    // version related

    public String getVersionName() {
        return prefs.getStringPreference(K_VERSION_NAME);
    }

    public void setVersionName(String versionName) {
        prefs.saveStringPreference(K_VERSION_NAME, versionName);
    }

    public int getVersionCode() {
        return prefs.getIntPreference(K_VERSION_CODE);
    }

    public void setVersionCode(int versionCode) {
        prefs.saveIntPreference(K_VERSION_CODE, versionCode);
    }

    public int getVersionCodeForSdk() {
        return prefs.getIntPreference(K_SDK_VERSION_CODE);
    }

    public void setVersionCodeForSdk(int version) {
        prefs.saveIntPreference(K_SDK_VERSION_CODE, version);
    }

    // caching for map

    public Location getLastLocation() {
        String string = prefs.getStringPreference(K_LAST_POSITION, null);
        Location loc = null;
        if (string != null) {
            String[] coords = string.split(",");
            loc = new Location("cache");
            loc.setLatitude(Double.parseDouble(coords[0].trim()));
            loc.setLongitude(Double.parseDouble(coords[1].trim()));
        }
        return loc;
    }

    public void saveLastLocation(Location loc) {
        if (loc != null) {
            String s = loc.getLatitude() + "," + loc.getLongitude();
            prefs.saveStringPreference(K_LAST_POSITION, s);
        }
    }

    public float getRecordingMapZoom() {
        return prefs.getFloatPreference(K_RECORDING_MAP_ZOOM, 16f);
    }

    public void setRecordingMapZoom(float value) {
        prefs.saveFloatPreference(K_RECORDING_MAP_ZOOM, value);
    }

    public void postRecordingMapZoom(float value) {
        prefs.saveFloatPreference(K_RECORDING_MAP_ZOOM, value, true);
    }

    public String getMapResourcesPath() {
        return prefs.getStringPreference(K_MAP_RESOURCES_PATH, "");
    }

    public void setMapResourcesPath(String path) {
        prefs.saveStringPreference(K_MAP_RESOURCES_PATH, path);
    }

    // dynamic preferences

    public boolean isAutoUploadEnabled() {
        return prefs.getBooleanPreference(K_UPLOAD_AUTO);
    }

    public MutableLiveData<Boolean> getAutoUploadLive() {
        return prefs.getLiveBooleanPreference(K_UPLOAD_AUTO);
    }

    public boolean isDataUploadEnabled() {
        return prefs.getBooleanPreference(K_UPLOAD_DATA_ENABLED);
    }

    public MutableLiveData<Boolean> getDataUploadLive() {
        return prefs.getLiveBooleanPreference(K_UPLOAD_DATA_ENABLED);
    }

    public boolean isChargingUploadEnabled() {
        return prefs.getBooleanPreference(K_UPLOAD_CHARGING);
    }

    public MutableLiveData<Boolean> getChargingUploadLive() {
        return prefs.getLiveBooleanPreference(K_UPLOAD_CHARGING);
    }

    public MutableLiveData<Boolean> getSaveAuthLive() {
        return prefs.getLiveBooleanPreference(K_DEBUG_SAVE_AUTH);
    }

    public boolean getSaveAuth() {
        return prefs.getBooleanPreference(K_DEBUG_SAVE_AUTH);
    }

    public MutableLiveData<Boolean> getUsingMetricUnitsLive() {
        return prefs.getLiveBooleanPreference(K_DISTANCE_UNIT_METRIC);
    }

    public boolean isUsingMetricUnits() {
        return prefs.getBooleanPreference(K_DISTANCE_UNIT_METRIC);
    }

    public void setUsingMetricUnits(boolean value) {
        prefs.saveBooleanPreference(K_DISTANCE_UNIT_METRIC, value);
    }

    public MutableLiveData<Boolean> getExtStorageLive() {
        return prefs.getLiveBooleanPreference(K_EXTERNAL_STORAGE);
    }

    public boolean isUsingExternalStorage() {
        return prefs.getBooleanPreference(K_EXTERNAL_STORAGE);
    }

    public void setUsingExternalStorage(boolean value) {
        prefs.saveBooleanPreference(K_EXTERNAL_STORAGE, value);
    }

    public MutableLiveData<Boolean> getGamificationEnabledLive() {
        return prefs.getLiveBooleanPreference(K_GAMIFICATION, true);
    }

    public boolean isGamificationEnabled() {
        return prefs.getBooleanPreference(K_GAMIFICATION, true);
    }

    public void setGamificationEnabled(boolean value) {
        prefs.saveBooleanPreference(K_GAMIFICATION, value);
    }

    public MutableLiveData<Boolean> getMapEnabledLive() {
        return prefs.getLiveBooleanPreference(K_MAP_ENABLED, true);
    }

    public boolean isMapEnabled() {
        return prefs.getBooleanPreference(K_MAP_ENABLED, true);
    }

    public void setMapEnabled(boolean value) {
        prefs.saveBooleanPreference(K_MAP_ENABLED, value);
    }

    public MutableLiveData<Boolean> getMiniMapEnabledLive() {
        return prefs.getLiveBooleanPreference(K_RECORDING_MAP_ENABLED, true);
    }

    public boolean isMiniMapEnabled() {
        return prefs.getBooleanPreference(K_RECORDING_MAP_ENABLED, true);
    }

    public void setMiniMapEnabled(boolean value) {
        prefs.saveBooleanPreference(K_RECORDING_MAP_ENABLED, value);
    }

    public MutableLiveData<Integer> getObdTypeLive() {
        return prefs.getLiveIntPreference(K_OBD_TYPE);
    }

    public int getObdType() {
        return prefs.getIntPreference(K_OBD_TYPE);
    }

    public void setObdType(int value) {
        prefs.saveIntPreference(K_OBD_TYPE, value);
    }

    public MutableLiveData<Integer> getObdStatusLive() {
        return prefs.getLiveIntPreference(K_OBD_STATUS);
    }

    public int getObdStatus() {
        return prefs.getIntPreference(K_OBD_STATUS);
    }

    public boolean isDebugEnabled() {
        return prefs.getBooleanPreference(K_DEBUG_ENABLED);
    }

    public MutableLiveData<Boolean> getDebugEnabledLive() {
        return prefs.getLiveBooleanPreference(K_DEBUG_ENABLED);
    }

    public MutableLiveData<Integer> getServerTypeLive() {
        return prefs.getLiveIntPreference(K_DEBUG_SERVER_TYPE);
    }

    public int getServerType() {
        return prefs.getIntPreference(K_DEBUG_SERVER_TYPE);
    }

    public void setServerType(int value) {
        prefs.saveIntPreference(K_DEBUG_SERVER_TYPE, value);
    }

    // recording related

    public LiveData<Size> getResolutionLive() {
        MediatorLiveData<Size> result = new MediatorLiveData<>();
        Size size = new Size(0, 0);
        result.addSource(prefs.getLiveStringPreference(K_RESOLUTION, "0,0"), s -> {
            if (s != null) {
                String[] str = s.split(",");
                if (str.length > 1) {
                    size.width = Integer.parseInt(str[0].trim());
                    size.height = Integer.parseInt(str[1].trim());
                }
            }
        });
        result.setValue(size);
        return result;
    }

    public Size getResolution() {
        Size size = new Size(0, 0);
        String s = prefs.getStringPreference(K_RESOLUTION, "0,0");
        if (s != null) {
            String[] str = s.split(",");
            if (str.length > 1) {
                size.width = Integer.parseInt(str[0].trim());
                size.height = Integer.parseInt(str[1].trim());
            }
        }
        return size;
    }

    public void setResolution(Size resolution) {
        String res = resolution.width + "," + resolution.height;
        prefs.saveStringPreference(K_RESOLUTION, res);
    }

    public LiveData<Size> getPreviewResolutionLive() {
        MediatorLiveData<Size> result = new MediatorLiveData<>();
        Size size = new Size(0, 0);
        result.addSource(prefs.getLiveStringPreference(K_PREVIEW_RESOLUTION, "1600,1200"), s -> {
            if (s != null) {
                String[] str = s.split(",");
                if (str.length > 1) {
                    size.width = Integer.parseInt(str[0].trim());
                    size.height = Integer.parseInt(str[1].trim());
                }
            }
        });
        result.setValue(size);
        return result;
    }

    public Size getPreviewResolution() {
        String s = prefs.getStringPreference(K_PREVIEW_RESOLUTION, "0,0");
        Size result = new Size(0, 0);
        if (s != null) {
            String[] str = s.split(",");
            if (str.length > 1) {
                result.width = Integer.parseInt(str[0].trim());
                result.height = Integer.parseInt(str[1].trim());
            }
        }
        return result;
    }

    public void setPreviewResolution(Size resolution) {
        String res = resolution.width + "," + resolution.height;
        prefs.saveStringPreference(K_PREVIEW_RESOLUTION, res);
    }

    public MutableLiveData<Boolean> getSafeModeLive() {
        return prefs.getLiveBooleanPreference(K_SAFE_MODE_ENABLED);
    }

    public boolean isSafeMode() {
        return prefs.getBooleanPreference(K_SAFE_MODE_ENABLED);
    }

    public void setSafeMode(boolean value) {
        prefs.saveBooleanPreference(K_SAFE_MODE_ENABLED, value);
    }

    public MutableLiveData<Boolean> getStaticFocusLive() {
        return prefs.getLiveBooleanPreference(K_FOCUS_MODE_STATIC);
    }

    public boolean isStaticFocus() {
        return prefs.getBooleanPreference(K_FOCUS_MODE_STATIC);
    }

    public void setStaticFocus(boolean value) {
        prefs.saveBooleanPreference(K_FOCUS_MODE_STATIC, value);
    }

    public MutableLiveData<Boolean> getNewCameraApiLive() {
        return prefs.getLiveBooleanPreference(K_USE_CAMERA_API_NEW);
    }

    public boolean isNewCameraApi() {
        return prefs.getBooleanPreference(K_USE_CAMERA_API_NEW);
    }

    public void setNewCameraApi(boolean value) {
        prefs.saveBooleanPreference(K_USE_CAMERA_API_NEW, value);
    }

    public List<Size> getSupportedResolutions() {
        ArrayList<Size> list = new ArrayList<>();
        Set<String> set = prefs.getStringSetPreference(PreferenceTypes.K_SUPPORTED_RESOLUTIONS);
        for (String val : set) {
            list.add(new Size(val));
        }
        return list;
    }

    public void setSupportedResolutions(List<Size> list) {
        HashSet<String> set = new HashSet<>();
        for (Size size : list) {
            set.add(size.width + "x" + size.height);
        }
        prefs.saveStringSetPreference(PreferenceTypes.K_SUPPORTED_RESOLUTIONS, set);
    }

    // account related

    public boolean isLoggedIn() {
        String token = prefs.getStringPreference(K_ACCESS_TOKEN);
        return token != null && !token.equals("");
    }

    public LiveData<Boolean> observeLogin() {
        return Transformations.map(prefs.getLiveStringPreference(K_ACCESS_TOKEN), s -> s != null && !s.equals(""));
    }

    public int getLoginType() {
        return prefs.getIntPreference(K_LOGIN_TYPE);
    }

    public void setLoginType(int type) {
        prefs.saveIntPreference(K_LOGIN_TYPE, type);
    }

    public LiveData<String> getAuthTokenLive() {
        return prefs.getLiveStringPreference(K_ACCESS_TOKEN);
    }

    public String getAuthToken() {
        return prefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);
    }

    public void saveAuthToken(String accessToken) {
        prefs.saveStringPreference(PreferenceTypes.K_ACCESS_TOKEN, accessToken);
    }

    public String getUserName() {
        return prefs.getStringPreference(K_USER_NAME);
    }

    public void setUserName(String userName) {
        prefs.saveStringPreference(K_USER_NAME, userName);
    }

    public String getUserId() {
        return prefs.getStringPreference(K_USER_ID);
    }

    public void setUserId(String id) {
        prefs.saveStringPreference(K_USER_ID, id);
    }

    public String getUserDisplayName() {
        return prefs.getStringPreference(K_DISPLAY_NAME);
    }

    public void setUserDisplayName(String userDisplayName) {
        prefs.saveStringPreference(K_DISPLAY_NAME, userDisplayName);
    }

    public String getUserPhotoUrl() {
        return prefs.getStringPreference(K_USER_PHOTO_URL);
    }

    public void setUserPhotoUrl(String url) {
        prefs.saveStringPreference(K_USER_PHOTO_URL, url);
    }

    public LiveData<String> getUserPhotoUrlLive() {
        return prefs.getLiveStringPreference(K_USER_PHOTO_URL);
    }

    public LiveData<Integer> getUserTypeLive() {
        return prefs.getLiveIntPreference(K_USER_TYPE, TYPE_UNKNOWN);
    }

    public int getUserType() {
        return prefs.getIntPreference(K_USER_TYPE, TYPE_UNKNOWN);
    }

    public void setUserType(int type) {
        prefs.saveIntPreference(K_USER_TYPE, type);
    }

    public boolean isDriver() {
        int userType = prefs.getIntPreference(K_USER_TYPE);
        return UserData.isDriver(userType);
    }

    public LiveData<Boolean> isDriverLive() {
        return Transformations.map(prefs.getLiveIntPreference(K_USER_TYPE), UserData::isDriver);
    }

    // hints & tutorials related set-and-forget type

    public boolean loggedIn() {
        String userName = prefs.getStringPreference(K_USER_NAME);
        String token = prefs.getStringPreference(K_ACCESS_TOKEN);
        return !"".equals(userName) && !"".equals(token);
    }

    public boolean shouldShowTapToShoot() {
        return prefs.getBooleanPreference(K_SHOW_TAP_TO_SHOOT, true);
    }

    public void setShouldShowTapToShoot(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_TAP_TO_SHOOT, value);
    }

    public boolean shouldShowBackgroundHint() {
        return prefs.getBooleanPreference(K_SHOW_BACKGROUND_HINT, true);
    }

    public void setShouldShowBackgroundHint(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_BACKGROUND_HINT, value);
    }

    public boolean shouldShowRecordingSummary() {
        return prefs.getBooleanPreference(K_SHOW_RECORDING_SUMMARY, true);
    }

    public void setShouldShowRecordingSummary(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_RECORDING_SUMMARY, value);
    }

    public boolean shouldShowClearRecentsWarning() {
        return prefs.getBooleanPreference(K_SHOW_CLEAR_RECENTS_WARNING, false);
    }

    public void setShouldShowClearRecentsWarning(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_CLEAR_RECENTS_WARNING, value);
    }

    public boolean shouldShowWalkthrough() {
        return prefs.getBooleanPreference(K_SHOW_INTRO, true);
    }

    public void setShouldShowWalkthrough(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_INTRO, value);
    }

    public boolean shouldShowSafeModeMessage() {
        return prefs.getBooleanPreference(K_SHOW_SAFE_MODE_MESSAGE, false);
    }

    public void setShouldShowSafeModeMessage(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_SAFE_MODE_MESSAGE, value);
    }

    public boolean shouldShowDeleteConfirmation() {
        return prefs.getBooleanPreference(K_SHOW_DELETE_DIALOG, true);
    }

    public void setShouldShowDeleteConfirmation(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_DELETE_DIALOG, value);
    }

    public boolean shouldShowTapOnMap() {
        return prefs.getBooleanPreference(K_HINT_TAP_ON_MAP, true);
    }

    public void setShouldShowTapOnMap(boolean value) {
        prefs.saveBooleanPreference(K_HINT_TAP_ON_MAP, value);
    }

    public boolean shouldShowDriverDialog() {
        return prefs.getBooleanPreference(K_SHOW_DRIVER_MODE_DIALOG, true);
    }

    // runtime & crash related

    public void setShouldShowDriverDialog(boolean value) {
        prefs.saveBooleanPreference(K_SHOW_DRIVER_MODE_DIALOG, value);
    }

    /*
     * this value will be true only if it's called for the first time
     **/
    public boolean isFirstRun() {
        boolean value = prefs.getBooleanPreference(K_FIRST_RUN, true);
        if (value) {
            prefs.saveBooleanPreference(K_FIRST_RUN, false);
        }
        return value;
    }

    public int getFfmpegCrashCounter() {
        return prefs.getIntPreference(K_FFMPEG_CRASH_COUNTER);
    }

    public void setFfmpegCrashCounter(int value) {
        prefs.saveIntPreference(K_FFMPEG_CRASH_COUNTER, value);
    }

    public int getRestartCounter() {
        return prefs.getIntPreference(K_RESTART_COUNTER);
    }

    public void setRestartCounter(int value) {
        prefs.saveIntPreference(K_RESTART_COUNTER, value);
    }

    public boolean getCrashed() {
        return prefs.getBooleanPreference(K_CRASHED);
    }

    public void setCrashed(boolean value) {
        prefs.saveBooleanPreference(K_CRASHED, value);
    }

    @Override
    public double getDistance() {
        return prefs.getFloatPreference(PreferenceTypes.K_TOTAL_DISTANCE);
    }

    @Override
    public void setDistance(double value) {
        prefs.saveFloatPreference(PreferenceTypes.K_TOTAL_DISTANCE, (float) value);
    }

    @Override
    public double getAcceptedDistance() {
        return prefs.getFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_ACCEPTED_DISTANCE);
    }

    @Override
    public void setAcceptedDistance(double value) {
        prefs.saveFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_ACCEPTED_DISTANCE, (float) value);
    }

    @Override
    public double getRejectedDistance() {
        return prefs.getFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_REJECTED_DISTANCE);
    }

    @Override
    public void setRejectedDistance(double value) {
        prefs.saveFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_REJECTED_DISTANCE, (float) value);
    }

    @Override
    public double getObdDistance() {
        return prefs.getFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_OBD_DISTANCE);
    }

    @Override
    public void setObdDistance(double value) {
        prefs.saveFloatPreference(PreferenceTypes.K_DRIVER_TOTAL_OBD_DISTANCE, (float) value);
    }

    @Override
    public int getTracksCount() {
        return prefs.getIntPreference(PreferenceTypes.K_DRIVER_TRACKS_COUNT);
    }

    @Override
    public void setTracksCount(int value) {
        prefs.saveIntPreference(PreferenceTypes.K_DRIVER_TRACKS_COUNT, value);
    }

    @Override
    public int getPhotosCount() {
        return prefs.getIntPreference(PreferenceTypes.K_DRIVER_PHOTOS_COUNT);
    }

    @Override
    public void setPhotosCount(int value) {
        prefs.saveIntPreference(PreferenceTypes.K_DRIVER_PHOTOS_COUNT, value);
    }
}
