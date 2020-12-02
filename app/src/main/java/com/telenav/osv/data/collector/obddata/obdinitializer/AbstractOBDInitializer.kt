package com.telenav.osv.data.collector.obddata.obdinitializer

import com.telenav.osv.data.collector.obddata.OBDConstants
import com.telenav.osv.data.collector.obddata.manager.ObdDataListener
import timber.log.Timber
import java.lang.Thread.sleep
import java.util.*

/**
 * abstract class used for sending the initialization commands to an OBD device
 * extended by different obd versions
 */
abstract class AbstractOBDInitializer internal constructor(protected var obdDataListener: ObdDataListener?, private val deviceVersion: String?) {
    private val ecuIdMap: Map<String, String>
    private val retryManager: InitializationRetryManager
    fun setupObdParser() {
        val s0: String?
        val e0: String?
        val d: String?
        val l0: String?
        when (deviceVersion) {
            ATConstants.V1_5 -> {
                d = retryManager.dResponse
                Timber.tag(TAG).d("rawData d=%s", d)
                s0 = retryManager.s0Response
                Timber.tag(TAG).d("rawData s0=%s", s0)
                e0 = retryManager.e0Response
                Timber.tag(TAG).d("rawData e0=%s", e0)
                l0 = retryManager.l0Response
                Timber.tag(TAG).d("rawData l0=%s", l0)
            }
            ATConstants.V2_1 -> {
                d = retryManager.dResponse
                Timber.tag(TAG).d("rawData d=%s", d)
                s0 = retryManager.s0Response
                Timber.tag(TAG).d("rawData s0=%s", s0)
                e0 = retryManager.e0Response
                Timber.tag(TAG).d("rawData e0=%s", e0)
                l0 = retryManager.l0Response
                Timber.tag(TAG).d("rawData l0=%s", l0)
            }
            else -> {
                setupObdDevice()
                Timber.tag(TAG).d("DEBUG: unknown version")
            }
        }
    }

    protected abstract fun getAtResponse(sendingCommand: String): String?

