package com.telenav.osv.jarvis.login.utils

import android.content.Context
import android.content.Intent
import com.telenav.osv.R
import com.telenav.osv.activity.MainActivity
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.command.LogoutCommand
import com.telenav.osv.common.dialog.KVDialog
import com.telenav.osv.event.EventBus
import com.telenav.osv.manager.network.LoginManager

object LoginUtils {

    @JvmStatic
    fun launchAppWithLoginActivity(context: Context) {
        EventBus.post(LogoutCommand())
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra(MainActivity.KEY_INIT_LOGIN, true)
        context.startActivity(intent)
    }

    @JvmStatic
    fun getSessionExpiredDialog(context: Context): KVDialog {
        var sessionExpireDialog: KVDialog? = null
        sessionExpireDialog = KVDialog.Builder(context)
                .setTitleResId(R.string.session_expired_dialog_title)
                .setInfoResId(R.string.session_expired_dialog_message)
                .setPositiveButton(R.string.login_label) {
                    launchAppWithLoginActivity(context)
                    sessionExpireDialog?.dismiss()
                }
                .setIconLayoutVisibility(false)
                .setCancelableOnOutsideClick(false)
                .setCancelable()
                .build()
        return sessionExpireDialog
    }

    @JvmStatic
    fun restartApp(context: Context) {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    @JvmStatic
    fun isLoginTypePartner(appPrefs: ApplicationPreferences) =
            LoginManager.LOGIN_TYPE_PARTNER == appPrefs.getStringPreference(PreferenceTypes.K_LOGIN_TYPE)
}