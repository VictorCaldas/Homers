package com.homers.UI;

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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;
import com.google.gson.Gson;
import com.google.android.gms.nearby.connection.Connections;
import com.homers.Model.ChatMessage;
import com.homers.Model.Data;
import com.homers.R;
import com.homers.Service.GpsServices;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationListener,
        GpsStatus.Listener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener,
        Connections.ConnectionRequestListener,
        Connections.MessageListener,
        Connections.EndpointDiscoveryListener {


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ENCODE = "UTF-8";
    private String mRemoteHostEndpoint;
    private static final int REQUEST_RESOLVE_ERROR = 1;
    final private int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static int[] NETWORK_TYPES = {ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_ETHERNET};
    private boolean firstfix;
    private boolean isConnected;
    private boolean mResolvingError = false;
    private boolean mIsHost = false;
    private long CONNECTION_TIME_OUT = 15000;
    private SharedPreferences sharedPreferences;
    private TextView status;
    private TextView currentSpeed;
    private TextView longitude;
    private TextView latitude;
    private LocationManager mLocationManager;
    private static Data data;
    private List<String> remotePeerEndpoints;
    private Toolbar toolbar;
    private FloatingActionButton fab;
    private ProgressBar progressBarCircularIndeterminate;
    private ArrayAdapter<String> adapter;
    private Data.onGpsServiceUpdate onGpsServiceUpdate;
    private GoogleApiClient mGoogleApiClient;
    private NearbyConnectionCallbacks mConnectionCallbacks = new NearbyConnectionCallbacks();
    private NearbyConnectionFailedListener mFailedListener = new NearbyConnectionFailedListener();
    private Strategy mStrategy = new Strategy.Builder().setDiscoveryMode(Strategy.DISCOVERY_MODE_DEFAULT).setDistanceType(Strategy.DISTANCE_TYPE_DEFAULT).setTtlSeconds(Strategy.TTL_SECONDS_DEFAULT).build();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupView();

        data = new Data(onGpsServiceUpdate);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(mConnectionCallbacks)
                .addOnConnectionFailedListener(mFailedListener)
                .addApi(Nearby.CONNECTIONS_API)
                .build();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        onGpsServiceUpdate = new Data.onGpsServiceUpdate() {
            @Override
            public void update() {

            }
        };
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
            //showGpsDisabledDialog();
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
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
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
        Intent intent = new Intent(getApplicationContext(), MapActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.in, R.anim.in2);
    }


    private void setupView(){
        remotePeerEndpoints = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, remotePeerEndpoints);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);

        //fab.setVisibility(View.INVISIBLE);
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        status = (TextView) findViewById(R.id.status);
        longitude = (TextView) findViewById(R.id.latitude);
        latitude = (TextView) findViewById(R.id.longitude);
        currentSpeed = (TextView) findViewById(R.id.currentSpeed);
        progressBarCircularIndeterminate = (ProgressBar) findViewById(R.id.progressBarCircularIndeterminate);
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

                    }
                    break;
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    break;
            }
        }
    }

    private void sendMessage() {
        final ChatMessage chatMessage = new ChatMessage("Carro 1", System.currentTimeMillis(), 0.1,0.2,0.2);

        byte[] content;
        try {
            content = chatMessage.toString().getBytes(ENCODE);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, e.toString());
            return;
        }
        Message message = new Message(content, ChatMessage.TYPE_BEACON);
        Nearby.Messages.publish(mGoogleApiClient, message)
                .setResultCallback(new ResultCallback<Status>(){
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            Log.e(TAG, "pablito style" + status.toString());
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

    @Override
    public void onClick(View view) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    @Override
    public void onEndpointFound(String endpointId, String deviceId, final String serviceId, String endpointName) {
        byte[] payload = null;

        Nearby.Connections.sendConnectionRequest(mGoogleApiClient, deviceId, endpointId, payload,
                new Connections.ConnectionResponseCallback() {
                    @Override
                    public void onConnectionResponse(String s, Status status, byte[] bytes) {
                        if (status.isSuccess()) {
                            MainActivity.this.status.setText("Connected to: " + s);
                            Nearby.Connections.stopDiscovery(mGoogleApiClient, serviceId);
                            mRemoteHostEndpoint = s;
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                            if (!mIsHost) {
                                isConnected = true;
                            }
                        } else {
                            MainActivity.this.status.setText("Connection to " + s + " failed");
                            if (!mIsHost) {
                                isConnected = false;
                            }
                        }
                    }
                }, this);
    }

    @Override
    public void onEndpointLost(String s) {
        if (!mIsHost) {
            isConnected = false;
        }
    }

    @Override
    public void onMessageReceived(String s, byte[] bytes, boolean b) {

    }

    @Override
    public void onDisconnected(String s) {

    }

    private class NearbyConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.e(TAG, "connect failed.");
        }
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






    public static Data getData() {
        return data;
    }

    private class NearbyConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
        @Override
        public void onConnected(Bundle connectionHint) {
                Nearby.Messages.getPermissionStatus(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                       // sendMessage();
                      //  Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener);
                        Log.e(TAG, "Conectou");
                        discover();

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


    private boolean isConnectedToNetwork() {
        ConnectivityManager connManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        for (int networkType : NETWORK_TYPES) {
            NetworkInfo info = connManager.getNetworkInfo(networkType);
            if (info != null && info.isConnectedOrConnecting()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public void onConnectionRequest(final String remoteEndpointId, final String remoteDeviceId,
                                    final String remoteEndpointName, byte[] payload) {
        if (mIsHost) {
            Nearby.Connections.acceptConnectionRequest(mGoogleApiClient, remoteEndpointId, payload, this)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                if (!remotePeerEndpoints.contains(remoteEndpointId)) {
                                    remotePeerEndpoints.add(remoteEndpointId);
                                }

                                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                sendMessage(remoteDeviceId + " connected!");
                            }
                        }
                    });
        } else {
            Nearby.Connections.rejectConnectionRequest(mGoogleApiClient, remoteEndpointId);
        }
    }


    private void sendMessage(String message) {
        if (mIsHost) {
            final ChatMessage chatMessage = new ChatMessage("Carro 1", System.currentTimeMillis(), 0.1,0.2,0.2);
            byte[] content;
            try {
                content = chatMessage.toString().getBytes(ENCODE);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, e.toString());
                return;
            }

            Nearby.Connections.sendReliableMessage(mGoogleApiClient, remotePeerEndpoints, content);
            //adapter.add(message);
            //adapter.notifyDataSetChanged();
        } else {
            Nearby.Connections.sendReliableMessage(mGoogleApiClient, mRemoteHostEndpoint,
                    (Nearby.Connections.getLocalDeviceId(mGoogleApiClient) + " says: " + message).getBytes());
        }
    }

    private void discover() {
        if (!isConnectedToNetwork())
            return;

        String serviceId = getString(R.string.service_id);
        Nearby.Connections.startDiscovery(mGoogleApiClient, serviceId, CONNECTION_TIME_OUT, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            MainActivity.this.status.setText("Discovering");
                        }
                    }
                });
    }
}
