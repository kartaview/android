package com.telenav.osv.manager.network.parser;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import android.os.Looper;
import android.util.Log;
import com.telenav.osv.item.network.TrackCollection;

/**
 * Created by kalmanb on 8/1/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Looper.class, Log.class})
public class DriverTracksParserTest extends JsonParserTest {

    @Before
    public void setup() {
        PowerMockito.mockStatic(Log.class);
    }
    @Test
    public void parse() throws Exception {
        String json = readJson();
        TrackCollection tracks = new DriverTracksParser().parse(json);
    }

    @Override
    protected String getFileName() {
        return "driverTracks.json";
    }
}