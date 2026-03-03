package com.example.weatherapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.network.PrefManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetailedForecast extends AppCompatActivity {
    private LineChart dynamicChart;
    private String type; // "pressure", "humidity", etc.
    private ImageView btnBack;
    private PrefManager prefManager;
    private List<ForecastResponse.ForecastItem> forecastList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed_forecast);

        // 1. Initialize Views
        btnBack = findViewById(R.id.btnBack);
        dynamicChart = findViewById(R.id.detailChart);
        TextView tvTitle = findViewById(R.id.tvDetailTitle);

        prefManager = new PrefManager(this);
        // 2. GET DATA FROM INTENT FIRST (Crucial step)
        type = getIntent().getStringExtra("forecast_type");
        forecastList = (List<ForecastResponse.ForecastItem>) getIntent().getSerializableExtra("forecast_data");
        RecyclerView recyclerView = findViewById(R.id.rvDetailedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

// Pass the type ("pressure", "wind", etc.) to the adapter
        DetailedAdapter adapter = new DetailedAdapter(forecastList, type, prefManager);
        recyclerView.setAdapter(adapter);

        if (forecastList == null || forecastList.isEmpty()) {
            finish(); // Safety check
            return;
        }

        // 3. FILTER FOR TODAY
        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
        List<ForecastResponse.ForecastItem> todayList = new ArrayList<>();

        for (ForecastResponse.ForecastItem item : forecastList) {
            if (item.dateText.startsWith(todayDate)) {
                todayList.add(item);
            }
        }

        // Fallback if today is almost over: show next 8 items
        if (todayList.isEmpty()) {
            todayList = forecastList.subList(0, Math.min(8, forecastList.size()));
        }

        // 4. SET TITLE AND UPDATE GRAPH WITH FILTERED LIST
        if (type != null) {
            tvTitle.setText(type.substring(0, 1).toUpperCase() + type.substring(1) + " Forecast");
            updateGraphData(type, todayList); // Pass the filtered list here
        }

        btnBack.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            triggerVibration(this);
            v.postDelayed(this::finish, 150);
        });
    }
    private void updateGraphData(String paramType, List<ForecastResponse.ForecastItem> displayList) {
        ArrayList<Entry> entries = new ArrayList<>();

        // 1. Loop through the FILTERED list (todayList)
        for (int i = 0; i < displayList.size(); i++) {
            float val = 0;
            ForecastResponse.ForecastItem item = displayList.get(i);

            switch (paramType) {
                case "pressure":
                    val = (float) item.main.pressure;
                    if (prefManager.getPressureUnit().equals("inHg")) val *= 0.02953f;
                    break;
                case "humidity":
                    val = (float) item.main.humidity;
                    break;
                case "wind":
                    val = (float) item.wind.speed;
                    String wUnit = prefManager.getWindUnit();
                    if (wUnit.equals("mph")) val *= 2.237f;
                    else if (wUnit.equals("km/h")) val *= 3.6f;
                    break;
                case "visibility":
                    // Convert meters to km or miles
                    val = (float) item.visibility / 1000f;
                    break;
            }
            entries.add(new Entry(i, val));
        }

        // 2. Setup Dataset and Style
        LineDataSet dataSet = new LineDataSet(entries, paramType);
        styleDataSet(dataSet, Color.WHITE, true);

        LineData data = new LineData(dataSet);
        dynamicChart.setData(data);

        // 3. Configure X-Axis (The Hours)
        XAxis xAxis = dynamicChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(displayList.size()); // Ensures every hour shows up

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < displayList.size()) {
                    return formatToHour(displayList.get(index).dateText);
                }
                return "";
            }
        });

        // 4. Final Polish
        dynamicChart.getAxisRight().setEnabled(false); // Hide right Y axis
        dynamicChart.getAxisLeft().setTextColor(Color.WHITE);
        dynamicChart.getDescription().setEnabled(false); // Hide "Description Label"
        dynamicChart.animateX(1000);
        dynamicChart.invalidate();
    }


    private String formatToHour(String dateStr) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outFormat = new SimpleDateFormat("hh a", Locale.getDefault());
            return outFormat.format(inFormat.parse(dateStr));
        } catch (Exception e) { return ""; }
    }
    private void triggerVibration(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                vibrator.vibrate(20); // 20ms for older devices
            }
        }
    }

    private void styleDataSet(LineDataSet set, int color, boolean fill) {
        set.setColor(color);
        set.setCircleColor(color);
        set.setLineWidth(3f); // Thicker line
        set.setCircleRadius(5f);
        set.setCircleHoleColor(Color.parseColor("#1F1F1F")); // Matches background
        set.setDrawValues(false); // Hide numbers to keep it clean (use MarkerView instead)
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curves

        // Enable Highlighting
        set.setHighLightColor(Color.WHITE);
        set.setDrawHorizontalHighlightIndicator(false);

        if (fill) {
            set.setDrawFilled(true);
            // Create a subtle gradient from white/color to transparent
            set.setFillDrawable(ContextCompat.getDrawable(this, R.drawable.chart_gradient));
        }
    }
}