package com.telenav.osv.utils;

import java.text.DecimalFormat;
import com.skobbler.ngx.SKCoordinate;


public class ComputingDistance {

    /**
     * the number of feet in a mile
     */
    public static final int FEETINMILE = 5280;

    /**
     * the number of km/h in 1 m/s
     */
    public static final double SPEED_IN_KILOMETRES = 3.6;

    /**
     * number of mi/h in 1 m/s
     */
    public static final double SPEED_IN_MILES = 2.2369;

    /**
     * the number of yards in a mile
     */
    public static final int YARDSINMILE = 1760;

    /**
     * the limit of feet where the distance should be converted into miles
     */
    public static final int LIMIT_TO_MILES = 1500;

    /**
     * The pattern used to format the distance shown in post navigation screen
     */
    public static final String ONE_DECIMAL_DISTANCE_FORMATTER_PATTERN = "0.0";

    /**
     * Factor to convert rad into degree.
     */
    private static final double RAD2DEGFACTOR = 180.0 / Math.PI;

    /**
     * Factor to convert degree into rad.
     */
    private static final double DEG2RADFACTOR = Math.PI / 180.0;

    /**
     * Factor <i>pi</i> / 2.
     */
    private static final double KHalfPi = (Math.PI / 2d);

    /**
     * the number of meters in a km
     */
    private static final int METERSINKM = 1000;

    /**
     * the number of meters in a mile
     */
    private static final double METERSINMILE = 1609.34;

    /**
     * converter from meters to yards
     */
    private static final double METERSTOYARDS = 1.0936133;

    /**
     * the number of feet in a yard
     */
    private static final int FEETINYARD = 3;

    /**
     * converter from meters to feet
     */
    private static final double METERSTOFEET = 3.2808399;

    /**
     * The radius of the earth as arithmetic mean of small and large semi-axis.
     * this radius is inaccurate! Oblateness not considered. WGS84 should
     * be 6371000.8
     */
    private static final double KEarthRadius = 6367444;

    /**
     * Radius of the large equatorial axis a refering to the WGS84 ellipsoid
     * (1979).
     */
    private static final double KEquatorialRadius = 6378137.0d;

    /**
     * Radius of the small polar axis b refering to the WGS84 ellipsoid (1979).
     */
    private static final double KPolarRadius = 6356752.3142d;

    /**
     * Convenience method for {@link #distanceBetween(double, double, double, double)}
     * @return distance on surface in meter
     */
    public static double distanceBetween(final SKCoordinate point_A, final SKCoordinate point_B) {
        return distanceBetween(point_A.getLongitude(), point_A.getLatitude(), point_B.getLongitude(), point_B.getLatitude());
    }

    /**
     * Calculates the distance between the points (point_A_long, point_A_lat)
     * and (point_B_long, point_B_lat). We assume that the earth is an ellipsoid
     * with two different semi-axis. The curvature of the earth is not
     * considered.
     * <p/>
     * This method should be used for distance calculation if the points are
     * closer than 5 km. The calculation needs less power than the SphereLength
     * method and is more accurate for small distances.
     * <p/>
     * On large distances, the missing correction of earth curvature will
     * influence the result.
     * @param point_A_long the longitude of the first point in decimal degree
     * @param point_A_lat latitude of the first point in decimal degree
     * @param point_B_long longitude of the second point in decimal degree
     * @param point_B_lat latitude of the seconds point in decimal degree
     * @return distance on surface in meter
     */
    public static double distanceBetween(final double point_A_long, final double point_A_lat,
                                         final double point_B_long, final double point_B_lat) {
        // calculates angle between latitude
        final double deltaLat = (point_B_lat - point_A_lat) * DEG2RADFACTOR;
        // calculates angle between longitude
        final double deltaLon = (point_B_long - point_A_long) * DEG2RADFACTOR;
        // calculates the earth readius at the specific latitude
        final double currentRadius = KEquatorialRadius * Math.cos(point_A_lat * DEG2RADFACTOR);
        // multiplies the laitude by the smaller polar radius
        final double meter_Y = KPolarRadius * deltaLat;
        // multiplies the longitude by the current earth radius.
        final double meter_X = currentRadius * deltaLon;
        // calculates the distance between the two points assuming that the
        // curvature
        // is equal in X and Y using pythagos' theorem.
        return Math.sqrt(meter_X * meter_X + meter_Y * meter_Y);
    }

