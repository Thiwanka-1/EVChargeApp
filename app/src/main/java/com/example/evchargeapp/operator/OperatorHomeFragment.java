package com.example.evchargeapp.operator;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.widget.*;
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
import com.example.evchargeapp.utils.JwtUtil;
import com.example.evchargeapp.utils.SessionManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OperatorHomeFragment extends Fragment {

    private TextView kpiStations, kpiToday, kpiInProg, kpiPending;
    private LinearLayout stationsContainer;
    private RecyclerView rvToday;
    private TextView tvEmpty;
    private FloatingActionButton fabScan;

    private StationsService stationsApi;
    private BookingsService bookingsApi;

    private final List<StationDto> myStations = new ArrayList<>();
    private final List<BookingDto> todayBookings = new ArrayList<>();
    private OperatorBookingsAdapter adapter;

    private @Nullable String pendingStartBookingId = null;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_operator_home, c, false);

        kpiStations = v.findViewById(R.id.kpiStations);
        kpiToday    = v.findViewById(R.id.kpiToday);
        kpiInProg   = v.findViewById(R.id.kpiInProgress);
        kpiPending  = v.findViewById(R.id.kpiPending);
        stationsContainer = v.findViewById(R.id.stationsContainer);
        rvToday = v.findViewById(R.id.rvToday);
        tvEmpty = v.findViewById(R.id.tvEmptyToday);
        fabScan = v.findViewById(R.id.fabQuickScan);

        String base = getString(R.string.base_url);
        stationsApi = ApiClient.get(requireContext(), base).create(StationsService.class);
        bookingsApi = ApiClient.get(requireContext(), base).create(BookingsService.class);

        rvToday.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OperatorBookingsAdapter(new OperatorBookingsAdapter.RowAction() {
            @Override public void onDetails(BookingDto b) {
                BookingDetailsDialog.display(
                        requireActivity().getSupportFragmentManager(),
                        b,
                        getStationById(b.stationId)
                );
            }
            @Override public void onApprove(BookingDto b) { approveBooking(b.id); }
            @Override public void onReject(BookingDto b) { promptReject(b.id); }
            @Override public void onStart(BookingDto b) { startScan(b.id); }
            @Override public void onComplete(BookingDto b) { completeBooking(b.id); }
        });
        rvToday.setAdapter(adapter);

        fabScan.setOnClickListener(v1 -> startScan(null)); // quick scan

        return v;
    }

    @Override public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        myStations.clear();
        todayBookings.clear();
        adapter.setItems(todayBookings);
        tvEmpty.setVisibility(View.GONE);

        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                if (!isAdded()) return;
                if (!res.isSuccessful() || res.body()==null) {
                    Toast.makeText(requireContext(), "Failed to load stations", Toast.LENGTH_SHORT).show();
                    return;
                }
                // <-- FIX: use SessionManager token
                String token = SessionManager.getToken(requireContext());
                String me = JwtUtil.getSubject(token);

                for (StationDto s : res.body()) {
                    if (s.isActive && s.operatorUserIds != null && me != null && s.operatorUserIds.contains(me)) {
                        myStations.add(s);
                    }
                }
                renderStations();
                kpiStations.setText(String.valueOf(myStations.size()));

                fetchTodayBookingsForMyStations();
            }
            @Override public void onFailure(Call<List<StationDto>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void renderStations() {
        stationsContainer.removeAllViews();
        LayoutInflater li = LayoutInflater.from(requireContext());
        for (StationDto s : myStations) {
            View card = li.inflate(R.layout.row_operator_station, stationsContainer, false);
            ((TextView) card.findViewById(R.id.tvStationTitle))
                    .setText((s.name!=null && s.name.trim().length()>0? s.name : s.stationId) + " (" + s.stationId + ")");
            ((TextView) card.findViewById(R.id.tvStationSub))
                    .setText((s.type==null?"-":s.type) + " • " + (s.address==null?"":s.address));

            EditText etSlots = card.findViewById(R.id.etSlots);
            etSlots.setText(String.valueOf(s.availableSlots));

            card.findViewById(R.id.btnSaveSlots).setOnClickListener(v -> {
                int val;
                try { val = Integer.parseInt(etSlots.getText().toString().trim()); }
                catch (Exception e){ Toast.makeText(requireContext(),"Invalid slots",Toast.LENGTH_SHORT).show(); return; }
                saveSlots(s.stationId, val);
            });
            stationsContainer.addView(card);
        }
    }

    private void saveSlots(String stationId, int val){
        stationsApi.updateSlots(stationId, val).enqueue(new Callback<Map<String,String>>() {
            @Override public void onResponse(Call<Map<String,String>> call, Response<Map<String,String>> res) {
                if (!isAdded()) return;
                if (res.isSuccessful()) {
                    Toast.makeText(requireContext(), "Slots updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "Update failed", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String,String>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchTodayBookingsForMyStations(){
        if (myStations.isEmpty()) {
            updateKpisAndList();
            return;
        }
        final int[] remain = { myStations.size() };
        for (StationDto s : myStations) {
            bookingsApi.getForStation(s.stationId).enqueue(new Callback<List<BookingDto>>() {
                @Override public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> res) {
                    if (res.isSuccessful() && res.body()!=null) {
                        List<BookingDto> all = res.body();
                        long startDay = startOfToday();
                        long endDay   = startDay + 24L*60L*60L*1000L;
                        for (BookingDto b : all) {
                            Long st = parse(b.startTimeUtc); Long en = parse(b.endTimeUtc);
                            if (st!=null && en!=null && st < endDay && en > startDay) {
                                b.__stationName = (getStationById(b.stationId) != null && getStationById(b.stationId).name != null
                                        && !getStationById(b.stationId).name.trim().isEmpty())
                                        ? getStationById(b.stationId).name
                                        : b.stationId;
                                todayBookings.add(b);
                            }
                        }
                    }
                    if (--remain[0] == 0) updateKpisAndList();
                }
                @Override public void onFailure(Call<List<BookingDto>> call, Throwable t) {
                    if (--remain[0] == 0) updateKpisAndList();
                }
            });
        }
    }

    private void updateKpisAndList(){
        int pending=0, inProg=0;
        for (BookingDto b : todayBookings) {
            String s = (b.status==null?"":b.status);
            if ("Pending".equalsIgnoreCase(s)) pending++;
            if ("InProgress".equalsIgnoreCase(s)) inProg++;
        }
        kpiPending.setText(String.valueOf(pending));
        kpiInProg.setText(String.valueOf(inProg));
        kpiToday.setText(String.valueOf(todayBookings.size()));

        Collections.sort(todayBookings, (a,b)->{
            Long as = parse(a.startTimeUtc), bs = parse(b.startTimeUtc);
            if (as==null||bs==null) return 0;
            return as.compareTo(bs);
        });

        adapter.setItems(todayBookings);
        tvEmpty.setVisibility(todayBookings.isEmpty()? View.VISIBLE : View.GONE);
    }

    private StationDto getStationById(String id){
        for (StationDto s : myStations) if (s.stationId.equals(id)) return s;
        return null;
    }

    // -------------------- Actions --------------------

    private void approveBooking(String id){
        BookingsService.ApproveRequest body = new BookingsService.ApproveRequest();
        body.approve = true;
        bookingsApi.approveOrReject(id, body).enqueue(new Callback<BookingsService.ApproveResponse>() {
            @Override public void onResponse(Call<BookingsService.ApproveResponse> call, Response<BookingsService.ApproveResponse> res) {
                if (!isAdded()) return;
                if (res.isSuccessful()) { Toast.makeText(requireContext(), "Approved", Toast.LENGTH_SHORT).show(); load(); }
                else Toast.makeText(requireContext(), "Approve failed", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<BookingsService.ApproveResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void promptReject(String id){
        final EditText et = new EditText(requireContext());
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        et.setHint("Reason (optional)");

        new AlertDialog.Builder(requireContext())
                .setTitle("Reject booking?")
                .setView(et)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reject", (d, w) -> {
                    BookingsService.ApproveRequest body = new BookingsService.ApproveRequest();
                    body.approve = false;
                    body.reason = et.getText().toString().trim();
                    bookingsApi.approveOrReject(id, body).enqueue(new Callback<BookingsService.ApproveResponse>() {
                        @Override public void onResponse(Call<BookingsService.ApproveResponse> call, Response<BookingsService.ApproveResponse> res) {
                            if (!isAdded()) return;
                            if (res.isSuccessful()) { Toast.makeText(requireContext(), "Rejected", Toast.LENGTH_SHORT).show(); load(); }
                            else Toast.makeText(requireContext(), "Reject failed", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<BookingsService.ApproveResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .show();
    }

    private void startScan(@Nullable String bookingId){
        pendingStartBookingId = bookingId; // if null -> quick scan
        IntentIntegrator integrator = IntentIntegrator.forSupportFragment(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.setPrompt("Scan booking QR");
        integrator.initiateScan();
    }

    @Override public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        IntentResult result = IntentIntegrator.parseActivityResult(req, res, data);
        if (result == null) return;
        String qr = result.getContents();
        if (qr == null) return;

        if (pendingStartBookingId != null) {
            doStart(pendingStartBookingId, qr);
            pendingStartBookingId = null;
        } else {
            BookingDto match = null;
            for (BookingDto b : todayBookings) {
                if ("Approved".equalsIgnoreCase(b.status) && b.qrCode != null && b.qrCode.equals(qr)) {
                    match = b; break;
                }
            }
            if (match == null) {
                Toast.makeText(requireContext(), "No approved booking found for this QR", Toast.LENGTH_LONG).show();
            } else {
                doStart(match.id, qr);
            }
        }
    }

    private void doStart(String id, String qr){
        BookingsService.StartRequest body = new BookingsService.StartRequest();
        body.qrCode = qr;
        bookingsApi.start(id, body).enqueue(new Callback<BookingsService.MessageResponse>() {
            @Override public void onResponse(Call<BookingsService.MessageResponse> call, Response<BookingsService.MessageResponse> res) {
                if (!isAdded()) return;
                if (res.isSuccessful()) { Toast.makeText(requireContext(), "Charging started", Toast.LENGTH_SHORT).show(); load(); }
                else Toast.makeText(requireContext(), "Invalid QR or state", Toast.LENGTH_LONG).show();
            }
            @Override public void onFailure(Call<BookingsService.MessageResponse> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeBooking(String id){
        new AlertDialog.Builder(requireContext())
                .setTitle("Complete session?")
                .setMessage("Mark charging as completed.")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (d,w) -> {
                    bookingsApi.complete(id).enqueue(new Callback<BookingsService.MessageResponse>() {
                        @Override public void onResponse(Call<BookingsService.MessageResponse> call, Response<BookingsService.MessageResponse> res) {
                            if (!isAdded()) return;
                            if (res.isSuccessful()) { Toast.makeText(requireContext(), "Completed", Toast.LENGTH_SHORT).show(); load(); }
                            else Toast.makeText(requireContext(), "Cannot complete (state)", Toast.LENGTH_SHORT).show();
                        }
                        @Override public void onFailure(Call<BookingsService.MessageResponse> call, Throwable t) {
                            if (!isAdded()) return;
                            Toast.makeText(requireContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
                }).show();
    }

    // ---- time helpers
    private static Long parse(String iso){
        if (iso == null) return null;
        String[] p = new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'","yyyy-MM-dd'T'HH:mm:ss'Z'","yyyy-MM-dd'T'HH:mm'Z'"};
        for (String f : p){
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date d = sdf.parse(iso);
                if (d != null) return d.getTime();
            } catch (Exception ignored){}
        }
        return null;
    }

    private static long startOfToday(){
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c.getTimeInMillis();
    }
}
