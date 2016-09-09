package com.telenav.osv.obd;

import android.util.Log;
import com.telenav.osv.obd.VehicleDataListener;

/**
 * Created by dianat on 3/25/2016.
 */
public class OBDHelper {
    /**
     * ELM327 commands sent to the obd2
     */
    /**
     * command for RPM - Revolutions Per Minute
     * the final 1 is because of this: it will wait till 1 answer comes in, and it will directly send it back
     */
    public static final String CMD_RPM = "01 0C1";

    /**
     * ELM327 commands sent to the obd2
     */
    /**
     * command for FUEL LEVEL - 0% to 100%
     * the final 1 is because of this: it will wait till 1 answer comes in, and it will directly send it back
     */
    public static final String CMD_FUEL_LEVEL = "01 2F1";

    /**
     * command for speed
     */
    public static final String CMD_SPEED = "010D1";

    /**
     * command for speed
     */
    public static final String CMD_WARM_START = "AT WS";

    /**
     * command for speed
     */
    public static final String CMD_FAST_INIT = "AT FI";

    /**
     * command for engine load
     */
    public static final String CMD_ENGINE_LOAD = "01 04";

    /**
     * command for coolant temperature
     */
    public static final String CMD_COOLANT_TEMP = "01 05";

    private static final String TAG = "OBDHelper";

    public static final String CMD_SET_AUTO = "AT SP 00";

    public static final String CMD_DEVICE_DESCRIPTION = "AT @1";

    public static final String CMD_DESCRIBE_PROTOCOL = "AT DP";

    /**
     * converts the characteristic result to integer value according to the command type formula
     * the formulas used for calculation where taken for: https://en.wikipedia.org/wiki/OBD-II_PIDs
     * @param charResult - the characteristic result
     * @return - the integer value
     */
    public static int convertResult(String charResult, VehicleDataListener vehicleDataListener) {
        String request = null;
        String response = null;
        String[] arrayRes = charResult.split("\r");
        if (arrayRes.length > 1) {
            request = arrayRes[0];
            response = arrayRes[1];
        }
        String[] splitResponse = response != null ? response.split(" ") : new String[0];
        if (request != null) {
            switch (request) {
                case CMD_FUEL_LEVEL: {

                    if (splitResponse.length >= 3) {
                        // formula used: (256A + B)/4, where A is the first byte and B the second byte
                        int fuelLevel = (100 * Integer.parseInt(splitResponse[2], 16) / 255);
                        if (vehicleDataListener != null) {
//                            vehicleDataListener.onFuelLevel(fuelLevel);
                        }
                        return fuelLevel;
                    }
                }
                break;
                case CMD_RPM:
                    if (splitResponse.length >= 4) {
                        // formula used: (256A + B)/4, where A is the first byte and B the second byte
                        int rpm = (Integer.parseInt(splitResponse[2], 16) * 256 + Integer.parseInt(splitResponse[3], 16)) / 4;
                        if (vehicleDataListener != null) {
//                            vehicleDataListener.onRPM(rpm);
                        }
                        return rpm;
                    }
                    break;
                case CMD_SPEED:
                    if (splitResponse.length >= 3) {
                        // formula used: A, where A is the first byte
                        Integer speed = Integer.parseInt(splitResponse[2], 16);
                        if (vehicleDataListener != null) {
                            vehicleDataListener.onSpeed(speed);
                        }
                        return speed;
                    }
//                    else if (splitResponse.length >= 1){
//                        return splitResponse[0];
//                    }
                    break;
                case CMD_ENGINE_LOAD:
                    if (splitResponse.length >= 4) {
                        // formula used: 100 * A/255, where A is the first byte
                        return Integer.parseInt(splitResponse[2], 16) * 100 / 255;
                    }
                    break;
                case CMD_COOLANT_TEMP:
                    if (splitResponse.length >= 4) {
                        // formula used: A - 40, where A is the first byte
                        return Integer.parseInt(splitResponse[2], 16) - 40;
                    }
                    break;
                default:
                    Log.e(TAG, "convertResult: cannot parse");
            }
        }
        return -1;
    }

    /**
     * converts the characteristic result to integer value according to the command type formula
     * the formulas used for calculation where taken for: https://en.wikipedia.org/wiki/OBD-II_PIDs
     * @param charResult - the characteristic result
     * @return - the integer value
     */
    public static String convertRequest(String charResult) {
        String request = null;
        String[] arrayRes = charResult.split("\r");
        if (arrayRes.length > 1) {
            request = arrayRes[0];
        }
        if (request != null) {
            switch (request) {
                case CMD_FUEL_LEVEL:
                    return CMD_FUEL_LEVEL;
                case CMD_RPM:
                    return CMD_RPM;
                case CMD_SPEED:
                    return CMD_SPEED;
                case CMD_ENGINE_LOAD:
                    return CMD_ENGINE_LOAD;
                case CMD_COOLANT_TEMP:
                    return CMD_COOLANT_TEMP;
                default:
                    throw new IllegalArgumentException("Invalid command: " + request);
            }
        }
        return null;
    }
}
