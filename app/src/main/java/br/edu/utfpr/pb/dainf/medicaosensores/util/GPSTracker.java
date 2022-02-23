package br.edu.utfpr.pb.dainf.medicaosensores.util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.GpsSatellite;
import android.location.Location;
import android.location.GpsStatus.Listener;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import java.math.BigDecimal;

public class GPSTracker extends Service implements LocationListener, Listener {
    private final Context mContext;

    // flag para GPS status
    private boolean isGPSEnabled = false;

    // flag para network status
    private boolean isNetworkEnabled = false;

    // flag para GPS status
    private boolean canGetLocation = false;

    private Location location;
    private double latitude;
    private double longitude;
    private double speed = 0.0;

    // A distância mínima para mudar Atualizações em metros
    private static long MIN_DISTANCE_CHANGE_FOR_UPDATES = 0; // 10 metros

    // O tempo mínimo entre as atualizações em milissegundos
    private static long MIN_TIME_BW_UPDATES = 0; // 1 minute=1000 * 60 * 1

    // Declarar a Location Manager
    protected LocationManager locationManager;

    //Número de satélites
    private int satellites = 0;
    private int satellitesInFix = 0;
    private int timetofix = 0;

    public GPSTracker(Context context, long timeUpdate) {
        this.mContext = context;
        this.MIN_TIME_BW_UPDATES = timeUpdate;

        getLocation();
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);
            // obter o status GPS
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // obter o status da rede
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            Log.d("GPS", "GPS");
            if (!isGPSEnabled && !isNetworkEnabled) {
                //Nenhum provedor de rede está habilitado
            } else {
                this.canGetLocation = true;
                locationManager.addGpsStatusListener(this);
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("GPS Enabled", "GPS Enabled");
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(
                                LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();

                        }
                    }
                }
                // Primeiro localização obter de provedor de rede
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    if (location == null) {
                        Log.d("Network", "Network");
                        location = locationManager.getLastKnownLocation(
                                LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return location;
    }

    @SuppressLint("MissingPermission")
    public void changeSource(boolean gps) {
        if (!gps) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            if (location == null) {
                Log.d("Network", "Network");
                location = locationManager.getLastKnownLocation(
                        LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
            }
        }else{
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            Log.d("GPS Enabled", "GPS Enabled");
            if (locationManager != null) {
                location = locationManager.getLastKnownLocation(
                        LocationManager.GPS_PROVIDER);
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void onGpsStatusChanged(int event) {
        timetofix = locationManager.getGpsStatus(null).getTimeToFirstFix();
        satellitesInFix = 0;
        satellites = 0;
        for (GpsSatellite sat : locationManager.getGpsStatus(null).getSatellites()) {
            if (sat.usedInFix()) {
                satellitesInFix++;
            }
            satellites++;
        }
        Log.d("SAT", "SAT " + satellitesInFix + "/" + satellites);

        if (location.hasSpeed()) {
            this.speed = round(location.getSpeed() * 3.6, 3, BigDecimal.ROUND_HALF_UP);
        }else{
            this.speed = 0.0;
        }
    }

    public static double round(double unrounded, int precision, int roundingMode) {
        BigDecimal bd = new BigDecimal(unrounded);
        BigDecimal rounded = bd.setScale(precision, roundingMode);
        return rounded.doubleValue();
    }

    /**
     * Calling this function will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        // return latitude
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        // return longitude
        return longitude;
    }

    public double getSpeed(){
        return this.speed;
    }

    public void setSpeed(double speed){
        this.speed = speed;
    }

    /**
     * Função para verificar GPS / Wi-Fi habilitado
     *
     * @return boolean
     */
    public boolean canGetLocation() {
        return this.canGetLocation;
    }

    /**
     * Function para abrir o alerta
     */
    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);

        // titulo da mensagem de config
        alertDialog.setTitle("Configurar GPS");

        // mensagem de configuração
        alertDialog.setMessage("GPS não Ativado. Você deseja abrir o menu de configuração?");

        // On pressing Settings button
        alertDialog.setPositiveButton("Configuração", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });

        // btn cancelar
        alertDialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // Exibir alerta de mensagem
        alertDialog.show();
    }

    @Override
    public void onLocationChanged(Location location) {
        getLocation();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public int getSatellites() {
        return satellites;
    }

    public int getSatellitesInFix() {
        return satellitesInFix;
    }

    public int getTimetofix() {
        return timetofix;
    }
}