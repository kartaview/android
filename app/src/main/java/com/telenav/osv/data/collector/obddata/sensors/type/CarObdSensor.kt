package com.telenav.osv.data.collector.obddata.sensors.type

/**
 * Created by ovidiuc2 on 11/10/16.
 */
/**
 * used for converting the hexadecimal response data to a real sensor value
 * @param <T> - the type of data returned
</T> */
interface CarObdSensor<T : Number?> {
    fun convertValue(hexResponse: String): T
}