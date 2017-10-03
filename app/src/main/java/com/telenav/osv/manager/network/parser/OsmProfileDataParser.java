package com.telenav.osv.manager.network.parser;

import com.telenav.osv.item.network.OsmProfileData;
import com.telenav.osv.utils.Log;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Created by kalmanb on 8/3/17.
 */
public class OsmProfileDataParser {

  private static final String TAG = "OsmProfileDataParser";

  public OsmProfileData parse(String xml) {
    OsmProfileData profileData = new OsmProfileData();
    try {
      InputStream is = new ByteArrayInputStream(xml.getBytes("UTF-8"));
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(is);
      doc.getDocumentElement().normalize();

      NodeList nodeList = doc.getElementsByTagName("user");
      for (int i = 0; i < nodeList.getLength(); i++) {
        Node node = nodeList.item(i);
        Element fstElmnt = (Element) node;
        NodeList list = fstElmnt.getElementsByTagName("img");

        for (int j = 0; j < list.getLength(); j++) {
          Node node2 = list.item(j);
          profileData.setProfilePictureUrl(((Element) node2).getAttribute("href"));
        }
      }
    } catch (Exception e) {
      Log.d(TAG, Log.getStackTraceString(e));
    }

    return profileData;
  }
}
