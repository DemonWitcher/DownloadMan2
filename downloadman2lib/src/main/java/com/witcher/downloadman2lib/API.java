package com.witcher.downloadman2lib;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

public interface API {

    @Streaming
    @GET
    Call<ResponseBody> download(@Header("Range") String range, @Url String url);
    @HEAD
    Call<Void> getHttpHeader(@Url String url);

}
