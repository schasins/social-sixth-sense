package com.schasins.phoneserver;

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends ActionBarActivity {

    private LocationManager locationManager;
    private String locationProvider;

    private String approachString;
    private String locationString;

    private HttpResponse httpResponse;
    private boolean httpError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER
        locationManager = setUpLocationManager();
        setupUI();
    }

    private void setupUI() {
        findViewById(R.id.send_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postData();
            }
        });
        findViewById(R.id.refresh_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refreshData();
            }
        });
    }

    private void readArduinoData() {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            return;
        }

        // Read some data! Most have just one port (port 0).
        List<UsbSerialPort> ports = driver.getPorts();
        UsbSerialPort port = ports.get(0); //FIXME: how do we know this is port 0?
        try {
            port.open(connection);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1,
                                UsbSerialPort.PARITY_NONE); //FIXME: how to pick args 2, 3, 4?
            byte buffer[] = new byte[16];
            int numBytesRead = port.read(buffer, 1000);
            Log.d("Main Activity", "Read " + numBytesRead + " bytes.");
            port.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private LocationManager setUpLocationManager(){
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {}
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        // Or, use GPS location data:
        // String locationProvider = LocationManager.GPS_PROVIDER;
        return locationManager;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void refreshData() {
        calculateApproachString();
        calculateLocationString();
        ((TextView) findViewById(R.id.approach_field)).setText(approachString);
        ((TextView) findViewById(R.id.location_field)).setText(locationString);
    }

    private void calculateApproachString() {
        //TODO: get value from Arduino
        readArduinoData();
        approachString = "0.5";
    }

    private void calculateLocationString() {
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        String lastKnownLocationStr = "";
        if (lastKnownLocation != null) {
            lastKnownLocationStr = lastKnownLocation.toString();
            locationString = lastKnownLocationStr;
        } else {
            locationString = getString(R.string.field_error);
        }
    }

    public void postData() {
        httpError = false;
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        try {
                            notify();
                            // Create a new HttpClient and Post Header
                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost("http://kaopad.cs.berkeley.edu:1234/data");

                            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                            long timestamp = calendar.getTimeInMillis();

                            Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                            String lat = "";
                            String lon = "";
                            if (lastKnownLocation != null){
                                lat = String.valueOf(lastKnownLocation.getLatitude());
                                lon = String.valueOf(lastKnownLocation.getLongitude());
                            }

                            // Add your data
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                            nameValuePairs.add(new BasicNameValuePair("approach", ".5"));
                            nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(timestamp)));
                            nameValuePairs.add(new BasicNameValuePair("lat", lat));
                            nameValuePairs.add(new BasicNameValuePair("long", lon));
                            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                            // Execute HTTP Post Request
                            httpResponse = httpclient.execute(httppost);

                        } catch (ClientProtocolException e) {
                            // TODO Auto-generated catch block
                            httpError = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        httpError = true;
                    } finally {
                        if (httpError)
                            showToast(getString(R.string.field_error));
                        else
                            showToast("" + httpResponse);
                    }
                }
            }
        });
        thread.start();
    }

    /* Runs a toast containing _TEXT on the UI thread. */
    private void showToast(CharSequence _text) {
        final String text = "" + _text;
        findViewById(R.id.root_activity).post(new Runnable() {
            @Override
            public void run() {
                if (text.isEmpty()) {
                    Context context = getApplicationContext();
                    int duration = Toast.LENGTH_LONG;
                    Toast.makeText(context, text, duration).show();
                }
            }
        });
    }

}
