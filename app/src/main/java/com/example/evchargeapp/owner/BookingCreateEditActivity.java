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
import com.example.evchargeapp.ui.BookingReviewDialog;

import java.io.IOException;
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
    private static final int TZ_OFFSET_MINUTES_SL = 330; // UTC+5:30 Sri Lanka

    // local selections
    private final Calendar dateLocal = Calendar.getInstance();
    private final Calendar startLocal = Calendar.getInstance();
    private final Calendar endLocal = Calendar.getInstance();

    private @Nullable String editingId = null;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_create_edit);

        tvTitle = findViewById(R.id.tvTitle);
        spStation = findViewById(R.id.spStation);
        tvDate = findViewById(R.id.tvDate);
        tvStart = findViewById(R.id.tvStart);
        tvEnd = findViewById(R.id.tvEnd);
        tvAvailability = findViewById(R.id.tvAvailability);
        tvStationHint = findViewById(R.id.tvStationHint);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        String base = getString(R.string.base_url);
        stationsApi = ApiClient.get(this, base).create(StationsService.class);
        bookingsApi = ApiClient.get(this, base).create(BookingsService.class);

        // defaults: round to nearest 5 min; end +1h
        roundTo5Min(startLocal);
        endLocal.setTimeInMillis(startLocal.getTimeInMillis() + 60 * 60 * 1000);
        // ensure start is not in the past
        Calendar now = Calendar.getInstance();
        if (startLocal.before(now)) {
            startLocal.setTimeInMillis(now.getTimeInMillis());
            roundTo5Min(startLocal);
            endLocal.setTimeInMillis(startLocal.getTimeInMillis() + 60 * 60 * 1000);
        }

        tvDate.setOnClickListener(v -> pickDate());
        tvStart.setOnClickListener(v -> pickTime(true));
        tvEnd.setOnClickListener(v -> pickTime(false));
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        spStation.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { loadAvailability(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Editing?
        editingId = getIntent().getStringExtra("bookingId");
        if (editingId != null) {
            tvTitle.setText(R.string.booking_edit);
            spStation.setEnabled(false);          // cannot change station when editing
            spStation.setAlpha(0.6f);
            tvStationHint.setVisibility(View.VISIBLE);
        }

        loadStations(() -> {
            if (editingId != null) fetchBooking(editingId);
            renderSelections();
        });
    }

    private void loadStations(Runnable after){
        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                stations.clear(); stationLabels.clear();
                if (res.isSuccessful() && res.body()!=null){
                    for (StationDto s : res.body()){
                        if (s.isActive) {
                            stations.add(s);
                            String label = (s.name!=null && s.name.trim().length()>0) ? s.name + " ("+s.stationId+")" : s.stationId;
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
                Toast.makeText(BookingCreateEditActivity.this, R.string.err_load_stations, Toast.LENGTH_SHORT).show();
                ArrayAdapter<String> ad = new ArrayAdapter<>(BookingCreateEditActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, stationLabels);
                spStation.setAdapter(ad);
                after.run();
            }
        });
    }

    private void fetchBooking(String id){
        bookingsApi.getById(id).enqueue(new Callback<BookingDto>() {
            @Override public void onResponse(Call<BookingDto> call, Response<BookingDto> res) {
                if (!res.isSuccessful() || res.body()==null) { Toast.makeText(BookingCreateEditActivity.this, R.string.err_load_booking, Toast.LENGTH_SHORT).show(); return; }
                BookingDto b = res.body();

                // station
                int idx = 0;
                for (int i=0;i<stations.size();i++){
                    if (stations.get(i).stationId.equals(b.stationId)) { idx = i; break; }
                }
                spStation.setSelection(idx);

                // times (ISO -> local calendars)
                Long s = parseIsoUtc(b.startTimeUtc);
                Long e = parseIsoUtc(b.endTimeUtc);
                if (s != null) startLocal.setTimeInMillis(s);
                if (e != null) endLocal.setTimeInMillis(e);
                dateLocal.setTimeInMillis(startLocal.getTimeInMillis());

                renderSelections();
            }
            @Override public void onFailure(Call<BookingDto> call, Throwable t) {
                Toast.makeText(BookingCreateEditActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderSelections(){
        tvDate.setText(fmtLocal(dateLocal.getTimeInMillis(), "yyyy-MM-dd"));
        tvStart.setText(fmtLocal(startLocal.getTimeInMillis(), "HH:mm"));
        tvEnd.setText(fmtLocal(endLocal.getTimeInMillis(), "HH:mm"));
        loadAvailability();
    }

    private void pickDate(){
        Calendar today = startOfDay(Calendar.getInstance());
        Calendar max = startOfDay((Calendar) today.clone());
        max.add(Calendar.DAY_OF_YEAR, 7); // today + 7 days

        DatePickerDialog dlg = new DatePickerDialog(this,
                (view, y, m, d) -> {
                    dateLocal.set(Calendar.YEAR, y);
                    dateLocal.set(Calendar.MONTH, m);
                    dateLocal.set(Calendar.DAY_OF_MONTH, d);

                    // keep start/end on same selected day
                    syncDay(startLocal, dateLocal);
                    syncDay(endLocal, dateLocal);

                    // if selected is today and time is in the past -> snap to now
                    Calendar now = Calendar.getInstance();
                    if (isSameDay(dateLocal, now) && startLocal.before(now)) {
                        startLocal.setTimeInMillis(now.getTimeInMillis());
                        roundTo5Min(startLocal);
                        endLocal.setTimeInMillis(startLocal.getTimeInMillis() + 60*60*1000);
                        Toast.makeText(this, R.string.err_time_past_today, Toast.LENGTH_SHORT).show();
                    }
                    renderSelections();
                },
                dateLocal.get(Calendar.YEAR),
                dateLocal.get(Calendar.MONTH),
                dateLocal.get(Calendar.DAY_OF_MONTH));

        // restrict selectable days
        try {
            dlg.getDatePicker().setMinDate(today.getTimeInMillis());
            dlg.getDatePicker().setMaxDate(max.getTimeInMillis());
        } catch (Exception ignored) {}
        dlg.show();
    }

    private void pickTime(boolean start){
        Calendar target = start ? startLocal : endLocal;
        new TimePickerDialog(this, (view, hour, minute) -> {
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute - (minute % 5)); // snap to 5-min

            // validate against "today" minimum
            Calendar now = Calendar.getInstance();
            if (isSameDay(dateLocal, now)) {
                Calendar chosen = (Calendar) target.clone();
                syncDay(chosen, dateLocal);
                if (start && chosen.before(now)) {
                    // bump start to now rounded
                    startLocal.setTimeInMillis(now.getTimeInMillis());
                    roundTo5Min(startLocal);
                    Toast.makeText(this, R.string.err_start_past, Toast.LENGTH_SHORT).show();
                }
                if (!start) {
                    // end can't be in the past if today
                    if (chosen.before(now)) {
                        endLocal.setTimeInMillis(Math.max(now.getTimeInMillis(), startLocal.getTimeInMillis() + 5*60*1000));
                        roundTo5Min(endLocal);
                        Toast.makeText(this, R.string.err_end_past, Toast.LENGTH_SHORT).show();
                    }
                }
            }

            // end must be after start
            if (endLocal.getTimeInMillis() <= startLocal.getTimeInMillis()) {
                endLocal.setTimeInMillis(startLocal.getTimeInMillis() + 30*60*1000); // push by 30min
                Toast.makeText(this, R.string.err_end_after_start, Toast.LENGTH_SHORT).show();
            }

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
        String dateLocalStr = fmtLocal(dateLocal.getTimeInMillis(), "yyyy-MM-dd");

        bookingsApi.availability(stationId, dateLocalStr, TZ_OFFSET_MINUTES_SL)
                .enqueue(new retrofit2.Callback<BookingsService.AvailabilityResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<BookingsService.AvailabilityResponse> call,
                                           retrofit2.Response<BookingsService.AvailabilityResponse> res) {
                        if (!res.isSuccessful() || res.body() == null || res.body().availability == null) {
                            tvAvailability.setText(getString(R.string.availability_hint));
                            return;
                        }
                        String startHH = fmtLocal(startLocal.getTimeInMillis(), "HH");
                        StringBuilder sb = new StringBuilder(getString(R.string.availability_slots_prefix));
                        int count = 0;
                        for (BookingsService.AvailabilityResponse.Slot s : res.body().availability) {
                            if (s.time.startsWith(startHH) && count < 4) {
                                if (count > 0) sb.append("  |  ");
                                sb.append(s.time).append(": ").append(s.availableSlots);
                                count++;
                            }
                        }
                        if (count == 0) sb = new StringBuilder(getString(R.string.availability_hint));
                        tvAvailability.setText(sb.toString());
                    }
                    @Override public void onFailure(retrofit2.Call<BookingsService.AvailabilityResponse> call, Throwable t) {
                        tvAvailability.setText(getString(R.string.availability_hint));
                    }
                });
    }

    // ---- SAVE with client validations + specific server errors ----
    private void save(){
        int pos = spStation.getSelectedItemPosition();
        if (pos < 0 || pos >= stations.size()) {
            Toast.makeText(this, R.string.err_select_station, Toast.LENGTH_SHORT).show();
            return;
        }

        Calendar now = Calendar.getInstance();
        Calendar todayStart = startOfDay((Calendar) now.clone());
        Calendar max = startOfDay((Calendar) todayStart.clone());
        max.add(Calendar.DAY_OF_YEAR, 7);

        // 1) date: today..+7d only (client-side gate)
        if (dateLocal.before(todayStart)) {
            Toast.makeText(this, R.string.err_past_date, Toast.LENGTH_LONG).show();
            return;
        }
        if (dateLocal.after(max)) {
            Toast.makeText(this, R.string.err_over_7_days, Toast.LENGTH_LONG).show();
            return;
        }

        // 2) if today, start must be >= now
        if (isSameDay(dateLocal, now) && startLocal.before(now)) {
            Toast.makeText(this, R.string.err_start_past, Toast.LENGTH_LONG).show();
            return;
        }

        // 3) end > start
        if (endLocal.getTimeInMillis() <= startLocal.getTimeInMillis()){
            Toast.makeText(this, R.string.err_end_after_start, Toast.LENGTH_LONG).show();
            return;
        }

        StationDto st = stations.get(pos);
        String stationLabel = (st.name!=null && !st.name.trim().isEmpty()) ? st.name + " ("+st.stationId+")" : st.stationId;

        String dateText  = fmtLocal(dateLocal.getTimeInMillis(), "yyyy-MM-dd");
        String startText = fmtLocal(startLocal.getTimeInMillis(), "HH:mm");
        String endText   = fmtLocal(endLocal.getTimeInMillis(), "HH:mm");
        String duration  = humanDuration(endLocal.getTimeInMillis() - startLocal.getTimeInMillis());

        String isoStart = toIsoUtc(startLocal.getTimeInMillis());
        String isoEnd   = toIsoUtc(endLocal.getTimeInMillis());
        String mode = (editingId == null) ? "create" : "update";

        BookingReviewDialog
                .newInstance(mode, stationLabel, dateText, startText, endText, duration)
                .setDecision(() -> {
                    if (editingId == null){
                        // CREATE
                        BookingCreateRequest body = new BookingCreateRequest();
                        body.stationId = st.stationId;
                        body.startTimeUtc = isoStart;
                        body.endTimeUtc = isoEnd;

                        bookingsApi.create(body).enqueue(new Callback<BookingDto>() {
                            @Override public void onResponse(Call<BookingDto> call, Response<BookingDto> res) {
                                if (res.isSuccessful()) {
                                    Toast.makeText(BookingCreateEditActivity.this, R.string.msg_created, Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    showSpecificServerError(res);
                                }
                            }
                            @Override public void onFailure(Call<BookingDto> call, Throwable t) {
                                Toast.makeText(BookingCreateEditActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // UPDATE
                        BookingUpdateRequest body = new BookingUpdateRequest();
                        body.startTimeUtc = isoStart;
                        body.endTimeUtc   = isoEnd;

                        bookingsApi.update(editingId, body).enqueue(new Callback<Void>() {
                            @Override public void onResponse(Call<Void> call, Response<Void> res) {
                                if (res.isSuccessful()) {
                                    Toast.makeText(BookingCreateEditActivity.this, R.string.msg_updated, Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    showSpecificServerError(res);
                                }
                            }
                            @Override public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(BookingCreateEditActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                })
                .show(getSupportFragmentManager(), "BookingReview");
    }

    private void showSpecificServerError(Response<?> res) {
        String fallback = getString(R.string.err_server_generic);
        try {
            String body = res.errorBody() != null ? res.errorBody().string() : "";
            if (body == null) body = "";
            String lower = body.toLowerCase(Locale.US);

            if (lower.contains("7") && lower.contains("day")) {
                Toast.makeText(this, getString(R.string.err_over_7_days), Toast.LENGTH_LONG).show();
            } else if (lower.contains("12") && lower.contains("hour")) {
                // server uses 12h rule for update/cancel
                Toast.makeText(this, getString(R.string.err_12h_rule), Toast.LENGTH_LONG).show();
            } else if (lower.contains("overlap")) {
                Toast.makeText(this, getString(R.string.err_overlap), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, fallback, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Toast.makeText(this, fallback, Toast.LENGTH_LONG).show();
        }
    }

    // ---- helpers ----
    private static void syncDay(Calendar target, Calendar day){
        target.set(Calendar.YEAR, day.get(Calendar.YEAR));
        target.set(Calendar.MONTH, day.get(Calendar.MONTH));
        target.set(Calendar.DAY_OF_MONTH, day.get(Calendar.DAY_OF_MONTH));
    }

    private static void roundTo5Min(Calendar c) {
        int m = c.get(Calendar.MINUTE);
        c.set(Calendar.MINUTE, m - (m % 5));
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
    }

    private static boolean isSameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private static Calendar startOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
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

    private static String humanDuration(long ms){
        long mins = Math.max(0, ms / 60000L);
        long h = mins / 60L;
        long m = mins % 60L;
        if (h == 0) return m + "m";
        if (m == 0) return h + "h";
        return h + "h " + m + "m";
    }
}
