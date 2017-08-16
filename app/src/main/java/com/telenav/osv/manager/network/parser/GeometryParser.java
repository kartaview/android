package com.telenav.osv.manager.network.parser;

import org.json.JSONArray;
import org.json.JSONObject;
import com.skobbler.ngx.SKCoordinate;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.network.GeometryCollection;
import com.telenav.osv.utils.Log;

/**
 * json parser for geometry
 * Created by kalmanb on 8/3/17.
 */
public class GeometryParser extends ApiResponseParser<GeometryCollection> {

    private static final String TAG = "GeometryParser";

    private static final int ONLINE_SEGMENTS_ID_MODIFIER = 100000;

    @Override
    public GeometryCollection getHolder() {
        return new GeometryCollection();
    }

    public GeometryCollection parse(String json) {
        GeometryCollection collectionData = super.parse(json);
        try {
            if (json != null && !json.isEmpty()) {
                JSONObject obj = new JSONObject(json);
                final JSONArray tracks = obj.getJSONArray("currentPageItems");
                final int totalTracks = obj.getJSONArray("totalFilteredItems").getInt(0);

                Log.d(TAG, "parse: processing");
                try {
                    final int alreadyDisplayed = ONLINE_SEGMENTS_ID_MODIFIER;
                    Log.d(TAG, "parse: number of segments = " + tracks.length());
                    Log.d(TAG, "parse: totalSegments = " + totalTracks + " , page = " + 1 + " , already displayed = " + alreadyDisplayed);
                    if (tracks.length() > 0) {
                        for (int i = alreadyDisplayed; i < alreadyDisplayed + tracks.length(); i++) {
                            JSONArray track = tracks.getJSONObject(i - alreadyDisplayed).getJSONArray("track");
                            int coverage = -1;
                            try {
                                coverage = tracks.getJSONObject(i - alreadyDisplayed).getInt("coverage");
                            } catch (Exception ignored) {}
                            if (coverage == 0) {
                                continue;
                            }
                            final Polyline polyline = new Polyline(i);
                            polyline.coverage = coverage;
                            SKCoordinate coordinate;
                            for (int j = 0; j < track.length(); j++) {
                                if (Thread.interrupted()) {
                                    return collectionData;
                                }
                                double lat = track.getJSONArray(j).getDouble(0);
                                double lon = track.getJSONArray(j).getDouble(1);
                                coordinate = new ImageCoordinate(lat, lon, j);
                                polyline.getNodes().add(coordinate);
                            }
                            collectionData.getSegmentList().add(polyline);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            } else {
                Log.d(TAG, "parse: request response is empty: " + json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return collectionData;
    }
}
