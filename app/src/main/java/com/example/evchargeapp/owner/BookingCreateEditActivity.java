package com.example.evchargeapp.owner;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.BookingsService;
import com.example.evchargeapp.api.StationsService;
import com.example.evchargeapp.models.BookingCreateRequest;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.BookingUpdateRequest;
import com.example.evchargeapp.models.StationDto;

import java.text.SimpleDateFormat;
import java.util.*;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BookingCreateEditActivity extends AppCompatActivity {

    private TextView tvTitle, tvDate, tvStart, tvEnd, tvAvailability, tvStationHint;
    private Spinner spStation;
    private Button btnSave, btnCancel;

    private StationsService stationsApi;
    private BookingsService bookingsApi;

    private final List<StationDto> stations = new ArrayList<>();
    private final List<String> stationLabels = new ArrayList<>();

    // local-time selections
    private final Calendar dateLocal  = Calendar.getInstance();
    private final Calendar startLocal = Calendar.getInstance();
    private final Calendar endLocal   = Calendar.getInstance();

    private @Nullable String editingId = null;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_create_edit);

        tvTitle        = findViewById(R.id.tvTitle);
        spStation      = findViewById(R.id.spStation);
        tvStationHint  = findViewById(R.id.tvStationHint);
        tvDate         = findViewById(R.id.tvDate);
        tvStart        = findViewById(R.id.tvStart);
        tvEnd          = findViewById(R.id.tvEnd);
        tvAvailability = findViewById(R.id.tvAvailability);
        btnSave        = findViewById(R.id.btnSave);
        btnCancel      = findViewById(R.id.btnCancel);

        String base = getString(R.string.base_url);
        stationsApi = ApiClient.get(this, base).create(StationsService.class);
        bookingsApi = ApiClient.get(this, base).create(BookingsService.class);

        // defaults: snap to 30-min grid; end = +60m
        startLocal.set(Calendar.MINUTE, (startLocal.get(Calendar.MINUTE) / 30) * 30);
        startLocal.set(Calendar.SECOND, 0);
        startLocal.set(Calendar.MILLISECOND, 0);
        endLocal.setTimeInMillis(startLocal.getTimeInMillis() + 60 * 60 * 1000);
        syncDay(dateLocal, startLocal);

        tvDate.setOnClickListener(v -> pickDate());
        tvStart.setOnClickListener(v -> pickTime(true));
        tvEnd.setOnClickListener(v -> pickTime(false));
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        spStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { loadAvailability(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Are we editing?
        editingId = getIntent().getStringExtra("bookingId");
        if (editingId != null) tvTitle.setText(R.string.booking_edit);

        loadStations(() -> {
            if (editingId != null) fetchBooking(editingId);
            renderSelections();
        });
    }

    private void loadStations(Runnable after) {
        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                stations.clear(); stationLabels.clear();
                if (res.isSuccessful() && res.body() != null) {
                    for (StationDto s : res.body()) {
                        if (s.isActive) {
                            stations.add(s);
                            String label = (s.name!=null && !s.name.trim().isEmpty())
                                    ? s.name + " (" + s.stationId + ")"
                                    : s.stationId;
                            stationLabels.add(label);
                        }
                    }
                }
                ArrayAdapter<String> ad = new ArrayAdapter<>(BookingCreateEditActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, stationLabels);
                spStation.setAdapter(ad);
                after.run();
            }
            @Override public void onFailure(Call<List<StationDto>> call, Throwable t) {
                Toast.makeText(BookingCreateEditActivity.this, "Failed to load stations", Toast.LENGTH_SHORT).show();
                ArrayAdapter<String> ad = new ArrayAdapter<>(BookingCreateEditActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, stationLabels);
                spStation.setAdapter(ad);
                after.run();
            }
        });
    }

    private void fetchBooking(String id) {
        bookingsApi.getById(id).enqueue(new Callback<BookingDto>() {
            @Override public void onResponse(Call<BookingDto> call, Response<BookingDto> res) {
                if (!res.isSuccessful() || res.body() == null) {
                    Toast.makeText(BookingCreateEditActivity.this, "Failed to load booking", Toast.LENGTH_SHORT).show();
                    return;
                }
                BookingDto b = res.body();

                // station (CANNOT change station when editing – backend disallows it)
                int idx = 0;
                for (int i = 0; i < stations.size(); i++) {
                    if (stations.get(i).stationId.equals(b.stationId)) { idx = i; break; }
                }
                spStation.setSelection(idx);
                spStation.setEnabled(false);
                tvStationHint.setVisibility(View.VISIBLE);

                // times (ISO -> local calendars)
                Long s = parseIsoUtc(b.startTimeUtc);
                Long e = parseIsoUtc(b.endTimeUtc);
                if (s != null) startLocal.setTimeInMillis(s);
                if (e != null) endLocal.setTimeInMillis(e);
                syncDay(dateLocal, startLocal);

                renderSelections();
            }
            @Override public void onFailure(Call<BookingDto> call, Throwable t) {
                Toast.makeText(BookingCreateEditActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSelections() {
        tvDate.setText(fmtLocal(dateLocal.getTimeInMillis(), "yyyy-MM-dd"));
        tvStart.setText(fmtLocal(startLocal.getTimeInMillis(), "HH:mm"));
        tvEnd.setText(fmtLocal(endLocal.getTimeInMillis(), "HH:mm"));
        loadAvailability();
    }

    private void pickDate() {
        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    dateLocal.set(Calendar.YEAR, y);
                    dateLocal.set(Calendar.MONTH, m);
                    dateLocal.set(Calendar.DAY_OF_MONTH, d);
                    syncDay(startLocal, dateLocal);
                    syncDay(endLocal, dateLocal);
                    renderSelections();
                },
                dateLocal.get(Calendar.YEAR),
                dateLocal.get(Calendar.MONTH),
                dateLocal.get(Calendar.DAY_OF_MONTH));
        dlg.show();
    }

    private void pickTime(boolean start) {
        Calendar target = start ? startLocal : endLocal;
        new TimePickerDialog(this, (view, hour, minute) -> {
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute - (minute % 5)); // 5-minute snap
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            renderSelections();
        }, target.get(Calendar.HOUR_OF_DAY), target.get(Calendar.MINUTE), true).show();
    }

    private void loadAvailability() {
        int pos = spStation.getSelectedItemPosition();
        if (pos < 0 || pos >= stations.size()) {
            tvAvailability.setText(getString(R.string.availability_hint));
            return;
        }
        String stationId = stations.get(pos).stationId;
        String date = fmtUtc(dateLocal.getTimeInMillis(), "yyyy-MM-dd");

        bookingsApi.availability(stationId, date).enqueue(new Callback<BookingsService.AvailabilityResponse>() {
            @Override public void onResponse(Call<BookingsService.AvailabilityResponse> call, Response<BookingsService.AvailabilityResponse> res) {
                if (!res.isSuccessful() || res.body()==null || res.body().availability==null) {
                    tvAvailability.setText(getString(R.string.availability_hint)); return;
                }
                // Render a few slots around chosen hour
                String hour = fmtLocal(startLocal.getTimeInMillis(), "HH");
                StringBuilder sb = new StringBuilder();
                int shown = 0;
                for (BookingsService.AvailabilityResponse.Slot s : res.body().availability) {
                    if (s.time.startsWith(hour) && shown < 4) {
                        if (shown == 0) sb.append("Availability: ");
                        else sb.append("  |  ");
                        sb.append(s.time).append(" → ").append(s.availableSlots);
                        shown++;
                    }
                }
                tvAvailability.setText(shown == 0 ? getString(R.string.availability_hint) : sb.toString());
            }
            @Override public void onFailure(Call<BookingsService.AvailabilityResponse> call, Throwable t) {
                tvAvailability.setText(getString(R.string.availability_hint));
            }
        });
    }

    private void save() {
        int pos = spStation.getSelectedItemPosition();
        if (pos < 0 || pos >= stations.size()) { Toast.makeText(this, "Select a station", Toast.LENGTH_SHORT).show(); return; }

        if (endLocal.getTimeInMillis() <= startLocal.getTimeInMillis()) {
            Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show(); return;
        }

        String stationId = stations.get(pos).stationId;
        String isoStart = toIsoUtc(startLocal.getTimeInMillis());
        String isoEnd   = toIsoUtc(endLocal.getTimeInMillis());

        if (editingId == null) {
            BookingCreateRequest body = new BookingCreateRequest();
            body.stationId = stationId;
            body.startTimeUtc = isoStart;
            body.endTimeUtc   = isoEnd;

            bookingsApi.create(body).enqueue(new Callback<BookingDto>() {
                @Override public void onResponse(Call<BookingDto> call, Response<BookingDto> res) {
                    if (res.isSuccessful()) { Toast.makeText(BookingCreateEditActivity.this, "Created", Toast.LENGTH_SHORT).show(); finish(); }
                    else Toast.makeText(BookingCreateEditActivity.this, "Server rejected (7-day / overlap / 12h rules)", Toast.LENGTH_LONG).show();
                }
                @Override public void onFailure(Call<BookingDto> call, Throwable t) {
                    Toast.makeText(BookingCreateEditActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // EDIT: backend only allows time change (not station)
            BookingUpdateRequest body = new BookingUpdateRequest();
            body.startTimeUtc = isoStart;
            body.endTimeUtc   = isoEnd;

            bookingsApi.update(editingId, body).enqueue(new Callback<Void>() {
                @Override public void onResponse(Call<Void> call, Response<Void> res) {
                    if (res.isSuccessful()) { Toast.makeText(BookingCreateEditActivity.this, "Updated", Toast.LENGTH_SHORT).show(); finish(); }
                    else Toast.makeText(BookingCreateEditActivity.this, "Update refused (12h / overlap / 7-day)", Toast.LENGTH_LONG).show();
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(BookingCreateEditActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    // ---- time helpers (API 24 safe) ----
    private static void syncDay(Calendar target, Calendar day){
        target.set(Calendar.YEAR, day.get(Calendar.YEAR));
        target.set(Calendar.MONTH, day.get(Calendar.MONTH));
        target.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
    }

    private static Long parseIsoUtc(String iso){
        if (iso == null) return null;
        String[] p = new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm'Z'"};
        for (String s : p){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(s, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d != null) return d.getTime();
            } catch (Exception ignored){}
        }
        return null;
    }

    private static String toIsoUtc(long ms){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date(ms));
    }
    private static String fmtLocal(long ms, String pattern){
        SimpleDateFormat out = new SimpleDateFormat(pattern, Locale.getDefault());
        out.setTimeZone(TimeZone.getDefault());
        return out.format(new Date(ms));
    }
    private static String fmtUtc(long ms, String pattern){
        SimpleDateFormat out = new SimpleDateFormat(pattern, Locale.US);
        out.setTimeZone(TimeZone.getTimeZone("UTC"));
        return out.format(new Date(ms));
    }
}
