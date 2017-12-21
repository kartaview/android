package com.telenav.osv.manager.network;

import java.util.ArrayList;
import java.util.Collections;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.content.SharedPreferences;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.telenav.osv.R;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.http.DeleteSequenceRequest;
import com.telenav.osv.http.DriverPayRatesRequest;
import com.telenav.osv.http.DriverProfileRequest;
import com.telenav.osv.http.LeaderboardRequest;
import com.telenav.osv.http.ListPaymentsRequest;
import com.telenav.osv.http.ListPhotosRequest;
import com.telenav.osv.http.ListSequencesRequest;
import com.telenav.osv.http.ProfileRequest;
import com.telenav.osv.http.requestFilters.LeaderboardRequestFilter;
import com.telenav.osv.http.requestFilters.ListRequestFilter;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.DriverData;
import com.telenav.osv.item.network.PayRateData;
import com.telenav.osv.item.network.PayRateItem;
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.parser.DriverDataParser;
import com.telenav.osv.manager.network.parser.DriverTracksParser;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.LeaderboardParser;
import com.telenav.osv.manager.network.parser.PayRateDataParser;
import com.telenav.osv.manager.network.parser.PaymentCollectionParser;
import com.telenav.osv.manager.network.parser.PhotoCollectionParser;
import com.telenav.osv.manager.network.parser.TrackCollectionParser;
import com.telenav.osv.manager.network.parser.UserDataParser;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class UserDataManager extends NetworkManager implements Response.ErrorListener {

    public static final String SERVER_STATUS_UPLOADING = "NEW";

    public static final String SERVER_STATUS_UPLOADED = "UPLOAD_FINISHED";

    public static final String SERVER_STATUS_PROCESSED = "PROCESSING_FINISHED";

    public static final String SERVER_STATUS_APPROVED = "APPROVED";

    public static final String SERVER_STATUS_REJECTED = "REJECTED";

    public static final String SERVER_STATUS_TBD = "TBD";

    private static final String ENVIRONMENT_PLACEHOLDER = "&&";

    private static final String PROTOCOL = "http://";

    private static final String TAG = "UserDataManager";

    /**
     * download photo file reques url
     */
    public static String URL_DOWNLOAD_PHOTO = PROTOCOL + ENVIRONMENT_PLACEHOLDER;

    /**
     * delete sequence request url
     */
    private static String URL_DELETE_SEQUENCE = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "sequence/remove/";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_TRACKS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "tracks/";

    /**
     * list ALL sequences request url
     */
    private static String URL_VERSION = PROTOCOL + ENVIRONMENT_PLACEHOLDER + "version";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_MY_SEQUENCES = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "list/my-list/";

    /**
     * list photos from a specific sequence url
     */
    private static String URL_LIST_PHOTOS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "sequence/photo-list/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_PROFILE_DETAILS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "user/details/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PAY_RATE_INFO = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "user/byod/payrates";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PROFILE_DETAILS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "user/byod/details";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PAYMENTS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "user/byod/payments";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_UPLOADED_TRACKS = PROTOCOL + ENVIRONMENT_PLACEHOLDER + URL_VER + "user/byod/uploaded-tracks";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_LEADERBOARD = PROTOCOL + ENVIRONMENT_PLACEHOLDER + "gm-leaderboard";

    private UserDataParser mUserDataParser = new UserDataParser();

    private DriverDataParser mDriverDataParser = new DriverDataParser();

    private TrackCollectionParser mUserTrackParser = new TrackCollectionParser();

    private DriverTracksParser mDriverTracksParser = new DriverTracksParser();

    private LeaderboardParser mLeaderboardParser = new LeaderboardParser();

    private PhotoCollectionParser mPhotoCollectionParser = new PhotoCollectionParser();

    private PaymentCollectionParser mPaymentCollectionParser = new PaymentCollectionParser();

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    private PayRateDataParser mDriverPayRateDataParser = new PayRateDataParser();

    public UserDataManager(Context context) {
        super(context);
        setEnvironment();
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
    }

    @Override
    protected void setEnvironment() {
        super.setEnvironment();
        URL_DELETE_SEQUENCE = URL_DELETE_SEQUENCE.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_LIST_MY_SEQUENCES = URL_LIST_MY_SEQUENCES.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_LIST_PHOTOS = URL_LIST_PHOTOS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_DOWNLOAD_PHOTO = URL_DOWNLOAD_PHOTO.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_PROFILE_DETAILS = URL_PROFILE_DETAILS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_DRIVER_PROFILE_DETAILS = URL_DRIVER_PROFILE_DETAILS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_DRIVER_PAY_RATE_INFO = URL_DRIVER_PAY_RATE_INFO.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_DRIVER_PAYMENTS = URL_DRIVER_PAYMENTS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_DRIVER_UPLOADED_TRACKS = URL_DRIVER_UPLOADED_TRACKS.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_LEADERBOARD = URL_LEADERBOARD.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        URL_VERSION = URL_VERSION.replace(ENVIRONMENT_PLACEHOLDER, URL_ENV[mCurrentServer]);
        Log.d(TAG, "setEnvironment: " + URL_ENV[mCurrentServer]);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * error listener for volley
     * @param error error thrown
     */
    @Override
    public void onErrorResponse(VolleyError error) {
        try {
            Log.e(TAG, "onErrorResponse" + new String(error.networkResponse.data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * lists a certain amount of sequences from the server from the given page
     * @param listener request listener
     * @param pageNumber number of the page
     * @param itemsPerPage number of items per page
     */
    public void listSequences(final NetworkResponseDataListener<TrackCollection> listener, int pageNumber, int itemsPerPage) {
        ListSequencesRequest seqRequest = new ListSequencesRequest(URL_LIST_MY_SEQUENCES,
                new OsvRequestResponseListener<TrackCollectionParser, TrackCollection>(
                        mUserTrackParser) {

                    @Override
                    public void onSuccess(final int status,
                                          final TrackCollection trackCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, trackCollection);
                                Log.d(TAG, "listSequences: successful");
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status,
                                          final TrackCollection trackCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, trackCollection);
                                Log.d(TAG, "listSequences: successful");
                            }
                        });
                    }
                }, getAccessToken(), pageNumber, itemsPerPage);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(3500, 5, 1f));
        seqRequest.setShouldCache(false);
        cancelListTasks();
        mQueue.add(seqRequest);
    }

    /**
     * lists a certain amount of sequences from the server from the given page
     * @param listener request listener
     * @param pageNumber number of the page
     * @param itemsPerPage number of items per page
     */
    public void listDriverSequences(final NetworkResponseDataListener<TrackCollection> listener, int pageNumber, int itemsPerPage) {
        ListSequencesRequest seqRequest = new ListSequencesRequest(URL_DRIVER_UPLOADED_TRACKS,
                new OsvRequestResponseListener<DriverTracksParser, TrackCollection>(
                        mDriverTracksParser) {

                    @Override
                    public void onSuccess(final int status,
                                          final TrackCollection collectionData) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                Log.d(TAG, "listDriverSequences: failed");
                                listener.requestFinished(status, collectionData);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status,
                                          final TrackCollection collectionData) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                Log.d(TAG, "listDriverSequences: successful");
                                listener.requestFailed(status, collectionData);
                            }
                        });
                    }
                }, getAccessToken(), pageNumber, itemsPerPage);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(3500, 5, 1f));
        seqRequest.setShouldCache(false);
        cancelListTasks();
        mQueue.add(seqRequest);
    }

    /**
     * lists a certain amount of sequences from the server from the given page
     * @param listener request listener
     * @param pageNumber number of the page
     * @param itemsPerPage number of items per page
     */
    public void listDriverPayments(final NetworkResponseDataListener<PaymentCollection> listener, int pageNumber, int itemsPerPage) {
        ListPaymentsRequest seqRequest = new ListPaymentsRequest(URL_DRIVER_PAYMENTS,
                new OsvRequestResponseListener<PaymentCollectionParser, PaymentCollection>(
                        mPaymentCollectionParser) {

                    @Override
                    public void onSuccess(final int status,
                                          final PaymentCollection paymentCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, paymentCollection);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status,
                                          final PaymentCollection paymentCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, paymentCollection);
                            }
                        });
                    }
                }, getAccessToken(), pageNumber, itemsPerPage);
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(3500, 5, 1f));
        seqRequest.setShouldCache(false);
        cancelListTasks();
        mQueue.add(seqRequest);
    }

    /**
     * deletes a sequence from the server, together with the images that it contains
     * @param sequenceId online id of the sequence
     * @param listener request listener
     */
    public void deleteSequence(final int sequenceId, final NetworkResponseDataListener<ApiResponse> listener) {
        DeleteSequenceRequest seqRequest = new DeleteSequenceRequest(URL_DELETE_SEQUENCE,
                new OsvRequestResponseListener<HttpResponseParser, ApiResponse>(
                        mHttpResponseParser) {

                    @Override
                    public void onSuccess(final int status, final ApiResponse apiResponse) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, apiResponse);
                                Log.d(TAG, "deleteSequenceRecord: " + apiResponse);
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status, final ApiResponse apiResponse) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, apiResponse);
                                Log.d(TAG, "deleteSequenceRecord: " + apiResponse);
                            }
                        });
                    }
                }, sequenceId, getAccessToken());
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 5, 1f));
        mQueue.add(seqRequest);
    }

    public void getUserProfileDetails(final NetworkResponseDataListener<UserData> listener) {
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("")) {
            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                    mUserDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
            return;
        }
        ProfileRequest profileRequest =
                new ProfileRequest(URL_PROFILE_DETAILS, new OsvRequestResponseListener<UserDataParser, UserData>(mUserDataParser) {

                    @Override
                    public void onSuccess(final int status, final UserData userData) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                appPrefs.saveIntPreference(PreferenceTypes.K_USER_TYPE, userData.getUserType());

                                SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
                                SharedPreferences.Editor prefsEditor = prefs.edit();
                                prefsEditor.putInt(ProfileFragment.K_RANK, userData.getOverallRank());
                                prefsEditor.putInt(ProfileFragment.K_SCORE, userData.getTotalPoints());
                                prefsEditor.putInt(ProfileFragment.K_LEVEL, userData.getLevel());
                                prefsEditor.putInt(ProfileFragment.K_XP_PROGRESS, userData.getLevelProgress());
                                prefsEditor.putInt(ProfileFragment.K_XP_TARGET, userData.getLevelTarget());
                                prefsEditor.putString(ProfileFragment.K_TOTAL_PHOTOS, Utils.formatNumber(userData.getTotalPhotos()));
                                prefsEditor.putString(ProfileFragment.K_TOTAL_TRACKS, Utils.formatNumber(userData.getTotalTracks()));
                                prefsEditor.putString(ProfileFragment.K_TOTAL_DISTANCE, "" + userData.getTotalDistance());
                                prefsEditor.putString(ProfileFragment.K_OBD_DISTANCE, "" + userData.getObdDistance());
                                prefsEditor.apply();
                                listener.requestFinished(status, userData);
                                Log.d(TAG, "getUserProfileDetails: success");
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status, final UserData userData) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, userData);
                                Log.d(TAG, "getUserProfileDetails: failed");
                            }
                        });
                    }
                }, userName);
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    public void getDriverProfileDetails(final NetworkResponseDataListener<DriverData> listener) {
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("")) {
            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                    mDriverDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
            return;
        }
        DriverProfileRequest profileRequest = new DriverProfileRequest(URL_DRIVER_PROFILE_DETAILS,
                new OsvRequestResponseListener<DriverDataParser, DriverData>(mDriverDataParser) {
                    @Override
                    public void onSuccess(final int status, final DriverData driverData) {
                        runInBackground(() -> {
                            SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME,
                                    Context.MODE_PRIVATE);
                            SharedPreferences.Editor prefsEditor = prefs.edit();
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_CURRENT_ACCEPTED_DISTANCE,
                                    (float) driverData.getCurrentAcceptedDistance());
                            prefsEditor.putString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION,
                                    driverData.getPaymentModelVersion());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_CURRENT_PAYRATE,
                                    (float) driverData.getCurrentPayRate());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_CURRENT_VALUE,
                                    (float) driverData.getCurrentPaymentValue());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_TOTAL_ACCEPTED_DISTANCE,
                                    (float) driverData.getTotalAcceptedDistance());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_TOTAL_REJECTED_DISTANCE,
                                    (float) driverData.getTotalRejectedDistance());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_TOTAL_OBD_DISTANCE,
                                    (float) driverData.getTotalObdDistance());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_TRACKS_COUNT,
                                    (float) driverData.getTotalTracks());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_PHOTOS_COUNT,
                                    (float) driverData.getTotalPhotos());
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_TOTAL_VALUE,
                                    (float) driverData.getTotalPaidValue());
                            prefsEditor.putString(ProfileFragment.K_DRIVER_CURRENCY,
                                    driverData.getCurrency());
                            prefsEditor.apply();
                            listener.requestFinished(status, driverData);
                            Log.d(TAG, "getDriverProfileDetails: success");
                        });
                    }

                    @Override
                    public void onFailure(final int status, final DriverData driverData) {
                        runInBackground(() -> {
                            listener.requestFailed(status, driverData);
                            Log.d(TAG, "getDriverProfdriverDataileDetails: success");
                        });
                    }
                }, token);
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    public void getDriverPayRateDetails(final NetworkResponseDataListener<PayRateData> listener) {
        String userName = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
        String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);

        if (userName.equals("") || token.equals("")) {
            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                    mDriverPayRateDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
            return;
        }

        DriverPayRatesRequest driverPayRatesRequest = new DriverPayRatesRequest(URL_DRIVER_PAY_RATE_INFO,
                new OsvRequestResponseListener<PayRateDataParser, PayRateData>(mDriverPayRateDataParser) {

                    @Override
                    public void onSuccess(int status, PayRateData payRateData) {
                        runInBackground(() -> {

                            float maxPayRate = getMaxPayRate(payRateData);
                            SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME,
                                    Context.MODE_PRIVATE);
                            SharedPreferences.Editor prefsEditor = prefs.edit();
                            prefsEditor.putFloat(ProfileFragment.K_DRIVER_BYOD20_MAX_PAYRATE, maxPayRate);
                            prefsEditor.apply();
                            listener.requestFinished(status, payRateData);
                            Log.d(TAG, "DriverPayRatesRequest: success");
                        });
                    }

                    @Override
                    public void onFailure(int status, PayRateData payrateData) {
                        listener.requestFailed(status, payrateData);
                    }
                }, token);

        driverPayRatesRequest.setShouldCache(false);
        mQueue.add(driverPayRatesRequest);
    }

    public void getLeaderboardData(final NetworkResponseDataListener<UserCollection> listener, String date, String countryCode) {
        LeaderboardRequest leaderboardRequest =
                new LeaderboardRequest(URL_LEADERBOARD, new OsvRequestResponseListener<LeaderboardParser, UserCollection>(mLeaderboardParser) {

                    @Override
                    public void onSuccess(final int status, final UserCollection userCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, userCollection);
                                Log.d(TAG, "getLeaderboardData: success");
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status, final UserCollection userCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, userCollection);
                                Log.d(TAG, "getLeaderboardData: success");
                            }
                        });
                    }
                }, date, countryCode, null);
        mQueue.cancelAll(new LeaderboardRequestFilter());
        leaderboardRequest.setShouldCache(false);
        mQueue.add(leaderboardRequest);
    }

    /**
     * lists the details of the images in an online sequence
     * @param sequenceId online sequence id
     * @param listener request listener
     */
    public void listImages(final int sequenceId, final NetworkResponseDataListener<PhotoCollection> listener) {
        ListPhotosRequest seqRequest = new ListPhotosRequest(URL_LIST_PHOTOS,
                new OsvRequestResponseListener<PhotoCollectionParser, PhotoCollection>(
                        mPhotoCollectionParser) {

                    @Override
                    public void onSuccess(final int status, final PhotoCollection photoCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFinished(status, photoCollection);
                                Log.d(TAG, "listImages: success");
                            }
                        });
                    }

                    @Override
                    public void onFailure(final int status, final PhotoCollection photoCollection) {
                        runInBackground(new Runnable() {

                            @Override
                            public void run() {
                                listener.requestFailed(status, photoCollection);
                                Log.d(TAG, "listImages: success");
                            }
                        });
                    }
                }, sequenceId, getAccessToken());
        mQueue.add(seqRequest);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLoginChanged(LoginChangedEvent event) {
        mAccessToken = null;
    }

    private float getMaxPayRate(PayRateData payRateData) {
        ArrayList<PayRateItem> obdOrderedPayRates = new ArrayList<>(payRateData.getPayRates());
        ArrayList<PayRateItem> nonObdOrderedPayRates = new ArrayList<>(obdOrderedPayRates);

        Collections.sort(obdOrderedPayRates, (item1, item2) -> (int) (item1.obdPayRateValue - item2.obdPayRateValue));
        Collections.sort(nonObdOrderedPayRates, (item1, item2) -> (int) (item1.nonObdPayRateValue - item2.nonObdPayRateValue));

        return Math.max(obdOrderedPayRates.get(0).obdPayRateValue, nonObdOrderedPayRates.get(0).nonObdPayRateValue);
    }

    /**
     * cancells all upload request added
     */
    private void cancelListTasks() {
        Log.d(TAG, "cancelListTasks: cancelled map list tasks and listSegments");
        ListRequestFilter listFilter = new ListRequestFilter();
        mQueue.cancelAll(listFilter);
    }
}
