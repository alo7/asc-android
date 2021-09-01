package com.alo7.android.ascsample;

import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * @author haiyue.meng
 */
public interface APIService {
    @GET("system/token")
    Observable<Credential> getToken(
            @Query("app_id") String appId, @Query("app_secret") String appSecret);
}
