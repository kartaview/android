package com.telenav.osv.item.network;

/**
 * Created by kalmanb on 8/3/17.
 */
public class ApiResponse {

  private int apiCode = -1;

  private String apiMessage = "Unknown";

  private int httpCode = -1;

  private String httpMessage = "Unknown";

  public int getApiCode() {
    return apiCode;
  }

  public void setApiCode(int apiCode) {
    this.apiCode = apiCode;
  }

  public String getApiMessage() {
    return apiMessage;
  }

  public void setApiMessage(String apiMessage) {
    this.apiMessage = apiMessage;
  }

  public int getHttpCode() {
    return httpCode;
  }

  public void setHttpCode(int httpCode) {
    this.httpCode = httpCode;
  }

  public String getHttpMessage() {
    return httpMessage;
  }

  public void setHttpMessage(String httpMessage) {
    this.httpMessage = httpMessage;
  }

  @Override
  public String toString() {
    return "ApiResponse{" + "apiCode=" + apiCode + ", apiMessage='" + apiMessage + '\'' + ", httpCode=" + httpCode + ", httpMessage='" +
        httpMessage + '\'' + '}';
  }
}
