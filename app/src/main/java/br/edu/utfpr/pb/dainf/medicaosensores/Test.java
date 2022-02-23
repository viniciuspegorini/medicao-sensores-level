package br.edu.utfpr.pb.dainf.medicaosensores;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import br.edu.utfpr.pb.dainf.medicaosensores.model.Configuration;
import br.edu.utfpr.pb.dainf.medicaosensores.util.GPSData;
import br.edu.utfpr.pb.dainf.medicaosensores.util.GPSTracker;

public class Test extends AppCompatActivity implements SensorEventListener {
    private Sensor accelerometer;
    private Sensor gyroscope;
    SensorManager sensorManager;
    //GPSTracker gps;

    GPSTracker gps;

    float sensorX;
    float sensorY;
    float sensorZ;

    float sensorXG;
    float sensorYG;
    float sensorZG;

    TextView txtAccel;
    TextView txtA;
    TextView txtGPS;
    TextView txtFile;
    TextView txtVelocidade;

    private Handler hGPSScreen;
    private Handler hGPSLogger;
    private int iGPSInterval = 1000;
    private int iGPSScreenInterval = 1000;

    private Handler hFileHandler;
    private int iFileInterval = 1000 * 1;

    private Handler hScreenHandler;
    private int iScreenInterval = 1000 * 1;

    private static int PERMISSION_ALL = 1;


    private File directory;
    private String directoryName;
    private String directoryApp;
    private String gpsFile;
    private String accFile;
    private String gyroFile;

    private String gpsData;
    private String accelData;
    private String gyroData;
    private String screenGPSData;

    private Long startTime;
    private Configuration config;

