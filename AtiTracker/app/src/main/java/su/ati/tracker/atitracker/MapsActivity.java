package su.ati.tracker.atitracker;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static su.ati.tracker.atitracker.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public static final String EXTRA_START_LAT = "su.ati.tracker.atitracker.START_LAT";
    public static final String EXTRA_START_LON = "su.ati.tracker.atitracker.START_LON";
    public static final String EXTRA_END_LAT = "su.ati.tracker.atitracker.END_LAT";
    public static final String EXTRA_END_LON = "su.ati.tracker.atitracker.END_LON";

    private GoogleMap mMap;
    private int countMarkers;
    private Marker start;
    private Marker end;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                checkAddedAndBack();
            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        LatLng ati = new LatLng(59.973844, 30.333971);
        mMap.addMarker(new MarkerOptions().position(ati).title("ATI"));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(ati)
                .zoom(14)
                .build();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);

        mMap.moveCamera(cameraUpdate);
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override public void onMapClick(LatLng latLng) {
                if (countMarkers < 2) {
                    ++countMarkers;
                    if (countMarkers == 1) {
                        start = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .title("start"));
                    } else {
                        end = mMap.addMarker(new MarkerOptions()
                                .position(latLng)
                                .draggable(true)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                .title("end"));
                    }
                }
            }
        });
    }

    public void closeActivityWithResult() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_START_LAT, start.getPosition().latitude);
        resultIntent.putExtra(EXTRA_START_LON, start.getPosition().longitude);
        resultIntent.putExtra(EXTRA_END_LAT, end.getPosition().latitude);
        resultIntent.putExtra(EXTRA_END_LON, end.getPosition().longitude);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        checkAddedAndBack();
    }

    private void checkAddedAndBack() {

        if (start == null && end == null) {
            Toast.makeText(this, "укажите начальную и конечную точки", Toast.LENGTH_SHORT).show();
        } else if (start == null) {
            Toast.makeText(this, "укажите начальную точку", Toast.LENGTH_SHORT).show();
        } else if (end == null) {
            Toast.makeText(this, "укажите конечную точку", Toast.LENGTH_SHORT).show();
        } else {
            closeActivityWithResult();
        }
    }

}
