package com.telenav.osv.manager.playback.framesprovider;

import android.database.Cursor;
import com.telenav.osv.db.SequenceDB;
import com.telenav.osv.item.ImageCoordinate;
import com.telenav.osv.item.ImageFile;
import com.telenav.osv.item.OSVFile;
import com.telenav.osv.manager.playback.framesprovider.data.TrackInfo;
import com.telenav.osv.utils.Log;
import java.util.ArrayList;
import java.util.Collections;
import javax.inject.Inject;

/**
 * Concrete implementation of a {@link AbstractFramesProvider} which loads a JPEG sequence from the local device storage.
 *
 * Created by catalinj on 9/28/17.
 */
public class LocalStorageFramesProvider extends AbstractFramesProvider {

  private static final String TAG = LocalStorageFramesProvider.class.getSimpleName();

  private SequenceDB mSequenceDB;

  @Inject
  public LocalStorageFramesProvider(SequenceDB db) {
    mSequenceDB = db;
  }

  @Override
  public void fetchFrames(int sequenceId) {
    final ArrayList<ImageFile> nodes = new ArrayList<>();
    final ArrayList<ImageCoordinate> track = new ArrayList<>();

    Cursor cursor = mSequenceDB.getFrames(sequenceId);

    if (cursor == null) {
      notifyListenerOfFailure();
    }

    while (cursor != null && !cursor.isAfterLast()) {

      int index = cursor.getInt(cursor.getColumnIndex(SequenceDB.FRAME_SEQ_INDEX));
      String path = cursor.getString(cursor.getColumnIndex(SequenceDB.FRAME_FILE_PATH));
      double lat = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LAT));
      double lon = cursor.getDouble(cursor.getColumnIndex(SequenceDB.FRAME_LON));

      if (lat != 0.0 && lon != 0.0) {
        ImageCoordinate coord = new ImageCoordinate(lat, lon, index);
        track.add(coord);

        ImageFile imageFile = new ImageFile(new OSVFile(path), index, coord);
        nodes.add(imageFile);
      }
      cursor.moveToNext();
    }

    if (cursor != null) {
      cursor.close();
    }

    Collections.sort(nodes, (lhs, rhs) -> lhs.index - rhs.index);
    Collections.sort(track, (lhs, rhs) -> lhs.index - rhs.index);
    Log.d(TAG, "listImages: id=" + sequenceId + " length=" + nodes.size());

    notifyListenerOfLoadComplete(new TrackInfo(nodes, track));
  }
}
