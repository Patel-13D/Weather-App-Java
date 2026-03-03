package com.example.weatherapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class ForecastResponse implements Serializable {
    @SerializedName("list")
    public List<ForecastItem> list;

    public static class ForecastItem implements Serializable {
        @SerializedName("dt") public long timestamp;
        @SerializedName("main") public Main main;
        @SerializedName("weather") public List<Weather> weather;
        @SerializedName("dt_txt") public String dateText;
        @SerializedName("wind") public Wind wind;
        @SerializedName("visibility") public int visibility;

        // Getters to match your adapter calls
        public long getDt() { return timestamp; }
        public Main getMain() { return main; }
        public List<Weather> getWeather() { return weather; }
    }

    public static class Main implements Serializable {
        @SerializedName("temp") public double temp;
        @SerializedName("temp_min") public double tempMin; // Added this
        @SerializedName("temp_max") public double tempMax; // Added this
        @SerializedName("humidity") public int humidity;
        @SerializedName("pressure") public int pressure;  // Good to have for the 24h view
        @SerializedName("feels_like") public double feelsLike;

        public double getTemp() { return temp; }
        public double getTempMin() { return tempMin; } // Getter for min
        public double getTempMax() { return tempMax; } // Getter for max
    }

    public static class Weather implements Serializable {
        @SerializedName("description") public String description;
        @SerializedName("icon") public String icon;

        public String getIcon() { return icon; }
        public String getDescription() { return description; }
    }

    public static class Wind implements Serializable {
        @SerializedName("speed") public double speed;
        @SerializedName("deg") public int deg;
    }
}