package com.telenav.osv.manager.network.parser;

import org.json.JSONObject;
import com.telenav.osv.item.network.IssueData;

/**
 * Created by kalmanb on 8/3/17.
 */
public class IssueCreationParser extends ApiResponseParser<IssueData> {
    @Override
    public IssueData getHolder() {
        return new IssueData();
    }

    public IssueData parse(String json) {
        IssueData issueData = super.parse(json);

        try {
            JSONObject response = new JSONObject(json);
            JSONObject osv = response.getJSONObject("osv");
            JSONObject issue = osv.getJSONObject("issue");
            int id = issue.getInt("id");
            issueData.setOnlineID(id);
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return issueData;
    }
}
