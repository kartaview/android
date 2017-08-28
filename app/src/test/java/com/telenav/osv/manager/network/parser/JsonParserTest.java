package com.telenav.osv.manager.network.parser;

import com.telenav.osv.MockUtil;

/**
 * Created by kalmanb on 8/1/17.
 */
public abstract class JsonParserTest {

  protected abstract String getFileName();

  public String readJson() {
    String filename = getFileName();
    return MockUtil.getJsonFromFile(this, filename);
  }
}
