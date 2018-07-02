package com.cry.downloaddemo;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.HEAD;
import retrofit2.http.Header;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

/**
 * Created by a2957 on 2018/7/2.
 */

public interface DownloadApi {
    @GET
    @Streaming
    Flowable<Response<ResponseBody>> download(
            @Header("Range") String range, @Url String url);

    @HEAD
    Observable<Response<Void>> HEAD(
            @Header("Range") String range, @Url String url);

    @HEAD
    Observable<Response<Void>> HEAD_WithIfRange(
            @Header("Range") final String range,
            @Header("If-Range") final String lastModify,
            @Url String url);

    @GET
    Observable<Response<Void>> GET_withIfRange(
            @Header("Range") final String range,
            @Header("If-Range") final String lastModify,
            @Url String url);
}
