package com.homers.UI;


import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

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

import retrofit.Callback;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;


public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    public GoogleMap googleMap;
    public Toolbar toolbar;
    private BottomSheetBehavior mBottomSheetBehavior;
    private ProgressDialog progressDialog;
    private TextView nameStreet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        setupMap();
        setupToolbar();
        beginLoad();
        getRetrofitObject();
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapview);
        mapFragment.getMapAsync(this);
        View bottomSheet = findViewById(R.id.bottom_sheet);
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

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
    }

    public void putMarker(GoogleMap map, Double lat, Double lon, String forum) {

        googleMap.setPadding(0, 0, 0, 0);
        googleMap.getUiSettings().setCompassEnabled(false);

        LatLng marker = new LatLng(lat, lon);
        MarkerOptions markerOptions = new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        map.addMarker(markerOptions.position(marker).title(forum));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 15));

        CameraPosition cameraPosition = new CameraPosition.Builder().target(marker).zoom(17).bearing(90).build();
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        progressDialog.dismiss();
    }

    private void setupToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void  getInformationRoute(String jsonLine) throws JSONException {
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

        if (Integer.parseInt(speed) > 30) {
            nameStreet.setTextColor(getApplicationContext().getResources().getColor(R.color.pin_green));
        } else if (Integer.parseInt(speed) < 29 ) {
            nameStreet.setTextColor(getApplicationContext().getResources().getColor(R.color.pin_red));
        }
        nameStreet.setText(latitude + "," + longitude + " - Ultima checagem:"+ time);
        putMarker(googleMap,  Double.parseDouble(latitude),  Double.parseDouble(longitude), rvc_name);
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
}