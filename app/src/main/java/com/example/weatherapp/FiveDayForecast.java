package com.example.weatherapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FiveDayForecast extends AppCompatActivity {

    private LineChart chart;
    private ImageView btnBack;
    private PrefManager prefManager;

    // Inside your FiveDayForecast class

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_five_day_forecast);
        prefManager = new PrefManager(this);

        // Initialize all charts
        LineChart tempChart = findViewById(R.id.tempChart);
        LineChart humidityChart = findViewById(R.id.humidityChart);
        LineChart pressureChart = findViewById(R.id.pressureChart);
        LineChart windChart = findViewById(R.id.windChart);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> {
            // 1. Load and start the animation
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.btn_press);
            v.startAnimation(animation);

            // 2. Optional: Haptic feedback
            triggerVibration(this);

            // 3. Small delay so the user sees the button move before the screen closes
            v.postDelayed(this::finish, 150);
            finish();
        });

        List<ForecastResponse.ForecastItem> receivedList = (List<ForecastResponse.ForecastItem>) getIntent().getSerializableExtra("forecast_list");

        if (receivedList != null && !receivedList.isEmpty()) {
            // Setup each chart individually
            setupParameterChart(tempChart, receivedList, "Temperature (°C)", "Temp");
            setupParameterChart(humidityChart, receivedList, "Humidity (%)", "Hum");
            setupParameterChart(pressureChart, receivedList, "Pressure (hPa)", "Pres");
            setupParameterChart(windChart, receivedList, "Wind Speed (m/s)", "Wind");
        }
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

    // Update the method signature to accept a specific chart
    private void setupParameterChart(LineChart chart, List<ForecastResponse.ForecastItem> forecastList, String title, String type) {
        ArrayList<Entry> entries = new ArrayList<>();
        String tempUnit = prefManager.getTempUnit();
        String windUnit = prefManager.getWindUnit();
        String pressUnit = prefManager.getPressureUnit();
        String dynamicTitle = title;
        if (type.equals("Temp")) dynamicTitle = "Temperature (" + tempUnit + ")";
        if (type.equals("Wind")) dynamicTitle = "Wind Speed (" + windUnit + ")";
        if (type.equals("Pres")) dynamicTitle = "Pressure (" + pressUnit + ")";

        for (int i = 0; i < forecastList.size(); i++) {
            float value = 0;
            ForecastResponse.ForecastItem item = forecastList.get(i);

            switch (type) {
                case "Temp":
                    value = (float) item.main.temp;
                    if (tempUnit.equals("°F")) {
                        value = (value * 1.8f) + 32;
                    }
                    break;
                case "Hum":
                    value = (float) item.main.humidity;
                    break;
                case "Pres":
                    value = (float) item.main.pressure;
                    if (pressUnit.equals("inHg")) {
                        value = value * 0.02953f;
                    }
                    break;
                case "Wind":
                    value = (float) item.wind.speed; // Default m/s from API
                    if (windUnit.equals("mph")) {
                        value = value * 2.237f;
                    } else if (windUnit.equals("km/h")) {
                        value = value * 3.6f;
                    }
                    break;
            }
            entries.add(new Entry(i, value));
        }

        LineDataSet dataSet = new LineDataSet(entries, dynamicTitle);
        styleDataSet(dataSet, Color.WHITE, true);

        chart.setData(new LineData(dataSet));

        // Applying your Rules to the specific 'chart' passed in
        chart.getDescription().setEnabled(false);

        // Title at Top Center
        chart.getLegend().setEnabled(true);
        chart.getLegend().setTextColor(Color.WHITE);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.TOP);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);

        // X-axis at Bottom with Day Names
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawGridLines(false);

// --- THE FIX STARTS HERE ---
        xAxis.setGranularity(1f); // Only show labels on whole numbers (indices)
        xAxis.setGranularityEnabled(true);
        xAxis.setCenterAxisLabels(false); // Keeps the label directly under the point

        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;

                // 1. Check if index is valid
                if (index >= 0 && index < forecastList.size()) {
                    // 2. ONLY show a name if it's the FIRST point of that day
                    // This prevents "Today" from repeating 8 times
                    if (index == 0) return getSmartDayName(forecastList.get(index).dateText);

                    String currentDay = getSmartDayName(forecastList.get(index).dateText);
                    String previousDay = getSmartDayName(forecastList.get(index - 1).dateText);

                    if (!currentDay.equals(previousDay)) {
                        return currentDay;
                    }
                }
                return ""; // Return nothing for points in between
            }
        });

        chart.getAxisLeft().setTextColor(Color.WHITE);
        chart.getAxisRight().setEnabled(false);
        chart.animateX(1000);
        chart.invalidate();
    }

    // KEEP THIS: It makes the chart look smooth and professional
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

    // KEEP THIS: It handles the "Today/Tomorrow" logic
    private String getSmartDayName(String dateStr) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = inFormat.parse(dateStr);
            if (date == null) return "";

            Calendar now = Calendar.getInstance();
            Calendar target = Calendar.getInstance();
            target.setTime(date);

            if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) return "Today";

            now.add(Calendar.DAY_OF_YEAR, 1);
            if (now.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) return "Tomorrow";

            return new SimpleDateFormat("EEE", Locale.getDefault()).format(date);
        } catch (Exception e) {
            return "";
        }
    }
}