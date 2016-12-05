package com.homers;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.gc.materialdesign.views.ProgressBarCircularIndeterminate;
import com.gc.materialdesign.widgets.Dialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.gson.Gson;
import com.melnykov.fab.FloatingActionButton;

import java.io.UnsupportedEncodingException;


public class MainActivity extends ActionBarActivity implements LocationListener, GpsStatus.Listener {

    private SharedPreferences sharedPreferences;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ENCODE = "UTF-8";
    private static final int REQUEST_RESOLVE_ERROR = 1;
    private boolean mResolvingError = false;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private LocationManager mLocationManager;
    private static Data data;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private ProgressBarCircularIndeterminate progressBarCircularIndeterminate;
    private TextView status;
    private TextView currentSpeed;
    private TextView longitude;
    private TextView latitude;
    private Data.onGpsServiceUpdate onGpsServiceUpdate;
    private boolean firstfix;
    private GoogleApiClient mGoogleApiClient;
    private NearbyConnectionCallbacks mConnectionCallbacks = new NearbyConnectionCallbacks();
    private NearbyConnectionFailedListener mFailedListener = new NearbyConnectionFailedListener();
    private Strategy mStrategy = new Strategy.Builder()
            .setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT)
            .setDistanceType(Strategy.DISTANCE_TYPE_DEFAULT)
            .setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT)
            .build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        data = new Data(onGpsServiceUpdate);
        createGoogleApiClient();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.INVISIBLE);


        onGpsServiceUpdate = new Data.onGpsServiceUpdate() {
            @Override
            public void update() {

            }
        };

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        status = (TextView) findViewById(R.id.status);
        longitude = (TextView) findViewById(R.id.latitude);
        latitude = (TextView) findViewById(R.id.longitude);
        currentSpeed = (TextView) findViewById(R.id.currentSpeed);
        progressBarCircularIndeterminate = (ProgressBarCircularIndeterminate) findViewById(R.id.progressBarCircularIndeterminate);
    }

    private void handleStartSolution(Status status) {
        if (!mResolvingError) {
            try {
                status.startResolutionForResult(MainActivity.this, REQUEST_RESOLVE_ERROR);
                mResolvingError = true;
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    public void onFabClick(View v) {
        sendMessage();
    }

    public void onRefreshClick(View v) {
        resetData();
        stopService(new Intent(getBaseContext(), GpsServices.class));
    }


    private void createGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mFailedListener)
                .build();
    }


    @Override
    protected void onResume() {
        super.onResume();

        firstfix = true;
        if (!data.isRunning()) {
            Gson gson = new Gson();
            String json = sharedPreferences.getString("data", "");
            data = gson.fromJson(json, Data.class);
        }
        if (data == null) {
            data = new Data(onGpsServiceUpdate);
        } else {
            data.setOnGpsServiceUpdate(onGpsServiceUpdate);
        }

        if (mLocationManager.getAllProviders().indexOf(LocationManager.GPS_PROVIDER) >= 0) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0, this);
        } else {
            Log.w("MainActivity", "No GPS location provider found. GPS data display will not be available.");
        }

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDisabledDialog();
        }

        mLocationManager.addGpsStatusListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(this);
        mLocationManager.removeGpsStatusListener(this);
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(data);
        prefsEditor.putString("data", json);
        prefsEditor.commit();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(new Intent(getBaseContext(), GpsServices.class));
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location.hasAccuracy()) {
            SpannableString s = new SpannableString(String.format("%.0f", location.getAccuracy()) + "m");
            s.setSpan(new RelativeSizeSpan(0.75f), s.length()-1, s.length(), 0);


            if (firstfix){
                status.setText("");
                fab.setVisibility(View.VISIBLE);
                if (!data.isRunning() && !longitude.getText().equals("")) {

                }
                firstfix = false;
            }
        }else{
            firstfix = true;
        }

        if (location.hasSpeed()) {
            progressBarCircularIndeterminate.setVisibility(View.GONE);
            SpannableString s = new SpannableString(String.format("%.0f", location.getSpeed() * 3.6) + "km/h");
            s.setSpan(new RelativeSizeSpan(0.25f), s.length()-4, s.length(), 0);
            currentSpeed.setText(s);
            longitude.setText(String.valueOf(location.getLongitude()));
            latitude.setText(String.valueOf(location.getLatitude()));
        }

    }

    public void onGpsStatusChanged (int event) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED
        && this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE_ASK_PERMISSIONS);

        }else{


        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                int satsInView = 0;
                int satsUsed = 0;
                Iterable<GpsSatellite> sats = gpsStatus.getSatellites();
                for (GpsSatellite sat : sats) {
                    satsInView++;
                    if (sat.usedInFix()) {
                        satsUsed++;
                    }
                }

                if (satsUsed == 0) {
                    fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));
                    data.setRunning(false);
                    status.setText("");
                    stopService(new Intent(getBaseContext(), GpsServices.class));
                    fab.setVisibility(View.VISIBLE);

                    status.setText(getResources().getString(R.string.waiting_for_fix));
                    firstfix = true;
                }
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    showGpsDisabledDialog();
                }
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;
            }
        }
    }

    public void showGpsDisabledDialog(){
        Dialog dialog = new Dialog(this, getResources().getString(R.string.gps_disabled), getResources().getString(R.string.please_enable_gps));

        dialog.setOnAcceptButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent("android.settings.LOCATION_SOURCE_SETTINGS"));
            }
        });
        dialog.show();
    }

    private void sendMessage() {

        final ChatMessage chatMessage = new ChatMessage("", System.currentTimeMillis(), 0.1,0.1,0.1);

        byte[] content;
        try {
            content = chatMessage.toString().getBytes(ENCODE);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
            return;
        }
        Message message = new Message(content, ChatMessage.TYPE_USER_CHAT);
        Nearby.Messages.publish(mGoogleApiClient, message, mStrategy)
                .setResultCallback(new ResultCallback<Status>(){
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                           //addNewMessage(chatMessage);
                        } else {
                            if (status.hasResolution()) {
                                handleStartSolution(status);
                            } else {
                                Log.e(TAG, "sendMessage failed." + status.toString());
                            }
                        }
                    }
                });
    }


    public void resetData(){
        fab.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play));

        longitude.setText("");
        latitude.setText("");

        data = new Data(onGpsServiceUpdate);
    }

    private class NearbyConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(TAG, "connect failed.");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    private MessageListener mMessageListener = new MessageListener() {
        @Override
        public void onFound(Message message) {
            if (message != null) {
                String json;
                try {
                    json = new String(message.getContent(), ENCODE);
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, e.toString());
                    return;
                }
            // addNewMessage(ChatMessage.fromJson(json));
            }
        }
    };




    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    public static Data getData() {
        return data;
    }

    private class NearbyConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle bundle) {
            Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener, mStrategy);
                    } else {
                        if (status.hasResolution()){
                            if (!mResolvingError){
                                handleStartSolution(status);
                            }
                        } else {
                            Log.e(TAG, "Mensagem Não Enviada");
                        }
                    }
                }
            });
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    }

    public void onBackPressed(){
        Intent a = new Intent(Intent.ACTION_MAIN);
        a.addCategory(Intent.CATEGORY_HOME);
        a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(a);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {}

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) {}
}
