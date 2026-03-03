package com.example.weatherapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.example.weatherapp.models.WeatherResponse;
import com.example.weatherapp.network.PrefManager;
import com.example.weatherapp.viewmodel.WeatherViewModel;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class AddNewLocation extends AppCompatActivity {
    private ImageView btnBack;
    private EditText etSearchCity;
    private PrefManager prefManager;
    private RecyclerView rvAddCities;
    private ImageView search;
    private CityAdapter adapter ;
    private List<WeatherResponse> cityList = new ArrayList<>();
    private WeatherViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_new_location);
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(WeatherViewModel.class);

        btnBack = findViewById(R.id.btnBack);
        etSearchCity = findViewById(R.id.etSearchCity);
        rvAddCities = findViewById(R.id.rvAddCities);
        search = findViewById(R.id.imgSearch);
        MaterialCardView searchCard = findViewById(R.id.searchCard);
        // Inside AddNewLocation.java onCreate

        // Inside onCreate
        ImageView cleanAll = findViewById(R.id.cleanAll);
        cleanAll.setOnClickListener(v -> {
            if (cityList.size() > 1) {
                // Pass null and -1 because we aren't swiping a specific card
                showCustomDeleteDialog(null, -1, true);
            } else {
                Toast.makeText(this, "Nothing to clear", Toast.LENGTH_SHORT).show();
            }
        });

        searchCard.setRippleColor(android.content.res.ColorStateList.valueOf(Color.parseColor("#33000000")));
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

        prefManager = new PrefManager(this);

        // 2. Receive the "Live" location from Intent
        WeatherResponse currentLoc = (WeatherResponse) getIntent().getSerializableExtra("current_location_weather");

        // 3. Load "History" from local storage
        List<WeatherResponse> savedCities = prefManager.getSavedCities();

        // 4. Combine them
        cityList.clear();
        if (currentLoc != null) {
            cityList.add(currentLoc); // Put GPS location at the top
        }

        for (WeatherResponse saved : savedCities) {
            // Avoid adding the current city twice if it was already saved
            if (currentLoc == null || !saved.cityName.equalsIgnoreCase(currentLoc.cityName)) {
                cityList.add(saved);
            }
        }

        // 5. Setup RecyclerView with a click listener to return data to MainActivity
        adapter = new CityAdapter(cityList, selectedCity -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("selected_city_data", selectedCity);
            setResult(RESULT_OK, resultIntent);
            finish(); // Close this activity and go back to Main
        });
      rvAddCities.setLayoutManager(new LinearLayoutManager(this));
      rvAddCities.setAdapter(adapter);

        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                // If the item is at position 0, return 0 for swipe flags to disable sliding
                if (viewHolder.getAdapterPosition() == 0) {
                    return makeMovementFlags(0, 0);
                }
                return super.getMovementFlags(recyclerView, viewHolder);
            }
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                if (position == 0) {
                    // Bounce back if it's the current location
                    adapter.notifyItemChanged(0);
                    Toast.makeText(AddNewLocation.this, "Cannot delete current location", Toast.LENGTH_SHORT).show();
                } else {
                    showCustomDeleteDialog(viewHolder, position, false);
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState, boolean isCurrentlyActive) {

                if (viewHolder.itemView.getTranslationX() == 0 && !isCurrentlyActive) {
                    super.onChildDraw(c, recyclerView, viewHolder, 0, dY, actionState, false);
                    return;
                }

                // 1. Calculate button width (approx 20-25% of the card width)
                float buttonWidth = viewHolder.itemView.getWidth() * 0.25f;

                // 2. Cap the swipe so it stops exactly where the button is
                float limitedDx = Math.max(dX, -buttonWidth);

                // 3. Draw the Red Button area
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, limitedDx, dY, actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(Color.parseColor("#E53935")) // Red color
                        .addSwipeLeftActionIcon(R.drawable.delete)
                        .setSwipeLeftActionIconTint(Color.WHITE)
                        .create()
                        .decorate();

                // 4. IMPORTANT: Pass limitedDx to super, NOT dX
                super.onChildDraw(c, recyclerView, viewHolder, limitedDx, dY, actionState, isCurrentlyActive);
            }
        };
// Create the helper and link it to your RecyclerView
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(rvAddCities);

        searchCard.setOnClickListener(v -> {
            etSearchCity.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearchCity, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });

// Add this: Visual feedback when the EditText is active
        etSearchCity.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Optional: darken the card slightly or change stroke to show it's active
                searchCard.setStrokeWidth(2);
                searchCard.setStrokeColor(Color.parseColor("#4DFFFFFF"));
            } else {
                searchCard.setStrokeWidth(0);
            }
        });

