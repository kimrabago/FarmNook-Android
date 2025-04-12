package com.ucb.capstone.farmnook.data.service

import com.ucb.capstone.farmnook.data.model.algo.RecommendationRequest
import com.ucb.capstone.farmnook.data.model.algo.RecommendationResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {
    @POST("/recommend")
    fun getRecommendation(@Body request: RecommendationRequest): Call<RecommendationResponse>
}