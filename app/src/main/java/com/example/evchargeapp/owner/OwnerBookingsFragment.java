package com.example.evchargeapp.owner;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.BookingsService;
import com.example.evchargeapp.api.StationsService;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.StationDto;
import com.example.evchargeapp.ui.BookingDetailsDialog;
import com.example.evchargeapp.utils.QrDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerBookingsFragment extends Fragment {

    private RecyclerView rv;
    private TextView tvEmpty;
    private EditText etSearch;
    private Spinner spFilter;

    private OwnerBookingsAdapter adapter;
    private BookingsService api;
    private StationsService stationsApi;

    private final Map<String, StationDto> stationMap = new HashMap<>();
    private final List<BookingDto> rawData = new ArrayList<>(); // last loaded from server

    // Filter state
    private enum Filter { ALL, UPCOMING, HISTORY }
    private Filter currentFilter = Filter.ALL;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_owner_bookings, c, false);

        rv = v.findViewById(R.id.rvBookings);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        spFilter = v.findViewById(R.id.spFilter);
        etSearch = v.findViewById(R.id.etSearch);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OwnerBookingsAdapter(new OwnerBookingsAdapter.RowAction() {
            @Override public void onDetails(BookingDto b) {
                StationDto st = stationMap.get(b.stationId);
                BookingDetailsDialog.show(requireActivity().getSupportFragmentManager(), b, st);
            }
            @Override public void onEdit(BookingDto b) {
                Intent i = new Intent(requireContext(), BookingCreateEditActivity.class);
                i.putExtra("bookingId", b.id);
                startActivity(i);
            }
            @Override public void onCancel(BookingDto b) { cancelBooking(b.id); }
            @Override public void onQr(BookingDto b) {
                if (b.qrCode == null || b.qrCode.trim().isEmpty()) {
                    Toast.makeText(requireContext(), R.string.qr_not_available, Toast.LENGTH_SHORT).show();
                } else {
                    QrDialog.display(requireActivity().getSupportFragmentManager(), b.qrCode);
                }
            }
            @Override public void onNavigate(BookingDto b, StationDto st) {
                if (st == null) {
                    Toast.makeText(requireContext(), R.string.na, Toast.LENGTH_SHORT).show();
                    return;
                }
                String label = (st.name != null && !st.name.trim().isEmpty()) ? st.name : st.stationId;
                String uri = String.format(Locale.US, "geo:%f,%f?q=%f,%f(%s)",
                        st.latitude, st.longitude, st.latitude, st.longitude,
                        Uri.encode(label));
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                intent.setPackage("com.google.android.apps.maps");
                try { startActivity(intent); }
                catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri))); }
            }
        });
        rv.setAdapter(adapter);

        v.findViewById(R.id.fabAddBooking).setOnClickListener(btn ->
                startActivity(new Intent(requireContext(), BookingCreateEditActivity.class)));

        String base = getString(R.string.base_url);
        api = ApiClient.get(requireContext(), base).create(BookingsService.class);
        stationsApi = ApiClient.get(requireContext(), base).create(StationsService.class);

        setupFilterUi();
        setupSearch();

        // preload stations for names/addresses
        loadStations();

        return v;
    }

    @Override public void onResume() {
        super.onResume();
        loadFromServer();
    }

    private void setupFilterUi() {
        List<String> items = new ArrayList<>();
        items.add(getString(R.string.filter_all));       // index 0
        items.add(getString(R.string.filter_upcoming));  // index 1
        items.add(getString(R.string.filter_history));   // index 2
        ArrayAdapter<String> ad = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, items);
        spFilter.setAdapter(ad);

        spFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                Filter next = (position == 1) ? Filter.UPCOMING : (position == 2) ? Filter.HISTORY : Filter.ALL;
                if (next != currentFilter) {
                    currentFilter = next;
                    loadFromServer(); // fetch correct set
                } else {
                    applyClientFilter(); // same list, just re-filter search
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyClientFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadStations() {
        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                if (!isAdded()) return;
                stationMap.clear();
                if (res.isSuccessful() && res.body() != null) {
                    for (StationDto s : res.body()) stationMap.put(s.stationId, s);
                }
                adapter.setStationsMap(stationMap);
            }
            @Override public void onFailure(Call<List<StationDto>> call, Throwable t) {
                if (!isAdded()) return;
                adapter.setStationsMap(stationMap);
            }
        });
    }

    /** Load from server according to currentFilter (All / Upcoming / History). */
    private void loadFromServer() {
        tvEmpty.setVisibility(View.GONE);

        Callback<List<BookingDto>> cb = new Callback<List<BookingDto>>() {
            @Override public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> res) {
                if (!isAdded()) return;
                if (res.isSuccessful() && res.body()!=null) {
                    rawData.clear();
                    rawData.addAll(res.body());
                    applyClientFilter(); // apply search to freshly loaded data
                } else {
                    Toast.makeText(requireContext(), R.string.error_loading_bookings, Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        };

        if (currentFilter == Filter.UPCOMING) {
            api.getMyUpcoming().enqueue(cb);
        } else if (currentFilter == Filter.HISTORY) {
            api.getMyHistory().enqueue(cb);
        } else { // ALL
            api.getMyBookings().enqueue(cb);
        }
    }

    /** Apply text search to rawData and update adapter. */
    private void applyClientFilter() {
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        List<BookingDto> out = new ArrayList<>();
        if (q.isEmpty()) {
            out.addAll(rawData);
        } else {
            for (BookingDto b : rawData) {
                String status = (b.status == null ? "" : b.status.toLowerCase(Locale.getDefault()));
                StationDto st = stationMap.get(b.stationId);
                String name = (st != null && st.name != null) ? st.name.toLowerCase(Locale.getDefault()) : "";
                String sid  = (b.stationId == null ? "" : b.stationId.toLowerCase(Locale.getDefault()));
                if (status.contains(q) || name.contains(q) || sid.contains(q)) {
                    out.add(b);
                }
            }
        }
        adapter.setItems(out);
        tvEmpty.setVisibility(out.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void cancelBooking(String id) {
        api.cancel(id).enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    Toast.makeText(requireContext(), R.string.cancelled, Toast.LENGTH_SHORT).show();
                    loadFromServer();
                } else {
                    Toast.makeText(requireContext(), R.string.cannot_cancel_rule, Toast.LENGTH_LONG).show();
                }
            }
            @Override public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
