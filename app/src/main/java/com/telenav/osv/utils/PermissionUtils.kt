package com.telenav.osv.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.telenav.osv.application.KVApplication
import java.util.*

/**
 * Object with helper methods related to permissions.
 */
object PermissionUtils {

    /**
     * @return *true* if the [permission] is granted, *false* otherwise. This method uses internally [ContextCompat.checkSelfPermission] with the given [context] param.
     * @param context the [Context] required to check for permission.
     * @param permission the identifier for the permission to be check against.
     */
    fun isPermissionGranted(context: Context?, permission: String): Boolean {
        return context != null && ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * @return *true* if the [permissions] are granted, *false* otherwise. This method uses internally [ContextCompat.checkSelfPermission] with the given [context] param.
     * @param context the [Context] required to check for permission.
     * @param permissions the collection of identifiers for the permissions to be check against.
     */
    fun isPermissionGranted(context: Context?, permissions: Array<String>): Boolean {
        var result = false
        permissions.forEach {
            result = isPermissionGranted(context, it)
        }
        return result
    }

    /**
     * @return *true* if the permission is granted, *false* otherwise.
     * @param activity the activity for which the rationale would be checked.
     * @param permission the identifier for the permission to be check against.
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    fun checkPermissionsForGPS(activity: Activity): Boolean {
        val needed = ArrayList<String>()
        val locationPermitted = ContextCompat.checkSelfPermission(activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (locationPermitted == PackageManager.PERMISSION_DENIED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                needed.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }
        if (needed.size > 0) {
            val array = arrayOfNulls<String>(needed.size)
            needed.toArray(array)
            ActivityCompat.requestPermissions(activity, array, KVApplication.LOCATION_PERMISSION)
            return false
        }
        return true
    }
}