    protected fun delay(ms: Int) {
        var waitCondition = false
        synchronized(this) {
            try {
                while (!waitCondition) {
                    sleep(ms.toLong())
                    waitCondition = true
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    /**
     * in case of an unknown OBD device version, apply all AT commands for initialization
     */
    private fun setupObdDevice() {
        val d: String = retryManager.dResponse
        Timber.tag(TAG).d("rawData d=%s", d)
        val s0: String = retryManager.s0Response
        Timber.tag(TAG).d("rawData s0=%s", s0)
        val e0: String = retryManager.e0Response
        Timber.tag(TAG).d("rawData e0=%s", e0)
        val l0: String = retryManager.l0Response
        Timber.tag(TAG).d("rawData l0=%s", l0)
    }

    /**
     * commands for finding all ECU's for a specific vehicle, and listening
     * to the main ECU:
     * AT DP (describe the protocol of the vehicle)
     * AT H1 (for setting the headers on - to see the ECU ID's)
     * 0100 - to visualize all the ECU's that respond
     * AT SH hhh - filters the responses to be received only from hhh
     * AT H0 (set the headers back to OFF)
     */
    private fun setupVehicleProtocol() {
        var protocol: String = retryManager.deviceProtocol
        protocol = protocol.replace(" ".toRegex(), "")

        Timber.tag(TAG).d("Vehicle protocol: %s", protocol)
        val vehicleProtocolManager = VehicleProtocolManager()
        val bits: Int = vehicleProtocolManager.getProtocolBits(protocol)
        if (bits != 0) {
            val h1Response = getAtResponse(ATConstants.H1)
            //even though 0100 is not really an AT command, it is used in this case just for finding the number of ECUs,
            //therefore helps for the OBD initialization
            val response0100 = retryManager.get0100Response()
            if (response0100 != null) {
                val mainEcu: String? = vehicleProtocolManager.getMainEcu(response0100, bits)
                Timber.tag(TAG).d("DEBUG: main ecu= %s", mainEcu)
                if (mainEcu != null) {
                    val shResponse = retryManager.getShResponse(mainEcu)
                    Timber.tag(TAG).d("DEBUG: AT SH= %s", shResponse)
                }
            }
            val h0Response: String = retryManager.h0Response
            Timber.tag(TAG).d("raw: h0 response is %s", h0Response)
        }
    }

    private fun initializeEcuMap(): Map<String, String> {
        val ecuMap: MutableMap<String, String> = HashMap()
        ecuMap["7E8"] = "7E0" //engine ECU ID
        ecuMap["7E9"] = "7E1" //transmission ECU ID
        ecuMap["7EA"] = "7E2"
        ecuMap["7EB"] = "7E3"
        ecuMap["7EC"] = "7E4"
        ecuMap["7ED"] = "7E5"
        ecuMap["7EE"] = "7E6"
        ecuMap["7EF"] = "7E7"

        //iso 29 bit ecu id's
        ecuMap["18DAF110"] = "18DAF110" //engine ECU ID
        ecuMap["18DAF118"] = "18DAF118" //transmission ECU ID
        return ecuMap
    }

    /**
     * handles bad responses to commands, and implements a retry mechanism
     */
    private inner class InitializationRetryManager {
        val TOTAL_RETRIES = 5
        var ecuFailCount = 0
        var dpFailCount = 0
        private var shFailCount = 0
        private var h0FailCount = 0
        private var s0FailCount = 0
        private var e0FailCount = 0
        private var dFailCount = 0
        private var l0FailCount = 0
        fun get0100Response(): String? {
            if (ecuFailCount < TOTAL_RETRIES) {
                var response0100 = getAtResponse(ECU_CMD)
                if (response0100 == null) {
                    ecuFailCount = 0
                    return null
                }
                Timber.tag(TAG).d("DEBUG: 0100 response= %s", response0100)
                response0100 = response0100.replace("\r".toRegex(), "")
                if (response0100.isEmpty()) {
                    ecuFailCount++
                    delay(200)
                    return get0100Response()
                }
                if (response0100.contains(OBDConstants.SEARCHING)) {
                    ecuFailCount++
                    delay(200)
                    return get0100Response()
                }
                if (!response0100.contains(ECU_CMD_PREFIX_RESPONSE)) {
                    ecuFailCount++
                    delay(200)
                    return get0100Response()
                }
                return response0100
            }
            ecuFailCount = 0
            return null
        }

        val deviceProtocol: String
            get() {
                if (dpFailCount < TOTAL_RETRIES) {
                    var dpResponse = getAtResponse(ATConstants.DP)
                    if (dpResponse == null) {
                        dpFailCount = 0
                        return ""
                    }
                    dpResponse = dpResponse.replace(" ".toRegex(), "")
                    if (!dpResponse.contains(ATConstants.PROTOCOL_ISO15765_4) && !dpResponse.contains(ATConstants.PROTOCOL_ISO9141_2)
                            && !dpResponse.contains(ATConstants.PROTOCOL_ISO14230_4_KWP) && !dpResponse.contains(ATConstants.PROTOCOL_SAE_J1939)
                            && !dpResponse.contains(ATConstants.PROTOCOL_SAE_J1850) && !dpResponse.contains(ATConstants.PROTOCOL_USER1_CAN)
                            && !dpResponse.contains(ATConstants.PROTOCOL_USER2_CAN)) {
                        dpFailCount++
                        delay(200)
                        return deviceProtocol
                    }
                    return dpResponse
                }
                dpFailCount = 0
                return ""
            }

        fun getShResponse(mainEcu: String): String {
            if (shFailCount < TOTAL_RETRIES) {
                val shResponse = getAtResponse(ATConstants.SH + ecuIdMap[mainEcu])
                if (shResponse == null) {
                    shFailCount = 0
                    return ""
                }
                return if (!shResponse.contains("OK")) {
                    shFailCount++
                    delay(200)
                    getShResponse(mainEcu)
                } else {
                    shResponse
                }
            }
            return ""
        }

        val h0Response: String
            get() {
                if (h0FailCount < TOTAL_RETRIES) {
                    val h0Response = getAtResponse(ATConstants.H0)
                    if (h0Response == null) {
                        h0FailCount = 0
                        return ""
                    }
                    return if (!h0Response.contains("OK")) {
                        h0FailCount++
                        delay(200)
                        h0Response
                    } else {
                        h0Response
                    }
                }
                h0FailCount = 0
                return ""
            }
        val s0Response: String
            get() {
                if (s0FailCount < TOTAL_RETRIES) {
                    val s0Response = getAtResponse(ATConstants.S0)
                    if (s0Response == null) {
                        s0FailCount = 0
                        return ""
                    }
                    return if (!s0Response.contains("OK")) {
                        s0FailCount++
                        delay(200)
                        s0Response
                    } else {
                        s0Response
                    }
                }
                s0FailCount = 0
                return ""
            }
        val e0Response: String
            get() {
                if (e0FailCount < TOTAL_RETRIES) {
                    val e0Response = getAtResponse(ATConstants.E0)
                    if (e0Response == null) {
                        e0FailCount = 0
                        return ""
                    }
                    return if (!e0Response.contains("OK")) {
                        e0FailCount++
                        delay(200)
                        e0Response
                    } else {
                        e0Response
                    }
                }
                e0FailCount = 0
                return ""
            }
        val dResponse: String
            get() {
                if (dFailCount < TOTAL_RETRIES) {
                    val dResponse = getAtResponse(ATConstants.D)
                    if (dResponse == null) {
                        dFailCount = 0
                        return ""
                    }
                    return if (!dResponse.contains("OK")) {
                        dFailCount++
                        delay(200)
                        dResponse
                    } else {
                        dResponse
                    }
                }
                dFailCount = 0
                return ""
            }
        val l0Response: String
            get() {
                if (l0FailCount < TOTAL_RETRIES) {
                    val l0Response = getAtResponse(ATConstants.L0)
                    if (l0Response == null) {
                        l0FailCount = 0
                        return ""
                    }
                    return if (!l0Response.contains("OK")) {
                        l0FailCount++
                        delay(200)
                        l0Response
                    } else {
                        l0Response
                    }
                }
                l0FailCount = 0
                return ""
            }
    }

    companion object {
        val TAG = AbstractOBDInitializer::class.java.simpleName
        const val AT_WAITING_TIME = 200
        private const val ECU_CMD = "0100"
        private const val ECU_CMD_PREFIX_RESPONSE = "4100"
    }

    init {
        ecuIdMap = initializeEcuMap()
        retryManager = InitializationRetryManager()
    }
}