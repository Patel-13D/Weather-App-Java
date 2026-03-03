package com.example.weatherapp;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide; // Make sure Glide is in your build.gradle
import com.example.weatherapp.models.ForecastResponse;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

public class MyMarkerView extends MarkerView {

    private final TextView tvDay, tvRain;
    private final ImageView ivIcon;
    private final List<ForecastResponse.ForecastItem> forecastList;

    public MyMarkerView(Context context, int layoutResource, List<ForecastResponse.ForecastItem> list) {
        super(context, layoutResource);
        this.forecastList = list;
        tvDay = findViewById(R.id.tvChartDay);
        tvRain = findViewById(R.id.tvChartRain);
        ivIcon = findViewById(R.id.ivChartIcon);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();

        if (index >= 0 && index < forecastList.size()) {
            ForecastResponse.ForecastItem item = forecastList.get(index);


            // 2. Set Temp Text
            tvDay.setText(Math.round(e.getY()) + "°");

            // 3. Load Icon from OpenWeather Website
            String iconCode = item.weather.get(0).icon;
            String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

            Glide.with(getContext())
                    .load(iconUrl)
                    .into(ivIcon);
        }
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}