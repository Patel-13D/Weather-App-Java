package com.example.weatherapp;

import android.content.Context;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import com.example.weatherapp.network.PrefManager;

public class Setting extends AppCompatActivity {

    private PrefManager prefManager;
    private SwitchCompat switchNightUpdate;
    private ImageView btnBack;
    private AutoCompleteTextView autoTemp, autoWind, autoPressure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        prefManager = new PrefManager(this);
        initViews();
        setupSpinners();
        setupSwitch();

        btnBack.setOnClickListener(v -> {
            triggerVibration(this);
            finish();
        });
    }

    private void initViews() {
        autoTemp = findViewById(R.id.autoCompleteTemp); // Add this
        autoWind = findViewById(R.id.autoCompleteWind);
        autoPressure = findViewById(R.id.autoCompletePressure);
        switchNightUpdate = findViewById(R.id.switchNightUpdate);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupSpinners() {
        setupSingleDropdown(autoTemp, new String[]{"°C", "°F"}, prefManager.getTempUnit(), "temp");
        setupSingleDropdown(autoWind, new String[]{"km/h", "mph", "m/s"}, prefManager.getWindUnit(), "wind");
        setupSingleDropdown(autoPressure, new String[]{"hPa", "mbar", "inHg"}, prefManager.getPressureUnit(), "pressure");
    }

    private void setupSingleDropdown(AutoCompleteTextView tv, String[] options, String savedValue, String type) {
        // Custom Adapter to handle the literal checkmark logic manually
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, options) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                android.widget.CheckedTextView checkedTextView = view.findViewById(android.R.id.text1);

                String item = getItem(position);
                // Compare the item to the current saved preference
                if (item != null && item.equals(savedValue)) {
                    checkedTextView.setChecked(true);
                } else {
                    checkedTextView.setChecked(false);
                }
                return view;
            }
        };

        tv.setAdapter(adapter);
        tv.setText(savedValue, false);

        tv.setOnItemClickListener((parent, view, position, id) -> {
            String selected = options[position];

            // 1. Save selection
            switch (type) {
                case "temp": prefManager.setTempUnit(selected); break;
                case "wind": prefManager.setWindUnit(selected); break;
                case "pressure": prefManager.setPressureUnit(selected); break;
            }

            // 2. Instant Physical Feedback
            triggerVibration(Setting.this);

            // 3. REFRESH: Re-run setup for this specific view to update the checkmark state
            setupSingleDropdown(tv, options, selected, type);
        });
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

    private void setupSwitch() {
        switchNightUpdate.setChecked(prefManager.isNightUpdateEnabled());
        switchNightUpdate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefManager.setNightUpdate(isChecked);
            if (isChecked) {
                Toast.makeText(this, "Auto-update enabled for 23:00 - 07:00", Toast.LENGTH_SHORT).show();
            }
        });
    }
}