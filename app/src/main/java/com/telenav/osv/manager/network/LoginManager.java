package com.telenav.osv.manager.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.LoginEvent;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.OSVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.hardware.ContactsPermissionEvent;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.http.RequestResponseListener;
import com.telenav.osv.listener.OAuthResultListener;
import com.telenav.osv.ui.fragment.OAuthDialogFragment;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.Utils;
import io.fabric.sdk.android.Fabric;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

/**
 * component responsible for logging in using several accounts
 * Created by Kalman on 28/02/2017.
 */
public class LoginManager implements GoogleApiClient.OnConnectionFailedListener {

    public final static String TAG = "LoginManager";


    public static final String LOGIN_TYPE_OSM = "osm";

    public static final String LOGIN_TYPE_GOOGLE = "google";

    public static final String LOGIN_TYPE_FACEBOOK = "facebook";

    private static final int REQUEST_CODE_LOGIN_GOOGLE = 10001;

    /**
     * request url for login details of OSM user
     */
    private static final String URL_USER_DETAILS = "http://api.openstreetmap.org/api/0.6/user/details";

    /**
     * consumer key used for oauth 1.0a
     */
    private static final String CONSUMER_KEY = "rBWV8Eaottv44tXfdLofdNvVemHOL62Lsutpb9tw";

    /**
     * consumer secret key used for oauth 1.0a
     */
    private static final String CONSUMER_SECRET_KEY = "rpmeZIp49sEjjcz91X9dsY0vD1PpEduixuPy8T6S";

    private final OSVApplication mContext;

    private GoogleApiClient mGoogleApiClient;

    private CallbackManager mFacebookCallbackManager;

    private ApplicationPreferences appPrefs;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private UploadManager mUploadManager;

    private FirebaseAuth mAuth;

    private Handler mBackgroundHandler;

