package com.abtahiapp.imagerecognitionbytensorflowlite
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface KnowledgeGraphApiService {

    @GET("v1/entities:search")
    fun getEntityDetails(
        @Query("query") query: String,
        @Query("key") apiKey: String
    ): Call<KnowledgeGraphResponse>
}
