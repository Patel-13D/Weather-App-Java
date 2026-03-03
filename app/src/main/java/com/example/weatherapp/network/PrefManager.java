package com.example.weatherapp.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.weatherapp.models.WeatherResponse;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PrefManager {
    private static final String PREF_NAME = "WeatherAppPrefs";
    private static final String KEY_CITIES = "saved_cities_list";
    private static final String KEY_TEMP_UNIT = "temp_unit";
    private static final String KEY_WIND_UNIT = "wind_unit";
    private static final String KEY_NIGHT_UPDATE = "night_update";
    private static final String KEY_PRESSURE_UNIT = "pressure_unit";

    private final SharedPreferences sharedPreferences;
    private final Gson gson;

    public PrefManager(Context context) {
        // Initialize SharedPreferences and Gson
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    /**
     * Converts the list of WeatherResponse objects into a JSON string and saves it.
     */
    public void setTempUnit(String unit){
          sharedPreferences.edit().putString(KEY_TEMP_UNIT,unit).apply();
    }
    public String getTempUnit() {
        // Returns °C by default if nothing is saved
        return sharedPreferences.getString(KEY_TEMP_UNIT, "°C");
    }
    public void setWindUnit(String unit){
        sharedPreferences.edit().putString(KEY_WIND_UNIT,unit).apply();
    }
    public String getWindUnit() {
        return sharedPreferences.getString(KEY_WIND_UNIT, "km/h");
    }

    public void setPressureUnit(String unit){
        sharedPreferences.edit().putString(KEY_PRESSURE_UNIT,unit).apply();
    }
    public String getPressureUnit() {
        return sharedPreferences.getString(KEY_PRESSURE_UNIT, "hPa");
    }
    public void setNightUpdate(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_NIGHT_UPDATE, enabled).apply();
    }
    public boolean isNightUpdateEnabled() {
        return sharedPreferences.getBoolean(KEY_NIGHT_UPDATE, false);
    }
    public void saveCities(List<WeatherResponse> cityList) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(cityList);
        editor.putString(KEY_CITIES, json);
        editor.apply(); // Saves asynchronously in the background
    }

    /**
     * Retrieves the JSON string and converts it back into a List of WeatherResponse objects.
     */
    public List<WeatherResponse> getSavedCities() {
        String json = sharedPreferences.getString(KEY_CITIES, null);

        // If nothing is saved yet, return an empty list
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }

        // Define the type of collection we want to retrieve
        Type type = new TypeToken<ArrayList<WeatherResponse>>() {}.getType();
        return gson.fromJson(json, type);
    }
}