package com.telenav.osv.common.service.location

import android.app.IntentService
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.ResultReceiver
import com.telenav.osv.R
import com.telenav.osv.utils.Log
import java.io.IOException
import java.util.*

class IntentServiceFetchAddress : IntentService(NAME_SERVICE) {

    private val TAG by lazy { IntentServiceFetchAddress::class.java.name }

    var receiver: ResultReceiver? = null

    override fun onHandleIntent(intent: Intent?) {
        val geocoder = Geocoder(this, Locale.getDefault())

        var errorMessage = ""

        // Get the location passed to this service through an extra.
        val location = intent!!.getParcelableExtra<Location>(LOCATION_DATA_LOC)
        receiver = intent.getParcelableExtra(RECEIVER)
        val sequenceId = intent.getStringExtra(LOCATION_DATA_SEQ_ID)
        val sequencePos = intent.getIntExtra(LOCATION_DATA_SEQ_POS, 0)

        var addresses: List<Address> = emptyList()

        try {
            addresses = geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    // In this sample, we get just a single address.
                    RESULTS_NUMBER)
        } catch (ioException: IOException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available)
            Log.e(TAG, errorMessage, ioException)
        } catch (illegalArgumentException: IllegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used)
            Log.e(TAG, "$errorMessage. Latitude = $location.latitude , " +
                    "Longitude =  $location.longitude", illegalArgumentException)
        }

        // Handle case where no address was found.
        if (addresses.isEmpty()) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found)
                Log.e(TAG, errorMessage)
            }
            deliverResultToReceiver(FAILURE_RESULT, errorMessage, sequenceId, sequencePos)
        } else {
            val address = addresses[0]
            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            val addressFragments = with(address) {
                (0..maxAddressLineIndex).map { getAddressLine(it) }
            }
            Log.i(TAG, getString(R.string.address_found))
            deliverResultToReceiver(SUCCESS_RESULT,
                    addressFragments.joinToString(separator = "\n"),
                    sequenceId,
                    sequencePos)
        }
    }

    private fun deliverResultToReceiver(resultCode: Int, message: String, sequenceId: String, position: Int) {
        val bundle = Bundle().apply {
            putString(RESULT_DATA_KEY, message)
            putString(RESULT_DATA_SEQUENCE_ID, sequenceId)
            putInt(RESULT_DATA_SEQUENCE_POSITION, position)
        }
        receiver?.send(resultCode, bundle)
    }

    companion object {
        private const val NAME_SERVICE = "intent-service-fetch-address"
        const val SUCCESS_RESULT = 0
        const val FAILURE_RESULT = 1
        const val PACKAGE_NAME = "com.google.android.gms.location.sample.locationaddress"
        const val RECEIVER = "$PACKAGE_NAME.RECEIVER"
        const val RESULT_DATA_KEY = "${PACKAGE_NAME}.RESULT_DATA_KEY"
        const val RESULT_DATA_SEQUENCE_ID = "${PACKAGE_NAME}.RESULT_DATA_SEQUENCE_ID"
        const val RESULT_DATA_SEQUENCE_POSITION = "${PACKAGE_NAME}.RESULT_DATA_SEQUENCE_POSITION"
        const val LOCATION_DATA_LOC = "${PACKAGE_NAME}.LOCATION_DATA_LOC"
        const val LOCATION_DATA_SEQ_ID = "${PACKAGE_NAME}.LOCATION_DATA_SEQ_ID"
        const val LOCATION_DATA_SEQ_POS = "${PACKAGE_NAME}.LOCATION_DATA_SEQ_POS"
        private const val RESULTS_NUMBER = 1
    }
}