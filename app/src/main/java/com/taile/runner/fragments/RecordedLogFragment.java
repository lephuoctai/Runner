package com.taile.runner.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taile.runner.R;
import com.taile.runner.adapters.RunRecordsAdapter;
import com.taile.runner.models.RunRecord;
import com.taile.runner.storage.RunRecordsManager;

import java.util.List;

public class RecordedLogFragment extends Fragment {

    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private RunRecordsAdapter adapter;
    private RunRecordsManager recordsManager;

    public RecordedLogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recorded_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recordsManager = new RunRecordsManager(requireContext());

        // Set up RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewRunRecords);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Set up empty state view
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        // Load and display records
        loadRecords();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecords(); // Refresh data when fragment resumes
    }

    private void loadRecords() {
        List<RunRecord> records = recordsManager.getRecordsSortedByDate();

        // Show empty state if no records
        if (records.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            tvEmptyState.setVisibility(View.GONE);

            // Update adapter with records
            if (adapter == null) {
                adapter = new RunRecordsAdapter(records);
                recyclerView.setAdapter(adapter);
            } else {
                adapter.updateData(records);
                adapter.notifyDataSetChanged();
            }
        }
    }
}
