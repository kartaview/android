package com.telenav.osv.application;

/**
 * Created by Kalman on 10/7/2015.
 */
public final class PreferenceTypes {

    public static final String K_FTUE = "ftue";

    public static final String K_APP_RUN_TIME_COUNTER = "appRunTimeCounter";

    public static final String K_POS_LAT = "pref_pos_lat";

    public static final String K_POS_LON = "pref_pos_lon";

    public static final String K_UPLOAD_DATA_ENABLED = "upload_data_enabled";

    public static final String K_UPLOAD_AUTO = "upload_auto";

    public static final String K_ACCESS_TOKEN = "access_token";

    public static final String K_USER_NAME = "user_name";

    public static final String K_USER_ID = "user_id";

    public static final String K_DEBUG_ENABLED = "debugEnabled";

    public static final String K_DEBUG_SAVE_AUTH = "saveAuth";

    public static final String K_DEBUG_RECORDING_TAGGING = "recordingTagging";

    public static final String K_DEBUG_SERVER_TYPE = "serverType";

    public static final String K_URL_ENVIRONMENT = "urlEnvironment";

    public static final String K_RESTART_COUNTER = "restartCounter";

    public static final String K_DISTANCE_UNIT_METRIC = "distanceUnit";

    public static final String K_SIGN_DETECTION_ENABLED = "signDetection";

    public static final String K_REGION_US = "signDetectionRegionUS";

    public static final String K_INTRO_SHOWN = "walkthroughShown";

    public static final String K_RUN_COUNTER = "runCounter";

    public static final String K_EXTERNAL_STORAGE = "externalStorage";

    public static final String K_HINT_BACKGROUND = "hintBackground";

    public static final String K_HINT_TAP_ON_MAP = "hintTapOnMap";

    public static final String K_VERSION_CODE = "versionCode";

    public static final String K_MAP_DISABLED = "mapDisabled";

    public static final String K_MAP_ENABLED = "mapEnabled";

    public static final String K_OBD_TYPE = "obdType";

    public static final String K_RESOLUTION_WIDTH = "resolutionWidth";

    public static final String K_RESOLUTION_HEIGHT = "resolutionHeight";

    /**
     * This preference is removed from version {@code 3.2.0}.
     */
    @Deprecated
    public static final String K_RECORDING_MAP_ENABLED = "recordingMapEnabled";

    public static final String K_RECORDING_MINI_MAP_ENABLED = "recordingMiniMapEnabled";

    public static final String K_SKIP_DELETE_DIALOG = "showDeleteDialog";

    public static final String K_GAMIFICATION = "gamification";

    public static final String K_CRASHED = "crashed";

    public static final String K_SHOW_SAFE_MODE_MESSAGE = "showSafeModeMessage";

    public static final String K_VIDEO_MODE_ENABLED = "videoModeEnabled";

    public static final String K_SHOW_CLEAR_RECENTS_WARNING = "clearRecentsWarning";

    public static final String K_USER_TYPE = "userTypeSecond";

    public static final int USER_TYPE_UNKNOWN = -1;

    public static final int USER_TYPE_CONTRIBUTOR = 0;

    public static final int USER_TYPE_QA = 1;

    public static final String K_USER_PHOTO_URL = "userPhotoUrl";

    public static final String K_DISPLAY_NAME = "displayName";

    public static final String K_LOGIN_TYPE = "loginType";

    public static final String K_NEW_ENCODER_CRASH_COUNTER = "newEncoderCrashCounter";

    public static final String K_RECORDING_MAP_ZOOM = "recordingMapZoom";

    public static final String K_UPLOAD_CHARGING = "uploadCharging";

    public static final String K_HIDE_RECORDING_SUMMARY = "hideSummary";

    @Deprecated
    public static final String K_DRIVER_MODE_DIALOG_SHOWN = "driverModeDialogShown";

    public static final String K_FOCUS_MODE_STATIC = "focusMode";

    public static final String K_USE_CAMERA_API_NEW = "cameraApi";

    public static final String K_USED_CAMERA_ID = "usedCameraId";

    public static final String K_RECORD_START_TIME = "recordStartTime";

    public static final String K_BLE_DEVICE_ADDRESS = "bleDeviceAddress";

    public static final String K_OBD_MANUAL_STOPPED = "obdManualStopped";

    public static final String K_CURRENT_SEQUENCE_ID = "currentSequenceId";

    public static final String K_TAGGING_MODE = "recordingTaggingMode";

    public static final String K_DEBUG_BENCHMARK_SHUTTER_LOGIC = "benchmarkShutterLogic";

    public static final String JARVIS_USER_ID = "jarvisUserId";

    public static final String JARVIS_USER_NAME = "jarvisUserName";

    public static final String JARVIS_ACCESS_TOKEN = "jarvisAccessToken";

    public static final String JARVIS_REFRESH_TOKEN = "jarvisRefreshToken";

    /**
     * This preference is removed from version {@link KVApplication#APP_VERSION_CODE_2_7_4}.
     */
    @Deprecated
    static final String K_SAFE_MODE_ENABLED = "safeModeEnabled";

    private PreferenceTypes() {
    }
}