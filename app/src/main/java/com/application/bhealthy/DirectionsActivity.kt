package com.application.bhealthy

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DirectionsActivity : AppCompatActivity() {

    private lateinit var retrofit: Retrofit
    private lateinit var apiService: ApiService
    val apiKey = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_directions)

        retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        // Create the ApiService
        apiService = retrofit.create(ApiService::class.java)

        val originText = findViewById<EditText>(R.id.originText)
        val destinationText = findViewById<EditText>(R.id.destText)
        val avgSpeedText = findViewById<TextView>(R.id.avgSpeedText)
        val trafficSpeedText = findViewById<TextView>(R.id.trafficSpeedText)

        // Measure heart rate button
        val findDirectionsButton = findViewById<Button>(R.id.getDirections)
        findDirectionsButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val origin = originText.text.toString()
                    val destination = destinationText.text.toString()
                    val call = apiService.getDistanceMatrix(origin, destination, "now",
                        "driving", apiKey)
                    if (origin.isEmpty() || destination.isEmpty()) {
                        avgSpeedText.text = "Origin or Destination cannot be empty"
                        return@launch
                    }
                    call.enqueue(object : Callback<JsonObject> {
                        override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                            if (response.isSuccessful) {
                                val jsonResponse = response.body()
                                // Check if the JSON response is not null
                                if (jsonResponse != null) {
//                                    println(jsonResponse)
                                    avgSpeedText.text = jsonResponse.toString()

                                    val distanceValue = jsonResponse.getAsJsonArray("rows")[0]
                                        .asJsonObject
                                        .getAsJsonArray("elements")[0]
                                        .asJsonObject
                                        .getAsJsonObject("distance")
                                        .get("value")
                                        .asInt

                                    val durationNormal = jsonResponse.getAsJsonArray("rows")[0]
                                        .asJsonObject
                                        .getAsJsonArray("elements")[0]
                                        .asJsonObject
                                        .getAsJsonObject("duration")
                                        .get("value")
                                        .asInt

                                    val durationInTraffic = jsonResponse.getAsJsonArray("rows")[0]
                                        .asJsonObject
                                        .getAsJsonArray("elements")[0]
                                        .asJsonObject
                                        .getAsJsonObject("duration_in_traffic")
                                        .get("value")
                                        .asInt
                                    val avgSpeed = String.format("%.2f", (distanceValue.toDouble()*2.23694) / durationNormal.toDouble())
                                    val trafficSpeed = String.format("%.2f", (distanceValue.toDouble()*2.23694) / durationInTraffic.toDouble())
                                    avgSpeedText.text = "Average Speed : $avgSpeed mph"
                                    trafficSpeedText.text = "Current Speed : $trafficSpeed mph"
                                }
                            } else {
                                println("Error encountered")
                                avgSpeedText.text = "Error encountered"
                            }
                        }

                        override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                            println(t.message)
                        }
                    })
                } catch (e: Exception) {
                    // Handle errors, such as network issues or exceptions
                }
            }
        }
    }

}