package com.skobbler.sensorlib.sign;

/**
 * Created by Kalman on 7/8/2015.
 */
public class SignType {

    private String fileName = "";

    private int signType;

    public enum enSignType {

        //        eStopSign(0x0001), //stop sign
//        eGiveWay(0x0002), //stop sign
//        eNoLeft(0x0003), //stop sign
//        eHwSign(0x0004), //stop sign
//        eSpeedLimit(0x01F0), //speed limit with value unknown
//        eSpeedLimit5(0x0110), //speed limit 5 km/h
//        eSpeedLimit10(0x0120), //speed limit 10 km/h
//        eSpeedLimit20(0x0130), //speed limit 20 km/h
//        eSpeedLimit30(0x0140), //speed limit 30 km/h
//        eSpeedLimit40(0x0150), //speed limit 40 km/h
//        eSpeedLimit50(0x0160), //speed limit 50 km/h
//        eSpeedLimit60(0x0170), //speed limit 60 km/h
//        eSpeedLimit70(0x0180), //speed limit 70 km/h
//        eSpeedLimit80(0x0190), //speed limit 80 km/h
//        eSpeedLimit100(0x01A0), //speed limit 100 km/h
//        eSpeedLimit110(0x01B0), //speed limit 110 km/h
//        eSpeedLimit120(0x01C0), //speed limit 120 km/h
//        eSpeedLimit130(0x01D0), //speed limit 130 km/h
//        eRegulatorySign(0x0100), // unknown regulatory sign
//        eRegLeft(0x0101), // regulatory left
//        eRegLeftNow(0x0102), // regulatory left now
//        eRegRight(0x0103), // regulatory right
//        eRegRightNow(0x0104), // regulatory right now
//        eRegRightLeft(0x0105), // regulatory rignt or left
//        eRegStraight(0x0106), // regulatory straight
//        eRegStraightLeft(0x0107), // regulatory straight or left
//        eRegStraightRight(0x0108), // regulatory straight or right
//        eSpeedLimitUSConstruction(0x02F0),
//        eSpeedLimitUSConstruction25(0x0210),
//        eSpeedLimitUSConstruction35(0x0220),
//        eSpeedLimitUSConstruction40(0x0230),
//        eOtherSign(0x1000), // other type of sign
        eUndefinedSign(0x0000), // undefined sign type

        eStopSign(0x0021),
        eGiveWay(0x0022),

        eSpeedLimit5(0x0041, 5),
        eSpeedLimit10(0x0042, 10),
        eSpeedLimit20(0x0043, 20),
        eSpeedLimit30(0x0044, 30),
        eSpeedLimit40(0x0045, 40),
        eSpeedLimit50(0x0046, 50),
        eSpeedLimit60(0x0047, 60),
        eSpeedLimit70(0x0048, 70),
        eSpeedLimit80(0x0049, 80),
        eSpeedLimit90(0x004A, 90),
        eSpeedLimit100(0x004B, 100),
        eSpeedLimit110(0x004C, 110),
        eSpeedLimit120(0x004D, 120),
        eSpeedLimit130(0x004E, 130),

        eSpeedLimit5US(0x0101, 5, true),
        eSpeedLimit10US(0x0102, 10, true),
        eSpeedLimit15US(0x0103, 15, true),
        eSpeedLimit20US(0x0104, 20, true),
        eSpeedLimit25US(0x0105, 25, true),
        eSpeedLimit30US(0x0106, 30, true),
        eSpeedLimit35US(0x0107, 35, true),
        eSpeedLimit40US(0x0108, 40, true),
        eSpeedLimit45US(0x0109, 45, true),
        eSpeedLimit50US(0x010A, 50, true),
        eSpeedLimit55US(0x010B, 55, true),
        eSpeedLimit60US(0x010C, 60, true),
        eSpeedLimit65US(0x010D, 65, true),
        eSpeedLimit70US(0x010E, 70, true),
        eSpeedLimit75US(0x010F, 75, true),
        eSpeedLimit80US(0x0110, 80, true),

        eRegulatoryLeft(0x0201),
        eRegulatoryLeftNow(0x0202),
        eRegulatoryRight(0x0203),
        eRegulatoryRightNow(0x0204),
        eRegulatoryRightLeft(0x0205),
        eRegulatoryStraight(0x0206),
        eRegulatoryStraightLeft(0x0207),
        eRegulatoryStraightRight(0x0208),

        eTurnRestrictionLeft(0x0401),
        eTurnRestrictionRight(0x0402),
        eTurnRestrictionUTurn(0x0403),
        eTurnRestrictionLeftUTurn(0x040);

        private final int speed;

        private final String file;

        private final boolean isUS;

        private final int value;


        enSignType(int value) {
            this.value = value;
            this.speed = -1;
            this.isUS = false;
            this.file = "";
        }

        enSignType(int value, int speed) {
            this.value = value;
            this.speed = speed;
            this.isUS = false;
            this.file = "speed_limit_" + speed + ".png";
        }

        enSignType(int value, int speed, boolean isUS) {
            this.value = value;
            this.speed = speed;
            this.isUS = isUS;
            this.file = "speed_Limit_" + speed + "_US" + ".png";
        }

        public static enSignType forInt(int value) {
            for (enSignType type : values()) {
                if (type.value == value) {
                    return type;
                }
            }
            return eUndefinedSign;
        }

        public int getValue() {
            return value;
        }

        public int getSpeedLimit() {
            return speed;
        }

        public boolean isUS() {
            return isUS;
        }

        public boolean isSpeedLimit() {
            return speed != -1;
        }

        public String getFile() {
            return file;
        }

        @Override
        public String toString() {
            return this.name() + " - " + value;
        }
    }

    public SignType() {
    }

    public SignType(int signType, String fileName) {
        this.signType = signType;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "" + signType + " ,fileName = " + fileName + "\n";
    }

    public int getSignType() {
        return signType;
    }

    public void setSignType(int type) {
        this.signType = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
