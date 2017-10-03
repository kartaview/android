package com.telenav.osv.manager.network;

import javax.inject.Inject;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.content.SharedPreferences;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.telenav.osv.R;
import com.telenav.osv.data.ProfilePreferences;
import com.telenav.osv.http.DeleteSequenceRequest;
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
import com.telenav.osv.item.network.PaymentCollection;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.item.view.profile.DriverProfileData;
import com.telenav.osv.item.view.profile.StatisticsData;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.parser.DriverDataParser;
import com.telenav.osv.manager.network.parser.DriverTracksParser;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.LeaderboardParser;
import com.telenav.osv.manager.network.parser.PaymentCollectionParser;
import com.telenav.osv.manager.network.parser.PhotoCollectionParser;
import com.telenav.osv.manager.network.parser.TrackCollectionParser;
import com.telenav.osv.manager.network.parser.UserDataParser;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import static com.telenav.osv.data.Preferences.URL_ENV;

/**
 * *
 * Created by Kalman on 10/6/2015.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class UserDataManager extends NetworkManager implements Response.ErrorListener {

    private static final String TAG = "UserDataManager";

    /**
     * download photo file reques url
     */
    public static String URL_DOWNLOAD_PHOTO = "http://" + "&&";

    /**
     * delete sequence request url
     */
    private static String URL_DELETE_SEQUENCE = "http://" + "&&" + URL_VER + "sequence/remove/";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_TRACKS = "http://" + "&&" + URL_VER + "tracks/";

    /**
     * list ALL sequences request url
     */
    private static String URL_VERSION = "http://" + "&&" + "version";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_MY_SEQUENCES = "http://" + "&&" + URL_VER + "list/my-list/";

    /**
     * list photos from a specific sequence url
     */
    private static String URL_LIST_PHOTOS = "http://" + "&&" + URL_VER + "sequence/photo-list/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_PROFILE_DETAILS = "http://" + "&&" + URL_VER + "user/details/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PROFILE_DETAILS = "http://" + "&&" + URL_VER + "user/byod/details";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PAYMENTS = "http://" + "&&" + URL_VER + "user/byod/payments";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_UPLOADED_TRACKS = "http://" + "&&" + URL_VER + "user/byod/uploaded-tracks";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_LEADERBOARD = "http://" + "&&" + "gm-leaderboard";

    private final MutableLiveData<DriverProfileData> mDriverProfileData;

    private final MutableLiveData<StatisticsData> mDriverStatsData;

    private final ProfilePreferences profilePrefs;

    private UserDataParser mUserDataParser = new UserDataParser();

    private DriverDataParser mDriverDataParser = new DriverDataParser();

    private TrackCollectionParser mUserTrackParser = new TrackCollectionParser();

    private DriverTracksParser mDriverTracksParser = new DriverTracksParser();

    private LeaderboardParser mLeaderboardParser = new LeaderboardParser();

    private PhotoCollectionParser mPhotoCollectionParser = new PhotoCollectionParser();

    private PaymentCollectionParser mPaymentCollectionParser = new PaymentCollectionParser();

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    @Inject
    public UserDataManager(Context context, ProfilePreferences prefs, MutableLiveData<DriverProfileData> driverData,
                           MutableLiveData<StatisticsData> statsData) {
        super(context, prefs);
        this.profilePrefs = prefs;
        this.mDriverProfileData = driverData;
        this.mDriverStatsData = statsData;
    }

    @Override
    protected void setupUrls() {
        URL_DELETE_SEQUENCE = URL_DELETE_SEQUENCE.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_MY_SEQUENCES = URL_LIST_MY_SEQUENCES.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_PHOTOS = URL_LIST_PHOTOS.replace("&&", URL_ENV[mCurrentServer]);
        URL_DOWNLOAD_PHOTO = URL_DOWNLOAD_PHOTO.replace("&&", URL_ENV[mCurrentServer]);
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_PROFILE_DETAILS = URL_PROFILE_DETAILS.replace("&&", URL_ENV[mCurrentServer]);
        URL_DRIVER_PROFILE_DETAILS = URL_DRIVER_PROFILE_DETAILS.replace("&&", URL_ENV[mCurrentServer]);
        URL_DRIVER_PAYMENTS = URL_DRIVER_PAYMENTS.replace("&&", URL_ENV[mCurrentServer]);
        URL_DRIVER_UPLOADED_TRACKS = URL_DRIVER_UPLOADED_TRACKS.replace("&&", URL_ENV[mCurrentServer]);
        URL_LEADERBOARD = URL_LEADERBOARD.replace("&&", URL_ENV[mCurrentServer]);
        URL_VERSION = URL_VERSION.replace("&&", URL_ENV[mCurrentServer]);
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
            Log.d(TAG, Log.getStackTraceString(e));
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
                        runInBackground(() -> {
                            listener.requestFinished(status, trackCollection);
                            Log.d(TAG, "listSequences: successful");
                        });
                    }

                    @Override
                    public void onFailure(final int status,
                                          final TrackCollection trackCollection) {
                        runInBackground(() -> {
                            listener.requestFailed(status, trackCollection);
                            Log.d(TAG, "listSequences: successful");
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
                        runInBackground(() -> {
                            Log.d(TAG, "listDriverSequences: failed");
                            listener.requestFinished(status, collectionData);
                        });
                    }

                    @Override
                    public void onFailure(final int status,
                                          final TrackCollection collectionData) {
                        runInBackground(() -> {
                            Log.d(TAG, "listDriverSequences: successful");
                            listener.requestFailed(status, collectionData);
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
                        runInBackground(() -> listener.requestFinished(status, paymentCollection));
                    }

                    @Override
                    public void onFailure(final int status,
                                          final PaymentCollection paymentCollection) {
                        runInBackground(() -> listener.requestFailed(status, paymentCollection));
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
                        runInBackground(() -> {
                            listener.requestFinished(status, apiResponse);
                            Log.d(TAG, "deleteSequenceRecord: " + apiResponse);
                        });
                    }

                    @Override
                    public void onFailure(final int status, final ApiResponse apiResponse) {
                        runInBackground(() -> {
                            listener.requestFailed(status, apiResponse);
                            Log.d(TAG, "deleteSequenceRecord: " + apiResponse);
                        });
                    }
                }, sequenceId, getAccessToken());
        seqRequest.setRetryPolicy(new DefaultRetryPolicy(2500, 5, 1f));
        mQueue.add(seqRequest);
    }

    public void getUserProfileDetails(final NetworkResponseDataListener<UserData> listener) {
        String userName = appPrefs.getUserName();
        if (!appPrefs.isLoggedIn()) {
            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                    mUserDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
            return;
        }
        ProfileRequest profileRequest =
                new ProfileRequest(URL_PROFILE_DETAILS, new OsvRequestResponseListener<UserDataParser, UserData>(mUserDataParser) {

                    @Override
                    public void onSuccess(final int status, final UserData userData) {
                        runInBackground(() -> {
                            appPrefs.setUserType(userData.getUserType());

                            SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor prefsEditor = prefs.edit();
                            prefsEditor.putInt(ProfileFragment.K_RANK, userData.getOverallRank());
                            prefsEditor.putInt(ProfileFragment.K_SCORE, userData.getTotalPoints());
                            prefsEditor.putInt(ProfileFragment.K_LEVEL, userData.getLevel());
                            prefsEditor.putInt(ProfileFragment.K_XP_PROGRESS, userData.getLevelProgress());
                            prefsEditor.putInt(ProfileFragment.K_XP_TARGET, userData.getLevelTarget());
                            prefsEditor.putInt(ProfileFragment.K_TOTAL_PHOTOS, (int) userData.getTotalPhotos());
                            prefsEditor.putInt(ProfileFragment.K_TOTAL_TRACKS, (int) userData.getTotalTracks());
                            prefsEditor.putFloat(ProfileFragment.K_TOTAL_DISTANCE, (float) userData.getTotalDistance());
                            prefsEditor.putFloat(ProfileFragment.K_OBD_DISTANCE, (float) userData.getTotalObdDistance());
                            prefsEditor.apply();
                            listener.requestFinished(status, userData);
                            Log.d(TAG, "getUserProfileDetails: success");
                        });
                    }

                    @Override
                    public void onFailure(final int status, final UserData userData) {
                        runInBackground(() -> {
                            listener.requestFailed(status, userData);
                            Log.d(TAG, "getUserProfileDetails: failed");
                        });
                    }
                }, userName);
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    public void getDriverProfileDetails(final NetworkResponseDataListener<DriverData> listener) {
        if (!appPrefs.isLoggedIn()) {
            if (listener != null) {
                listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                        mDriverDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
            }
            return;
        }
        DriverProfileRequest profileRequest =
                new DriverProfileRequest(
                        URL_DRIVER_PROFILE_DETAILS,
                        new OsvRequestResponseListener<DriverDataParser, DriverData>(
                                mDriverDataParser) {

                            @Override
                            public void onSuccess(final int status, final DriverData driverData) {
                                runInBackground(() -> {
                                    cacheToLegacyPrefs(driverData);//todo to be removed in new impl
                                    listener.requestFinished(status, driverData);

                                    //todo code below only used in new profile fragment impl.
                                    DriverProfileData driverProfileData = mDriverProfileData.getValue();
                                    if (driverProfileData == null) {
                                        driverProfileData = new DriverProfileData();
                                    }
                                    driverProfileData.setCurrency(driverData.getCurrency());
                                    driverProfileData.setCurrentAccepted(driverData.getCurrentAcceptedDistance());
                                    driverProfileData.setValue(driverData.getCurrentPaymentValue());
                                    driverProfileData.setRate(driverData.getCurrentPayRate());
                                    driverProfileData.setPaymentValue(driverData.getTotalPaidValue());
                                    driverProfileData.setName(appPrefs.getUserDisplayName());
                                    driverProfileData.setPhotoUrl(appPrefs.getUserPhotoUrl());
                                    driverProfileData.setUsername(appPrefs.getUserName());
                                    mDriverProfileData.postValue(driverProfileData);

                                    StatisticsData driverStatsData = mDriverStatsData.getValue();
                                    if (driverStatsData == null) {
                                        driverStatsData = new StatisticsData();
                                    }
                                    driverStatsData.setAcceptedDistance(driverData.getTotalAcceptedDistance());
                                    driverStatsData.setRejectedDistance(driverData.getTotalRejectedDistance());
                                    driverStatsData.setObdDistance(driverData.getTotalObdDistance());
                                    driverStatsData.setTotalPhotos((int) driverData.getTotalPhotos());
                                    driverStatsData.setTotalTracks((int) driverData.getTotalTracks());
                                    mDriverStatsData.postValue(driverStatsData);

                                    //cache to new preferences impl.
                                    profilePrefs.setDistance(driverStatsData.getDistance());
                                    profilePrefs.setAcceptedDistance(driverStatsData.getAcceptedDistance());
                                    profilePrefs.setRejectedDistance(driverStatsData.getRejectedDistance());
                                    profilePrefs.setObdDistance(driverStatsData.getObdDistance());
                                    profilePrefs.setPhotosCount(driverStatsData.getTotalPhotos());
                                    profilePrefs.setTracksCount(driverStatsData.getTotalTracks());
                                    Log.d(TAG, "getDriverProfileDetails: success");
                                });
                            }

                            @Override
                            public void onFailure(final int status, final DriverData driverData) {
                                runInBackground(() -> {
                                    listener.requestFailed(status, driverData);
                                    Log.d(TAG, "getDriverProfileDetails: success");
                                });
                            }
                        }, getAccessToken());
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    public void getLeaderboardData(final NetworkResponseDataListener<UserCollection> listener, String date, String countryCode) {
        LeaderboardRequest leaderboardRequest =
                new LeaderboardRequest(URL_LEADERBOARD, new OsvRequestResponseListener<LeaderboardParser, UserCollection>(mLeaderboardParser) {

                    @Override
                    public void onSuccess(final int status, final UserCollection userCollection) {
                        runInBackground(() -> {
                            listener.requestFinished(status, userCollection);
                            Log.d(TAG, "getLeaderboardData: success");
                        });
                    }

                    @Override
                    public void onFailure(final int status, final UserCollection userCollection) {
                        runInBackground(() -> {
                            listener.requestFailed(status, userCollection);
                            Log.d(TAG, "getLeaderboardData: success");
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
                        runInBackground(() -> {
                            listener.requestFinished(status, photoCollection);
                            Log.d(TAG, "listImages: success");
                        });
                    }

                    @Override
                    public void onFailure(final int status, final PhotoCollection photoCollection) {
                        runInBackground(() -> {
                            listener.requestFailed(status, photoCollection);
                            Log.d(TAG, "listImages: success");
                        });
                    }
                }, sequenceId, getAccessToken());
        mQueue.add(seqRequest);
    }

    private void cacheToLegacyPrefs(DriverData driverData) {
        SharedPreferences prefs = mContext
                .getSharedPreferences(ProfileFragment.PREFS_NAME,
                        Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor
                .putFloat(ProfileFragment.K_DRIVER_CURRENT_ACCEPTED_DISTANCE,
                        (float) driverData.getCurrentAcceptedDistance());
        prefsEditor.putFloat(ProfileFragment.K_DRIVER_CURRENT_PAYRATE,
                (float) driverData.getCurrentPayRate());
        prefsEditor.putFloat(ProfileFragment.K_DRIVER_CURRENT_VALUE,
                (float) driverData.getCurrentPaymentValue());
        prefsEditor
                .putFloat(ProfileFragment.K_DRIVER_TOTAL_ACCEPTED_DISTANCE,
                        (float) driverData.getTotalAcceptedDistance());
        prefsEditor
                .putFloat(ProfileFragment.K_DRIVER_TOTAL_REJECTED_DISTANCE,
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
