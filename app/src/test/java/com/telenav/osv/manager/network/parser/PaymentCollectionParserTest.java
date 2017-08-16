package com.telenav.osv.manager.network.parser;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import android.os.Looper;
import android.util.Log;
import com.telenav.osv.item.network.PaymentCollection;

/**
 *
 * Created by kalmanb on 8/1/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Looper.class, Log.class})
public class PaymentCollectionParserTest extends JsonParserTest {

    @Before
    public void setup(){
        PowerMockito.mockStatic(Log.class);
    }
    @Test
    public void parse() throws Exception {
        String json = readJson();
        PaymentCollection payments = new PaymentCollectionParser().parse(json);
        Assert.assertTrue(payments.getTotalFilteredItems() != 0);
        Assert.assertTrue(payments.getCurrency().length() > 0);
        Assert.assertTrue(payments.getPaymentList().size() > 0);
        Assert.assertTrue(payments.getPaymentList().get(0).getDistance() > 0);
    }

    @Override
    protected String getFileName() {
        return "driverPayments.json";
    }
}