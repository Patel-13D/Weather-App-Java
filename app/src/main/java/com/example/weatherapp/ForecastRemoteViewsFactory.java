package com.example.weatherapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.example.weatherapp.models.HourlyModel;
import java.util.ArrayList;
import java.util.List;

public class ForecastRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<HourlyModel> dataList = new ArrayList<>();

    public ForecastRemoteViewsFactory(Context context) {
        this.context = context;
    }

    @Override
    public void onDataSetChanged() {
        SharedPreferences prefs = context.getSharedPreferences("WIDGET_PREFS", Context.MODE_PRIVATE);
        String jsonList = prefs.getString("hourly_json", null);

        if (jsonList != null) {
            // Reuse GSON to convert String back to List
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<HourlyModel>>(){}.getType();
            dataList = new com.google.gson.Gson().fromJson(jsonList, type);
        }
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.item_hourly_detailed);
        HourlyModel item = dataList.get(position);

        rv.setTextViewText(R.id.tvHour, item.getHour());
        rv.setImageViewResource(R.id.ivHourIcon, item.getIconRes());
        rv.setTextViewText(R.id.tvHourTemp, item.getTemp());
        rv.setTextViewText(R.id.tvConditionText, item.getCondition());

        return rv;
    }

    @Override public int getCount() { return dataList.size(); }
    @Override public long getItemId(int position) { return position; }
    @Override public boolean hasStableIds() { return true; }
    @Override public int getViewTypeCount() { return 1; }
    @Override public void onCreate() {}
    @Override public void onDestroy() { dataList.clear(); }
    @Override public RemoteViews getLoadingView() { return null; }
}