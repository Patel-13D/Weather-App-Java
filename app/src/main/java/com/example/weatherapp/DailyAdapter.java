package com.example.weatherapp;

import android.graphics.Color;
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

import java.util.Date;
import java.util.List;

public class DailyAdapter extends RecyclerView.Adapter<DailyAdapter.ViewHolder> {
    private List<ForecastResponse.ForecastItem> dayList;
    private OnDayClickListener listener;
    private PrefManager prefManager;
    private int textColor = Color.WHITE;

    public interface OnDayClickListener {
        void onDayClick(int position);
    }

    public DailyAdapter(List<ForecastResponse.ForecastItem> dayList, PrefManager prefManager, OnDayClickListener listener) {
        this.dayList = dayList;
        this.prefManager = prefManager;
        this.listener = listener;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DailyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_daily, parent, false);
        return new DailyAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DailyAdapter.ViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = dayList.get(position);
        double tempVal = item.main.temp;
        String unit = prefManager.getTempUnit();
        if (unit.equals("°F")) {
            tempVal = (tempVal * 1.8) + 32;
        }
        holder.temp.setText(Math.round(tempVal) + unit);

        // 2. Set Condition (Capitalizing the first letter)
        String desc = item.weather.get(0).description;
        holder.condition.setText(desc.substring(0, 1).toUpperCase() + desc.substring(1));

        // 3. Set the Day Name using our helper method
        holder.day.setText(getDayName(item.dateText));

        // 4. Handle the Dynamic Theme Colors
        holder.day.setTextColor(textColor);
        holder.temp.setTextColor(textColor);
        holder.condition.setTextColor(textColor);

        String iconCode = item.weather.get(0).icon;
        String iconUrl = "https://openweathermap.org/img/wn/" + iconCode + "@2x.png";

        Glide.with(holder.itemView.getContext())
                .load(iconUrl)
                        .into(holder.imgWeatherIcon);

        // 5. Click Listener to trigger the Chart Switch in MainActivity
        holder.itemView.setOnClickListener(v -> {
            int currentPos = holder.getAdapterPosition();
            if (currentPos != RecyclerView.NO_POSITION) {
                listener.onDayClick(currentPos);
            }
        });
    }

    // Helper method to convert "yyyy-MM-dd HH:mm:ss" to "Wednesday"
    private String getDayName(String dateStr) {
        try {
            java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.util.Date date = inFormat.parse(dateStr);
            long timeInMillis = date.getTime();

            // 1. CHECK IF IT IS TODAY
            if (android.text.format.DateUtils.isToday(timeInMillis)) {
                return "Today";
            }

            // 2. CHECK IF IT IS TOMORROW
            if (isTomorrow(timeInMillis)) {
                return "Tomorrow";
            }

            // 3. Otherwise, return the full Day Name (e.g., Monday)
            java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault());
            return outFormat.format(date);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Helper method to accurately check for tomorrow
    private boolean isTomorrow(long timeInMillis) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1);

        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTimeInMillis(timeInMillis);

        return tomorrow.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
                tomorrow.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR);
    }

    @Override
    public int getItemCount() {
        if (dayList == null) return 0;

        // If the list has more than 3 items, only show 3.
        // Otherwise, show whatever is available (1 or 2).
        return Math.min(dayList.size(), 3);
    }

    // THIS IS THE MISSING PIECE THAT FIXES YOUR ERROR
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView day, temp, condition;
        ImageView imgWeatherIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            day = itemView.findViewById(R.id.txtDay);
            temp = itemView.findViewById(R.id.txtTemp);
            condition = itemView.findViewById(R.id.txtCondition);
            imgWeatherIcon = itemView.findViewById(R.id.imgWeatherIcon);
        }
    }
}