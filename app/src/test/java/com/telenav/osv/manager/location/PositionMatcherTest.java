package com.telenav.osv.manager.location;

import android.content.Context;
import android.os.Looper;
import android.util.Log;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.map.SKBoundingBox;
import com.telenav.osv.MockUtil;
import com.telenav.osv.data.ApplicationPreferences;
import com.telenav.osv.data.Preferences;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.Polyline;
import com.telenav.osv.item.Segment;
import com.telenav.osv.manager.network.GeometryRetriever;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * tests for position matching
 * Created by Kalman on 09/05/2017.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Looper.class, Log.class})
public class PositionMatcherTest {

  private static final String TAG = "PositionMatcherTest";

  private PositionMatcher mMatcher;

  private SKCoordinate centerPosition;

  @Mock
  private Context context;

  @Before
  public void setup() throws Exception {
    PowerMockito.mockStatic(Log.class);
    MockUtil.mockMainThreadHandler();
    mMatcher = new PositionMatcher(new GeometryRetriever(context, new Preferences(new ApplicationPreferences(context))));

    String json = MockUtil.getJsonFromFile(this, "geometry.json");
    centerPosition = new SKCoordinate(46.77324496178227, 23.593161462125305);
    final SKBoundingBox requestedBB = PositionMatcher.getBoundingBoxForRegion(centerPosition, 3500);
    final SKBoundingBox newLastBB = PositionMatcher.getBoundingBoxForRegion(centerPosition, 1500);
    mMatcher.offerNewSegments(centerPosition, parseTracks(json), newLastBB, requestedBB);
  }

  private ArrayList<Polyline> parseTracks(String json) {
    ArrayList<Polyline> segments = new ArrayList<>();
    try {
      JSONObject obj = new JSONObject(json);
      final JSONArray tracks = obj.getJSONArray("currentPageItems");

      if (tracks.length() > 0) {
        for (int i = 0; i < tracks.length(); i++) {
          JSONArray track = tracks.getJSONObject(i).getJSONArray("track");
          int coverage = -1;
          try {
            coverage = tracks.getJSONObject(i).getInt("coverage");
          } catch (Exception ignored) {
            com.telenav.osv.utils.Log.d(TAG, com.telenav.osv.utils.Log.getStackTraceString(ignored));
          }
          final Polyline polyline = new Polyline(i);
          polyline.coverage = coverage;
          SKCoordinate coordinate;
          for (int j = 0; j < track.length(); j++) {
            double lat = track.getJSONArray(j).getDouble(0);
            double lon = track.getJSONArray(j).getDouble(1);
            coordinate = new ImageCoordinate(lat, lon, j);
            polyline.getNodes().add(coordinate);
          }
          segments.add(polyline);
        }
      }
    } catch (JSONException e) {
      Log.d(TAG, Log.getStackTraceString(e));
    }
    return segments;
  }

  @After
  public void tearDown() throws Exception {
    mMatcher = null;
  }

