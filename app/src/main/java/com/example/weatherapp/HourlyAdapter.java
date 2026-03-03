package com.example.weatherapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.network.PrefManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.ViewHolder> {
    private List<ForecastResponse.ForecastItem> hourlyList;
    private PrefManager prefManager;

    public HourlyAdapter(List<ForecastResponse.ForecastItem> hourlyList, PrefManager prefManager) {
        this.hourlyList = hourlyList;
        this.prefManager = prefManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hourly_display, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = hourlyList.get(position);

        // Convert Unix timestamp to "7 AM"
        long time = item.getDt() * 1000L;
        SimpleDateFormat sdf = new SimpleDateFormat("h a", Locale.getDefault());
        holder.tvHour.setText(sdf.format(new Date(time)));

        double tempVal = item.getMain().getTemp();
        String unit = prefManager.getTempUnit();

        if (unit.equals("°F")) {
            tempVal = (tempVal * 1.8) + 32;
        }
        holder.tvTemp.setText(Math.round(tempVal) + "°");
        // Use Glide or Picasso to load the weather icon
        String iconUrl = "https://openweathermap.org/img/wn/" + item.getWeather().get(0).getIcon() + "@2x.png";
        Glide.with(holder.itemView.getContext()).load(iconUrl).into(holder.ivIcon);
    }

    @Override
    public int getItemCount() {
        return hourlyList != null ? hourlyList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvHour, tvTemp;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHour = itemView.findViewById(R.id.tvHour);
            tvTemp = itemView.findViewById(R.id.tvHourTemp);
            ivIcon = itemView.findViewById(R.id.ivHourIcon);
        }
    }
}