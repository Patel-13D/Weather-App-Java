package com.example.weatherapp;

import static androidx.activity.result.ActivityResultCallerKt.registerForActivityResult;

import android.Manifest;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.weatherapp.WeatherWidgetProvider;
import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.models.HourlyModel;
import com.example.weatherapp.network.PrefManager;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.example.weatherapp.models.WeatherResponse;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private WeatherViewModel viewModel;
    private TextView tvCity, tvDate, tvTemp, tvDescription, tvMaxMin, tvWind, tvHumidity;
    private LineChart tempChart;
    private ImageView map, setting, add, widget;
    private MaterialButton btnMoreForecast;
    private ConstraintLayout mainLayout;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;

    private List<ForecastResponse.ForecastItem> hourlyList = new ArrayList<>();
    private List<ForecastResponse.ForecastItem> filteredList = new ArrayList<>();
    private final String API_KEY = "3a28c58470d5436d01bce8c7c6c70e5f";
    private androidx.activity.result.ActivityResultLauncher<Intent> addLocationLauncher;
    private WeatherResponse currentWeatherData;
    private PrefManager prefManager;
    private WeatherResponse gpsWeatherData;     // ALWAYS stays as your local GPS city
    private WeatherResponse searchedWeatherData;  // Changes when you click a new city
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAppWidgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        add = findViewById(R.id.add);
        widget = findViewById(R.id.widget);
        prefManager = new PrefManager(this);

        tvCity = findViewById(R.id.tvCityName);
        tvDate = findViewById(R.id.tvDate);
        tvTemp = findViewById(R.id.tvTemp);
        tvDescription = findViewById(R.id.tvDescription);
        tvMaxMin = findViewById(R.id.tvMaxMin);
        tvWind = findViewById(R.id.tvWind);
        tvHumidity = findViewById(R.id.tvHumidity);
        mainLayout = findViewById(R.id.mainLayout);
        map = findViewById(R.id.map);
        setting = findViewById(R.id.setting);
        btnMoreForecast = findViewById(R.id.btnMoreForecast);
        // Inside MainActivity.java
        findViewById(R.id.cardPressure).setOnClickListener(v -> openDetailedForecast("pressure"));
        findViewById(R.id.cardHumidity).setOnClickListener(v -> openDetailedForecast("humidity"));
        findViewById(R.id.cardVisibility).setOnClickListener(v -> openDetailedForecast("visibility"));


        widget.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, WidgetSettingsActivity.class);
            startActivity(i);
        });

        add.setOnClickListener(v -> {
            triggerVibration(this);
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));

            Intent i = new Intent(MainActivity.this, AddNewLocation.class);
            i.putExtra("current_location_weather", currentWeatherData);
            addLocationLauncher.launch(i);
        });

        setting.setOnClickListener(v->{
            triggerVibration(this);
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));
            Intent i = new Intent(MainActivity.this, Setting.class);
            startActivity(i);
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(
                Color.parseColor("#1976D2"),
                Color.parseColor("#D02A02")
        );
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.parseColor("#1F1F1F"));
        swipeRefreshLayout.setOnRefreshListener(() -> {
            checkLocationPermission();
            triggerVibration(this);

        });
        addLocationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        WeatherResponse selectedCity = (WeatherResponse) result.getData()
                                .getSerializableExtra("selected_city_data");

                        if (selectedCity != null) {
                            // 1. Assign it to the "Searched" variable, NOT the "GPS" variable
                            this.searchedWeatherData = selectedCity;

                            // 2. Update UI with this specific city
                            updateUI(selectedCity);

                            // 3. IMPORTANT: Only fetch forecast for this city,
                            // don't trigger the GPS "getUserLocation()" method again!
                            fetchWeatherData(selectedCity.coord.lat, selectedCity.coord.lon,false);
                        }
                    }
                }
        );

        viewModel = new ViewModelProvider(this).get(WeatherViewModel.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        RecyclerView rvHourly = findViewById(R.id.rvHourlyForecast);
        rvHourly.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        HourlyAdapter hourlyAdapter = new HourlyAdapter(hourlyList, prefManager);
        rvHourly.setAdapter(hourlyAdapter);

        ScrollView scrollView = findViewById(R.id.scrollView);
        LinearLayout headerLayout = findViewById(R.id.headerLayout);

        scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            float fadeRange = 800f;
            float alpha = 1.0f - Math.max(0.0f, Math.min(1.0f, scrollY / fadeRange));
            headerLayout.setAlpha(alpha);
            headerLayout.setTranslationY(0f);
        });

        checkLocationPermission();
    }
    private void openDetailedForecast(String type) {
        Intent intent = new Intent(this, DetailedForecast.class);
        intent.putExtra("forecast_type", type);
        // Pass your existing hourly/daily list so you don't have to fetch it again
        intent.putExtra("forecast_data", new ArrayList<>(hourlyList));
        startActivity(intent);
    }
    private void updateUI(WeatherResponse response) {

        TextView txtHumPer = findViewById(R.id.txtHumPer);
        TextView txtpre = findViewById(R.id.txtPre);
        TextView txtFeel = findViewById(R.id.txtFeel);
        TextView tvVisibilityValue = findViewById(R.id.tvVisibilityValue);
        TextView sunset = findViewById(R.id.sunset);
        TextView sunrise = findViewById(R.id.sunrise);
        LinearLayout headerLayout = findViewById(R.id.headerLayout);
        ProgressBar progressBar = findViewById(R.id.progressBar);
        ScrollView scrollView = findViewById(R.id.scrollView);

        headerLayout.setAlpha(1.0f);
        headerLayout.setTranslationY(0f);

        btnMoreForecast.setOnClickListener(v -> {
            if (filteredList != null && !filteredList.isEmpty()) {
                Intent i = new Intent(MainActivity.this, FiveDayForecast.class);
                i.putExtra("forecast_list", new ArrayList<>(filteredList));
                startActivity(i);
            }
        });

        if (swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }


        tvCity.setText(response.cityName);
        tvTemp.setText(Math.round(response.main.temp) + "°");

        tvWind.setText("Wind: " + response.wind.speed + " km/h");
        tvHumidity.setText("Humidity: " + response.main.humidity + "%");
        txtHumPer.setText(response.main.humidity + "%");
        txtFeel.setText(Math.round(response.main.feelsLike) + "°");
        txtpre.setText(response.main.pressure + " hPa");

        if (response.sys != null) {
            sunrise.setText(formatTime(response.sys.sunrise));
            sunset.setText(formatTime(response.sys.sunset));
        }

        double visibilityKm = response.visibility / 1000.0;
        tvVisibilityValue.setText(visibilityKm + " km");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEEE―dd MMM", java.util.Locale.getDefault());
        tvDate.setText(sdf.format(new java.util.Date()));

        String icon = response.weather.get(0).icon;
        double currentTemp = response.main.temp;
        String apiDescription = response.weather.get(0).description;

        if (icon.contains("n")) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvDescription.setText("Clear night sky.");
            updateAllTextColors(Color.WHITE);
        } else {
            if (currentTemp > 27) {
                mainLayout.setBackgroundColor(Color.parseColor("#D02A02"));
                tvDescription.setText("The floor is lava.");
                updateAllTextColors(Color.BLACK);
            } else {
                mainLayout.setBackgroundColor(Color.parseColor("#1976D2"));
                String capitalized = apiDescription.substring(0, 1).toUpperCase() + apiDescription.substring(1);
                tvDescription.setText(capitalized);
                updateAllTextColors(Color.BLACK);
            }
        }
        progressBar.setVisibility(View.GONE);
        scrollView.setVisibility(View.VISIBLE);
        // Inside updateUI
        double maxTemp = response.main.tempMax;
        double minTemp = response.main.tempMin;
        double feelTemp = response.main.feelsLike;
        String tempUnit = prefManager.getTempUnit();
        double displayTemp = response.main.temp;

        if (tempUnit.equals("°F")) {
            maxTemp = (maxTemp * 1.8) + 32;
            minTemp = (minTemp * 1.8) + 32;
            feelTemp = (feelTemp * 1.8) + 32;
            displayTemp = (displayTemp * 1.8) + 32;
        }

        tvMaxMin.setText(Math.round(minTemp) + "° — " + Math.round(maxTemp) + "°");
        txtFeel.setText(Math.round(feelTemp) + "°");
        String tempValue = String.valueOf(Math.round(displayTemp));
        String fullTempString = tempValue + tempUnit;

