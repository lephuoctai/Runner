package com.taile.runner.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.taile.runner.R;
import com.taile.runner.models.RunRecord;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class RunRecordsAdapter extends RecyclerView.Adapter<RunRecordsAdapter.ViewHolder> {

    private List<RunRecord> records;
    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

    public RunRecordsAdapter(List<RunRecord> records) {
        this.records = records;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_run_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RunRecord record = records.get(position);

        // Format date and time
        String startDateTime = dateTimeFormat.format(new Date(record.getStartTime()));

        // Format duration
        long durationMillis = record.getDuration();
        String duration = formatDuration(durationMillis);

        // Set data to views
        holder.tvDateTime.setText(startDateTime);
        holder.tvDistance.setText(String.format(Locale.getDefault(), "%.2f km", record.getDistance()));
        holder.tvDuration.setText(duration);
        holder.tvSteps.setText(String.format(Locale.getDefault(), "%d steps", record.getSteps()));
        holder.tvSpeed.setText(String.format(Locale.getDefault(), "%.1f m/s", record.getAvgSpeed()));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void updateData(List<RunRecord> newRecords) {
        this.records = newRecords;
        notifyDataSetChanged();
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvDistance, tvDuration, tvSteps, tvSpeed;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDistance = itemView.findViewById(R.id.tvRecordDistance);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvSteps = itemView.findViewById(R.id.tvRecordSteps);
            tvSpeed = itemView.findViewById(R.id.tvRecordSpeed);
        }
    }
}
