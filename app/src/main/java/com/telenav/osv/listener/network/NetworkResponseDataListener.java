package com.telenav.osv.listener.network;

import com.telenav.osv.item.network.ApiResponse;

/**
 * Created by kalmanb on 8/3/17.
 */
public interface NetworkResponseDataListener<T extends ApiResponse> {

    /**
     * Status code for a successful request.
     */
    public static final int HTTP_OK = 200;

    /**
     * Status code for a successful request with no content information.
     * @since 1.11
     */
    public static final int HTTP_NO_CONTENT = 204;

    /**
     * Status code for a resource corresponding to any one of a set of representations.
     */
    public static final int HTTP_MULTIPLE_CHOICES = 300;

    /**
     * Status code for a resource that has permanently moved to a new URI.
     */
    public static final int HTTP_MOVED_PERMANENTLY = 301;

    /**
     * Status code for a resource that has temporarily moved to a new URI.
     */
    public static final int HTTP_FOUND = 302;

    /**
     * Status code for a resource that has moved to a new URI and should be retrieved using GET.
     */
    public static final int HTTP_SEE_OTHER = 303;

    /**
     * Status code for a resource that access is allowed but the document has not been modified.
     */
    public static final int HTTP_NOT_MODIFIED = 304;

    /**
     * Status code for a resource that has temporarily moved to a new URI.
     */
    public static final int HTTP_TEMPORARY_REDIRECT = 307;

    /**
     * Status code for a request that requires user authentication.
     */
    public static final int HTTP_BAD_REQUEST = 400;

    /**
     * Status code for a request that requires user authentication.
     */
    public static final int HTTP_UNAUTHORIZED = 401;

    /**
     * Status code for a server that understood the request, but is refusing to fulfill it.
     */
    public static final int HTTP_FORBIDDEN = 403;

    /**
     * Status code for a server that has not found anything matching the Request-URI.
     */
    public static final int HTTP_NOT_FOUND = 404;

    /**
     * Status code for a request that could not be completed due to a timeout.
     */
    public static final int HTTP_TIMEOUT = 409;

    /**
     * Status code for a request that could not be completed due to a resource conflict.
     */
    public static final int HTTP_CONFLICT = 409;

    /**
     * Status code for an internal server error.
     */
    public static final int HTTP_SERVER_ERROR = 500;

    /**
     * Status code for a bad gateway.
     * @since 1.16
     */
    public static final int HTTP_BAD_GATEWAY = 502;

    /**
     * Status code for a service that is unavailable on the server.
     */
    public static final int HTTP_SERVICE_UNAVAILABLE = 503;

    public static final int API_SUCCESS = 600;

    public static final int API_EMPTY_RESPONSE = 601;

    public static final int API_INCIDENTS = 602;

    public static final int API_ARGUMENT_MISSING = 610;

    public static final int API_ARGUMENT_WRONG_TYPE = 611;

    public static final int API_ARGUMENT_OUT_OF_RANGE = 612;

    public static final int API_OPERATION_NOT_ALLOWED = 618;

    public static final int API_DUPLICATE_ENTRY = 660;

    public static final int API_INCORRECT_STATUS = 671;

    public static final int API_UNEXPECTED_SERVER_ERROR = 690;

    /**
     * Error used when the server error is unknown
     */
    public static final int API_EXCEPTIONAL_FAILURE = -115;

    void requestFailed(int status, T details);

    void requestFinished(int status, T details);
}
