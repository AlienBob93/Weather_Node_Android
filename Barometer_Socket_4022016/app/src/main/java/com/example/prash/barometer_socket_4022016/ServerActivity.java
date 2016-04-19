package com.example.prash.barometer_socket_4022016;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ServerActivity extends AppCompatActivity {

    public static final int SERVERPORT = 8080;

    private TextView IP;

    private static List<Float> Pressure = new ArrayList<>(),
            time = new ArrayList<>();
    private static List<Double> CoordinatesX = new ArrayList<>(),
            CoordinatesY = new ArrayList<>();

    private TextView Pressure_1, Pressure_2, Pressure_3,
            Location_1, Location_2, Location_3,
            Rate, Forecast, Area_of_Interest;

    private static float meanPressure = 0, meanTime = 0, rateOfPressurChange = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        IP = (TextView) findViewById(R.id.IP);
        IP.setText(getIpAddress());

        Pressure_1 = (TextView) findViewById(R.id.Pressure_1);
        Pressure_2 = (TextView) findViewById(R.id.Pressure_2);
        Pressure_3 = (TextView) findViewById(R.id.Pressure_3);
        Location_1 = (TextView) findViewById(R.id.Location_1);
        Location_2 = (TextView) findViewById(R.id.Location_2);
        Location_3 = (TextView) findViewById(R.id.Location_3);

        Rate = (TextView) findViewById(R.id.Rate);
        Forecast = (TextView) findViewById(R.id.Forcast);
        Area_of_Interest = (TextView) findViewById(R.id.Around_Location);

        //<editor-fold desc="Listener Thread">
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    //Create a server socket object and bind it to a port
                    ServerSocket socServer = new ServerSocket(SERVERPORT);
                    //Create server side client socket reference
                    Socket socClient = null;
                    //Infinite loop will listen for client requests to connect
                    while (true) {
                        //Accept the client connection and hand over communication to server side client socket
                        socClient = socServer.accept();
                        //For each client new instance of AsyncTask will be created
                        ServerAsyncTask serverAsyncTask = new ServerAsyncTask();
                        //Start the AsyncTask execution
                        //Accepted client socket object will pass as the parameter
                        serverAsyncTask.execute(new Socket[] {socClient});
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //</editor-fold>
    }

    /**
     * AsyncTask which handles the communication with clients
     */
    class ServerAsyncTask extends AsyncTask<Socket, Void, String> {
        //Background task which serve for the client
        @Override
        protected String doInBackground(Socket... params) {
            String result = null;
            //Get the accepted socket object
            Socket mySocket = params[0];
            try {
                //Get the data input stream coming from the client
                InputStream is = mySocket.getInputStream();
                //Get the output stream to the client
                PrintWriter out = new PrintWriter(
                        mySocket.getOutputStream(), true);
                //Write data to the data output stream
                out.println("Connected");
                //Buffer the data input stream
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(is));
                //Read the contents of the data buffer
                result = br.readLine();
                //Close the client connection
                mySocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            //After finishing the execution of background task data will be write the text view
            String sensorData[] = s.split("\\s+");

            // Gathering the client data, the number of pressure data points can varied
            // the time interval between the data points can be changed in the client side.
            CoordinatesX.add(Double.parseDouble(sensorData[0]));
            CoordinatesY.add(Double.parseDouble(sensorData[1]));
            Pressure.add(Float.parseFloat(sensorData[2]));
            time.add((float) 5*60*Pressure.size()-1);
            //time.add((float) 5*60);

            //<editor-fold desc="Display the last three data points">
            Pressure_1.setText("" + Pressure.get(Pressure.size() - 1) + "hPa");
            Location_1.setText("" + CoordinatesX.get(CoordinatesX.size() - 1) + ", " +
                    CoordinatesY.get(CoordinatesX.size() - 1));
            if (Pressure.size() >= 2) {
                Pressure_2.setText("" + Pressure.get(Pressure.size() - 2) + "hPa");
                Location_2.setText("" + CoordinatesX.get(CoordinatesX.size() - 2) + ", " +
                        CoordinatesY.get(CoordinatesX.size() - 2));

                if (Pressure.size() >= 3) {
                    Pressure_3.setText("" + Pressure.get(Pressure.size() - 3) + "hPa");
                    Location_3.setText("" + CoordinatesX.get(CoordinatesX.size() - 3) + ", " +
                            CoordinatesY.get(CoordinatesX.size() - 3));
                }
            }
            //</editor-fold>

            //<editor-fold desc="Check rate of rise or fall of pressure">
            if (Pressure.size() < 2) {
                Rate.setText("Waiting for sufficient data");
                Forecast.setText("Accuracy increases with use");
            } else {
                // applying least square method to calculate the slope of collected data
                // https://www.easycalculation.com/analytical/learn-least-square-regression.php
                float sumP = 0, sumT = 0, sumPT = 0, sumT2 = 0, N = Pressure.size();
                for (int i = 0; i < N; i++) {
                    sumP += Pressure.get(i);
                    sumT += time.get(i);
                }
                meanPressure = sumP/N;
                meanTime = sumT/N;
                Log.d("UI", "mP " + meanPressure + " mT " + meanTime);
                for (int i = 0; i < N; i++) {
                    sumPT += (Pressure.get(i) - meanPressure)*(time.get(i) - meanTime);
                    sumT2 += (time.get(i) - meanTime)*(time.get(i) - meanTime);
                }
                rateOfPressurChange = sumPT/sumT2;
                Log.d("UI", "mPT " + sumPT + " mT2 " + sumT2);
                Log.d("UI", "m " + rateOfPressurChange);

				setForecastText(meanPressure, rateOfPressurChange);
                Rate.setText("" + rateOfPressurChange + "hPa/sec");
            }
            //</editor-fold>

            //<editor-fold desc="calculate the rough center of the area of interest from the location data">
            double sumX = 0, sumY = 0;
            for (int i = 0; i < CoordinatesX.size(); i++) {
                sumX += CoordinatesX.get(i);
                sumY += CoordinatesY.get(i);
            }
            double meanX = sumX/CoordinatesX.size(), meanY = sumY/CoordinatesY.size();
            //</editor-fold>
            Area_of_Interest.setText("" + meanX + ", " + meanY);

            //<editor-fold desc="keeping the number collected data points below 120">
            if (Pressure.size() > 120) {
                Pressure.remove(0);
                CoordinatesX.remove(0);
                CoordinatesY.remove(0);
                time.remove(0);
            }
            //</editor-fold>
        }
    }

    private void setForecastText(float meanP, float PRateChange) {
        /* since on a normal steady day the changes in pressure isn't greater than 3.38hPa
         the conditions for rapidly falling and rising pressure is chosen to be 0.1hPa/sec
         http://www.sciencecompany.com/How-a-Barometer-Works.aspx */
        if (meanP > 1022.68) {
            if (PRateChange > 0) {
                Forecast.setText("Continued Fair");
            } if (PRateChange < 0) {
                Forecast.setText("Fair");
            } if (PRateChange < -3.1) {
                Forecast.setText("Cloudy, may get Warmer");
            }
        } else if (meanP > 1009.14 && meanP < 1022.68) {
            if (PRateChange > 0) {
                Forecast.setText("Will continue to Same a present");
            } if (PRateChange < 0) {
                Forecast.setText("may be Slight Change in the conditions");
            } if (PRateChange < -3.1) {
                Forecast.setText("Rain Likely");
            }
        } else if (meanP < 1009.14) {
            if (PRateChange > 0) {
                Forecast.setText("will Clear, may get Cooler");
            } if (PRateChange < 0) {
                Forecast.setText("Rain");
            } if (PRateChange < -3.1) {
                Forecast.setText("Stormy");
            }
        }
    }

    private String getIpAddress() {
        String ip = "";
        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface
                    .getNetworkInterfaces();
            while (enumNetworkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = enumNetworkInterfaces
                        .nextElement();
                Enumeration<InetAddress> enumInetAddress = networkInterface
                        .getInetAddresses();
                while (enumInetAddress.hasMoreElements()) {
                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {
                        ip += inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            ip += "Something's Wrong! " + e.toString() + "\n";
        }

        return ip;
    }
}
