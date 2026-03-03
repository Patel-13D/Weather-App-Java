package com.example.weatherapp;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class WidgetSettingsActivity extends AppCompatActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        setContentView(R.layout.activity_widget_settings);

        // Catch the ID if this was opened via the phone's widget menu (long press)
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        setupPreviews();

        // --- CLICK LISTENERS ---
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.cardStyleGlass).setOnClickListener(v -> confirmStyle(1));
        findViewById(R.id.cardStyleSlim).setOnClickListener(v -> confirmStyle(2));
        findViewById(R.id.cardStyleModern).setOnClickListener(v -> confirmStyle(3));
    }

    private void setupPreviews() {
        SharedPreferences prefs = getSharedPreferences("WIDGET_PREFS", MODE_PRIVATE);
        String currentTemp = prefs.getString("temp", "25°");
        String currentCity = prefs.getString("city_name", "Location");

        // Glass Preview
        View glassInclude = findViewById(R.id.previewGlass);
        if (glassInclude != null) {
            ((TextView) glassInclude.findViewById(R.id.widget1Temp)).setText(currentTemp);
            ((TextView) glassInclude.findViewById(R.id.weatherInfo)).setText(currentCity);
        }

        // Slim Preview
        View slimInclude = findViewById(R.id.previewSlim);
        if (slimInclude != null) {
            ((TextView) slimInclude.findViewById(R.id.widget1Temp)).setText(currentTemp);
            ((TextView) slimInclude.findViewById(R.id.weatherInfo)).setText(currentCity);
        }

        // Modern Preview
        View modernInclude = findViewById(R.id.previewModern);
        if (modernInclude != null) {
            ((TextView) modernInclude.findViewById(R.id.widget1Temp)).setText(currentTemp);
            ((TextView) modernInclude.findViewById(R.id.cityName)).setText(currentCity + "\nWEATHER");

            for (int i = 0; i < 7; i++) {
                int nameId = getResources().getIdentifier("day" + (i + 1) + "_name", "id", getPackageName());
                int maxId = getResources().getIdentifier("day" + (i + 1) + "_max", "id", getPackageName());
                TextView tvName = modernInclude.findViewById(nameId);
                TextView tvMax = modernInclude.findViewById(maxId);
                if (tvName != null) tvName.setText(prefs.getString("day_" + i + "_name", "Day " + (i+1)));
                if (tvMax != null) tvMax.setText(prefs.getString("day_" + i + "_max", "0°"));
            }
        }
    }

    private void confirmStyle(int styleId) {
        String msg = (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID)
                ? "Apply this style to your widget?"
                : "Add this widget style to your home screen?";

        new AlertDialog.Builder(this)
                .setTitle("Widget Style")
                .setMessage(msg)
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (mAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        // Scenario 1: User is editing an EXISTING widget
                        applyStyleToExisting(styleId);
                    } else {
                        // Scenario 2: User is adding a NEW widget from the app
                        pinWidgetToHomeScreen(styleId);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void applyStyleToExisting(int styleId) {
        SharedPreferences.Editor editor = getSharedPreferences("WIDGET_PREFS", MODE_PRIVATE).edit();
        editor.putInt("selected_style_" + mAppWidgetId, styleId);
        editor.apply();

        // Force refresh the specific ID
        refreshSpecificWidget();

        // Send success back to system
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private void pinWidgetToHomeScreen(int styleId) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            AppWidgetManager appWidgetManager = getSystemService(AppWidgetManager.class);
            ComponentName myProvider = new ComponentName(this, WeatherWidgetProvider.class);

            if (appWidgetManager.isRequestPinAppWidgetSupported()) {
                Intent intent = new Intent(this, WeatherWidgetProvider.class);
                intent.setAction("ACTION_WIDGET_PINNED");
                intent.putExtra("chosen_style", styleId);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        styleId,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

                appWidgetManager.requestPinAppWidget(myProvider, null, pendingIntent);

                Toast.makeText(this, "Accept the system popup to finish!", Toast.LENGTH_LONG).show();
                // Optionally finish() here to return to MainActivity
                finish();
            }
        } else {
            Toast.makeText(this, "Your device doesn't support direct pinning.", Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshSpecificWidget() {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        Intent intent = new Intent(this, WeatherWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{mAppWidgetId});
        sendBroadcast(intent);
        appWidgetManager.notifyAppWidgetViewDataChanged(mAppWidgetId, R.id.rvWidget1Preview);
    }
}