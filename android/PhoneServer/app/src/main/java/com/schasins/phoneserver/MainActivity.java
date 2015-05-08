package com.schasins.phoneserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
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

    private byte buffer[] = new byte[4];
    private float arduinoValue = 0.0f;
    private EditText username;

//    /* Bluetooth stuff */
//    private final static int REQUEST_ENABLE_BT = 1;
//    private static final long SCAN_PERIOD = 10000;
//
//    private BluetoothAdapter mBluetoothAdapter;
//    private LeDeviceListAdapter mLeDeviceListAdapter;
//    private boolean mScanning;
//    private Handler mHandler;
//
//    // Device scan callback.
//    private BluetoothAdapter.LeScanCallback mLeScanCallback =
//            new BluetoothAdapter.LeScanCallback() {
//                @Override
//                public void onLeScan(final BluetoothDevice device, int rssi,
//                                     byte[] scanRecord) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            mLeDeviceListAdapter.addDevice(device);
//                            mLeDeviceListAdapter.notifyDataSetChanged();
//                        }
//                    });
//                }
//            };

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
                postData(0);
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
//        postToUI("BEGIN: readArduinoData...");
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            postToUI("ERROR: Found no drivers for Arduino.");
            return;
        }
//        postToUI("Found drivers...");

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            postToUI("ERROR: could not connect to Arduino.");
            return;
        }
//        postToUI("Found connection...");

        // Read some data! Most have just one port (port 0).
        List<UsbSerialPort> ports = driver.getPorts();
        UsbSerialPort port = ports.get(0); //FIXME: how do we know this is port 0?
        try {
            port.open(connection);
            port.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1,
                                UsbSerialPort.PARITY_NONE); //FIXME: how to pick args 2, 3, 4?

            int numBytesRead = port.read(buffer, 1000);
            int value = ByteBuffer.wrap(buffer).getInt(0);
            Log.d("Main Activity", "Read " + numBytesRead + " bytes.");
//            postToUI("Read " + numBytesRead + " bytes.");
            postToUI("Value: " + Arrays.toString(buffer));
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
            String lat = String.valueOf(lastKnownLocation.getLatitude());
            String lon = String.valueOf(lastKnownLocation.getLongitude());
            locationString = "(" + lat + ", " + lon + ")";
        } else {
            locationString = getString(R.string.field_error);
        }
    }

    public void postData(int _value) {
        httpError = false;
        final int value = _value;
        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                synchronized (this) {
                    try {
                        try {
                            notify();
                            // Create a new HttpClient and Post Header
                            HttpClient httpclient = new DefaultHttpClient();
                            HttpPost httppost = new HttpPost("<serverloc>/data"); //TODO: choose server location

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
                            nameValuePairs.add(new BasicNameValuePair("approach", ""+ value));
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

    private void postToUI(CharSequence _text) {
        final String text = "" + _text;
        findViewById(R.id.root_activity).post(new Runnable() {
            @Override
            public void run() {
                showToast(text);
            }
        });
    }

    /* Must be run on UI thread */
    private void showToast(CharSequence text) {
        if (text != null) {
            Context context = getApplicationContext();
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, text, duration).show();
        }
    }

//    private void initBlueTooth() {
//        // Initializes Bluetooth adapter.
//        final BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        mBluetoothAdapter = bluetoothManager.getAdapter();
//        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//    }
//
//    private void scanLeDevice(final boolean enable) {
//        if (enable) {
//            // Stops scanning after a pre-defined scan period.
//            mHandler.postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    mScanning = false;
//                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
//                }
//            }, SCAN_PERIOD);
//
//            mScanning = true;
//            mBluetoothAdapter.startLeScan(mLeScanCallback);
//        } else {
//            mScanning = false;
//            mBluetoothAdapter.stopLeScan(mLeScanCallback);
//        }
//    }

}
