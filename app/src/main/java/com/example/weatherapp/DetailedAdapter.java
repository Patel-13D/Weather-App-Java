package com.example.weatherapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.models.ForecastResponse;
import com.example.weatherapp.network.PrefManager;

import java.util.List;

public class DetailedAdapter extends RecyclerView.Adapter<DetailedAdapter.ViewHolder> {
    private List<ForecastResponse.ForecastItem> list;
    private String type;
    private PrefManager prefManager;

    public DetailedAdapter(List<ForecastResponse.ForecastItem> list, String type, PrefManager prefManager) {
        this.list = list;
        this.type = type.toLowerCase(); // "pressure", "humidity", or "wind"
        this.prefManager = prefManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse your item_daily or create a simple item_detailed row
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_detailed_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ForecastResponse.ForecastItem item = list.get(position);

        // 1. Reuse your Date Formatting logic
        holder.tvTime.setText(getDateTimeLabel(item.dateText));

        // 2. Dynamic Value Logic based on the card clicked
        String valueStr = "";
        switch (type) {
            case "pressure":
                double press = item.main.pressure;
                String pUnit = prefManager.getPressureUnit();
                if (pUnit.equals("inHg")) press *= 0.02953;
                valueStr = Math.round(press) + " " + pUnit;
                break;

            case "humidity":
                valueStr = item.main.humidity + "%";
                break;

            case "wind":
                double wind = item.wind.speed;
                String wUnit = prefManager.getWindUnit();
                if (wUnit.equals("mph")) wind *= 2.237;
                else if (wUnit.equals("km/h")) wind *= 3.6;
                valueStr = String.format("%.1f %s", wind, wUnit);
                break;
            case "visibility":
                // Converting meters (from API) to km for a cleaner look
                double visKm = item.visibility / 1000.0;
                valueStr = String.format("%.1f km", visKm);
                break;

        }
        holder.tvValue.setText(valueStr);
    }

    @Override
    public int getItemCount() {
        return list.size(); // Show full 5-day list (approx 40 items)
    }

    private String getDateTimeLabel(String dateStr) {
        try {
            java.text.SimpleDateFormat in = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            java.text.SimpleDateFormat out = new java.text.SimpleDateFormat("EEE, hh a", java.util.Locale.getDefault());
            return out.format(in.parse(dateStr));
        } catch (Exception e) { return dateStr; }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvValue;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTime = itemView.findViewById(R.id.tvRowTime);
            tvValue = itemView.findViewById(R.id.tvRowValue);
        }
    }
}