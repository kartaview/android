package com.telenav.osv.utils;

import com.skobbler.ngx.SKCoordinate;

public class ComputingDistance {

    /**
     * Factor to convert degree into rad.
     */
    private static final double DEG2RADFACTOR = Math.PI / 180.0;

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
    public static double distanceBetween(final double point_A_long, final double point_A_lat, final double point_B_long,
                                         final double point_B_lat) {
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
        return Math.sqrt(dx * dx + dy * dy);
    }
}