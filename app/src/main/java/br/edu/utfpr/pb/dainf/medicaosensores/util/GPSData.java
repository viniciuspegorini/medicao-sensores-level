package br.edu.utfpr.pb.dainf.medicaosensores.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.location.GpsSatellite;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class GPSData implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        Listener,
        LocationListener {

    //Pegar localização
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    Location mLastLocation;
    private double latitude;
    private double longitude;


    //Número de satélites não é possível com o FusedLocation
    private int satellites = 0;
    private int satellitesInFix = 0;
    LocationManager locationManager;
    Context mContext;
    private boolean isGPSEnabled = false;
    private boolean canGetLocation = false;
    private static long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 metros
    private static long MIN_TIME_BW_UPDATES = 500; // 1 minute=1000 * 60 * 1

    @SuppressLint("MissingPermission")
    public GPSData(Context context) {
        mContext = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener((Activity) mContext, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                longitude = location.getLongitude();
                                latitude = location.getLatitude();
                            }
                        }
                    });
            enableLocationManager();
        }
    }

    @SuppressLint("MissingPermission")
    public void enableLocationManager() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(mContext.LOCATION_SERVICE);
            // obter o status GPS
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!isGPSEnabled) {
                //O GPS não está habilitado
            } else {
                this.canGetLocation = true;
                locationManager.addGpsStatusListener(this);
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        mGoogleApiClient.connect();
    }

    public void stop() {
        mGoogleApiClient.disconnect();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(Bundle bundle) {
        try {
            mLastLocation = mFusedLocationClient.getLastLocation().getResult();//LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                this.longitude = mLastLocation.getLongitude();
                this.latitude = mLastLocation.getLatitude();
            }
        } catch (Exception e) {
        }
    }

    @SuppressLint("MissingPermission")
    public void updateLocation() {
        try {
            mLastLocation = mFusedLocationClient.getLastLocation().getResult();
            if (mLastLocation != null) {
                this.longitude = mLastLocation.getLongitude();
                this.latitude = mLastLocation.getLatitude();
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getSatellites() {
        return satellites;
    }

    public int getSatellitesInFix() {
        return satellitesInFix;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onGpsStatusChanged(int i) {
        satellitesInFix = 0;
        satellites = 0;
        for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
            if (sat.usedInFix()) {
                satellitesInFix++;
            }
            satellites++;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

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
}