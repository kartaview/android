package com.telenav.osv.manager.network;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.telenav.osv.R;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.model.details.gamification.GamificationDetails;
import com.telenav.osv.data.user.model.details.gamification.GamificationLevel;
import com.telenav.osv.data.user.model.details.gamification.GamificationRank;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.http.DeleteSequenceRequest;
import com.telenav.osv.http.LeaderboardRequest;
import com.telenav.osv.http.ListPhotosRequest;
import com.telenav.osv.http.ListSequencesRequest;
import com.telenav.osv.http.ProfileRequest;
import com.telenav.osv.http.requestFilters.LeaderboardRequestFilter;
import com.telenav.osv.http.requestFilters.ListRequestFilter;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.PhotoCollection;
import com.telenav.osv.item.network.TrackCollection;
import com.telenav.osv.item.network.UserCollection;
import com.telenav.osv.item.network.UserData;
import com.telenav.osv.jarvis.login.utils.LoginUtils;
import com.telenav.osv.listener.network.KVRequestResponseListener;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.manager.network.parser.HttpResponseParser;
import com.telenav.osv.manager.network.parser.LeaderboardParser;
import com.telenav.osv.manager.network.parser.PhotoCollectionParser;
import com.telenav.osv.manager.network.parser.SequenceParser;
import com.telenav.osv.manager.network.parser.UserDataParser;
import com.telenav.osv.network.endpoint.UrlProfile;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    private UserDataParser mUserDataParser = new UserDataParser();

    private SequenceParser mUserTrackParser = new SequenceParser(factoryServerEndpointUrl);

    private LeaderboardParser mLeaderboardParser = new LeaderboardParser();

    private PhotoCollectionParser mPhotoCollectionParser = new PhotoCollectionParser(factoryServerEndpointUrl);

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
        EventBus.register(this);
        VolleyLog.DEBUG = Utils.isDebugEnabled(mContext);
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
        ListSequencesRequest seqRequest = new ListSequencesRequest(factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.LIST_MY_SEQUENCES),
                new KVRequestResponseListener<SequenceParser, TrackCollection>(
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
                }, getAccessToken(), pageNumber, itemsPerPage, LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
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
        DeleteSequenceRequest seqRequest = new DeleteSequenceRequest(factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.DELETE_SEQUENCE),
                new KVRequestResponseListener<HttpResponseParser, ApiResponse>(
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
                }, sequenceId, getAccessToken(), LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
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

    public void getLeaderboardData(final NetworkResponseDataListener<UserCollection> listener, String date, String countryCode) {
        LeaderboardRequest leaderboardRequest =
                new LeaderboardRequest(factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.LEADERBOARD), new KVRequestResponseListener<LeaderboardParser, UserCollection>(mLeaderboardParser) {

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
                }, date, countryCode, null, LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
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
        ListPhotosRequest seqRequest = new ListPhotosRequest(factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.LIST_PHOTOS),
                new KVRequestResponseListener<PhotoCollectionParser, PhotoCollection>(
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
                }, sequenceId, getAccessToken(), LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        mQueue.add(seqRequest);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLoginChanged(LoginChangedEvent event) {
        mAccessToken = null;
    }

    /**
     * Requests the gamification details for the current signed in user. On success this will also persist those changed in the cache.
     * @param user the user for which the details will be persisted on success of the request.
     * @param listener the listener which will be notified with the request status.
     */
    private void requestUserProfileDetails(User user, final NetworkResponseDataListener<UserData> listener) {
        ProfileRequest profileRequest =
                new ProfileRequest(factoryServerEndpointUrl.getProfileEndpoint(UrlProfile.PROFILE_DETAILS), new KVRequestResponseListener<UserDataParser, UserData>(mUserDataParser) {

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
                }, user.getUserName(), LoginUtils.isLoginTypePartner(appPrefs), appPrefs.getStringPreference(PreferenceTypes.JARVIS_ACCESS_TOKEN));
        profileRequest.setShouldCache(false);
        profileRequest.setRetryPolicy(new DefaultRetryPolicy(150000, 0, 0f));
        mQueue.add(profileRequest);
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