// 2. Create a SpannableString
        android.text.SpannableString spannableTemp = new android.text.SpannableString(fullTempString);

// 3. Apply a smaller size to the unit part (the last characters)
// we use RelativeSizeSpan(0.5f) to make it 50% of the original size
        spannableTemp.setSpan(
                new android.text.style.RelativeSizeSpan(0.3f), // Adjust 0.3f to your liking
                tempValue.length(),                            // Start at the end of the number
                fullTempString.length(),                       // End at the end of the string
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );

// 4. Set the text to the TextView
        tvTemp.setText(tempValue+ "°");

        String windUnit = prefManager.getWindUnit();
        double displayWind = response.wind.speed;
        // Wind Speed: OpenWeather returns m/s
        if (windUnit.equals("mph")) {
            displayWind = displayWind * 2.237; // m/s to mph
        } else if (windUnit.equals("km/h")) {
            displayWind = displayWind * 3.6;   // m/s to km/h
        }

        tvWind.setText("Wind: "+String.format("%.1f %s", displayWind, windUnit));

        String pressUnit = prefManager.getPressureUnit();
        double displayPress = response.main.pressure;
        if (pressUnit.equals("inHg")) {
            displayPress = displayPress * 0.02953;
        }
        txtpre.setText(Math.round(displayPress) + " " + pressUnit);
    }
    private void save7DayForecast(List<ForecastResponse.ForecastItem> forecastList) {
        if (forecastList == null || forecastList.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("WIDGET_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Loop through the forecast list.
        // Usually, OpenWeather gives data in 3-hour chunks, so we skip by 8 to get daily data.
        int dayCounter = 0;
        for (int i = 0; i < forecastList.size() && dayCounter < 7; i += 8) {
            ForecastResponse.ForecastItem item = forecastList.get(i);

            // Save using keys like "day_0_name", "day_1_max", etc.
            editor.putString("day_" + dayCounter + "_name", formatToDayName(item.timestamp));
            editor.putString("day_" + dayCounter + "_max", Math.round(item.main.tempMax) + "°");
            editor.putString("day_" + dayCounter + "_min", Math.round(item.main.tempMin) + "°");

            // Save the icon for each day too!
            editor.putInt("day_" + dayCounter + "_icon", getIconResource(item.weather.get(0).icon));

            dayCounter++;
        }

        editor.apply();
        refreshWidget();
    }

    // Helper to turn timestamp into "Mon", "Tue", etc.
    private String formatToDayName(long timestamp) {
        java.util.Date date = new java.util.Date(timestamp * 1000L);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    private void refreshWidget() {
        Intent intent = new Intent(this, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(getApplication())
                .getAppWidgetIds(new ComponentName(getApplication(), WeatherWidgetProvider.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);
    }
    private void triggerVibration(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                vibrator.vibrate(20);
            }
        }
    }

    private void updateAllTextColors(int color) {
        tvCity.setTextColor(color);
        tvDate.setTextColor(color);
        tvTemp.setTextColor(color);
        tvDescription.setTextColor(color);
        tvMaxMin.setTextColor(color);
        tvWind.setTextColor(color);
        tvHumidity.setTextColor(color);
        map.setColorFilter(color);
        add.setColorFilter(color);
        setting.setColorFilter(color);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    fetchWeatherData(location.getLatitude(), location.getLongitude(), true);
                } else {
                    Toast.makeText(MainActivity.this, "Cannot get location", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchWeatherData(double lat, double lon,boolean isGps) {
        viewModel.getCurrentWeather(lat, lon, API_KEY).observe(this, response -> {
            if (response != null) {
                // ONLY overwrite the main variable if it's actually the GPS location
                if (isGps) {
                    this.currentWeatherData = response;
                }
                // Always update the UI to show what was just fetched
                updateUI(response);
            }

        });

        viewModel.getForecast(lat, lon, API_KEY).observe(this, forecastResponse -> {
            if (forecastResponse != null) {
                this.filteredList = filterDailyForecast(forecastResponse.list);
                this.hourlyList = new ArrayList<>(forecastResponse.list.subList(0, 10));
                save7DayForecast(forecastResponse.list);
                if (currentWeatherData != null) {
                    saveDataAndRefreshWidget(currentWeatherData, hourlyList);
                }

                RecyclerView rvDaily = findViewById(R.id.rvDailyForecast);
                rvDaily.setLayoutManager(new LinearLayoutManager(this));
                rvDaily.setAdapter(new DailyAdapter(filteredList, prefManager,position -> {}));

                this.hourlyList = new ArrayList<>(forecastResponse.list.subList(0, 10));
                RecyclerView rvHourly = findViewById(R.id.rvHourlyForecast);
                rvHourly.setAdapter(new HourlyAdapter(hourlyList, prefManager));
            }
        });
    }

    private List<ForecastResponse.ForecastItem> filterDailyForecast(List<ForecastResponse.ForecastItem> fullList) {
        List<ForecastResponse.ForecastItem> dailyList = new ArrayList<>();
        for (int i = 0; i < fullList.size(); i += 8) {
            dailyList.add(fullList.get(i));
        }
        return dailyList;
    }

    private String formatTime(long timestamp) {
        java.util.Date date = new java.util.Date(timestamp * 1000L);
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault());
        return sdf.format(date);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        WeatherResponse data = (searchedWeatherData != null) ? searchedWeatherData : currentWeatherData;

        if (data != null) {
            updateUI(data);
            fetchWeatherData(data.coord.lat, data.coord.lon, false);

            // Ensure both styles stay updated
            saveDataAndRefreshWidget(data, hourlyList);

            // Add this line to ensure the 7-day data is refreshed too
            if (filteredList != null && !filteredList.isEmpty()) {
                save7DayForecast(filteredList);
            }
        }
    }

    private void saveDataAndRefreshWidget(WeatherResponse response, List<ForecastResponse.ForecastItem> hourlyItems) {
        if (response == null || hourlyItems == null) return;

        SharedPreferences prefs = getSharedPreferences("WIDGET_PREFS", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // 1. Save Basic Info + CITY NAME
        editor.putString("temp", Math.round(response.main.temp) + "°");
        editor.putString("condition", response.weather.get(0).description);
        editor.putString("city_name", response.cityName); // SAVE THIS FOR LOCATION

        // 2. Map the main icon
        int mainIconRes = getIconResource(response.weather.get(0).icon);
        editor.putInt("main_icon_res", mainIconRes);

        // 3. Map to HourlyModel
        List<com.example.weatherapp.models.HourlyModel> widgetList = new ArrayList<>();
        for(int i = 0; i < Math.min(hourlyItems.size(), 10); i++) {
            ForecastResponse.ForecastItem item = hourlyItems.get(i);

            // Map the hourly icon dynamically instead of hardcoding R.drawable.rain
            int hourlyIconRes = getIconResource(item.weather.get(0).icon);

            widgetList.add(new com.example.weatherapp.models.HourlyModel(
                    formatTime(item.timestamp),
                    item.weather.get(0).description,
                    hourlyIconRes, // Dynamic icon
                    "0%",
                    Math.round(item.main.temp) + "°",
                    item.wind.speed + " km/h"
            ));
        }

        String jsonList = new com.google.gson.Gson().toJson(widgetList);
        editor.putString("hourly_json", jsonList);
        editor.apply();

        updateAllWidgets();
    }

    // Add this helper method to MainActivity to handle Icon mapping
    private int getIconResource(String iconCode) {
        // Matches OpenWeather icon codes to your drawable names
        if (iconCode.contains("01")) return R.drawable.sun; // or your clear sky icon
        if (iconCode.contains("02") || iconCode.contains("03") || iconCode.contains("04")) return R.drawable.cloudy;
        if (iconCode.contains("09") || iconCode.contains("10")) return R.drawable.rain;
        if (iconCode.contains("11")) return R.drawable.cloudy;
        return R.drawable.sun; // default
    }

    private void updateAllWidgets() {
        Context context = getApplicationContext();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisWidget = new ComponentName(context, WeatherWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

        // 1. Notify the Provider to run onUpdate() for all IDs
        Intent intent = new Intent(context, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        sendBroadcast(intent);

        // 2. CRITICAL: Tell any ListView/GridView to refresh its data
        // Without this, Style 1 might show old hourly data even after the city changes
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.rvWidget1Preview);
    }
}