  @Test
  public void onLocationChanged() throws Exception {
    //fairly straight street with 90 degree intersection
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774813, 23.597425));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774786, 23.597314));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774771, 23.597242));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774729, 23.597114));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774683, 23.596986));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.774596, 23.596802));

    Segment segment = mMatcher.match(new SKCoordinate(46.774336, 23.596351));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 179);
    Segment previousSegment = segment;
    segment = mMatcher.match(new SKCoordinate(46.774192, 23.595995));
    Assert.assertTrue(segment.getPolyline() == previousSegment.getPolyline());

    segment = mMatcher.match(new SKCoordinate(46.774062, 23.595683));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 475);
    previousSegment = segment;

    segment = mMatcher.match(new SKCoordinate(46.773910, 23.595355));
    Assert.assertTrue(segment.getPolyline() == previousSegment.getPolyline());
    previousSegment = segment;

    segment = mMatcher.match(new SKCoordinate(46.773826, 23.594971));
    Assert.assertTrue(segment.getPolyline() == previousSegment.getPolyline());
    previousSegment = segment;

    segment = mMatcher.match(new SKCoordinate(46.773803, 23.594437));
    Assert.assertTrue(segment.getPolyline() == previousSegment.getPolyline());

    //straight street with Y ending favouring the left one
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773684, 23.592530));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773718, 23.592518));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773789, 23.592489));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773854, 23.592469));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773908, 23.592448));
    mMatcher.offerLocationForBearing(new SKCoordinate(46.773950, 23.592433));

    segment = mMatcher.match(new SKCoordinate(46.774067, 23.592388));//todo test asserts
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 463);
    previousSegment = segment;
    segment = mMatcher.match(new SKCoordinate(46.774213, 23.592324));
    Assert.assertTrue(segment.getPolyline() == previousSegment.getPolyline());

    segment = mMatcher.match(new SKCoordinate(46.774267, 23.592297));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 463);

    segment = mMatcher.match(new SKCoordinate(46.774342, 23.592252));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 590);

    segment = mMatcher.match(new SKCoordinate(46.774420, 23.592188));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 540);

    segment = mMatcher.match(new SKCoordinate(46.774483, 23.592159));
    Assert.assertTrue(segment != null && segment.getPolyline().coverage == 540);
  }

  @Test
  public void pointIsInBB() throws Exception {
  }

  @Test
  public void testBearingCalculation() throws Exception {
    //straight line to north
    SKCoordinate coord1 = new SKCoordinate(46.773232, 23.592454);
    SKCoordinate coord2 = new SKCoordinate(46.772232, 23.592454);
    SKCoordinate coord3 = new SKCoordinate(46.771232, 23.592454);
    SKCoordinate coord4 = new SKCoordinate(46.770232, 23.592454);
    SKCoordinate coord5 = new SKCoordinate(46.769232, 23.592454);
    SKCoordinate coord6 = new SKCoordinate(46.768232, 23.592454);
    mMatcher.offerLocationForBearing(coord6);
    mMatcher.offerLocationForBearing(coord5);
    mMatcher.offerLocationForBearing(coord4);
    mMatcher.offerLocationForBearing(coord3);
    mMatcher.offerLocationForBearing(coord2);
    double bearing = mMatcher.getBearing(coord1);
    //ASSERTION relative to north
    Assert.assertTrue((int) bearing == 0);

    //curved line from south to east
    coord1 = new SKCoordinate(46.769512825136815, 23.59586477279663);
    coord2 = new SKCoordinate(46.77079145838921, 23.595585823059082);
    coord3 = new SKCoordinate(46.77193044520574, 23.59590768814087);
    coord4 = new SKCoordinate(46.772863661611794, 23.597216606140137);
    coord5 = new SKCoordinate(46.7734808982238, 23.59837532043457);
    coord6 = new SKCoordinate(46.77342946210967, 23.600521087646484);
    mMatcher.offerLocationForBearing(coord1);
    mMatcher.offerLocationForBearing(coord2);
    mMatcher.offerLocationForBearing(coord3);
    mMatcher.offerLocationForBearing(coord4);
    mMatcher.offerLocationForBearing(coord5);
    bearing = mMatcher.getBearing(coord6);
    //ASSERTION relative to north
    Assert.assertTrue((int) bearing > 270 && (int) bearing < 300);

    //straight line from south to west
    coord1 = new SKCoordinate(46.769512825136815, 23.59586477279663);
    coord2 = new SKCoordinate(46.769512825136815, 23.59486477279663);
    coord3 = new SKCoordinate(46.769512825136815, 23.59386477279663);
    coord4 = new SKCoordinate(46.769512825136815, 23.59286477279663);
    coord5 = new SKCoordinate(46.769512825136815, 23.59186477279663);
    coord6 = new SKCoordinate(46.769512825136815, 23.59086477279663);
    mMatcher.offerLocationForBearing(coord1);
    mMatcher.offerLocationForBearing(coord2);
    mMatcher.offerLocationForBearing(coord3);
    mMatcher.offerLocationForBearing(coord4);
    mMatcher.offerLocationForBearing(coord5);
    bearing = mMatcher.getBearing(coord6);
    //ASSERTION relative to north
    Assert.assertTrue((int) bearing == 90);
  }

  @Test
  public void isCoverageForLocation() throws Exception {
    SKCoordinate coordinate2 = new SKCoordinate(46.608171, 23.697070);
    Assert.assertTrue(mMatcher.isCoverageForLocation(centerPosition));
    Assert.assertTrue(!mMatcher.isCoverageForLocation(coordinate2));//other position outside bb
  }
}