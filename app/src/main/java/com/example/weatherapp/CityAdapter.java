package com.example.weatherapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.weatherapp.models.WeatherResponse;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class CityAdapter extends RecyclerView.Adapter<CityAdapter.ViewHolder> {

    private List<WeatherResponse> list;
    private OnCityClickListener listener;
    public CityAdapter(List<WeatherResponse> list, OnCityClickListener listener) {
        this.list = list;
        this.listener = listener;
    }
    public interface OnCityClickListener {
        void onCityClick(WeatherResponse city);
    }
    @NonNull
    @Override
    public CityAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.add_cities, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CityAdapter.ViewHolder holder, int position) {
        WeatherResponse data = list.get(position);
        holder.tvCity.setText(data.cityName);

        // 4. Set the Click Listener on the whole card
        // Corrected Click Listener
        holder.card.setOnClickListener(v -> {
            triggerVibration(v.getContext());
            if (listener != null) {
                listener.onCityClick(data);
            }
        });
        holder.card.setRippleColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#33000000")));

        if (position == 0) {
            // 1. Get the drawable from resources
            Drawable locationIcon = ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.map);

            if (locationIcon != null) {
                // 2. Adjust Size (e.g., 40px by 40px)
                locationIcon.setBounds(0, 0, 50, 50);

                // 3. Adjust Color (e.g., White or a custom color)
                Drawable wrappedDrawable = DrawableCompat.wrap(locationIcon);
                DrawableCompat.setTint(wrappedDrawable, Color.WHITE);

                // 4. Set the icon (left, top, right, bottom)
                holder.tvCity.setCompoundDrawables(null, null, wrappedDrawable, null);

                // Add some spacing between the icon and the text
                holder.tvCity.setCompoundDrawablePadding(16);
            }
        } else {
            holder.tvCity.setCompoundDrawables(null, null, null, null);
        }
        if (data.weather != null && !data.weather.isEmpty()) {
            holder.tvDec.setText(data.weather.get(0).description);

            // THIS IS THE FIX: Access the id from the first weather object
            int conditionId = data.weather.get(0).id;
            holder.card.setCardBackgroundColor(getWeatherColor(conditionId));
        }
        holder.tvTemp.setText(Math.round(data.main.temp)+ "°");
    }

    private void triggerVibration(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Modern "Tick" effect (clean and subtle)
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK));
            } else {
                // Legacy support (short 10ms buzz)
                vibrator.vibrate(10);
            }
        }
    }
    private int getWeatherColor(int conditionId) {
        if (conditionId >= 200 && conditionId < 600) {
            return Color.parseColor("#4A6274"); // Rainy/Stormy (Muted Blue-Grey)
        } else if (conditionId >= 600 && conditionId < 700) {
            return Color.parseColor("#7097A8"); // Snowy (Light Blue)
        } else if (conditionId == 800) {
            return Color.parseColor("#E67E22"); // Clear/Sunny (Orange)
        } else {
            return Color.parseColor("#5D6D7E"); // Cloudy/Default (Grey)
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static  class ViewHolder extends RecyclerView.ViewHolder{
        MaterialCardView card;
        TextView tvCity, tvDec , tvTemp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvDec = itemView.findViewById(R.id.tvDec);
            tvTemp = itemView.findViewById(R.id.tvTemp);
            card = itemView.findViewById(R.id.card);
        }
    }
}
