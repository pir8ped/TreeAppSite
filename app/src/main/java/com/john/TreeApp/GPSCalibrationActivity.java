package com.john.TreeApp;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import gps.GPSCalibrationManager;
import gps.ReferencePoint;

/**
 * Activity for managing GPS calibration settings and reference points.
 */
public class GPSCalibrationActivity extends BaseActivity {

    private GPSCalibrationManager calibrationManager;
    private RecyclerView recyclerView;
    private ReferencePointAdapter adapter;
    private TextView statusText;
    private TextView offsetText;
    private Button clearButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActivityLayout(R.layout.activity_gps_calibration);
        setActionBarTitle("GPS Calibration Settings");

        calibrationManager = GPSCalibrationManager.getInstance(this);

        // Find views
        recyclerView = findViewById(R.id.recycler_reference_points);
        statusText = findViewById(R.id.tv_calibration_status);
        offsetText = findViewById(R.id.tv_offset_info);
        clearButton = findViewById(R.id.btn_clear_calibration);
        Button addButton = findViewById(R.id.btn_add_reference);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReferencePointAdapter(calibrationManager.getReferencePoints());
        recyclerView.setAdapter(adapter);

        // Add button
        addButton.setOnClickListener(v -> {
            // Launch CalibrationRecordActivity in "Create New" mode (no extra)
            Intent intent = new Intent(this, CalibrationRecordActivity.class);
            startActivity(intent);
        });

        // Clear calibration button
        clearButton.setOnClickListener(v -> {
            calibrationManager.clearCalibration();
            updateCalibrationStatus();
            Toast.makeText(this, "Calibration cleared", Toast.LENGTH_SHORT).show();
        });

        updateCalibrationStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh list in case a new point was added or updated
        adapter.updateData(calibrationManager.getReferencePoints());
        updateCalibrationStatus();
    }

    private void updateCalibrationStatus() {
        if (calibrationManager.hasCalibration()) {
            int ageMinutes = calibrationManager.getCalibrationAgeMinutes();
            double offsetMeters = calibrationManager.getOffsetDistanceMeters();

            String status;
            if (calibrationManager.isCalibrationWarning()) {
                status = "⚠️ Calibration aging (" + ageMinutes + " min old)";
            } else if (calibrationManager.isCalibrationValid()) {
                status = "✓ Calibration active (" + ageMinutes + " min old)";
            } else {
                status = "⛔ Calibration expired";
            }

            statusText.setText(status);
            offsetText.setText(String.format("Offset: %.1f meters", offsetMeters));
            offsetText.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
        } else {
            statusText.setText("No active calibration");
            offsetText.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
        }
    }

    /**
     * Adapter for reference point list items.
     */
    private class ReferencePointAdapter extends RecyclerView.Adapter<ReferencePointAdapter.ViewHolder> {

        private List<ReferencePoint> items;

        ReferencePointAdapter(List<ReferencePoint> items) {
            this.items = items;
        }

        void updateData(List<ReferencePoint> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_reference_point, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ReferencePoint rp = items.get(position);
            holder.bind(rp, position);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView nameText;
            TextView coordsText;
            Button calibrateBtn;
            ImageButton deleteBtn;

            ViewHolder(View itemView) {
                super(itemView);
                nameText = itemView.findViewById(R.id.tv_name);
                coordsText = itemView.findViewById(R.id.tv_coordinates);
                calibrateBtn = itemView.findViewById(R.id.btn_calibrate);
                deleteBtn = itemView.findViewById(R.id.btn_delete);
            }

            void bind(ReferencePoint rp, int position) {
                nameText.setText(rp.getName());
                coordsText.setText(rp.getCoordinatesString());

                // Calibrate button
                calibrateBtn.setOnClickListener(v -> {
                    Intent intent = new Intent(GPSCalibrationActivity.this, CalibrationRecordActivity.class);
                    intent.putExtra(CalibrationRecordActivity.EXTRA_REF_INDEX, position);
                    startActivity(intent);
                });

                // Also allow clicking the whole item
                itemView.setOnClickListener(v -> calibrateBtn.performClick());

                // Delete button
                deleteBtn.setOnClickListener(v -> {
                    calibrationManager.deleteReferencePoint(position);
                    updateData(calibrationManager.getReferencePoints());
                });
            }
        }
    }
}