    public LoginManager(final OSVApplication context) {
        this.mContext = context;
        appPrefs = context.getAppPrefs();
        mUploadManager = context.getUploadManager();
        HandlerThread thread = new HandlerThread("LoginThread", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mBackgroundHandler = new Handler(thread.getLooper());
        try {
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e){
            Log.d(TAG, "LoginManager: " + Log.getStackTraceString(e));
            mAuth = null;
        }
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .requestIdToken(context.getString(R.string.google_client_id))
                    .requestScopes(new Scope(Scopes.PROFILE), new Scope("https://www.googleapis.com/auth/contacts.readonly"))
                    .build();
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();
            mGoogleApiClient.registerConnectionFailedListener(this);
            mGoogleApiClient.connect();
        } catch (Exception e){
            Log.d(TAG, "LoginManager: " + Log.getStackTraceString(e));
            mGoogleApiClient = null;
        }
        if (mAuth != null) {
            FirebaseAuth.AuthStateListener mAuthListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null) {//user signed in
                        String imgLink = null;
                        Uri url = user.getPhotoUrl();
                        if (url != null) {
                            imgLink = url.toString();
                        }
                        final String finalImgLink = imgLink;
                        appPrefs.saveStringPreference(PreferenceTypes.K_USER_PHOTO_URL, finalImgLink);
                        final String name = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
                        final String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);
                        final String displayName = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (name.equals("") || token.equals("")) {
                                    EventBus.postSticky(new LoginChangedEvent(false, "", "", "", false));
                                } else {
                                    EventBus.postSticky(new LoginChangedEvent(true, name, displayName, finalImgLink, false));
                                    cacheProfileDetails();
                                }
                            }
                        });

                    } else {//user signed out
                        Log.d(TAG, "onAuthStateChanged:signed_out");
                    }
                }
            };
            mAuth.addAuthStateListener(mAuthListener);
        }

        try {
            mFacebookCallbackManager = CallbackManager.Factory.create();

            com.facebook.login.LoginManager.getInstance().registerCallback(mFacebookCallbackManager,
                    new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            Log.d(TAG, "onSuccess: ");
                            handleFacebookAccessToken(loginResult.getAccessToken());
                        }

                        @Override
                        public void onCancel() {
                            Log.d(TAG, "onCancel: ");
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            Log.d(TAG, "onError: " + Log.getStackTraceString(exception));
                        }
                    });
        } catch (Exception e){
            Log.d(TAG, "LoginManager: " + Log.getStackTraceString(e));
            mFacebookCallbackManager = null;
        }
        EventBus.register(this);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LoginManager.REQUEST_CODE_LOGIN_GOOGLE) {
            if (resultCode == Activity.RESULT_OK) {
                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    // Google Sign In was successful, authenticate with Firebase
                    handleGoogleLoginResult(result);
                } else {
                    EventBus.postSticky(new LogoutCommand());
                }
            }
        } else {
            if (mFacebookCallbackManager != null) {
                mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: error connecting google api: " + connectionResult.getErrorMessage());
    }

    public void login(OSVActivity activity, String type) {
        switch (type) {
            case LOGIN_TYPE_FACEBOOK:
                if (mFacebookCallbackManager != null && mAuth != null) {
                    loginFacebook(activity);
                    break;
                }
            case LOGIN_TYPE_GOOGLE:
                if (mGoogleApiClient != null && mAuth != null) {
                    int cameraPermitted = ContextCompat.checkSelfPermission(activity, Manifest.permission.GET_ACCOUNTS);
                    if (cameraPermitted == PackageManager.PERMISSION_DENIED) {
                        EventBus.postSticky(new ContactsPermissionEvent(LOGIN_TYPE_FACEBOOK));
                    } else {
                        loginGoogle(activity);
                    }
                    break;
                }
            case LOGIN_TYPE_OSM:
                showOSMLoginScreen(activity);
                break;
        }
    }

    private void showOSMLoginScreen(final OSVActivity activity) {
        if (Utils.isInternetAvailable(activity)) {
            activity.enableProgressBar(true);
            showOSMLoginDialog(activity, new RequestResponseListener() {
                @Override
                public void requestFinished(int status, String xml) {
                    String imgLink = "";
                    try {
                        InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
                        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                        DocumentBuilder db = dbf.newDocumentBuilder();
                        Document doc = db.parse(is);
                        doc.getDocumentElement().normalize();

                        NodeList nodeList = doc.getElementsByTagName("user");
                        for (int i = 0; i < nodeList.getLength(); i++) {
                            Node node = nodeList.item(i);
                            Element fstElmnt = (Element) node;
                            NodeList list = fstElmnt.getElementsByTagName("img");

                            for (int j = 0; j < list.getLength(); j++) {
                                Node node2 = list.item(j);
                                imgLink = ((Element) node2).getAttribute("href");
                                Log.d(TAG, "requestFinished: profile picture" + imgLink);
                                if (imgLink.length() > 0) {
                                    appPrefs.saveStringPreference(PreferenceTypes.K_USER_PHOTO_URL, imgLink);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        activity.enableProgressBar(false);
                    }

                    final String finalName = appPrefs.getStringPreference(PreferenceTypes.K_DISPLAY_NAME);
                    final String token = appPrefs.getStringPreference(PreferenceTypes.K_ACCESS_TOKEN);
                    final String username = appPrefs.getStringPreference(PreferenceTypes.K_USER_NAME);
                    final String finalUrl = imgLink;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (username.equals("") || token.equals("")) {
                                EventBus.postSticky(new LoginChangedEvent(false, "", "", "", false));
                            } else {
                                EventBus.postSticky(new LoginChangedEvent(true, username, finalName, finalUrl, false));
                                cacheProfileDetails();
                            }
                            activity.enableProgressBar(false);
                            activity.finish();
                        }
                    });
                }


                @Override
                public void requestFinished(final int status) {
                    activity.enableProgressBar(false);
                }
            }, new OAuthDialogFragment.OnDetachListener() {
                @Override
                public void onDetach() {
                    activity.enableProgressBar(false);
                }
            });
        } else {
            activity.showSnackBar(R.string.no_internet_connection_label, Snackbar.LENGTH_SHORT);
        }
    }

    private void loginFacebook(OSVActivity activity) {
        if (mAuth.getCurrentUser() != null) {
            logoutFirebase();
        }
        com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("public_profile", "email"));
    }

    private void loginGoogle(OSVActivity activity) {
        if (mAuth.getCurrentUser() != null) {
            logoutFirebase();
        }
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, REQUEST_CODE_LOGIN_GOOGLE);
    }

    /**
     * displays a webview with the osm login site.
     * @param activity the ref for the support fragmenbt manager
     * @param listener response listener
     * @param onDetachListener on detach listener for the webview
     */
    private void showOSMLoginDialog(final FragmentActivity activity, final RequestResponseListener listener, final OAuthDialogFragment.OnDetachListener onDetachListener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final CommonsHttpOAuthConsumer consumer;
                    final CommonsHttpOAuthProvider provider;
                    consumer = new CommonsHttpOAuthConsumer(CONSUMER_KEY, CONSUMER_SECRET_KEY);

                    provider = new CommonsHttpOAuthProvider(
                            "https://www.openstreetmap.org/oauth/request_token",
                            "https://www.openstreetmap.org/oauth/access_token",
                            "https://www.openstreetmap.org/oauth/authorize");
                    provider.setOAuth10a(true);
                    String authUrl = provider.retrieveRequestToken(consumer, "osmlogin://telenav?");
                    OAuthDialogFragment newFragment = new OAuthDialogFragment();
                    newFragment.setOnDetachListener(onDetachListener);
                    newFragment.setURL(authUrl);
                    newFragment.setResultListener(new OAuthResultListener() {
                        @Override
                        public void onResult(final String url) {
                            new Thread(new Runnable() {
                                @SuppressWarnings("deprecation")
                                @Override
                                public void run() {
                                    try {
                                        Uri uri = Uri.parse(url);
                                        String oauthVerifier = uri.getQueryParameter("oauth_verifier");
                                        provider.retrieveAccessToken(consumer, oauthVerifier);
                                        String requestToken = consumer.getToken();
                                        String secretToken = consumer.getTokenSecret();
                                        HttpGet request = new HttpGet(URL_USER_DETAILS);
                                        consumer.sign(request);
                                        HttpClient httpclient = new DefaultHttpClient();
                                        HttpResponse response;
                                        final String responseString;
                                        response = httpclient.execute(request);
                                        StatusLine statusLine = response.getStatusLine();
                                        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                                            ByteArrayOutputStream out = new ByteArrayOutputStream();
                                            response.getEntity().writeTo(out);
                                            responseString = out.toString();
                                            out.close();
                                        } else {
                                            //Closes the connection.
                                            response.getEntity().getContent().close();
                                            throw new IOException(statusLine.getReasonPhrase());
                                        }
                                        authenticateUsing(UploadManager.URL_AUTH_OSM, requestToken, secretToken, responseString, listener);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        listener.requestFinished(RequestResponseListener.STATUS_FAILED);
                                    }
                                }
                            }).start();
                        }
                    });
                    FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
                    ft.addToBackStack("OAuthDialogFragment.TAG");
                    newFragment.show(ft, OAuthDialogFragment.TAG);
                } catch (Exception e) {
                    Log.e(TAG, "showOSMLoginDialog: logging in failed " + e.toString());
                    listener.requestFinished(RequestResponseListener.STATUS_FAILED);
                }
            }
        }).start();
    }

    private void authenticateUsing(final String url, String requestToken, String secretToken, final String resultText, final RequestResponseListener listener) {
        Log.d(TAG, "authenticateUsing: " + requestToken);
        mUploadManager.authenticate(url, requestToken, secretToken, new RequestResponseListener() {
            @Override
            public void requestFinished(int status) {
                listener.requestFinished(RequestResponseListener.STATUS_FAILED);
            }

            @Override
            public void requestFinished(int status, String result) {
                JSONObject obj;
                String accessToken = null;
                String name;
                String username;
                String type;
                String id;
                try {
                    obj = new JSONObject(result);
                    JSONObject osv = obj.getJSONObject("osv");
                    accessToken = osv.getString("access_token");
                    appPrefs.saveStringPreference(PreferenceTypes.K_ACCESS_TOKEN, accessToken);
                    id = osv.getString("id");
                    appPrefs.saveStringPreference(PreferenceTypes.K_USER_ID, id);
                    username = osv.getString("username");
                    appPrefs.saveStringPreference(PreferenceTypes.K_USER_NAME, username);
                    name = osv.getString("full_name");
                    appPrefs.saveStringPreference(PreferenceTypes.K_DISPLAY_NAME, name);
                    type = osv.getString("type");
                    appPrefs.saveStringPreference(PreferenceTypes.K_USER_TYPE, type);
                    String loginType = "OSM";
                    if (url.contains("facebook")){
                        loginType = "FACEBOOK";
                    } else if (url.contains("google")){
                        loginType = "GOOGLE";
                    }
                    appPrefs.saveStringPreference(PreferenceTypes.K_LOGIN_TYPE, loginType);
                } catch (JSONException e) {
                    Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                }
                if (accessToken == null) {
                    listener.requestFinished(RequestResponseListener.STATUS_FAILED);
                } else {
                    listener.requestFinished(RequestResponseListener.STATUS_SUCCESS_LOGIN, resultText);
                }
            }
        });
    }

    private void handleGoogleLoginResult(final GoogleSignInResult result) {
        mBackgroundHandler.post(new Runnable() {
            @Override
            public void run() {

                try {
                    Log.d(TAG, "handleSignInResult: success = " + result.isSuccess());
                    GoogleSignInAccount acct = result.getSignInAccount();
                    if (acct == null) {
                        EventBus.postSticky(new LogoutCommand());
                        return;
                    }
                    final AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
                    GoogleAccountCredential credential2 =
                            GoogleAccountCredential.usingOAuth2(
                                    mContext,
                                    Collections.singleton(Scopes.PROFILE)
                            );
                    credential2.setSelectedAccount(acct.getAccount());
                    String token = null;
                    try {
                        token = credential2.getToken();
                    } catch (IOException | GoogleAuthException e) {
                        Log.d(TAG, "handleGoogleLoginResult: " + Log.getStackTraceString(e));

                    }
                    authenticateUsing(UploadManager.URL_AUTH_GOOGLE, token, null, "", new RequestResponseListener() {
                        @Override
                        public void requestFinished(int status, String result) {
                            mAuth.signInWithCredential(credential)
                                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {
                                            Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
                                            if (!task.isSuccessful()) {
                                                EventBus.postSticky(new LogoutCommand());
                                            }
                                        }
                                    });
                        }

                        @Override
                        public void requestFinished(int status) {
                            if (status == RequestResponseListener.STATUS_FAILED) {
                                EventBus.postSticky(new LogoutCommand());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.d(TAG, "handleGoogleLoginResult: " + Log.getStackTraceString(e));
                    EventBus.postSticky(new LogoutCommand());
                }
            }
        });
    }

    private void cacheProfileDetails() {
        mUploadManager.getProfileDetails(new RequestResponseListener() {
            @Override
            public void requestFinished(int status, String result) {
                Log.d(TAG, "getProfileDetails: " + " status - > " + status + " result - > " + result);
                if (result != null && !result.isEmpty() && status == RequestResponseListener.STATUS_SUCCESS_PROFILE_DETAILS) {
                    final String obdDistance, totalDistance, totalPhotos, totalTracks;
                    int rank = 0, score = 0, level = 0, xpProgress = 0, xpTarget = 0;
                    try {
                        JSONObject obj = new JSONObject(result);
                        JSONObject osv = obj.getJSONObject("osv");
                        String userType = osv.getString("type");
                        appPrefs.saveStringPreference(PreferenceTypes.K_USER_TYPE, userType);

                        obdDistance = osv.getString("obdDistance");
                        totalDistance = osv.getString("totalDistance");
                        totalPhotos = Utils.formatNumber(osv.getDouble("totalPhotos"));
                        totalTracks = Utils.formatNumber(osv.getDouble("totalTracks"));
                        try {
                            JSONObject gamification = osv.getJSONObject("gamification");

                            score = gamification.getInt("total_user_points");
                            level = gamification.getInt("level");
                            xpProgress = gamification.getInt("level_progress");
                            try {
                                xpTarget = gamification.getInt("level_target");
                            } catch (Exception e) {
                                Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                            }
                            rank = gamification.getInt("rank");
                        } catch (Exception e) {
                            Log.w(TAG, "requestFinished: " + Log.getStackTraceString(e));
                        }

                        SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor prefsEditor = prefs.edit();
                        prefsEditor.putInt(ProfileFragment.K_RANK, rank);
                        prefsEditor.putInt(ProfileFragment.K_SCORE, score);
                        prefsEditor.putInt(ProfileFragment.K_LEVEL, level);
                        prefsEditor.putInt(ProfileFragment.K_XP_PROGRESS, xpProgress);
                        prefsEditor.putInt(ProfileFragment.K_XP_TARGET, xpTarget);
                        prefsEditor.putString(ProfileFragment.K_TOTAL_PHOTOS, totalPhotos);
                        prefsEditor.putString(ProfileFragment.K_TOTAL_TRACKS, totalTracks);
                        prefsEditor.putString(ProfileFragment.K_TOTAL_DISTANCE, totalDistance);
                        prefsEditor.putString(ProfileFragment.K_OBD_DISTANCE, obdDistance);
                        prefsEditor.apply();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void requestFinished(int status) {

            }
        });
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authenticateUsing(UploadManager.URL_AUTH_FACEBOOK, token.getToken(), "", "", new RequestResponseListener() {
            @Override
            public void requestFinished(int status, String result) {
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                                if (!task.isSuccessful()) {
                                    EventBus.postSticky(new LogoutCommand());
                                }
                            }
                        });
            }

            @Override
            public void requestFinished(int status) {
                if (status == RequestResponseListener.STATUS_FAILED) {
                    EventBus.postSticky(new LogoutCommand());
                }
            }
        });
    }


    private void logoutFirebase() {
        if (mAuth.getCurrentUser() != null) {
            //signed in with facebook or google
            FirebaseAuth.getInstance().signOut();
            // user is now signed out
            signOutGoogle();
            signOutFacebook();
            EventBus.postSticky(new LoginChangedEvent(false, "", "", "", false));
        }
    }

    private void signOutGoogle() {
        try {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        } catch (Exception e) {
            Log.d(TAG, "signOutGoogle: " + Log.getStackTraceString(e));
        }
    }

    private void signOutFacebook() {
        try {
            com.facebook.login.LoginManager.getInstance().logOut();
        } catch (Exception e) {
            Log.d(TAG, "signOutFacebook: " + Log.getStackTraceString(e));
        }
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onLogoutCommand(LogoutCommand command) {
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_ID, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_ACCESS_TOKEN, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_LOGIN_TYPE, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_NAME, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_DISPLAY_NAME, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_PHOTO_URL, "");
        appPrefs.saveStringPreference(PreferenceTypes.K_USER_TYPE, "");
        EventBus.postSticky(new LoginChangedEvent(false, "", "", "", false));
        logoutFirebase();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLoginChanged(LoginChangedEvent event) {
        if (Fabric.isInitialized()){
            if (event.logged) {
                String method = appPrefs.getStringPreference(PreferenceTypes.K_LOGIN_TYPE);
                String type = appPrefs.getStringPreference(PreferenceTypes.K_USER_TYPE);
                Answers.getInstance().logLogin(new LoginEvent().putSuccess(true).putMethod(method).putCustomAttribute("userType",type));
                Crashlytics.setString(Log.USER_TYPE, type);
            } else {
                Crashlytics.setString(Log.USER_TYPE, "");
            }
        }
        if (!event.logged) {
            SharedPreferences prefs = mContext.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            if (!Utils.DEBUG || !appPrefs.getBooleanPreference(PreferenceTypes.K_DEBUG_SAVE_AUTH)) {
                final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mContext).edit();
                editor.clear();
                editor.apply();
                //noinspection deprecation
                CookieSyncManager.createInstance(mContext);
                //noinspection deprecation
                CookieManager.getInstance().removeAllCookie();
            }
        }
    }

}
