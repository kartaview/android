package com.telenav.osv.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import com.telenav.osv.data.score.model.ScoreHistory;
import com.telenav.osv.obd.manager.ObdManager;

/**
 * @author cameliao
 */

public class ConverterHelperTest {

    @Test
    public void getScoreBreakdownDetailsWithSingleResult() {
        Map<Integer, ScoreHistory> scoreHistoryMap = new HashMap<>();
        String firstKey = UUID.randomUUID().toString();
        String secondKey = UUID.randomUUID().toString();
        ScoreHistory scoreHistoryFirst = new ScoreHistory(firstKey, 1, 2, 3);
        ScoreHistory scoreHistorySecond = new ScoreHistory(secondKey, 2, 1, 1);
        scoreHistoryMap.put(scoreHistoryFirst.getCoverage(), scoreHistoryFirst);
        scoreHistoryMap.put(scoreHistorySecond.getCoverage(), scoreHistorySecond);

        //the expected key in the map is 10
        int expectedKey = 10;
        int photoCount = scoreHistoryFirst.getPhotoCount() + scoreHistorySecond.getPhotoCount();
        int photoCountObd = scoreHistoryFirst.getObdPhotoCount() + scoreHistorySecond.getObdPhotoCount();
        Map<Integer, ScoreHistory> convertedScoreHistoryMap = ConverterHelper.getScoreBreakdownDetails(scoreHistoryMap);
        Assert.assertTrue(convertedScoreHistoryMap.containsKey(expectedKey));
        Assert.assertEquals(scoreHistoryFirst.getPhotoCount(), convertedScoreHistoryMap.get(expectedKey).getPhotoCount());
        Assert.assertEquals(photoCountObd, convertedScoreHistoryMap.get(expectedKey).getObdPhotoCount());
    }

    @Test
    public void getScoreBreakdownDetailsWithMultipleResults() {
        Map<Integer, ScoreHistory> scoreHistoryMap = new HashMap<>();
        ScoreHistory scoreHistoryFirst = new ScoreHistory(10, 50, 0, ObdManager.ObdState.OBD_CONNECTED);
        ScoreHistory scoreHistorySecond = new ScoreHistory(0, 8, 0, ObdManager.ObdState.OBD_CONNECTED);
        scoreHistoryMap.put(scoreHistoryFirst.getCoverage(), scoreHistoryFirst);
        scoreHistoryMap.put(scoreHistorySecond.getCoverage(), scoreHistorySecond);

        //the expected keys in the map are 1,10
        int[] expectedKeys = {1, 10};
        int[] expectedPhotoCount = {50, 8};
        Map<Integer, ScoreHistory> convertedScoreHistoryMap = ConverterHelper.getScoreBreakdownDetails(scoreHistoryMap);
        Assert.assertTrue(convertedScoreHistoryMap.containsKey(expectedKeys[0]));
        Assert.assertEquals(expectedPhotoCount[0], convertedScoreHistoryMap.get(expectedKeys[0]).getPhotoCount());
        Assert.assertTrue(convertedScoreHistoryMap.containsKey(expectedKeys[1]));
        Assert.assertEquals(expectedPhotoCount[1], convertedScoreHistoryMap.get(expectedKeys[1]).getPhotoCount());
    }

    @Test
    public void getScoreBreakdownDetailsWhenTheMapParamIsEmpty() {
        Map<Integer, ScoreHistory> scoreHistoryMap = new HashMap<>();
        Assert.assertTrue(ConverterHelper.getScoreBreakdownDetails(scoreHistoryMap).isEmpty());
    }

    @Test
    public void getScoreBreakdownDetailsWhenTheMapParamIsNull() {
        Assert.assertTrue(ConverterHelper.getScoreBreakdownDetails(null).isEmpty());
    }
}