    private int GPS_NETWORK = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_test);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        this.txtAccel = (TextView) findViewById(R.id.txtAccel);
        this.txtA = (TextView) findViewById(R.id.txtA);
        this.txtGPS = (TextView) findViewById(R.id.txtGPS);
        this.txtFile = (TextView) findViewById(R.id.txtFile);
        this.txtVelocidade = (TextView) findViewById(R.id.txtVelocidade);
        Typeface custom_font = Typeface.createFromAsset(getAssets(),  "fonts/lcd.ttf");
        txtVelocidade.setTypeface(custom_font);

        final Button btnStart = (Button) findViewById(R.id.btnStart);
        final Button btnStop = (Button) findViewById(R.id.btnStop);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLogging();
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLogging();
                btnStop.setEnabled(false);
                btnStart.setEnabled(true);
            }
        });

        this.gps = new GPSTracker(this, iGPSInterval);
        gps.getLocation();
        this.hGPSScreen = new Handler();
        this.tGPSScreen.run();
    }

    public boolean isNumeric(String s) {
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");
    }

    private void startLogging() {
        //CarregarConfigs
        Gson gson = new Gson();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.config = gson.fromJson(preferences.getString("config", ""), Configuration.class);


        if (Integer.parseInt(config.getTaxaAcelerometro()) > 0) {
            this.sensorManager.registerListener(this, accelerometer,
                    (int) Math.ceil((1000000 / Integer.parseInt(config.getTaxaAcelerometro()))));
            this.sensorManager.registerListener(this, gyroscope,
                    (int) Math.ceil((1000000 / Integer.parseInt(config.getTaxaAcelerometro()))));
        } else {
            this.sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            this.sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        }
        //this.sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        if ( isNumeric(config.getTaxaGPS() ) ) {
            this.gps = new GPSTracker(Test.this, Long.parseLong(config.getTaxaGPS()));
        }else{
            this.gps = new GPSTracker(Test.this, 1000l);
        }


        directoryName = "utfpr-pb_sensores";
        directoryApp = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + directoryName + "/";
        directory = new File(directoryApp);
        directory.mkdirs();


        String startFileName;
        startFileName = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                new Date(System.currentTimeMillis()))
                .replace(" ", "_").replace(":", "-");
        startTime = System.currentTimeMillis();

        accFile = startFileName + "-Acc.txt";
        gpsFile = startFileName + "-GPS.txt";
        gyroFile = startFileName + "-Gyro.txt";
        String configFile = startFileName + "-Config.txt";


        saveFile(gson.toJson(config), configFile);
        txtFile.setText("Log \n  '" + accFile + "'\n  '" + gpsFile + "'\n  '" + gyroFile + "'\n  '" + configFile + "'");



        this.accelData = "x;y;z;date" + System.getProperty("line.separator");
        this.gyroData = "x;y;z;date" + System.getProperty("line.separator");
        this.gpsData = "lon;lat;satFix;satTotal;kmh;date" + System.getProperty("line.separator");

        this.hGPSLogger = new Handler();
        this.tGPSLogger.run();

        this.hFileHandler = new Handler();
        this.tFileWriter.run();

        this.hScreenHandler = new Handler();
        this.tScreenRefresh.run();

    }

    private void stopLogging() {
        this.hGPSLogger.removeCallbacks(tGPSLogger);
        this.hFileHandler.removeCallbacks(tFileWriter);
        this.hScreenHandler.removeCallbacks(tScreenRefresh);
        this.sensorManager.unregisterListener(this, accelerometer);
        this.sensorManager.unregisterListener(this, gyroscope);
        this.gps.stopUsingGPS();
        writeFiles();
        this.accelData = "";
        this.gpsData = "";
        this.gyroData = "";
    }

    /**
     * é invocado pela plataforma Android todas as vezes que o acelerômetro sofre alguma modificação
     *
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorX = sensorEvent.values[0];
            sensorY = sensorEvent.values[1];
            sensorZ = sensorEvent.values[2];
            //System.currentTimeMillis()
            accelData += sensorX + ";" + sensorY + ";" + sensorZ + ";" +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(
                            new Date(System.currentTimeMillis()))
                    + System.getProperty("line.separator");
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorXG = sensorEvent.values[0];
            sensorYG = sensorEvent.values[1];
            sensorZG = sensorEvent.values[2];
            gyroData += sensorXG + ";" + sensorYG + ";" + sensorZG + ";" +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(
                            new Date(System.currentTimeMillis()))
                    + System.getProperty("line.separator");

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    Runnable tGPSLogger = new Runnable() {
        @Override
        public void run() {
            try {
                updateGPSData();
            } finally {
                hGPSLogger.postDelayed(tGPSLogger, iGPSInterval);
            }
        }
    };

    Runnable tGPSScreen = new Runnable() {
        @Override
        public void run() {
            try {
                updateGPSScreen();
            } finally {
                hGPSScreen.postDelayed(tGPSScreen, iGPSScreenInterval);
            }
        }
    };

    Runnable tFileWriter = new Runnable() {
        @Override
        public void run() {
            try {
                writeFiles();
            } finally {
                hFileHandler.postDelayed(tFileWriter, iFileInterval);
            }
        }
    };

    Runnable tScreenRefresh = new Runnable() {
        @Override
        public void run() {
            try {
                updateScreen();
            } finally {
                hScreenHandler.postDelayed(tScreenRefresh, iScreenInterval);
            }
        }
    };

    /**
     * Refreshes screen label information.
     */
    private void updateScreen() {
        txtAccel.setText("Accel/Gyro " +
                "\n  x: " + String.format("%.3f", sensorX) + " /  x: " + String.format("%.3f", sensorXG) +
                "\n  y: " + String.format("%.3f", sensorY) + " /  y: " + String.format("%.3f", sensorYG) +
                "\n  z: " + String.format("%.3f", sensorZ) + " /  z: " + String.format("%.3f", sensorZG));

        txtA.setText("Tempo: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000) + " (s).");

    }

    private void updateGPSData() {
        try {
            String sat;

            if (gps.getSatellitesInFix() == 0 &&
                    (gps.getLongitude() != 0 || gps.getLatitude() != 0)) {
                sat = "- Utilizando torres de celular.";

                if (GPS_NETWORK == 1){
                    gps.changeSource(false);
                }
                GPS_NETWORK = 2;
            } else {
                sat = "- Sat. " + gps.getSatellitesInFix() + "/" + gps.getSatellites();
                if (GPS_NETWORK == 2){
                    gps.changeSource(true);
                }
                GPS_NETWORK = 1;
            }
            Log.d("SAT", "SAT " + gps.getSatellitesInFix() + " - " + sat);

            screenGPSData = "GPS " + sat +
                    "\n  lon:" + gps.getLongitude() + " \n  lat:" + gps.getLatitude();
            gpsData += gps.getLongitude() + ";" + gps.getLatitude() + ";" +
                    gps.getSatellitesInFix() + ";" + gps.getSatellites() + ";" +
                    gps.getSpeed() + ";" +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(
                            new Date(System.currentTimeMillis()))
                    + System.getProperty("line.separator");



        } catch (Exception ex) {
            txtGPS.setText("GPS ERROR. " + ex.getMessage());
        }
    }

    private void updateGPSScreen() {
        try {
            String sat;

            if (gps.getSatellitesInFix() == 0 &&
                    (gps.getLongitude() != 0 || gps.getLatitude() != 0)) {
                sat = "- Utilizando torres de celular.";

                if (GPS_NETWORK == 1){
                    gps.changeSource(false);
                }
                GPS_NETWORK = 2;
            } else {
                sat = "- Sat. " + gps.getSatellitesInFix() + "/" + gps.getSatellites();
                if (GPS_NETWORK == 2){
                    gps.changeSource(true);
                }
                GPS_NETWORK = 1;
            }
            Log.d("SAT", "SAT " + gps.getSatellitesInFix() + " - " + sat);

            screenGPSData = "GPS " + sat +
                    "\n  lon:" + gps.getLongitude() + " \n  lat:" + gps.getLatitude();

            txtGPS.setText(screenGPSData);
            txtVelocidade.setText(gps.getSpeed() + " km/h");
        } catch (Exception ex) {
            txtGPS.setText("GPS ERROR. " + ex.getMessage());
        }
    }


    private void writeFiles() {
        try {
            Log.d("Dados", "Gravando dados GPS - " + gpsFile);
            Log.d("Dados", "Gravando dados Sensores - " + accFile);
            Log.d("Dados", "Gravando dados Sensores - " + gyroFile);
            String dados = gpsData;
            gpsData = "";
            saveFile(dados, gpsFile);

            dados = accelData;
            accelData = "";
            saveFile(dados, accFile);

            dados = gyroData;
            gyroData = "";
            saveFile(dados, gyroFile);
        } catch (Exception e) {
            Log.d("ERRO: ", "Erro ao salvar arquivo. (writeFiles()) - " + e.getMessage());
            Toast.makeText(this, "Erro ao salvar arquivo!",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveFile(String dataLog, String fileName) {
        try {
            File fileExt = new File(directoryApp, fileName);
            FileOutputStream fosExt;
            if (!fileExt.exists()) {
                fileExt.getParentFile().mkdirs();
                fosExt = new FileOutputStream(fileExt);
            } else {
                fosExt = new FileOutputStream(fileExt, true);
            }
            fosExt.write(dataLog.toString().getBytes());
            fosExt.close();
        } catch (IOException e) {
            Log.d("ERRO: ", "Erro ao salvar arquivo. (saveFile()) - " + e.getMessage());
            Toast.makeText(this, "Erro ao salvar arquivo!",
                    Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /*private void setGPSPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSAO_GPS);
            }
        }
    }

    private void setStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSAO_STORAGE);
            }
        }
    }*/

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

}