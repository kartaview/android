package com.telenav.osv;

import java.util.Locale;
import org.junit.Test;

/**
 * Example local unit test, which will execute on the development machine (host).
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void testLocaleParsing() throws Exception {
        Locale locale = new Locale("", "us", "");
//        Log.d("test", "testLocaleParsing: " + locale.getDisplayCountry());
        assert !locale.getDisplayCountry().equals("") && locale.getDisplayCountry() != null;
    }
}