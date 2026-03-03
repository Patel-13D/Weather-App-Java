package com.example.weatherapp.repository;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.models.WeatherResponse;
import com.example.weatherapp.network.RetrofitClient;
import com.example.weatherapp.network.WeatherApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WeatherRepository {
    private final WeatherApiService apiService;

    public WeatherRepository() {
        apiService = RetrofitClient.getService();
    }

    // Fetch Current Weather
    public LiveData<WeatherResponse> getCurrentWeather(double lat, double lon, String apiKey) {
        MutableLiveData<WeatherResponse> data = new MutableLiveData<>();

        apiService.getCurrentWeather(lat, lon, "metric", apiKey).enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }
    // Add this method to WeatherRepository.java

    public LiveData<WeatherResponse> getWeatherByCity(String cityName, String apiKey) {
        MutableLiveData<WeatherResponse> data = new MutableLiveData<>();

        // Using "q" for city name query as per OpenWeather API
        apiService.getWeatherByCity(cityName, apiKey, "metric").enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call, @NonNull Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    data.setValue(response.body());
                } else {
                    data.setValue(null); // Useful for showing "City Not Found"
                }
                Log.d("API_CHECK", "URL: " + call.request().url().toString());
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }

    // Fetch 5-Day Forecast
    public LiveData<ForecastResponse> getForecast(double lat, double lon, String apiKey) {
        MutableLiveData<ForecastResponse> data = new MutableLiveData<>();

        apiService.getForecast(lat, lon, "metric", apiKey).enqueue(new Callback<ForecastResponse>() {
            @Override
            public void onResponse(@NonNull Call<ForecastResponse> call, @NonNull Response<ForecastResponse> response) {
                if (response.isSuccessful()) {
                    data.setValue(response.body());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ForecastResponse> call, @NonNull Throwable t) {
                data.setValue(null);
            }
        });
        return data;
    }
}