// Optional: Make the whole card clickable to focus the EditText
        searchCard.setOnClickListener(v -> {
            etSearchCity.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(etSearchCity, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        });

// 1. Logic for clicking the Magnifying Glass icon
        search.setOnClickListener(v -> {
            v.startAnimation(AnimationUtils.loadAnimation(this, R.anim.btn_press));

            // 2. Add Haptic feedback
            triggerVibration(this);
            String query = etSearchCity.getText().toString().trim();
            performSearch(query);
        });

// 2. Logic for hitting "Search" on the Keyboard
        etSearchCity.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearchCity.getText().toString().trim();
                performSearch(query);
                return true;
            }
            return false;
        });


        if (cityList != null && !cityList.isEmpty()) {
            refreshSavedCities();
        }

    }

    private void showCustomDeleteDialog(RecyclerView.ViewHolder viewHolder, int position, boolean isDeleteAll) {
        // 1. Inflate the custom XML layout
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_confirm, null);

        // Find the Text components to change text dynamically
        // Check your XML for the correct IDs (e.g., @+id/tvTitle and @+id/tvMessage)
        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);

        Button btnDelete = dialogView.findViewById(R.id.btnDelete);
        Animation bounce = AnimationUtils.loadAnimation(this, R.anim.btn_press);

        // 2. Customize text based on mode
        if (isDeleteAll) {
            if (tvTitle != null) tvTitle.setText("Clear All Cities");
            if (tvMessage != null) tvMessage.setText("Are you sure you want to remove all searched locations?");
        } else {
            if (tvTitle != null) tvTitle.setText("Delete City");
            if (tvMessage != null) tvMessage.setText("Do you want to remove this city from your list?");
        }

        // 3. Create the Dialog
        AlertDialog dialog = new AlertDialog.Builder(AddNewLocation.this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 4. Set up the Delete Button
        btnDelete.setOnClickListener(v -> {
            v.startAnimation(bounce);
            triggerVibration(AddNewLocation.this);

            v.postDelayed(() -> {
                if (isDeleteAll) {
                    // LOGIC FOR DELETE ALL
                    if (cityList.size() > 1) {
                        WeatherResponse currentLoc = cityList.get(0); // Keep GPS location
                        cityList.clear();
                        cityList.add(currentLoc);
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    // LOGIC FOR SINGLE DELETE
                    cityList.remove(position);
                    adapter.notifyItemRemoved(position);
                }

                prefManager.saveCities(cityList);
                dialog.dismiss();
            }, 150);
        });

        // 5. Set up the Cancel Button
        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

        // 6. Reset swipe logic (Only if it was a single item swipe)
        dialog.setOnDismissListener(d -> {
            if (!isDeleteAll) {
                resetSwipe(viewHolder, position);
            }
        });

        dialog.show();
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
    // Add this helper method inside the itemTouchHelperCallback block
    private void resetSwipe(RecyclerView.ViewHolder viewHolder, int position) {
        // 1. Instantly hide the red background by forcing translation to 0
        viewHolder.itemView.setTranslationX(0f);

        // 2. Delay the adapter refresh slightly so the ItemTouchHelper "releases" the view
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            adapter.notifyItemChanged(position);
        }, 150);
    }

    private void refreshSavedCities() {
        // Loop through the list (skip index 0 if that is your 'Current Location' logic)
        for (int i = 0; i < cityList.size(); i++) {
            final int position = i;
            String cityName = cityList.get(i).cityName;

            viewModel.getWeatherByCityName(cityName, "3a28c58470d5436d01bce8c7c6c70e5f").observe(this, newResponse -> {
                if (newResponse != null) {
                    // Update the object in the list with fresh data
                    cityList.set(position, newResponse);

                    // Tell the adapter only this specific card changed (efficient)
                    adapter.notifyItemChanged(position);

                    // Save the refreshed data back to storage
                    prefManager.saveCities(cityList);
                }
            });
        }
    }

    // 3. The Helper Method (Create this outside onCreate)
    private void performSearch(String query) {
        if (!query.isEmpty()) {
            // Check for duplicates
            boolean isDuplicate = false;
            for (WeatherResponse city : cityList) {
                if (city.cityName.equalsIgnoreCase(query)) {
                    isDuplicate = true;
                    break;
                }
            }

            if (isDuplicate) {
                Toast.makeText(this, "City already in your list!", Toast.LENGTH_SHORT).show();
                etSearchCity.setText("");
                etSearchCity.clearFocus();
                // HIDE KEYBOARD after search
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    // We use etSearchCity.getWindowToken() directly because it's more reliable
                    // than checking getCurrentFocus() which can be null
                    imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
                }
            } else {
                fetchCityWeather(query);
                etSearchCity.setText("");
                etSearchCity.clearFocus();
                // HIDE KEYBOARD after search
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

                if (imm != null) {
                    // We use etSearchCity.getWindowToken() directly because it's more reliable
                    // than checking getCurrentFocus() which can be null
                    imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
                }
            }
        } else {
            Toast.makeText(this, "Please enter a city name", Toast.LENGTH_SHORT).show();
            etSearchCity.setText("");
            etSearchCity.clearFocus();
            // HIDE KEYBOARD after search
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                // We use etSearchCity.getWindowToken() directly because it's more reliable
                // than checking getCurrentFocus() which can be null
                imm.hideSoftInputFromWindow(etSearchCity.getWindowToken(), 0);
            }
        }
    }

    private void fetchCityWeather(String query) {
        viewModel.getWeatherByCityName(query, "3a28c58470d5436d01bce8c7c6c70e5f").observe(this, response -> {
                    if (response != null) {
                        // 1. Check for duplicates
                        boolean alreadyExists = false;
                        for (WeatherResponse city : cityList) {
                            if (city.cityName.equalsIgnoreCase(response.cityName)) {
                                alreadyExists = true;
                                break;
                            }
                        }

                        if (alreadyExists) {
                            Toast.makeText(this, "City already added!", Toast.LENGTH_SHORT).show();
                        } else {
                            // 2. Add the city if it's new
                            int indexToAdd = 1;
                            cityList.add(indexToAdd, response);
                            adapter.notifyItemInserted(indexToAdd);
                            prefManager.saveCities(cityList);
                            etSearchCity.setText("");
                            rvAddCities.scrollToPosition(indexToAdd);
                        }
                    } else {
                        Toast.makeText(this, "Location not found!", Toast.LENGTH_SHORT).show();
                    }

        });
    }
}
