package com.mtnfog.philter.service;

import com.mtnfog.philter.model.Status;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface PhilterService {

	@POST("/filter")
	Call<String> filter(@Body String text);
	
	@POST("/detect")
	Call<Double> detect(@Body String text);
	
	@GET("/status")
	Call<Status> status();

}
