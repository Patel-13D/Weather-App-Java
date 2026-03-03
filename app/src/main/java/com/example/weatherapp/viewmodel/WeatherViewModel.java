package com.example.weatherapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.models.WeatherResponse;
import com.example.weatherapp.repository.WeatherRepository;

public class WeatherViewModel extends ViewModel {
    private final WeatherRepository repository;

    public WeatherViewModel() {
        // Initialize the repository
        repository = new WeatherRepository();
    }

    // This method will be called by the Activity to get current weather
    public LiveData<WeatherResponse> getCurrentWeather(double lat, double lon, String apiKey) {
        return repository.getCurrentWeather(lat, lon, apiKey);
    }

    // This method will be called by the Activity to get the 5-day forecast
    public LiveData<ForecastResponse> getForecast(double lat, double lon, String apiKey) {
        return repository.getForecast(lat, lon, apiKey);
    }

    public LiveData<WeatherResponse> getWeatherByCityName(String cityName, String apiKey) {
        return repository.getWeatherByCity(cityName, apiKey);
    }
}