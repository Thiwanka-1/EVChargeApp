package com.example.evchargeapp.owner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.evchargeapp.R;
import com.example.evchargeapp.api.ApiClient;
import com.example.evchargeapp.api.BookingsService;
import com.example.evchargeapp.api.StationsService;
import com.example.evchargeapp.models.BookingDto;
import com.example.evchargeapp.models.StationDto;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OwnerHomeFragment extends Fragment implements OnMapReadyCallback {

    private TextView tvPending, tvApproved;
    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fused;
    private StationsService stationsApi;
    private BookingsService bookingsApi;

    // Default center (Colombo) in case we don't get location
    private static final LatLng DEFAULT_CENTER = new LatLng(6.9271, 79.8612);

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) enableMyLocation();
                else centerDefault();
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b) {
        View v = inf.inflate(R.layout.fragment_owner_home, c, false);
        tvPending = v.findViewById(R.id.tvPendingCount);
        tvApproved = v.findViewById(R.id.tvApprovedFuture);
        mapView = v.findViewById(R.id.mapView);

        // init services
        String base = getString(R.string.base_url);
        stationsApi = ApiClient.get(requireContext(), base).create(StationsService.class);
        bookingsApi = ApiClient.get(requireContext(), base).create(BookingsService.class);

        // location
        fused = LocationServices.getFusedLocationProviderClient(requireContext());

        // map
        mapView.onCreate(b);
        mapView.getMapAsync(this);

        // load stats
        loadBookingStats();

        return v;
    }

    private void loadBookingStats() {
        bookingsApi.getMyBookings().enqueue(new Callback<List<BookingDto>>() {
            @Override public void onResponse(Call<List<BookingDto>> call, Response<List<BookingDto>> res) {
                if (!res.isSuccessful() || res.body() == null) { showStatError(); return; }

                int pending = 0;
                int approvedFuture = 0;
                long nowMs = System.currentTimeMillis();

                for (BookingDto b : res.body()) {
                    String st = (b.status == null ? "" : b.status);
                    if ("Pending".equalsIgnoreCase(st)) pending++;

                    if ("Approved".equalsIgnoreCase(st)) {
                        Long startMs = parseIsoToMillis(b.startTimeUtc);
                        if (startMs != null && startMs > nowMs) approvedFuture++;
                    }
                }

                tvPending.setText(String.valueOf(pending));
                tvApproved.setText(String.valueOf(approvedFuture));
            }
            @Override public void onFailure(Call<List<BookingDto>> call, Throwable t) { showStatError(); }
        });
    }

    private void showStatError() {
        Toast.makeText(requireContext(), "Failed to load booking stats", Toast.LENGTH_SHORT).show();
        tvPending.setText("-");
        tvApproved.setText("-");
    }

    /**
     * Parses common ISO-8601 strings to epoch millis without java.time (API 24 safe).
     * Tries patterns with/without milliseconds and with timezone 'Z' or offset.
     */
    private @Nullable Long parseIsoToMillis(String iso) {
        if (iso == null) return null;

        // Try most common patterns first
        String[] patterns = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mmX"  // fallback
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(p, Locale.US);
                // Interpret result in UTC so comparisons are correct
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                ParsePosition pos = new ParsePosition(0);
                java.util.Date d = sdf.parse(iso, pos);
                if (d != null && pos.getIndex() == iso.length()) {
                    return d.getTime();
                }
            } catch (Exception ignored) { }
        }
        return null;
    }

    // MAP

    // 1) onMapReady: add MapsInitializer line (before you use the map)
    @Override public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        com.google.android.gms.maps.MapsInitializer.initialize(
                requireContext(),
                com.google.android.gms.maps.MapsInitializer.Renderer.LATEST,
                null
        );

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        // Try to center on user location; otherwise default
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            centerDefault(); // show a map immediately while waiting
        }

        // Then load station markers
        loadStations();
    }


    // 2) STRONG permission guard to silence lint and avoid crashes
    private void enableMyLocation() {
        if (googleMap == null) return;

        // Check again right here (lint requires a check at call site)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException ignored) { /* extra safety */ }

        fused.getLastLocation()
                .addOnSuccessListener(loc -> {
                    if (loc != null) centerOn(loc);
                    else centerDefault();
                })
                .addOnFailureListener(e -> centerDefault());
    }


    private void centerOn(@NonNull Location loc) {
        LatLng here = new LatLng(loc.getLatitude(), loc.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(here, 13f));
    }

    private void centerDefault() {
        if (googleMap != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, 11f));
        }
    }

    private void loadStations() {
        stationsApi.getAll().enqueue(new Callback<List<StationDto>>() {
            @Override public void onResponse(Call<List<StationDto>> call, Response<List<StationDto>> res) {
                if (!res.isSuccessful() || res.body() == null || googleMap == null) return;
                for (StationDto s : res.body()) {
                    if (!s.isActive) continue;
                    LatLng pos = new LatLng(s.latitude, s.longitude);
                    String title = (s.name == null ? s.stationId : s.name);
                    String snippet = String.format(Locale.getDefault(), "%s • %s • Slots: %d",
                            s.address == null ? "" : s.address,
                            s.type == null ? "-" : s.type,
                            s.availableSlots);
                    googleMap.addMarker(new MarkerOptions()
                            .position(pos)
                            .title(title)
                            .snippet(snippet));
                }
            }
            @Override public void onFailure(Call<List<StationDto>> call, Throwable t) { /* ignore */ }
        });
    }

    // MapView lifecycle
    // 3) MapView needs onStart / onStop too (newer play-services-maps)
    @Override public void onStart()  { super.onStart();  mapView.onStart();  }
    @Override public void onResume() { super.onResume(); mapView.onResume(); }
    @Override public void onPause()  { mapView.onPause();  super.onPause(); }
    @Override public void onStop()   { mapView.onStop();   super.onStop(); }
    @Override public void onDestroyView() { mapView.onDestroy(); super.onDestroyView(); }
    @Override public void onLowMemory()   { super.onLowMemory(); mapView.onLowMemory(); }

}
