package com.example.weatherapp.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
public class Weather implements Serializable{
    @SerializedName("main") public String main;
    @SerializedName("description") public String description;
    @SerializedName("icon") public String icon;
}