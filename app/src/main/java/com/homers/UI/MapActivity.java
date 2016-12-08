package com.homers.UI;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.homers.API.RestApi;

import com.homers.R;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public GoogleMap googleMap;
    public Toolbar toolbar;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ProgressDialog progressDialog;
    private TextView nameStreet;
    private double latitude_pin;
    private double longitude_pin;
    private double latitude_now;
    private double longitude_now;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private Location mCurrentLocation;
    private FloatingActionButton fab;
    LatLng fixPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        connectLocationAPI();
        setupMap();
        setupToolbar();
        beginLoad();
        getRetrofitObject();

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    Log.e("BottomSheet", "Expanded");
                    Animation TESTE = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
                     fab.setAnimation(TESTE);
                     fab.setVisibility(View.GONE);

                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    Log.e("BottomSheet", "Collapsed");
                    Animation animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
                    fab.setAnimation(animFadeIn);
                    fab.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                bottomSheet.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = MotionEventCompat.getActionMasked(event);
                        switch (action) {
                            case MotionEvent.ACTION_DOWN:
                               // Animation animFadeIn = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
                               // fab.setAnimation(animFadeIn);
                                //fab.setVisibility(View.VISIBLE);
                                return true;
                            case MotionEvent.ACTION_CANCEL:
                                //Animation TESTE = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
                               // fab.setAnimation(TESTE);
                               // fab.setVisibility(View.GONE);
                                return true;
                            case MotionEvent.ACTION_UP:
                                //Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
                              //  fab.setAnimation(animFadeOut);
                               // fab.setVisibility(View.GONE);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
            }
        });

        nameStreet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                markerTransitMove(latitude_pin, longitude_pin, googleMap);
            }
        });
    }

    private void connectLocationAPI() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }


    private void markerTransitMove(double latitude_pin, double longitude_pin, GoogleMap googleMap) {
        LatLng marker = new LatLng(latitude_pin, longitude_pin);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(marker).zoom(17).bearing(90).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void moveTarantino(LatLng marker) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(marker).zoom(17).bearing(90).build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);
        View bottomSheet = findViewById(R.id.bottom_sheet);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        nameStreet = (TextView) findViewById(R.id.nameStreet);
        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setPeekHeight(160);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void beginLoad() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.loading_street));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    public void onFabClickMap(View v) {
        moveTarantino(fixPosition);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
    }

    public void putMarker(GoogleMap map, Double lat, Double lon, String forum) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.getUiSettings().setCompassEnabled(false);
        LatLng marker = new LatLng(lat, lon);
        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        map.addMarker(markerOptions.position(marker).title(forum));

        progressDialog.dismiss();
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void getInformationRoute(String jsonLine) throws JSONException {
        String latitude = null;
        String longitude = null;
        String speed = null;
        String time = null;
        String rvc_name = null;

        JSONArray jObj = new JSONArray(jsonLine);
        for (int i = 0; i < jObj.length(); i++) {
            JSONObject json_data = jObj.getJSONObject(i);
            latitude = String.valueOf(json_data.get("latitude"));
            longitude = String.valueOf(json_data.get("longitude"));
            time = String.valueOf(json_data.get("time"));
            rvc_name = String.valueOf(json_data.get("rvc_name"));
            speed = String.valueOf(json_data.get("speed"));
        }
        latitude_pin = Double.parseDouble(latitude);
        longitude_pin = Double.parseDouble(longitude);

        if (Integer.parseInt(speed) > 30) {
            nameStreet.setTextColor(getApplicationContext().getResources().getColor(R.color.pin_green));
        } else if (Integer.parseInt(speed) < 29) {
            nameStreet.setTextColor(getApplicationContext().getResources().getColor(R.color.pin_red));
        }
        nameStreet.setText(latitude + "," + longitude + " - Informação recebida no:" + time);
        putMarker(googleMap, Double.parseDouble(latitude), Double.parseDouble(longitude), rvc_name);
    }


    private void getRetrofitObject() {
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade request) {
                request.addHeader("Content-Type", "application/json");
            }
        };

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setClient(new OkClient())
                .setEndpoint(getResources().getString(R.string.url_api))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(requestInterceptor)
                .build();

        RestApi api = restAdapter.create(RestApi.class);
        api.getRotas(new Callback<Response>() {
                         @Override
                         public void success(Response result, Response response) {
                             BufferedReader reader = null;
                             String output = "";
                             try {
                                 reader = new BufferedReader(new InputStreamReader(result.getBody().in()));
                                 output = reader.readLine();
                                 getInformationRoute(output);
                             } catch (IOException | JSONException e) {
                                 e.printStackTrace();
                             }
                         }

                         @Override
                         public void failure(RetrofitError error) {
                             Log.e("erro", "erro");
                         }
                     }
        );
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        updateUI();
    }

    private void updateUI() {
        latitude_now = mCurrentLocation.getLatitude();
        longitude_now = mCurrentLocation.getLongitude();

        LatLng marker = new LatLng(latitude_now, longitude_now);
        fixPosition = marker;
        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        googleMap.addMarker(markerOptions.position(marker).title("You"));
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            latitude_now = mLastLocation.getLatitude();
            longitude_now = mLastLocation.getLongitude();

            LatLng marker = new LatLng(latitude_now, longitude_now);
            fixPosition = marker;
            MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
            googleMap.addMarker(markerOptions.position(marker).title("You"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));
            CameraPosition cameraPosition = new CameraPosition.Builder().target(marker).zoom(17).bearing(90).build();
            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}