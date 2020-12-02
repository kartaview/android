package com.telenav.osv.manager.network;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.DefaultRetryPolicy;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.telenav.osv.R;
import com.telenav.osv.activity.OSVActivity;
import com.telenav.osv.application.ApplicationPreferences;
import com.telenav.osv.application.KVApplication;
import com.telenav.osv.application.PreferenceTypes;
import com.telenav.osv.command.LogoutCommand;
import com.telenav.osv.common.dialog.KVDialog;
import com.telenav.osv.data.user.datasource.UserDataSource;
import com.telenav.osv.data.user.model.User;
import com.telenav.osv.data.user.utils.UserUtils;
import com.telenav.osv.event.EventBus;
import com.telenav.osv.event.network.LoginChangedEvent;
import com.telenav.osv.event.ui.UserTypeChangedEvent;
import com.telenav.osv.http.AuthRequest;
import com.telenav.osv.item.AccountData;
import com.telenav.osv.item.network.ApiResponse;
import com.telenav.osv.item.network.AuthData;
import com.telenav.osv.item.network.OsmProfileData;
import com.telenav.osv.jarvis.login.network.JarvisLoginRequest;
import com.telenav.osv.jarvis.login.usecase.JarvisLoginUseCase;
import com.telenav.osv.listener.OAuthResultListener;
import com.telenav.osv.listener.network.KVRequestResponseListener;
import com.telenav.osv.listener.network.NetworkResponseDataListener;
import com.telenav.osv.listener.network.OsmAuthDataListener;
import com.telenav.osv.manager.network.parser.AuthDataParser;
import com.telenav.osv.network.endpoint.UrlLogin;
import com.telenav.osv.ui.fragment.OAuthDialogFragment;
import com.telenav.osv.ui.fragment.ProfileFragment;
import com.telenav.osv.utils.BackgroundThreadPool;
import com.telenav.osv.utils.Log;
import com.telenav.osv.utils.LogUtils;
import com.telenav.osv.utils.Utils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

/**
 * component responsible for logging in using several accounts
 * Created by Kalman on 28/02/2017.
 */
public class LoginManager extends NetworkManager implements GoogleApiClient.OnConnectionFailedListener {

    public static final String LOGIN_TYPE_OSM = "osm";

    public static final String LOGIN_TYPE_GOOGLE = "google";

    public static final String LOGIN_TYPE_FACEBOOK = "facebook";

    public static final String LOGIN_TYPE_PARTNER = "partner";

    private final static String TAG = "LoginManager";

    private static final int REQUEST_CODE_LOGIN_GOOGLE = 10001;

    private final KVApplication mContext;

    private final GoogleApiClient mGoogleApiClient;

    private final CallbackManager mFacebookCallbackManager;

    private final UserDataManager userDataManager;

    private final SharedPreferences profilePrefs;

    private ApplicationPreferences appPrefs;

    private FirebaseAuth mAuth;

    private AuthDataParser mAuthDataParser = new AuthDataParser();

    /**
     * Container for Rx disposables which will automatically dispose them after execute.
     */
    @NonNull
    private CompositeDisposable compositeDisposable;

    /**
     * Instance for {@code UserDataSource} which represents the user repository.
     */
    private UserDataSource userRepository;

    private JarvisLoginUseCase jarvisLoginUseCase;
    private String loginType = null;
    private KVDialog jarvisLoginFailureDialog = null;

