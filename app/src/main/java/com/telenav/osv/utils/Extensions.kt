package com.telenav.osv.utils

import android.content.Context
import android.util.TypedValue
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.telenav.osv.application.ApplicationPreferences
import com.telenav.osv.application.PreferenceTypes
import com.telenav.osv.jarvis.login.utils.LoginUtils
import com.telenav.osv.map.model.MapModes
import com.telenav.osv.recorder.camera.Camera
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Extension method for creating a [MutableLiveData] with a default value by calling internally with the [apply] function the setter.
 */
fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

/**
 * Transforms [Task] into a suspended function with respect to the type.
 */
suspend fun <T> Task<T>.await(): T? = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: RuntimeException("Unknown task exception"))
        }
    }
}

fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int, type: Int) -> Unit): T {
    itemView.setOnClickListener {
        event.invoke(adapterPosition, itemViewType)
    }
    return this
}

fun <T> concatenate(vararg lists: List<T>): List<T> {
    return listOf(*lists).flatten()
}

/**
 * Extension method for [Camera] to return the resolution based on video format.
 *
 * If the camera API used is Camera1 and the recording mode is video the images for encoder have the same size as the preview.
 * The camera1 doesn't support to set a custom size for frames which is not depending on the preview available sizes,
 * therefore the maximum size for a frame will be of 1920x1080.
 * @return the resolution as [Size] for the [Camera] used in video/jpeg.
 */
fun Camera.getResolution(): Size {
    return if (isVideoMode && !isCamera2Api) {
        Log.d("Camera", "getResolution. Status: Return preview size since Camera API V1 and video mode is on. Preview size: $previewSize")
        previewSize
    } else {
        Log.d("Camera", "getResolution. Status: Return picture size. Picture size: $pictureSize")
        pictureSize
    }
}

/**
 * Extension method to close the camera
 */
fun Camera.close() {
    if (isCameraOpen) {
        closeCamera()
    }
}


fun Context.getDimension(value: Float): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, this.resources.displayMetrics).toInt()
}

/**
 * Extension method for [ApplicationPreferences] which will return based on saved data the [MapModes]
 */
fun ApplicationPreferences.getMapMode(): MapModes {
    return if (this.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED)) {
        if (LoginUtils.isLoginTypePartner(this)) MapModes.GRID else MapModes.IDLE
    } else MapModes.DISABLED
}

/**
 * Extension method for [ApplicationPreferences] which will return the status for enabling or not the map for Recording feature
 */
fun ApplicationPreferences.enableMapForRecording(): Boolean {
    return this.getBooleanPreference(PreferenceTypes.K_MAP_ENABLED) && this.getBooleanPreference(PreferenceTypes.K_RECORDING_MINI_MAP_ENABLED, true)
}