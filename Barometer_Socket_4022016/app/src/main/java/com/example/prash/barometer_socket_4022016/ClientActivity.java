package com.example.prash.barometer_socket_4022016;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class ClientActivity extends AppCompatActivity
        implements View.OnClickListener, SensorEventListener, LocationListener {

    private static final String TAG = "UI";
    private static final String SERVERPORT = "8080";
    public static final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private static String SERVER_IP;
    private EditText Server_IP;
    private TextView tvServerMessage;
    private Button Connect, Stop;

    private int mInterval = 5000*60; // 5 seconds by default, can be changed later
    private Handler mHandler;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private LocationManager locationManager;
    public static Location currentBestLocation = null;
    public static Location lastBestLocation = null;
    public static Float currentPressure = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tvServerMessage = (TextView) findViewById(R.id.tv);
        Server_IP = (EditText) findViewById(R.id.ServerIP);
        Connect = (Button) findViewById(R.id.Connect);
        Stop = (Button) findViewById(R.id.Stop);

        mHandler = new Handler();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        Connect.setOnClickListener(this);
        Stop.setOnClickListener(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        } else {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            // onLocationChanged called every 5 seconds
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, this);
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.d(TAG, "Error suitable sensor not found");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.Connect:
                SERVER_IP = Server_IP.getText().toString();
                mStatusChecker.run();
                break;
            case R.id.Stop:
                mHandler.removeCallbacks(mStatusChecker);
                break;
        }
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                //this function can change value of mInterval.
                Snackbar.make(findViewById(R.id.ServerIP), "Sending Sensor data", Snackbar.LENGTH_LONG).show();
                //Create an instance of AsyncTask
                ClientAsyncTask clientAST = new ClientAsyncTask();
                //Pass the server ip, port and client message to the AsyncTask
                lastBestLocation = getLastBestLocation();
                String pressure = currentPressure.toString();

                String SensorData = "" + lastBestLocation.getLatitude() + " " +
                        lastBestLocation.getLongitude() + " " + pressure;
                Log.d(TAG, SensorData);

                clientAST.execute(new String[] { SERVER_IP, SERVERPORT, SensorData });
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    private Location getLastBestLocation() {
        long GPSLocationTime = 0;
        long NetLocationTime = 0;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            if (null != locationGPS) {
                GPSLocationTime = locationGPS.getTime();
            }

            if (null != locationNet) {
                NetLocationTime = locationNet.getTime();
            }

            if ( 0 < GPSLocationTime - NetLocationTime ) {
                return locationGPS;
            }
            else {
                return locationNet;
            }
        }
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if(currentBestLocation == null){
            currentBestLocation = location;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        currentPressure = event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * AsyncTask which handles the communication with the server
     */
    class ClientAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String result = null;
            try {
                //Create a client socket and define internet address and the port of the server
                Socket socket = new Socket(params[0],
                        Integer.parseInt(params[1]));
                //Get the input stream of the client socket
                InputStream is = socket.getInputStream();
                //Get the output stream of the client socket
                PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                //Write data to the output stream of the client socket
                out.println(params[2]);
                //Buffer the data coming from the input stream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));
                //Read data in the input buffer
                result = br.readLine();
                //Close the client socket
                socket.close();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }
        @Override
        protected void onPostExecute(String s) {
            //Write server message to the text view
            tvServerMessage.setText(s);
        }
    }
}
