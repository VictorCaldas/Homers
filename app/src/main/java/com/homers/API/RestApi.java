package com.homers.API;


import retrofit.Callback;
import retrofit.client.Response;
import retrofit.http.GET;


public interface RestApi {

    @GET("/rotas")
    void getRotas(
            Callback<Response> callback);
}
