package com.example.weatherapp.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class WeatherResponse implements Serializable {
    @SerializedName("main")
    public Main main;
    @SerializedName("wind")
    public Wind wind;
    @SerializedName("weather")
    public List<Weather> weather;
    @SerializedName("name")
    public String cityName;
    @SerializedName("visibility")
    public int visibility;

    // We can add a simple inner class for Sunrise/Sunset here
    @SerializedName("sys")
    public Sys sys;
    public static class Weather implements Serializable {
        @SerializedName("id")
        public int id; // This is the ID your Adapter needs!
        @SerializedName("main")
        public String mainCondition;
        @SerializedName("description")
        public String description;
        @SerializedName("icon")
        public String icon;
    }
    public static class Sys implements Serializable {
        @SerializedName("sunrise")
        public long sunrise;
        @SerializedName("sunset")
        public long sunset;
    }

    @SerializedName("coord")
    public Coord coord;

    public class Coord implements Serializable {
        @SerializedName("lat")
        public double lat;
        @SerializedName("lon")
        public double lon;
    }
}