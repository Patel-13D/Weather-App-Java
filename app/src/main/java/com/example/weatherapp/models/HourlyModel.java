package com.example.weatherapp.models;

public class HourlyModel {
    private String hour;
    private String condition;
    private int iconRes;
    private String precipitation;
    private String temp;
    private String wind;

    // Constructor: This is used to create a new hour object
    public HourlyModel(String hour, String condition, int iconRes, String precipitation, String temp, String wind) {
        this.hour = hour;
        this.condition = condition;
        this.iconRes = iconRes;
        this.precipitation = precipitation;
        this.temp = temp;
        this.wind = wind;
    }

    // Getters: These allow the Adapter and Widget to "read" the data
    public String getHour() { return hour; }
    public String getCondition() { return condition; }
    public int getIconRes() { return iconRes; }
    public String getPrecipitation() { return precipitation; }
    public String getTemp() { return temp; }
    public String getWind() { return wind; }
}
