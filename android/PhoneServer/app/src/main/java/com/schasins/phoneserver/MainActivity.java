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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

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

    private EditText username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationProvider = LocationManager.NETWORK_PROVIDER;
        // Or use LocationManager.GPS_PROVIDER
        locationManager = setUpLocationManager();
        setupUI();

        username = (EditText)findViewById(R.id.username);
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
        calculateApproachString();
        calculateLocationString();
        ((TextView) findViewById(R.id.approach_field)).setText(approachString);
        ((TextView) findViewById(R.id.location_field)).setText(locationString);
    }

    private void calculateApproachString() {
        //TODO: get value from Arduino
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

                            String userid =  username.getText().toString();

                            // Add your data
                            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                            nameValuePairs.add(new BasicNameValuePair("approach", ".5"));
                            nameValuePairs.add(new BasicNameValuePair("time", String.valueOf(timestamp)));
                            nameValuePairs.add(new BasicNameValuePair("lat", lat));
                            nameValuePairs.add(new BasicNameValuePair("long", lon));
                            nameValuePairs.add(new BasicNameValuePair("userid", userid));
                            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                            // Create a custom response handler
                            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                                    httpResponse = response;
                                    int status = response.getStatusLine().getStatusCode();
                                    if (status >= 200 && status < 300) {
                                        HttpEntity entity = response.getEntity();
                                        return entity != null ? EntityUtils.toString(entity) : null;
                                    } else {
                                        throw new ClientProtocolException("Unexpected response status: " + status);
                                    }
                                }

                            };

                            // Execute HTTP Post Request
                            String responseString = httpclient.execute(httppost,responseHandler);
                            JSONObject jObject = new JSONObject(responseString);
                            long approach = jObject.getLong("approach");
                            String uid = jObject.getString("userid");
                            System.out.println(approach);
                            ((TextView) findViewById(R.id.paired_field)).setText(uid);

                        } catch (ClientProtocolException e) {
                            // TODO Auto-generated catch block
                            httpError = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        httpError = true;
                    } finally {
                        findViewById(R.id.root_activity).post(new Runnable() {
                            @Override
                            public void run() {
                                if (httpError) {
                                    showToast(getString(R.string.field_error));
                                } else {
                                    showToast("" + httpResponse.getStatusLine().getStatusCode());
                                }
                            }
                        });
                    }
                }
            }
        });
        thread.start();
    }

    /* Must be run on UI thread */
    private void showToast(CharSequence text) {
        if (text != null) {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_LONG;
            Toast.makeText(context, text, duration).show();
        }
    }
}