    /**
     * converts a distance given in meters to the according distance in yards
     * @param distanceInMeters
     * @return
     */
    public static double distanceInYards(double distanceInMeters) {
        if (distanceInMeters != -1) {
            return distanceInMeters * METERSTOYARDS;
        } else {
            return distanceInMeters;
        }
    }

    /**
     * converts a distance given in meters to the according distance in feet
     * @param distanceInMeters
     * @return
     */
    public static double distanceInFeet(double distanceInMeters) {
        if (distanceInMeters != -1) {
            return distanceInMeters * METERSTOYARDS * FEETINYARD;
        } else {
            return distanceInMeters;
        }
    }

    /**
     * converts the distance given in feet/yards/miles/km to the according distance in meters
     * @param distance
     * @param initialUnit: 0 - feet
     * 1 - yards
     * 2 - mile
     * 3 - km
     * @return distance in meters
     */
    public static double distanceInMeters(double distance, int initialUnit) {
        if (distance != -1) {
            switch (initialUnit) {
                case 0:
                    return distance /= METERSTOFEET;
                case 1:
                    return distance /= METERSTOYARDS;
                case 2:
                    return distance *= METERSINMILE;
                case 3:
                    return distance *= METERSINKM;
            }
        }
        return distance;
    }


    /**
     * Formats the distance using the input pattern
     * @param distanceToFormat the distance to be formatted
     * @param formatPattern the pattern used to format
     * @return the String formatted distance
     */
    public static String formatDistance(double distanceToFormat, String formatPattern) {
        if (formatPattern != null) {
            String formattedDistance = new DecimalFormat(formatPattern).format(distanceToFormat);
            // the values we return have to be contain '.', not ',' (ex: 8.5 not
            // 8,5), because they have to be parsed further on
            return formattedDistance.replace(',', '.');
        } else {
            return String.valueOf(Math.round(distanceToFormat * 10) / 10).replace(',', '.');
        }
    }


    /**
     * converts radians into degrees
     * @param radians
     * @return
     */
    public static double toDegrees(final double radians) {
        return radians * RAD2DEGFACTOR;
    }


    /**
     * Convenience method for {@link #getAirDistance(double, double, double, double)}
     * @return distance on surface in meter
     */
    public static double getAirDistance(final SKCoordinate point_A, final SKCoordinate point_B) {
        return getAirDistance(point_A.getLongitude(), point_A.getLatitude(), point_B.getLongitude(), point_B.getLatitude());
    }

    /**
     * Returns the distance between the two given spheric coordinate pairs
     * (point_A_long, point_A_lat) and (point_B_long, point_B_lat). The distance
     * will be delivered in meter considering that the earth is a sphere with a
     * predefined radius.
     * <p/>
     * The distance is calculated according to the spheric trigonometry. The
     * accuracy of the distance is about 200 to 250 ppm if the distance is less
     * than 200 km. This is a maximum of 2,5 meter failure in 1 km.
     * @param point_A_long longitude of the first point in decimal degree
     * @param point_A_lat latitude of the first point in decimal degree
     * @param point_B_long longitude of the second point in decimal degree
     * @param point_B_lat latitude of the seconds point in decimal degree
     * @return distance on surface in meter
     */
    private static double getAirDistance(final double point_A_long, final double point_A_lat, final double point_B_long,
                                         final double point_B_lat) {
        // Convert degrees to radians
        final double pA_long_RAD = (point_A_long * DEG2RADFACTOR);
        final double pA_lat_RAD = (point_A_lat * DEG2RADFACTOR);
        final double pB_long_RAD = (point_B_long * DEG2RADFACTOR);
        final double pB_lat_RAD = (point_B_lat * DEG2RADFACTOR);
        
        /*
         * Side a and b are the angles from the pole to the latitude (=> 90 -
         * latitude). Gamma is the angle between the longitudes measured at the
         * pole. The missing side c can be calculated with the given sides a and
         * b and the angle gamma. Therefore the spherical law of cosines is
         * used.
         */
        final double cosb = Math.cos(KHalfPi - pA_lat_RAD);
        final double cosa = Math.cos(KHalfPi - pB_lat_RAD);
        final double cosGamma = Math.cos(pB_long_RAD - pA_long_RAD);
        final double sina = Math.sin(KHalfPi - pA_lat_RAD);
        final double sinb = Math.sin(KHalfPi - pB_lat_RAD);
        
        /*
         * Law of cosines for the sides (Spherical trigonometry) cos(c) = cos(a)
         * * cos(b) + sin(a) * sin(b) * cos(Gamma)
         */
        double cosc = cosa * cosb + sina * sinb * cosGamma;

        // Limit the cosine from 0 to 180 degrees.
        if (cosc < -1) {
            cosc = -1;
        }
        if (cosc > 1) {
            cosc = 1;
        }

        // Calculate the angle in radians for the distance
        final double side_c = Math.acos(cosc);

        // return the length in meter by multiplying the angle with
        // the standard sphere radius.
        return Math.max(0.0, KEarthRadius * side_c);
    }

