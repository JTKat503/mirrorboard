package com.teamcreators.mirrorboard.network;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;

/**
 * Annotations on the interface methods and its parameters
 * indicate how a request will be handled.
 *
 * @author Jianwei Li
 */
public interface ApiService {

    @POST("send")
    Call<String> sendRemoteMessage(
            @HeaderMap HashMap<String, String> headers,
            @Body String remoteBody
    );
}
