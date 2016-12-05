package com.homers;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public GoogleMap googleMap;
    public Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        setupMap();
        setupToolbar();


    }

    private void setupMap(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);

    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap =  map ;
    }

    public void putMarker(GoogleMap map, Double lat, Double lon, String forum){

        googleMap.setPadding(0, 0, 0, 0);
        googleMap.getUiSettings().setCompassEnabled(false);

        LatLng marker = new LatLng(lat, lon);
        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.pin));
        map.addMarker(markerOptions.position(marker).title(forum));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(marker).zoom(17).bearing(90).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void setupToolbar(){
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }}
