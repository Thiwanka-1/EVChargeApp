package com.example.evchargeapp.owner;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.BookingsService;
import com.example.evchargeapp.api.StationsService;
import com.example.evchargeapp.models.StationDto;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Owner -> Availability tab
 * - Pick a station & LOCAL date
 * - See 30-minute LOCAL slots for the day with free slot counts
 * Matches backend: GET /api/bookings/station/{id}/availability?dateLocal=yyyy-MM-dd&tzOffsetMinutes=330
 */
public class OwnerAvailabilityFragment extends Fragment {

    private static final int TZ_OFFSET_MINUTES_SL = 330; // Sri Lanka UTC+5:30

    private Spinner spStation;
    private TextView tvDate, tvMeta;
    private RecyclerView rv;
    private ProgressBar progress;
    private View empty;

    private StationsService stationsApi;
    private BookingsService bookingsApi;

    private final List<StationDto> stations = new ArrayList<>();
    private final List<String> stationLabels = new ArrayList<>();

    private final Calendar selectedDayLocal = Calendar.getInstance();

    private AvailabilityAdapter adapter;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_owner_availability, c, false);

        spStation = v.findViewById(R.id.spStation);
        tvDate    = v.findViewById(R.id.tvDate);
        tvMeta    = v.findViewById(R.id.tvMeta);
        rv        = v.findViewById(R.id.rvSlots);
        progress  = v.findViewById(R.id.progress);
        empty     = v.findViewById(R.id.viewEmpty);

        String base = getString(R.string.base_url);
        stationsApi  = ApiClient.get(requireContext(), base).create(StationsService.class);
        bookingsApi  = ApiClient.get(requireContext(), base).create(BookingsService.class);

        // 6 columns = 48 half-hour cells
        rv.setLayoutManager(new GridLayoutManager(requireContext(), 6));
        adapter = new AvailabilityAdapter();
        rv.setAdapter(adapter);

        tvDate.setText(fmtLocal(selectedDayLocal.getTimeInMillis(), "yyyy-MM-dd"));
        tvDate.setOnClickListener(v1 -> pickDate());

        spStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { loadAvailability(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        loadStations();

        return v;
    }

    private void pickDate(){
        DatePickerDialog dlg = new DatePickerDialog(requireContext(),
                (view, y, m, d) -> {
                    selectedDayLocal.set(Calendar.YEAR, y);
                    selectedDayLocal.set(Calendar.MONTH, m);
                    selectedDayLocal.set(Calendar.DAY_OF_MONTH, d);
                    tvDate.setText(fmtLocal(selectedDayLocal.getTimeInMillis(), "yyyy-MM-dd"));
                    loadAvailability();
                },
                selectedDayLocal.get(Calendar.YEAR),
                selectedDayLocal.get(Calendar.MONTH),
                selectedDayLocal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void loadStations() {
        progress.setVisibility(View.VISIBLE);
        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                progress.setVisibility(View.GONE);
                stations.clear(); stationLabels.clear();
                if (res.isSuccessful() && res.body()!=null) {
                    for (StationDto s : res.body()) {
                        if (!s.isActive) continue;
                        stations.add(s);
                        stationLabels.add((s.name!=null && !s.name.trim().isEmpty()) ? s.name + " ("+s.stationId+")" : s.stationId);
                    }
                }
                ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, stationLabels);
                spStation.setAdapter(ad);

                if (!stations.isEmpty()) loadAvailability();
                else showEmpty(getString(R.string.no_data));
            }
            @Override public void onFailure(Call<List<StationDto>> call, Throwable t) {
                progress.setVisibility(View.GONE);
                showEmpty(getString(R.string.av_meta_failed));
                ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, stationLabels);
                spStation.setAdapter(ad);
            }
        });
    }

    private void loadAvailability() {
        int pos = spStation.getSelectedItemPosition();
        if (pos < 0 || pos >= stations.size()) { showEmpty(getString(R.string.select_station)); return; }

        StationDto st = stations.get(pos);
        String stationId = st.stationId;
        String stationName = (st.name!=null && !st.name.trim().isEmpty()) ? st.name : st.stationId;

        // BACKEND NOW EXPECTS LOCAL DAY STRING
        String dateLocal = fmtLocal(selectedDayLocal.getTimeInMillis(), "yyyy-MM-dd");

        progress.setVisibility(View.VISIBLE);
        tvMeta.setText(getString(R.string.av_meta_loading, stationName));

        bookingsApi.availability(stationId, dateLocal, TZ_OFFSET_MINUTES_SL)
                .enqueue(new Callback<BookingsService.AvailabilityResponse>() {
                    @Override public void onResponse(Call<BookingsService.AvailabilityResponse> call, Response<BookingsService.AvailabilityResponse> res) {
                        progress.setVisibility(View.GONE);
                        if (!res.isSuccessful() || res.body()==null || res.body().availability==null) {
                            showEmpty(getString(R.string.av_meta_failed));
                            return;
                        }
                        adapter.setItems(res.body().availability);
                        if (res.body().availability.isEmpty()) showEmpty(getString(R.string.no_data));
                        else {
                            empty.setVisibility(View.GONE);
                            rv.setVisibility(View.VISIBLE);
                            tvMeta.setText(getString(R.string.av_meta_done, stationName, dateLocal));
                        }
                    }
                    @Override public void onFailure(Call<BookingsService.AvailabilityResponse> call, Throwable t) {
                        progress.setVisibility(View.GONE);
                        showEmpty(getString(R.string.av_meta_failed));
                    }
                });
    }

    private void showEmpty(String msg){
        rv.setVisibility(View.GONE);
        empty.setVisibility(View.VISIBLE);
        TextView tv = empty.findViewById(R.id.tvEmptyMsg);
        tv.setText(msg);
    }

    private static String fmtLocal(long ms, String pattern){
        SimpleDateFormat out = new SimpleDateFormat(pattern, Locale.getDefault());
        out.setTimeZone(TimeZone.getDefault());
        return out.format(new Date(ms));
    }

    // ---------------- Adapter ----------------

    static class AvailabilityAdapter extends RecyclerView.Adapter<AvailabilityAdapter.VH> {
        private final List<BookingsService.AvailabilityResponse.Slot> data = new ArrayList<>();

        void setItems(List<BookingsService.AvailabilityResponse.Slot> items){
            data.clear();
            if (items != null) data.addAll(items);
            notifyDataSetChanged();
        }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_availability_cell, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            BookingsService.AvailabilityResponse.Slot s = data.get(pos);
            h.tvTime.setText(s.time); // "HH:mm" (LOCAL)
            h.tvSlots.setText(String.valueOf(s.availableSlots));

            int bg;
            if (s.availableSlots <= 0) bg = R.drawable.bg_cell_red;
            else if (s.availableSlots <= 2) bg = R.drawable.bg_cell_amber;
            else bg = R.drawable.bg_cell_green;
            h.root.setBackgroundResource(bg);
        }

        @Override public int getItemCount() { return data.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvTime, tvSlots; View root;
            VH(@NonNull View v){
                super(v);
                root   = v.findViewById(R.id.cellRoot);
                tvTime = v.findViewById(R.id.tvTime);
                tvSlots= v.findViewById(R.id.tvSlots);
            }
        }
    }
}
