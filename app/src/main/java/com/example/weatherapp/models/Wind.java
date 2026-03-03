package com.example.weatherapp.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Wind implements Serializable {
    @SerializedName("speed") public double speed;
    @SerializedName("deg") public int deg;
}