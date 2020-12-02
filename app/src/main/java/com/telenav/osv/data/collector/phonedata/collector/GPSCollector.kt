package com.telenav.osv.data.collector.phonedata.collector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GpsStatus.NmeaListener
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.telenav.osv.data.collector.datatype.datatypes.*
import com.telenav.osv.data.collector.datatype.util.LibraryUtil
import com.telenav.osv.data.collector.phonedata.manager.PhoneDataListener
import timber.log.Timber

/**
 * GPSCollector class listen for location data including gps accuracy,
 * altitude, bearing, gps speed
 */
class GPSCollector(phoneDataListener: PhoneDataListener?, notifyHandler: Handler?) : PhoneCollector(phoneDataListener, notifyHandler), LocationListener, NmeaListener {
    private var isSensorRegistered = false
    private lateinit var context: Context
    private var mLocationManager: LocationManager? = null
    fun registerLocationListener(context: Context) {
        this.context = context
        if (!isSensorRegistered) {
            mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) === PackageManager.PERMISSION_GRANTED && mLocationManager != null) {
                mLocationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_IN_MILLISECONDS, SMALLEST_DISPLACEMENT_FOR_LOCATION_UPDATES_IN_MILLISECONDS.toFloat(), this)
                isSensorRegistered = true
            } else {
                sendSensorUnavailabilityStatus(PhonePositionObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
                sendSensorUnavailabilityStatus(AccuracyObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
                sendSensorUnavailabilityStatus(AltitudeObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
                sendSensorUnavailabilityStatus(BearingObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
                sendSensorUnavailabilityStatus(PhoneGpsSpeedObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
            }
        }
    }

    fun unregisterListener() {
        if (mLocationManager != null && isSensorRegistered) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
                Timber.tag(LibraryUtil.ERROR_TAG).e("The permission for location was not GRANTED")
            } else {
                mLocationManager!!.removeUpdates(this)
                isSensorRegistered = false
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    override fun onLocationChanged(location: Location) {
        val gpsData = GPSData(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        val positionObject: PositionObject = PhonePositionObject(LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
        positionObject.setLat(location.latitude)
        positionObject.setLon(location.longitude)
        gpsData.positionObject = positionObject
        determineTimestamp(location.elapsedRealtimeNanos, positionObject)
        if (location.hasAltitude()) {
            val altitudeObject = AltitudeObject(location.altitude, LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
            determineTimestamp(location.elapsedRealtimeNanos, altitudeObject)
            gpsData.altitudeObject = altitudeObject
        } else {
            sendSensorUnavailabilityStatus(AltitudeObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
        if (location.hasSpeed()) {
            val speedObject = PhoneGpsSpeedObject((location.speed * MPS_TO_KMP).toInt(), LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
            determineTimestamp(location.elapsedRealtimeNanos, speedObject)
            gpsData.speedObject = speedObject
        } else {
            sendSensorUnavailabilityStatus(PhoneGpsSpeedObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
        if (location.hasBearing()) {
            val bearingObject = BearingObject(location.bearing, LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
            determineTimestamp(location.elapsedRealtimeNanos, bearingObject)
            gpsData.bearingObject = bearingObject
        } else {
            sendSensorUnavailabilityStatus(BearingObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
        if (location.hasAccuracy()) {
            val accuracyObject = AccuracyObject(location.accuracy, LibraryUtil.PHONE_SENSOR_READ_SUCCESS)
            determineTimestamp(location.elapsedRealtimeNanos, accuracyObject)
            gpsData.accuracyObject = accuracyObject
        } else {
            sendSensorUnavailabilityStatus(AccuracyObject(LibraryUtil.PHONE_SENSOR_NOT_AVAILABLE))
        }
        determineTimestamp(location.elapsedRealtimeNanos, gpsData)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        // No implementation needed
    }

    override fun onProviderEnabled(provider: String) {
        // No implementation needed
    }

    override fun onProviderDisabled(provider: String) {
        // No implementation needed
    }

    /**
     * method from the [android.location.GpsStatus.NmeaListener] interface
     *
     * @param timestamp    the timestamp at which the nmeaSentence was received
     * @param nmeaSentence the message received from NMEA. These messages respect the follow a specific protocol,
     * which is detailed at this link: http://aprs.gids.nl/nmea/
     */
    override fun onNmeaReceived(timestamp: Long, nmeaSentence: String) {
        var latestHdop: Double
        val latestPdop: Double
        val latestVdop: Double
        val geoIdHeight: Double
        val ageOfDgpsData: Double
        val dgpsId: Double
        val nrOfSatellites: Int
        val nrOfSatellitesInView: Int
        val GPGSA_SATELLITES_IN_USE_INDEX = 14
        val PDOP_GPGSA_INDEX = 15
        val HDOP_GPGSA_INDEX = 16
        val VDOP_GPGSA_INDEX = 17
        val GPGGA_SATELLITES_IN_USE_INDEX = 7
        val HDOP_GPGGA_INDEX = 6
        val GEO_ID_HEIGHT_INDEX = 11
        val AGE_OF_GPS_DATA_INDEX = 13
        val DGPS_ID_INDEX = 14
        val GPGSV_SATELLITES_IN_VIEW_INDEX = 3
        val nmeaObject = NmeaObject()

        //loggingService.OnNmeaSentence(timestamp, nmeaSentence);
        Timber.tag(TAG).d("Nmea sentence: %s", nmeaSentence)
        if (TextUtils.isEmpty(nmeaSentence)) {
            return
        }
        val nmeaParts = nmeaSentence.split(",".toRegex()).toTypedArray()
        for (i in nmeaParts.indices) {
            Timber.tag(TAG).d("Nmea parts: %s", nmeaParts[i])
        }
        if (nmeaParts[0].equals(GPGSA, ignoreCase = true)) {
            nmeaObject.sentenceType = GPGSA
            if (nmeaParts.size > GPGSA_SATELLITES_IN_USE_INDEX) {
                nmeaObject.numberOfSatellitesInUse = getNumberOfActiveSatellites(nmeaParts)
            }
            if (nmeaParts.size > PDOP_GPGSA_INDEX && !nmeaParts[PDOP_GPGSA_INDEX].isEmpty()) {
                latestPdop = nmeaParts[PDOP_GPGSA_INDEX].toDouble()
                nmeaObject.pdopValue = latestPdop
            }
            if (nmeaParts.size > HDOP_GPGSA_INDEX && !nmeaParts[HDOP_GPGSA_INDEX].isEmpty()) {
                latestHdop = nmeaParts[HDOP_GPGSA_INDEX].toDouble()
                nmeaObject.hdopValue = latestHdop
            }
            if (nmeaParts.size > VDOP_GPGSA_INDEX && !nmeaParts[VDOP_GPGSA_INDEX].isEmpty() && !nmeaParts[VDOP_GPGSA_INDEX].startsWith("*")) {
                latestVdop = nmeaParts[VDOP_GPGSA_INDEX].split("\\*".toRegex()).toTypedArray()[0].toDouble()
                nmeaObject.vdopValue = latestVdop
            }
        }
        if (nmeaParts[0].equals(GPGGA, ignoreCase = true)) {
            nmeaObject.sentenceType = GPGGA
            if (nmeaParts.size > GPGGA_SATELLITES_IN_USE_INDEX && !nmeaParts[GPGGA_SATELLITES_IN_USE_INDEX].isEmpty()) {
                nrOfSatellites = nmeaParts[GPGGA_SATELLITES_IN_USE_INDEX].toInt()
                nmeaObject.numberOfSatellitesInUse = nrOfSatellites
            }
            if (nmeaParts.size > HDOP_GPGGA_INDEX && !nmeaParts[HDOP_GPGGA_INDEX].isEmpty()) {
                latestHdop = nmeaParts[HDOP_GPGGA_INDEX].toDouble()
                nmeaObject.hdopValue = latestHdop
            }
            if (nmeaParts.size > GEO_ID_HEIGHT_INDEX && !nmeaParts[GEO_ID_HEIGHT_INDEX].isEmpty()) {
                geoIdHeight = nmeaParts[GEO_ID_HEIGHT_INDEX].toDouble()
                nmeaObject.geoIdHeight = geoIdHeight
            }
            if (nmeaParts.size > AGE_OF_GPS_DATA_INDEX && !nmeaParts[AGE_OF_GPS_DATA_INDEX].isEmpty()) {
                ageOfDgpsData = nmeaParts[AGE_OF_GPS_DATA_INDEX].toDouble()
                nmeaObject.ageOfDgpsData = ageOfDgpsData
            }
            if (nmeaParts.size > DGPS_ID_INDEX && !nmeaParts[DGPS_ID_INDEX].isEmpty() && !nmeaParts[DGPS_ID_INDEX].startsWith("*")) {
                dgpsId = nmeaParts[DGPS_ID_INDEX].split("\\*".toRegex()).toTypedArray()[0].toDouble()
                nmeaObject.dgpsId = dgpsId
            }
        }
        if (nmeaParts[0].equals(GPGSV, ignoreCase = true)) {
            nmeaObject.sentenceType = GPGSV
            nrOfSatellitesInView = nmeaParts[GPGSV_SATELLITES_IN_VIEW_INDEX].toInt()
            nmeaObject.numberOfSatellitesInView = nrOfSatellitesInView
        }
        if (nmeaObject.containsInfo()) {
            determineTimestamp(timestamp, nmeaObject)
            Timber.tag(TAG).d("Nmea info: %s", nmeaObject.toString())
        }
    }

    /**
     * retrieves the number of satellites in use
     *
     * @param gpgsaMessage the GPGSA message that contains the ID's of the active satellites
     * @return the number of satellites in use
     */
    private fun getNumberOfActiveSatellites(gpgsaMessage: Array<String>): Int {
        var satellitesInUse = 0
        val initialSatelliteId = 3
        val lastSatelliteId = 14
        for (activeSatCounter in initialSatelliteId..lastSatelliteId) {
            if (gpgsaMessage[activeSatCounter].isNotEmpty()) {
                satellitesInUse++
            }
        }
        return satellitesInUse
    }

    companion object {
        val TAG = GPSCollector::class.java.simpleName

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
        private const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 0

        /**
         * The smallest displacement for the location update intervals
         */
        private const val SMALLEST_DISPLACEMENT_FOR_LOCATION_UPDATES_IN_MILLISECONDS: Long = 0

        /**
         * GPS DOP and active satellites
         */
        private const val GPGSA = "\$GPGSA"

        /**
         * Global Positioning System Fix Data
         */
        private const val GPGGA = "\$GPGGA"

        /**
         * GPS Satellites in view
         */
        private const val GPGSV = "\$GPGSV"

        private const val MPS_TO_KMP = 3.6
    }
}