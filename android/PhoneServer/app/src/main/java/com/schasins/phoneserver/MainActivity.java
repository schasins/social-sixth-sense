package com.schasins.phoneserver;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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

    LocationManager locationManager;
    String locationProvider;

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
        String approachString = calculateApproachString();
        String locationString = calculateLocationString();
        if (approachString.isEmpty()) {
            locationString = getString(R.string.field_error);
        }
        if (locationString.isEmpty()) {
            locationString = getString(R.string.field_error);
        }
        ((TextView) findViewById(R.id.approach_field)).setText(approachString);
        ((TextView) findViewById(R.id.location_field)).setText(locationString);
    }

    private String calculateApproachString() {
        //TODO: get value from Arduino
        return "0.5";
    }

    private String calculateLocationString() {
        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        String lastKnownLocationStr = "";
        if (lastKnownLocation != null) {
            lastKnownLocationStr = lastKnownLocation.toString();
        }
        return lastKnownLocationStr;
    }

    public void postData() {
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {

                try {
                    try {
                        // Create a new HttpClient and Post Header
                        HttpClient httpclient = new DefaultHttpClient();
                        HttpPost httppost = new HttpPost("http://kaopad.cs.berkeley.edu:1234/data");

                        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                        long timestamp = calendar.getTimeInMillis();

                        Location lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                        String lastKnownLocationStr = "";
                        if (lastKnownLocation != null) {
                            lastKnownLocationStr = lastKnownLocation.toString();
                        }

                        // Add your data
                        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                        nameValuePairs.add(new BasicNameValuePair("approach", ".5"));
                        nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(timestamp)));
                        nameValuePairs.add(new BasicNameValuePair("gps", lastKnownLocationStr));
                        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                        // Execute HTTP Post Request
                        HttpResponse response = httpclient.execute(httppost);

                    } catch (ClientProtocolException e) {
                        // TODO Auto-generated catch block
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();


    }
}