    public LoginManager(final KVApplication context, UserDataSource userRepository, JarvisLoginUseCase jarvisLoginUseCase) {
        super(context);
        compositeDisposable = new CompositeDisposable();
        this.jarvisLoginUseCase = jarvisLoginUseCase;
        this.userRepository = userRepository;
        this.mContext = context;
        appPrefs = context.getAppPrefs();
        profilePrefs = context.getSharedPreferences(ProfileFragment.PREFS_NAME, Context.MODE_PRIVATE);
        FirebaseApp.initializeApp(context.getApplicationContext());
        mAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestProfile()
                .requestIdToken(context.getString(R.string.google_client_id))
                .requestScopes(new Scope(Scopes.PROFILE), new Scope("https://www.googleapis.com/auth/contacts.readonly")).build();
        mGoogleApiClient = new GoogleApiClient.Builder(context).addApi(Auth.GOOGLE_SIGN_IN_API, gso).build();
        mGoogleApiClient.registerConnectionFailedListener(this);
        mGoogleApiClient.connect();
        userDataManager = new UserDataManager(context, userRepository);

        mAuth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {//user signed in
                Log.d(TAG, "onAuthStateChanged: user is from " + user.getProviderId());
                String imgLink = null;
                Uri url = user.getPhotoUrl();
                if (url != null) {
                    imgLink = url.toString();
                }
                final String finalImgLink = imgLink;
                appPrefs.saveStringPreference(PreferenceTypes.K_USER_PHOTO_URL, finalImgLink);
                Disposable disposable = userRepository
                        .getUser()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                //onSuccess
                                localUser -> {
                                    Log.d(TAG, String.format("loginManager constructor. Status: success. ID: %s. Message: User found.", localUser.getID()));
                                    onLoginSuccessful(new AccountData(localUser.getID(),
                                            localUser.getUserName(),
                                            localUser.getDisplayName(),
                                            finalImgLink,
                                            localUser.getUserType(),
                                            AccountData.getAccountTypeForString(localUser.getLoginType())));
                                },
                                //on error
                                throwable -> Log.d(TAG, String.format("loginManager constructor. Status: error. Message: %s.", throwable.getMessage())),
                                //OnComplete
                                () -> {
                                    Log.d(TAG, "loginManager constructor. Status: complete. Message: User not found.");
                                    EventBus.postSticky(new LoginChangedEvent(false, null));
                                    EventBus.postSticky(new UserTypeChangedEvent(PreferenceTypes.USER_TYPE_UNKNOWN));
                                }
                        );
                compositeDisposable.add(disposable);
            } else {//user signed out
                Log.d(TAG, "onAuthStateChanged:signed_out");
            }
        });

        mFacebookCallbackManager = CallbackManager.Factory.create();

        com.facebook.login.LoginManager.getInstance().registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {

            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(TAG, "mFacebookCallbackManager onSuccess: ");
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "mFacebookCallbackManager onCancel: ");
            }

            @Override
            public void onError(FacebookException exception) {
                Log.d(TAG, "mFacebookCallbackManager onError: " + Log.getStackTraceString(exception));
            }
        });
        EventBus.register(this);
    }

    @Override
    public void destroy() {
        compositeDisposable.clear();
        super.destroy();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: error connecting google api: " + connectionResult.getErrorMessage());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data, Context context) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + " resultCode=" + resultCode);
        if (requestCode == LoginManager.REQUEST_CODE_LOGIN_GOOGLE) {
            Log.d(TAG, "onActivityResult: REQUEST_CODE_LOGIN_GOOGLE");
            if (resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "onActivityResult: result OK");
                // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    Log.d(TAG, "onActivityResult: Google Sign In was successful, authenticate with Firebase");
                    handleGoogleLoginResult(result, context);
                } else {
                    Log.d(TAG, "onActivityResult: Google Sign In was unsuccessfull, rollback login");
                    EventBus.postSticky(new LogoutCommand());
                }
            }
        } else {
            Log.d(TAG, "onActivityResult: not google request code, forwarding to Facebook callback manager " + mFacebookCallbackManager);
            mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void login(OSVActivity activity, String type) {
        Log.d(TAG, "login: requested login type " + type);
        loginType = type;
        switch (type) {
            case LOGIN_TYPE_FACEBOOK:
                loginFacebook(activity);
                break;
            case LOGIN_TYPE_PARTNER:
            case LOGIN_TYPE_GOOGLE:
                loginGoogle(activity);
                break;
            case LOGIN_TYPE_OSM:
                showOSMLoginScreen(activity);
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onLogoutCommand(LogoutCommand command) {
        Disposable disposable = userRepository
                .deleteUser()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        //onComplete
                        () -> {
                            EventBus.postSticky(new LoginChangedEvent(false, null));
                            EventBus.postSticky(new UserTypeChangedEvent(PreferenceTypes.USER_TYPE_UNKNOWN));
                            logoutFirebase();
                        },
                        //onError
                        throwable -> {
                            Log.d(TAG, String.format("Delete user invalid. Message: %s", throwable.getMessage()));
                            Toast.makeText(mContext, R.string.something_wrong_try_again, Toast.LENGTH_SHORT).show();
                        }
                );
        compositeDisposable.add(disposable);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.BACKGROUND)
    public void onLoginChanged(LoginChangedEvent event) {
        Log.d(TAG, "onLoginChanged: logged=" + event.logged + " " + event.accountData);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        if (event.logged) {
            String method = appPrefs.getStringPreference(PreferenceTypes.K_LOGIN_TYPE);
            int type = appPrefs.getIntPreference(PreferenceTypes.K_USER_TYPE, -1);
            FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(mContext);
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.METHOD, method);
            bundle.putInt("user_type", type);
            analytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
            crashlytics.setCustomKey(Log.USER_TYPE, type);
        } else {
            crashlytics.setCustomKey(Log.USER_TYPE, "");
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

    private void onLoginSuccessful(final AccountData accountData) {
        Log.w(TAG, "onLoginSuccessful for account.");
        notifyAppOfSuccessfulLogin(accountData);
    }

    private void notifyAppOfSuccessfulLogin(AccountData accountData) {
        //notify right away...
        EventBus.postSticky(new LoginChangedEvent(true, accountData));
        EventBus.postSticky(new UserTypeChangedEvent(accountData.getUserType()));
    }

    private void showOSMLoginScreen(final OSVActivity activity) {
        if (Utils.isInternetAvailable(activity)) {
            activity.enableProgressBar(true);
            showOSMLoginDialog(activity, new OsmAuthDataListener() {

                String mProfilePictureUrl;

                @Override
                public void requestFailed(int status, AuthData details) {
                    activity.enableProgressBar(false);
                }

                @Override
                public void requestFinished(int status, OsmProfileData osmProfileData) {
                    if (osmProfileData != null) {
                        appPrefs.saveStringPreference(PreferenceTypes.K_USER_PHOTO_URL, osmProfileData.getProfilePictureUrl());
                        mProfilePictureUrl = osmProfileData.getProfilePictureUrl();
                    }
                    activity.enableProgressBar(false);
                }

                @Override
                public void requestFinished(int status, AuthData userData) {

                    final String id = userData.getId();
                    final String displayName = userData.getDisplayName();
                    final String token = userData.getAccessToken();
                    final String userName = userData.getUsername();
                    final int type = userData.getUserType();
                    final String loginType = userData.getLoginType();
                    final String finalImgLink = mProfilePictureUrl;
                    activity.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            if (userName.equals("") || token.equals("")) {
                                EventBus.postSticky(new LoginChangedEvent(false, null));
                                EventBus.postSticky(new UserTypeChangedEvent(PreferenceTypes.USER_TYPE_UNKNOWN));
                            } else {
                                onLoginSuccessful(new AccountData(id, userName, displayName, finalImgLink, type,
                                        AccountData.getAccountTypeForString(loginType)));
                            }
                            activity.enableProgressBar(false);
                            activity.finish();
                        }
                    });
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
            Log.d(TAG, "loginFacebook: firebase already has a logged user, from " + mAuth.getCurrentUser().getProviderId());
            logoutFirebase();
        }
        Log.d(TAG, "loginFacebook: sending request");
        com.facebook.login.LoginManager.getInstance().logInWithReadPermissions(activity, Arrays.asList("public_profile", "email"));
    }

    private void loginGoogle(OSVActivity activity) {
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "loginGoogle: firebase already has a logged user, from " + mAuth.getCurrentUser().getProviderId());
            logoutFirebase();
        }
        Log.d(TAG, "loginGoogle: sending intent");
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        activity.startActivityForResult(signInIntent, REQUEST_CODE_LOGIN_GOOGLE);
    }

    /**
     * displays a webview with the osm login site.
     * @param activity the ref for the support fragmenbt manager
     * @param listener response listener
     * @param onDetachListener on detach listener for the webview
     */
    private void showOSMLoginDialog(final FragmentActivity activity, final OsmAuthDataListener listener,
                                    final OAuthDialogFragment.OnDetachListener onDetachListener) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final CommonsHttpOAuthConsumer consumer;
                    final CommonsHttpOAuthProvider provider;
                    consumer = new CommonsHttpOAuthConsumer(activity.getString(R.string.osm_consumer_key),
                            activity.getString(R.string.osm_consumer_secret_key));

                    provider = new CommonsHttpOAuthProvider("https://www.openstreetmap.org/oauth/request_token",
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

                                        authenticate(factoryServerEndpointUrl.getLoginAuthentication(UrlLogin.OSM), requestToken, secretToken, listener);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        ApiResponse response = new ApiResponse();
                                        response.setHttpCode(403);
                                        response.setHttpMessage(e.getMessage());
                                        listener.requestFailed(NetworkResponseDataListener.HTTP_BAD_REQUEST, new AuthData());
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
                    ApiResponse response = new ApiResponse();
                    response.setHttpCode(403);
                    response.setHttpMessage(e.getMessage());
                    listener.requestFailed(NetworkResponseDataListener.HTTP_BAD_REQUEST, new AuthData());
                }
            }
        }).start();
    }

    /**
     * lists the details of the images in an online sequence
     * @param listener request listener
     */
    private void authenticate(final String url, final String requestToken, final String secretToken,
                              final NetworkResponseDataListener<AuthData> listener) {
        Log.d(TAG, "authenticate: " + url);
        AuthRequest request = new AuthRequest(url, new KVRequestResponseListener<AuthDataParser, AuthData>(mAuthDataParser) {

            @Override
            public void onSuccess(final int status, final AuthData authData) {
                Log.d(TAG, "authenticate: success");
                String loginType = "OSM";
                if (url.contains("facebook")) {
                    loginType = "FACEBOOK";
                } else if (url.contains("google")) {
                    loginType = "GOOGLE";
                }
                authData.setLoginType(loginType);
                Disposable disposable = userRepository
                        .saveUser(getUser(authData))
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe(
                                //onComplete
                                () -> {
                                    Log.d(TAG, "onLoginSuccess");
                                    listener.requestFinished(status, authData);
                                },
                                //onError
                                throwable -> Log.d(TAG, "onLoginFailure")
                        );
                compositeDisposable.add(disposable);
            }

            @Override
            public void onFailure(int status, AuthData authData) {
                Log.d(TAG, "authenticate: fail");
                listener.requestFailed(status, authData);
            }
        }, requestToken, secretToken);
        request.setRetryPolicy(new DefaultRetryPolicy(10000, 3, 1f));
        mQueue.add(request);
    }

    /**
     * @param authData the authetication response from the server api.
     * @return {@code User} representing the user created from authenticated data without any user details since requires different API call.
     */
    private User getUser(AuthData authData) {
        return new User(authData.getId(),
                authData.getAccessToken(),
                authData.getDisplayName(),
                authData.getLoginType(),
                authData.getUsername(),
                authData.getUserType(),
                0,
                null,
                null,
                null,
                null);
    }

    private void handleGoogleLoginResult(final GoogleSignInResult result, Context context) {
        BackgroundThreadPool.post(new Runnable() {

            @Override
            public void run() {
                try {
                    Log.d(TAG, "handleSignInResult: success = " + result.isSuccess());
                    GoogleSignInAccount acct = result.getSignInAccount();
                    if (acct == null) {
                        Log.d(TAG, "handleGoogleLoginResult: GoogleSignInAccount is null, rolling back login");
                        EventBus.postSticky(new LogoutCommand());
                        return;
                    }
                    final String googleAuthIdToken = acct.getIdToken();
                    final AuthCredential credential = GoogleAuthProvider.getCredential(googleAuthIdToken, null);
                    GoogleAccountCredential credential2 = GoogleAccountCredential.usingOAuth2(mContext, Collections.singleton(Scopes.PROFILE));
                    credential2.setSelectedAccount(acct.getAccount());
                    String token = null;
                    try {
                        token = credential2.getToken();
                    } catch (IOException | GoogleAuthException e) {
                        Log.d(TAG, "handleGoogleLoginResult: " + Log.getStackTraceString(e));
                    }
                    if (LoginManager.LOGIN_TYPE_GOOGLE.equals(loginType)) {
                        authenticate(factoryServerEndpointUrl.getLoginAuthentication(UrlLogin.Google), token, null, new NetworkResponseDataListener<AuthData>() {

                            @Override
                            public void requestFailed(int status, AuthData details) {
                                Log.d(TAG, "handleGoogleLoginResult requestFinished: rolling back login");
                                signOutGoogle();
                                EventBus.postSticky(new LogoutCommand());
                            }

                            @Override
                            public void requestFinished(int status, AuthData userData) {
                                Log.d(TAG, "handleGoogleLoginResult requestFinished: API token received, signing in with firebase as well");
                                signInWithFirebase(credential);
                            }
                        });
                    } else if (LoginManager.LOGIN_TYPE_PARTNER.equals(loginType)) {
                        if (googleAuthIdToken == null || token == null) {
                            LogUtils.logDebug(TAG, "Google Token is Null for Partner Login");
                            EventBus.postSticky(new LogoutCommand());
                        } else {
                            loginJarvis(googleAuthIdToken, token, credential, context);
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "handleGoogleLoginResult: " + Log.getStackTraceString(e));
                    Log.d(TAG, "handleGoogleLoginResult: rolling back login");
                    EventBus.postSticky(new LogoutCommand());
                }
            }
        });
    }

    private void loginJarvis(
            final String googleAuthIdToken,
            final String googleAccessToken,
            final AuthCredential credential,
            final Context context) {
        final String GOOGLE = "GOOGLE";
        JarvisLoginRequest loginRequest = new JarvisLoginRequest(GOOGLE, googleAuthIdToken);
        Disposable partnerLoginDisposable = jarvisLoginUseCase
                .jarvisLogin(googleAccessToken, loginRequest)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        jarvisLoginResponse -> {
                            compositeDisposable.add(userRepository
                                    .saveUser(UserUtils.getUser(jarvisLoginResponse))
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(Schedulers.io())
                                    .subscribe(
                                            //onComplete
                                            () -> signInWithFirebase(credential),
                                            //onError
                                            throwable -> {
                                                LogUtils.logDebug(TAG, "Failed to save user after Jarvis Login");
                                                signOutGoogle();
                                                EventBus.postSticky(new LogoutCommand());
                                            }
                                    ));
                        },
                        throwable -> {
                            LogUtils.logDebug(TAG, "Jarvis login request failed");
                            signOutGoogle();
                            EventBus.postSticky(new LogoutCommand());
                            showJarvisLoginFailureDialog(context);
                        }
                );
        compositeDisposable.add(partnerLoginDisposable);
    }

    private void showJarvisLoginFailureDialog(Context context) {
        if (jarvisLoginFailureDialog == null) {
            jarvisLoginFailureDialog = new KVDialog.Builder(context)
                    .setTitleResId(R.string.partner_login_failed_dialog_title)
                    .setTitleTextColor(R.color.color_EB3030)
                    .setInfoResId(R.string.partner_login_failed_dialog_message)
                    .setPositiveButton(R.string.close, v -> jarvisLoginFailureDialog.dismiss())
                    .setIconLayoutVisibility(false)
                    .build();
        }
        jarvisLoginFailureDialog.show();
    }

    /**
     * This method sign in a user in firebase
     * @param credential Google AuthCredential for user
     */
    private void signInWithFirebase(final AuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
            Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());
            if (!task.isSuccessful()) {
                EventBus.postSticky(new LogoutCommand());
            }
        }).addOnFailureListener(new OnFailureListener() {

            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "signInWithCredential:onFailure:" + Log.getStackTraceString(e));
                EventBus.postSticky(new LogoutCommand());
            }
        });
    }

    private void handleFacebookAccessToken(final AccessToken token) {
        Log.d(TAG, "handleFacebookAccessToken:" + token);

        final AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        authenticate(factoryServerEndpointUrl.getLoginAuthentication(UrlLogin.Facebook), token.getToken(), "", new NetworkResponseDataListener<AuthData>() {

            @Override
            public void requestFailed(int status, AuthData details) {
                Log.d(TAG, "handleFacebookAccessToken requestFinished: API call failed, rolling back login");
                EventBus.postSticky(new LogoutCommand());
            }

            @Override
            public void requestFinished(int status, AuthData userData) {
                Log.d(TAG, "handleFacebookAccessToken requestFinished: API token received, signing in with firebase as well");
                mAuth.signInWithCredential(credential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "handleFacebookAccessToken signInWithCredential:onComplete:" + task.isSuccessful());

                        if (!task.isSuccessful()) {
                            Log.d(TAG, "handleFacebookAccessTokenonComplete: unsuccessful, rolling back login");
                            EventBus.postSticky(new LogoutCommand());
                        }
                    }
                });
            }
        });
    }

    private void logoutFirebase() {
        if (mAuth.getCurrentUser() != null) {
            Log.d(TAG, "logoutFirebase: " + mAuth.getCurrentUser().getProviderId());
            //signed in with facebook or google
            FirebaseAuth.getInstance().signOut();
            // user is now signed out
            signOutGoogle();
            signOutFacebook();
        }
    }

    private void signOutGoogle() {
        Log.d(TAG, "signOutGoogle: ");
        try {
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
        } catch (Exception e) {
            Log.d(TAG, "signOutGoogle: " + Log.getStackTraceString(e));
        }
    }

    private void signOutFacebook() {
        Log.d(TAG, "signOutFacebook: ");
        try {
            com.facebook.login.LoginManager.getInstance().logOut();
        } catch (Exception e) {
            Log.d(TAG, "signOutFacebook: " + Log.getStackTraceString(e));
        }
    }
}
