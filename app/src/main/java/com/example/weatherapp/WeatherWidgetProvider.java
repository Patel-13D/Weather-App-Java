package com.example.weatherapp;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.RemoteViews;

public class WeatherWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SharedPreferences prefs = context.getSharedPreferences("WIDGET_PREFS", Context.MODE_PRIVATE);

        for (int appWidgetId : appWidgetIds) {
            // 1. Load style using the UNIQUE ID key
            int selectedStyle = prefs.getInt("selected_style_" + appWidgetId, 1);

            // 2. Select Layout
            int layoutId;
            if (selectedStyle == 3) layoutId = R.layout.widget_style_modern;
            else if (selectedStyle == 2) layoutId = R.layout.widget_style_slim;
            else layoutId = R.layout.widget_preview_layout;

            RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

            // 3. Update Current Weather Data
            views.setTextViewText(R.id.widget1Temp, prefs.getString("temp", "--°"));
            String city = prefs.getString("city_name", "Locating...");
            String condition = prefs.getString("condition", "");

            if (selectedStyle == 3) {
                views.setTextViewText(R.id.cityName, city + "\nWEATHER");
                views.setTextViewText(R.id.weatherInfo, condition);
            } else {
                views.setTextViewText(R.id.weatherInfo, city + " | " + condition);
            }

            int iconRes = prefs.getInt("main_icon_res", R.drawable.sun);
            views.setImageViewResource(R.id.widget1Icon, iconRes);

            // 4. STYLE 1: Setup Hourly ListView (Instance Specific)
            if (selectedStyle == 1) {
                Intent serviceIntent = new Intent(context, WidgetService.class);
                serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                // This URI trick makes the Intent unique so Android doesn't reuse the wrong list
                serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));
                views.setRemoteAdapter(R.id.rvWidget1Preview, serviceIntent);
                views.setEmptyView(R.id.rvWidget1Preview, R.id.widget1Temp); // Fallback if list empty
            }

            // 5. STYLE 3: Setup 7-Day Forecast Row
            if (selectedStyle == 3) {
                for (int i = 0; i < 7; i++) {
                    int nameId = context.getResources().getIdentifier("day" + (i + 1) + "_name", "id", context.getPackageName());
                    int iconId = context.getResources().getIdentifier("day" + (i + 1) + "_icon", "id", context.getPackageName());
                    int maxId = context.getResources().getIdentifier("day" + (i + 1) + "_max", "id", context.getPackageName());
                    int minId = context.getResources().getIdentifier("day" + (i + 1) + "_min", "id", context.getPackageName());

                    String dName = prefs.getString("day_" + i + "_name", null);
                    if (dName == null) {
                        // Hide columns if no data (e.g., Day 6 and 7 in 5-day forecast)
                        int containerId = context.getResources().getIdentifier("day" + (i + 1), "id", context.getPackageName());
                        if (containerId != 0) views.setViewVisibility(containerId, View.GONE);
                    } else {
                        views.setTextViewText(nameId, dName);
                        views.setImageViewResource(iconId, prefs.getInt("day_" + i + "_icon", R.drawable.sun));
                        views.setTextViewText(maxId, prefs.getString("day_" + i + "_max", "--°"));
                        views.setTextViewText(minId, prefs.getString("day_" + i + "_min", "--°"));
                    }
                }
            }

            // 6. Global Click Intent
            Intent appIntent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget1Icon, pendingIntent);

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        // MATCH THE MANIFEST ACTION
        if ("ACTION_WIDGET_PINNED".equals(intent.getAction())) {
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int styleId = intent.getIntExtra("chosen_style", 1);

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                SharedPreferences prefs = context.getSharedPreferences("WIDGET_PREFS", Context.MODE_PRIVATE);
                prefs.edit().putInt("selected_style_" + appWidgetId, styleId).apply();

                // Refresh the specific new widget
                AppWidgetManager mgr = AppWidgetManager.getInstance(context);
                onUpdate(context, mgr, new int[]{appWidgetId});
            }
        }
    }

    // --- NEW: CLEANUP LOGIC ---
    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        SharedPreferences.Editor editor = context.getSharedPreferences("WIDGET_PREFS", Context.MODE_PRIVATE).edit();
        for (int appWidgetId : appWidgetIds) {
            // Remove the style preference for this specific ID so it doesn't take up memory
            editor.remove("selected_style_" + appWidgetId);
        }
        editor.apply();
    }
}