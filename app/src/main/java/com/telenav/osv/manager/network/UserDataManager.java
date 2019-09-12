package com.telenav.osv.manager.network;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import android.content.Context;
import android.content.SharedPreferences;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.telenav.osv.R;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.driver.DriverDetails;
import com.telenav.osv.data.user.model.details.driver.DriverPayment;
import com.telenav.osv.data.user.model.details.driver.DriverToBePaid;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationLevel;
import com.telenav.osv.data.user.model.details.gamification.GamificationRank;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
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
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsvRequestResponseListener;
import com.telenav.osv.manager.network.parser.DriverDataParser;
import com.telenav.osv.manager.network.parser.DriverSequenceParser;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.LeaderboardParser;
import com.telenav.osv.manager.network.parser.PaymentCollectionParser;
import com.telenav.osv.manager.network.parser.PhotoCollectionParser;
import com.telenav.osv.manager.network.parser.SequenceParser;
import com.telenav.osv.manager.network.parser.UserDataParser;
import com.telenav.osv.network.FactoryServerEndpointUrl;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import androidx.annotation.NonNull;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

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

    private static final String TAG = "UserDataManager";

    /**
     * download photo file reques url
     */
    public static String URL_DOWNLOAD_PHOTO = FactoryServerEndpointUrl.SERVER_PLACEHOLDER;

    /**
     * delete sequence request url
     */
    private static String URL_DELETE_SEQUENCE = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/sequence/remove/";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_TRACKS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/tracks/";

    /**
     * list ALL sequences request url
     */
    private static String URL_VERSION = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "version";

    /**
     * list ALL sequences request url
     */
    private static String URL_LIST_MY_SEQUENCES = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/list/my-list/";

    /**
     * list photos from a specific sequence url
     */
    private static String URL_LIST_PHOTOS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/sequence/photo-list/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_PROFILE_DETAILS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/user/details/";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PAY_RATE_INFO = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/user/byod/payrates";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PROFILE_DETAILS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/user/byod/details";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_PAYMENTS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/user/byod/payments";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_DRIVER_UPLOADED_TRACKS = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "1.0/user/byod/uploaded-tracks";

    /**
     * list details of my profile from a specific url
     */
    private static String URL_LEADERBOARD = FactoryServerEndpointUrl.SERVER_PLACEHOLDER + "gm-leaderboard";

    private UserDataParser mUserDataParser = new UserDataParser();

    private DriverDataParser mDriverDataParser = new DriverDataParser();

    private SequenceParser mUserTrackParser = new SequenceParser();

    private DriverSequenceParser mDriverSequenceParser = new DriverSequenceParser();

    private LeaderboardParser mLeaderboardParser = new LeaderboardParser();

    private PhotoCollectionParser mPhotoCollectionParser = new PhotoCollectionParser();

    private PaymentCollectionParser mPaymentCollectionParser = new PaymentCollectionParser();

    private HttpResponseParser mHttpResponseParser = new HttpResponseParser();

    /**
     * Container for Rx disposables which will automatically dispose them after execute.
     */
    @NonNull
    private CompositeDisposable compositeDisposable;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    public UserDataManager(Context context, UserDataSource userRepository) {
        super(context);
        this.userRepository = userRepository;
        compositeDisposable = new CompositeDisposable();
        setEnvironment();
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
    }

    @Override
    protected void setEnvironment() {
        String serverEndpointUrl = factoryServerEndpointUrl.getServerEndpoint();
        URL_DELETE_SEQUENCE = URL_DELETE_SEQUENCE.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_LIST_MY_SEQUENCES = URL_LIST_MY_SEQUENCES.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_LIST_PHOTOS = URL_LIST_PHOTOS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_DOWNLOAD_PHOTO = URL_DOWNLOAD_PHOTO.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_LIST_TRACKS = URL_LIST_TRACKS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_PROFILE_DETAILS = URL_PROFILE_DETAILS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_DRIVER_PROFILE_DETAILS = URL_DRIVER_PROFILE_DETAILS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_DRIVER_PAY_RATE_INFO = URL_DRIVER_PAY_RATE_INFO.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_DRIVER_PAYMENTS = URL_DRIVER_PAYMENTS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_DRIVER_UPLOADED_TRACKS = URL_DRIVER_UPLOADED_TRACKS.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_LEADERBOARD = URL_LEADERBOARD.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        URL_VERSION = URL_VERSION.replace(FactoryServerEndpointUrl.SERVER_PLACEHOLDER, serverEndpointUrl);
        Log.d(TAG, String.format("setEnvironment. Status: set urls. Server endpoint: %s.", serverEndpointUrl));
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
                new OsvRequestResponseListener<SequenceParser, TrackCollection>(
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
                new OsvRequestResponseListener<DriverSequenceParser, TrackCollection>(
                        mDriverSequenceParser) {

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
    public void deleteSequence(final long sequenceId, final NetworkResponseDataListener<ApiResponse> listener) {
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
        compositeDisposable.clear();
        Disposable disposable = userRepository
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                        //onSuccess
                        user -> {
                            Log.d(TAG, "getUserProfileDetails. Status: success. Message: User found. Requesting profile details");
                            requestUserProfileDetails(user, listener);
                        },
                        //onError
                        throwable -> Log.d(TAG, String.format("getUserProfileDetails. Status: error. Message: %s", throwable.getMessage())),
                        //onComplete
                        () -> {
                            Log.d(TAG, "getUserProfileDetails. Status: complete. Message: User not found.");
                            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                                    mUserDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
                        }
                );
        compositeDisposable.add(disposable);
    }

    public void getDriverProfileDetails(final NetworkResponseDataListener<DriverData> listener) {
        compositeDisposable.clear();
        Disposable disposable = userRepository
                .getUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        user -> {
                            Log.d(TAG, "getDriverProfileDetails. Status: success. Message: User found. Requesting profile details");
                            requestDriverProfileDetails(user, listener);
                        },
                        //onError
                        throwable -> Log.d(TAG, String.format("getDriverProfileDetails. Status: error. Message: %s", throwable.getMessage())),
                        //onComplete
                        () -> {
                            Log.d(TAG, "getDriverProfileDetails. Status: complete. Message: User not found.");
                            listener.requestFinished(NetworkResponseDataListener.HTTP_UNAUTHORIZED,
                                    mDriverDataParser.parse(new VolleyError(mContext.getString(R.string.not_logged_in))));
                        }
                );
        compositeDisposable.add(disposable);
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
    public void listImages(final long sequenceId, final NetworkResponseDataListener<PhotoCollection> listener) {
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

    /**
     * Requests the driver details for the current signed in user. On success this will also persist those changed in the cache.
     * @param user the user for which the details will be persisted on success of the request.
     * @param listener the listener which will be notified with the request status.
     */
    private void requestDriverProfileDetails(User user, NetworkResponseDataListener<DriverData> listener) {
        DriverProfileRequest profileRequest = new DriverProfileRequest(URL_DRIVER_PROFILE_DETAILS,
                new OsvRequestResponseListener<DriverDataParser, DriverData>(mDriverDataParser) {
                    @Override
                    public void onSuccess(final int status, final DriverData driverData) {
                        runInBackground(() -> {
                            savePaymenyModelInPrefs(driverData.getPaymentModelVersion());
                            userRepository.updateCacheUserDetails(
                                    new DriverDetails(
                                            (int) driverData.getTotalPhotos(),
                                            (int) driverData.getTotalTracks(),
                                            driverData.getTotalObdDistance(),
                                            driverData.getTotalAcceptedDistance(),
                                            new DriverPayment(
                                                    driverData.getCurrency(),
                                                    driverData.getTotalPaidValue(),
                                                    new DriverToBePaid(
                                                            driverData.getCurrentAcceptedDistance(),
                                                            driverData.getCurrentPayRate(),
                                                            driverData.getCurrentPaymentValue()
                                                    ),
                                                    driverData.getPaymentModelVersion()
                                            ),
                                            driverData.getTotalRejectedDistance()
                                    )
                            );
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
                }, user.getAccessToken());
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    /**
     * Requests the gamification details for the current signed in user. On success this will also persist those changed in the cache.
     * @param user the user for which the details will be persisted on success of the request.
     * @param listener the listener which will be notified with the request status.
     */
    private void requestUserProfileDetails(User user, final NetworkResponseDataListener<UserData> listener) {
        ProfileRequest profileRequest =
                new ProfileRequest(URL_PROFILE_DETAILS, new OsvRequestResponseListener<UserDataParser, UserData>(mUserDataParser) {

                    @Override
                    public void onSuccess(final int status, final UserData userData) {
                        runInBackground(() -> {
                            userRepository.updateCacheUserDetails(new GamificationDetails(
                                    (int) userData.getTotalPhotos(),
                                    (int) userData.getTotalTracks(),
                                    userData.getObdDistance(),
                                    userData.getTotalDistance(),
                                    userData.getTotalPoints(),
                                    new GamificationLevel(
                                            userData.getLevel(),
                                            userData.getLevelTarget(),
                                            userData.getLevelProgress(),
                                            userData.getLevelName()
                                    ),
                                    new GamificationRank(
                                            userData.getWeeklyRank(),
                                            userData.getOverallRank()
                                    )));
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
                }, user.getUserName());
        profileRequest.setShouldCache(false);
        mQueue.add(profileRequest);
    }

    /**
     * ToDo: Remove this once Byod 2.0 logic is stripped of Shared Prefs
     * @param paymentModel the payment model
     */
    private void savePaymenyModelInPrefs(String paymentModel) {
        SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putString(ProfileFragment.K_DRIVER_PAYMENT_MODEL_VERSION, paymentModel);
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
