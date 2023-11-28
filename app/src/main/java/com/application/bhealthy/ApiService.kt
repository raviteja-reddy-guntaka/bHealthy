package com.application.bhealthy

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

//?units=imperial&origin=$encodedStartAddress&destination=$encodedEndAddress&departure_time=now&mode=driving&key=$apiKey

interface ApiService {
    @GET("api/distancematrix/json")
    fun getDistanceMatrix(@Query("origins") origins: String, @Query("destinations") destinations: String,
                          @Query("departure_time") departure_time: String, @Query("mode") mode: String,
                          @Query("key") key: String): Call<JsonObject>
//                          @Query("units") units: String, @Query("key") key: String): Call<JsonObject>

//    @GET("api/directions/json")
//    fun getDirections(@Query("origin") origin: String, @Query("destination") destination: String,
//                      @Query("departure_time") departure_time: String, @Query("mode") mode: String
//                      , @Query("key") key: String): Call<JsonObject>
}