    private static double sqr(double x) {
        return x * x;
    }

    private static double dist2(double vx, double vy, double wx, double wy) {
        return sqr(vx - wx) + sqr(vy - wy);
    }

    private static double distToSegmentSquared(SKCoordinate p, SKCoordinate v, SKCoordinate w) {
        double l2 = dist2(v.getLongitude(), v.getLatitude(), w.getLongitude(), w.getLatitude());
        if (l2 == 0) return dist2(p.getLongitude(), p.getLatitude(), v.getLongitude(), v.getLatitude());
        double t = ((p.getLongitude() - v.getLongitude()) * (w.getLongitude() - v.getLongitude()) + (p.getLatitude() - v.getLatitude()) * (w.getLatitude() - v.getLatitude())) / l2;
        t = Math.max(0, Math.min(1, t));
        return dist2(p.getLongitude(), p.getLatitude(), v.getLongitude() + t * (w.getLongitude() - v.getLongitude()),
                v.getLatitude() + t * (w.getLatitude() - v.getLatitude()));
    }

    public static double getDistanceFromLine(SKCoordinate position, SKCoordinate start, SKCoordinate end) {
        return Math.sqrt(distToSegmentSquared(position, start, end));
    }

    public static double getDistanceFromSegment(SKCoordinate origin, SKCoordinate pointA, SKCoordinate pointB) {
        SKCoordinate dap = new SKCoordinate(origin.getLatitude() - pointA.getLatitude(), origin.getLongitude() - pointA.getLongitude());
        SKCoordinate dab = new SKCoordinate(pointB.getLatitude() - pointA.getLatitude(), pointB.getLongitude() - pointA.getLongitude());
        double dot = dap.getLongitude() * dab.getLongitude() + dap.getLatitude() * dab.getLatitude();

        double squareLength = dab.getLongitude() * dab.getLongitude() + dab.getLatitude() * dab.getLatitude();
        double param = dot / squareLength;


        SKCoordinate nearestPoint = new SKCoordinate();
        if (param < 0 || (pointA.getLongitude() == pointB.getLongitude() && pointA.getLatitude() == pointB.getLatitude())) {
            nearestPoint.setLongitude(pointA.getLongitude());
            nearestPoint.setLatitude(pointA.getLatitude());
        } else if (param > 1) {
            nearestPoint.setLongitude(pointB.getLongitude());
            nearestPoint.setLatitude(pointB.getLatitude());
        } else {
            nearestPoint.setLongitude(pointA.getLongitude() + param * dab.getLongitude());
            nearestPoint.setLatitude(pointA.getLatitude() + param * dab.getLatitude());
        }

        double dx = origin.getLongitude() - nearestPoint.getLongitude();
        double dy = origin.getLatitude() - nearestPoint.getLatitude();
        double distance = Math.sqrt(dx * dx + dy * dy);

//        return nearestPoint;
        return distance;
    }
}