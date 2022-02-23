package br.edu.utfpr.pb.dainf.medicaosensores;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import java.util.List;

import br.edu.utfpr.pb.dainf.medicaosensores.model.Configuration;
import br.edu.utfpr.pb.dainf.medicaosensores.util.GPSTracker;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private Sensor accelerometer;
    private Sensor gyroscope;
    SensorManager sensorManager;
    GPSTracker gps;

    float sensorX;
    float sensorY;
    float sensorZ;

    TextView txtAccel;
    TextView txtA;
    TextView txtGPS;
    TextView txtFile;

    private Handler hGPSHandler;
    private int iGPSInterval = 1000 * 1;

    private Handler hFileHandler;
    private int iFileInterval = 1000 * 1;

    private Handler hScreenHandler;
    private int iScreenInterval = 1000 * 1;

    private static int PERMISSION_ALL = 1;


    private File directory;
    private String directoryName;
    private String diretoryApp;
    private String gpsFile;
    private String accFile;
    private String gyroFile;

    private String gpsData;
    private String accelData;
    private String gyroData;
    private String screenGPSData;

    private Long startTime;
    private Configuration config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION};
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        this.txtAccel = (TextView) findViewById(R.id.txtAccel);
        this.txtA = (TextView) findViewById(R.id.txtA);
        this.txtGPS = (TextView) findViewById(R.id.txtGPS);
        this.txtFile = (TextView) findViewById(R.id.txtFile);



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
    }

    public boolean isNumeric(String s) {
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");
    }

    private void startLogging(){
        //CarregarConfigs
        Gson gson = new Gson();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.config = gson.fromJson( preferences.getString("config", "") , Configuration.class);


        this.sensorManager.registerListener(this, accelerometer, 2000);// config.getTaxaAcelerometroValue());
        this.sensorManager.registerListener(this, gyroscope, 2000); //config.getTaxaAcelerometroValue());
        //this.sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        if ( isNumeric(config.getTaxaGPS() ) ) {
            this.gps = new GPSTracker(MainActivity.this, Long.parseLong(config.getTaxaGPS()));
        }else{
            this.gps = new GPSTracker(MainActivity.this, 100l);
        }
        directoryName = "utfpr-pb_sensores";
        diretoryApp = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                + "/" + directoryName + "/";
        directory = new File(diretoryApp);
        directory.mkdirs();


        String startFileName;
        Date data = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        startFileName = sdf.format(data).replace(" ", "_").replace(":","-");
        startTime = System.currentTimeMillis();

        accFile = startFileName + "-Acc.txt";
        gpsFile = startFileName + "-GPS.txt";
        gyroFile = startFileName + "-Gyro.txt";
        String configFile = startFileName + "-Config.txt";

        saveFile(gson.toJson(config), configFile);
        txtFile.setText("Log \n  '" + accFile + "'\n  '" + gpsFile + "'" + "'\n  '" + gyroFile + "'");

        this.accelData = "";
        this.gyroData = "";
        this.gpsData = "";

        this.hGPSHandler = new Handler();
        this.tGPSLogger.run();

        this.hFileHandler = new Handler();
        this.tFileWriter.run();

        this.hScreenHandler = new Handler();
        this.tScreenRefresh.run();

    }

    private void stopLogging(){
        this.hGPSHandler.removeCallbacks(tGPSLogger);
        this.hFileHandler.removeCallbacks(tFileWriter);
        this.hScreenHandler.removeCallbacks(tScreenRefresh);
        this.sensorManager.unregisterListener(this, accelerometer);
        this.sensorManager.unregisterListener(this, gyroscope);

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
        sensorX = sensorEvent.values[0];
        sensorY = sensorEvent.values[1];
        sensorZ = sensorEvent.values[2];

        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelData += sensorX + ";" + sensorY + ";" + sensorZ + ";" +
                    System.currentTimeMillis() + System.getProperty("line.separator");
        }else if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroData += sensorX + ";" + sensorY + ";" + sensorZ + ";" +
                    System.currentTimeMillis() + System.getProperty("line.separator");
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
                hGPSHandler.postDelayed(tGPSLogger, iGPSInterval);
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
        txtAccel.setText("Accel/Gyro \n  x: " + (sensorX) +
                "\n  y: " + (sensorY) +
                "\n  z: " + (sensorZ));

        txtA.setText("Tempo: " + String.valueOf( (System.currentTimeMillis() - startTime)/1000) + " (s).");
        txtGPS.setText(screenGPSData);
    }

    private void updateGPSData() {
        try {
            if (gps.canGetLocation()) {
                screenGPSData = "GPS - Sat. " + gps.getSatellitesInFix() + "/" + gps.getSatellites() +
                        "\n  lon:" + gps.getLongitude() + " \n  lat:" + gps.getLatitude();
                gpsData += gps.getLongitude() + ";" + gps.getLatitude() + ";" +
                        System.currentTimeMillis() + System.getProperty("line.separator");

                //Log.d("SAT","Satélites - " + gps.getSatellitesInFix() + "/" + gps.getSatellites());
            } else {
                txtGPS.setText("GPS: Error.");
            }
        } catch (Exception ex) {
            txtGPS.setText("GPS ERROR. " + ex.getMessage());
        }
    }

    private void writeFiles() {
        try {
            Log.d("Dados","Gravando dados GPS -> " + gpsFile);
            Log.d("Dados","Gravando dados Sensores -> " + accFile);
            Log.d("Dados","Gravando dados Sensores -> " + gyroFile);
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
            File fileExt = new File(diretoryApp, fileName);
            FileOutputStream fosExt;
            if (!fileExt.exists()) {
                fileExt.getParentFile().mkdirs();
                fosExt = new FileOutputStream(fileExt);
            }else {
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
