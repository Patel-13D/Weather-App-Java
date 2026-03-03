package com.example.weatherapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Main implements Serializable {
    @SerializedName("temp") public double temp;
    @SerializedName("feels_like") public double feelsLike;
    @SerializedName("temp_min") public double tempMin;
    @SerializedName("temp_max") public double tempMax;
    @SerializedName("pressure") public int pressure;
    @SerializedName("humidity") public int humidity;
}