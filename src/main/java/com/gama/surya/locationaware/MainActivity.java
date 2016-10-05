package com.gama.surya.locationaware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;

import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/*MODUL
1. Location Updates
2. Location Address*/

public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks,
        LocationListener {

    /*1. START*/
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final String LOCATION_KEY = "location_key";
    private static final String LAST_UPDATED_TIME = "last_updated_time";

    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private TextView mLatitude;
    private TextView mLongitude;
    private TextView mLastUpdate;
    private Location currentLocation;
    private String lastUpdateTime;
    /*1. FINISH*/

    /*2. START*/
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    protected String mAddressOutput;
    private AddressResultReceiver mResultReceiver;
    private TextView mLocationAddressTextView;
    private boolean mAddressRequested;
    ProgressBar mProgressBar;
    /*2. FINISH*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLatitude = (TextView) findViewById(R.id.current_latitude);
        mLongitude = (TextView) findViewById(R.id.current_longitude);
        mLastUpdate = (TextView) findViewById(R.id.last_update_time);

        /*2. START*/
        mResultReceiver = new AddressResultReceiver(new Handler());
        mLocationAddressTextView = (TextView) findViewById(R.id.location_address_view);
        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
        // Set default, then update using values stored in the Bundle
        mAddressRequested = false;
        mAddressOutput = "";
        updateUIWidgets();
        /*2. FINISH*/

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        /*
        PRIORITY_BALANCED_POWER_ACCURACY - Use this setting to request location precision to within a city block, which is an accuracy of approximately 100 meters. This is considered a coarse level of accuracy, and is likely to consume less power. With this setting, the location services are likely to use WiFi and cell tower positioning. Note, however, that the choice of location provider depends on many other factors, such as which sources are available.
        PRIORITY_HIGH_ACCURACY - Use this setting to request the most precise location possible. With this setting, the location services are more likely to use GPS to determine the location.
        PRIORITY_LOW_POWER - Use this setting to request city-level precision, which is an accuracy of approximately 10 kilometers. This is considered a coarse level of accuracy, and is likely to consume less power.
        PRIORITY_NO_POWER - Use this setting if you need negligible impact on power consumption, but want to receive location updates when available. With this setting, your app does not trigger any location updates, but receives locations triggered by other apps.
*/
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        // Untuk restore saved values dari previous instance of the activity
        updateValuesFromBundle(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // jika resume maka location update dijalankan
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // jika pause maka location update dihentikan
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    private void updateUI() {
        mLatitude.setText(String.format("%s: %f", "Latitude", currentLocation.getLatitude()));
        mLongitude.setText(String.format("%s: %f", "Longitude", currentLocation.getLongitude()));
        lastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        mLastUpdate.setText(String.format("%s %s", "Last Update Time : ", lastUpdateTime));
    }

    protected void startLocationUpdates() {
        // syarat untuk requestLocationUpdates bisa jalan
        if (ContextCompat.checkSelfPermission(this , android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnected(Bundle bundle) {
        // syarat untuk getLastConnection bisa jalan
        if (ContextCompat.checkSelfPermission(this , android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);


        if (currentLocation == null) {
            startLocationUpdates();
        }

        /*2. START*/
        startIntentService();
        mAddressRequested = true;
        /*2. FINISH*/

        updateUI();
    }



    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Toast.makeText(MainActivity.this, "Connection Suspended", Toast.LENGTH_SHORT).show();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }


    @Override
    public void onLocationChanged(Location changedLocation) {
        currentLocation = changedLocation;
        updateUI();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable(LOCATION_KEY, currentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME, lastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            // Update the value of currentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // currentLocation is not null.
                currentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of lastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME)) {
                lastUpdateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME);
            }

            /*2. START*/
            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
            /*2. FINISH*/

            updateUI();
        }
    }


    /*2. START*/

    //Receiver for data sent from FetchAddressIntentService
    @SuppressLint("ParcelCreator")
    public class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        // Receives data sent from FetchAddressIntentService and updates the UI in MainActivity
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            //Display the address string or an error message sent from the intent service
            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            //Show a toast message if an address was found
            if (resultCode == Constants.SUCCESS_RESULT) {
                Toast.makeText(getBaseContext(), "Address Found", Toast.LENGTH_SHORT).show();
            }

            //Reset. Enable the Fetch address button and stop showing the progress bar
            mAddressRequested = false;
            updateUIWidgets();
        }
    }

//     Creates an intent, adds location data to it as an extra, and starts the intent service for
//     fetching an address.
    protected void startIntentService() {
        // Create an intent for passing to the intent service responsible for fetching the address.
        Intent intent = new Intent(this, FetchAddressIntentService.class);

        // Pass the result receiver as an extra to the service.
        intent.putExtra(Constants.RECEIVER, mResultReceiver);

        // Pass the location data as an extra to the service.
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, currentLocation);

        // Start the service. If the service isn't already running, it is instantiated and started
        // (creating a process for it if needed); if it is running then it remains running. The
        // service kills itself automatically once all intents are processed.
        startService(intent);
    }

    /**
     * Toggles the visibility of the progress bar. Enables or disables the Fetch Address button.
     */
    private void updateUIWidgets() {
        if (mAddressRequested) {
            mProgressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            mProgressBar.setVisibility(ProgressBar.GONE);
        }
    }

    /**
     * Updates the address in the UI.
     */
    protected void displayAddressOutput() {
        mLocationAddressTextView.setText(mAddressOutput);
    }


    /*2. FINISH*/

}
