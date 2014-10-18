package com.apprevelations.myparkingplace;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MainActivity extends ActionBarActivity implements
		LocationListener,
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	
    private LocationRequest mLocationRequest;
    private LocationClient mLocationClient;
    SharedPreferences mPrefs;
    SharedPreferences.Editor mEditor;
    boolean mUpdatesRequested = false;
    
	private GoogleMap mGoogleMap;
	private SharedPreferences spref;
	private double lat, latitude;
	private double lng, longitude;
	int flag = 0;

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    
		spref = getSharedPreferences("Location_pref", Context.MODE_PRIVATE);

		flag = 0;
		Editor ed = spref.edit();
		ed.putInt("flag", flag);
		ed.commit();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        mUpdatesRequested = false;
        mPrefs = getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);
        mEditor = mPrefs.edit();
        mLocationClient = new LocationClient(this, this, this);

        getLocation();
        
        
		mGoogleMap = ((SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map)).getMap();

		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat,
				lng), 15));
		MarkerOptions marker = new MarkerOptions()
				.position(new LatLng(lat, lng)).title("You are Here")
				.draggable(false);

		mGoogleMap.addMarker(marker);
   
    }
    

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		super.onOptionsItemSelected(item);

		switch (item.getItemId()) {

		case R.id.action_park:
			// code for parked here

			if (isNetworkAvailable()) {
				getLocation();
					String slat = String.valueOf(lat);
					String slng = String
							.valueOf(lng);
					Editor editor = spref.edit();
					editor.putString("Lat", slat);
					editor.putString("Long", slng);
					editor.commit();
					Toast.makeText(getApplicationContext(),
							"Your Vehicle has been Parked", Toast.LENGTH_SHORT)
							.show();
				
			} else
				Toast.makeText(
						getApplicationContext(),
						"No Internet Connection!!\n Please Connect to Internet",
						Toast.LENGTH_SHORT).show();

			break;

		case R.id.action_parked:
			// code for go to parked place

			latitude = Double.parseDouble(spref.getString("Lat", "00.000"));
			longitude = Double.parseDouble(spref.getString("Long", "00.000"));

			if (latitude != 00.000 && longitude != 00.000) {

				mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
						new LatLng(latitude, longitude), 15));
				MarkerOptions marker = new MarkerOptions()
						.position(new LatLng(latitude, longitude))
						.title("Your Vehicle is Here").draggable(false);

				mGoogleMap.addMarker(marker);

				LatLng origin = new LatLng(lat, lng);
				LatLng dest = new LatLng(latitude, longitude);

				if (isNetworkAvailable()) {
					String url = getDirectionsUrl(origin, dest);

					DownloadTask downloadTask = new DownloadTask();

					// Start downloading json data from Google Directions API
					downloadTask.execute(url);

				} else {
					Toast.makeText(
							getApplicationContext(),
							"NO Internet Connection!!!\n Directions to Loaction cannot be shown",
							Toast.LENGTH_SHORT).show();
				}

			} else
				Toast.makeText(getApplicationContext(),
						"No vehicle Parked yet!!", Toast.LENGTH_SHORT).show();

			break;

		}
		return true;
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}


	
	private String getDirectionsUrl(LatLng origin, LatLng dest) {

		// Origin of route
		String str_origin = "origin=" + origin.latitude + ","
				+ origin.longitude;

		// Destination of route
		String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

		// Sensor enabled
		String sensor = "sensor=false";

		// Building the parameters to the web service
		String parameters = str_origin + "&" + str_dest + "&" + sensor;

		// Output format
		String output = "json";

		// Building the url to the web service
		String url = "https://maps.googleapis.com/maps/api/directions/"
				+ output + "?" + parameters;

		return url;
	}

	/** A method to download json data from url */
	private String downloadUrl(String strUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(strUrl);

			// Creating an http connection to communicate with url
			urlConnection = (HttpURLConnection) url.openConnection();

			// Connecting to url
			urlConnection.connect();

			// Reading data from url
			iStream = urlConnection.getInputStream();

			BufferedReader br = new BufferedReader(new InputStreamReader(
					iStream));

			StringBuffer sb = new StringBuffer();

			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

			data = sb.toString();

			br.close();

		} catch (Exception e) {
			Log.d("Exception while downloading url", e.toString());
		} finally {
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}

	// Fetches data from url passed
	private class DownloadTask extends AsyncTask<String, Void, String> {

		// Downloading data in non-ui thread
		@Override
		protected String doInBackground(String... url) {

			// For storing data from web service
			String data = "";

			try {
				// Fetching the data from web service
				data = downloadUrl(url[0]);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		// Executes in UI thread, after the execution of
		// doInBackground()
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			ParserTask parserTask = new ParserTask();

			// Invokes the thread for parsing the JSON data
			parserTask.execute(result);

		}
	}

	/** A class to parse the Google Places in JSON format */
	private class ParserTask extends
			AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

		// Parsing the data in non-ui thread
		@Override
		protected List<List<HashMap<String, String>>> doInBackground(
				String... jsonData) {

			JSONObject jObject;
			List<List<HashMap<String, String>>> routes = null;

			try {
				jObject = new JSONObject(jsonData[0]);
				DirectionsJSONParser parser = new DirectionsJSONParser();

				// Starts parsing data
				routes = parser.parse(jObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return routes;
		}

		// Executes in UI thread, after the parsing process
		@Override
		protected void onPostExecute(List<List<HashMap<String, String>>> result) {
			ArrayList<LatLng> points = null;
			PolylineOptions lineOptions = null;

			// Traversing through all the routes
			for (int i = 0; i < result.size(); i++) {
				points = new ArrayList<LatLng>();
				lineOptions = new PolylineOptions();

				// Fetching i-th route
				List<HashMap<String, String>> path = result.get(i);

				// Fetching all the points in i-th route
				for (int j = 0; j < path.size(); j++) {
					HashMap<String, String> point = path.get(j);

					double lat = Double.parseDouble(point.get("lat"));
					double lng = Double.parseDouble(point.get("lng"));
					LatLng position = new LatLng(lat, lng);

					points.add(position);
				}

				// Adding all the points in the route to LineOptions
				lineOptions.addAll(points);
				lineOptions.width(2);
				lineOptions.color(Color.RED);

			}

			// Drawing polyline in the Google Map for the i-th route
			mGoogleMap.addPolyline(lineOptions);
		}
	}

	
	
    
    
    @Override
    public void onStop() {

        if (mLocationClient.isConnected()) {
            stopPeriodicUpdates();
        }
        mLocationClient.disconnect();

        super.onStop();
    }

    @Override
    public void onPause() {

        mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, mUpdatesRequested);
        mEditor.commit();

        super.onPause();
    }


    @Override
    public void onStart() {

        super.onStart();
        mLocationClient.connect();

    }

    @Override
    public void onResume() {
        super.onResume();

        if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
        } else {
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            mEditor.commit();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        switch (requestCode) {

            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    case Activity.RESULT_OK:

                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        Toast.makeText(getApplicationContext(), R.string.connected, Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), R.string.resolved, Toast.LENGTH_SHORT).show();
                        
                    break;
                    default:
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        Toast.makeText(getApplicationContext(), R.string.disconnected, Toast.LENGTH_SHORT).show();
                        Toast.makeText(getApplicationContext(), R.string.no_resolution, Toast.LENGTH_SHORT).show();

                    break;
                }

            default:
               Log.d(LocationUtils.APPTAG,
                       getString(R.string.unknown_activity_request_code, requestCode));

               break;
        }
    }

    private boolean servicesConnected() {

        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));
            return true;
        } else {
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }

    public void getLocation() {

        if (servicesConnected()) {

            Location currentLocation = mLocationClient.getLastLocation();

            
         lat= LocationUtils.getLat(this, currentLocation);
        lng= LocationUtils.getLng(this, currentLocation);
        }
    }
    public void startUpdates(View v) {
        mUpdatesRequested = true;

        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    public void stopUpdates(View v) {
        mUpdatesRequested = false;

        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
    	Toast.makeText(getApplicationContext(), R.string.connected, Toast.LENGTH_SHORT).show();

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    @Override
    public void onDisconnected() {
    	Toast.makeText(getApplicationContext(), R.string.disconnected, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        if (connectionResult.hasResolution()) {
            try {

                connectionResult.startResolutionForResult(
                        this,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {

                e.printStackTrace();
            }
        } else {

            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
	Toast.makeText(getApplicationContext(), R.string.location_updated, Toast.LENGTH_SHORT).show();

    	
    	lat= LocationUtils.getLat(this, location);
    	lng= LocationUtils.getLng(this, location);
    	
    }
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        
        Toast.makeText(getApplicationContext(), R.string.location_requested, Toast.LENGTH_SHORT).show();
        
    }

    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
        
        Toast.makeText(getApplicationContext(), R.string.location_updates_stopped, Toast.LENGTH_SHORT).show();
        
    }

    private void showErrorDialog(int errorCode) {

        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            this,
            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        if (errorDialog != null) {

            ErrorDialogFragment errorFragment = new ErrorDialogFragment();
            errorFragment.setDialog(errorDialog);
            errorFragment.show(getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {

        private Dialog mDialog;
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }
    
    
    protected class GetAddressTask extends AsyncTask<Location, Void, String> {

        // Store the context passed to the AsyncTask when the system instantiates it.
        Context localContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {

            // Required by the semantics of AsyncTask
            super();

            // Set a Context for the background task
            localContext = context;
        }

        /**
         * Get a geocoding service instance, pass latitude and longitude to it, format the returned
         * address, and return the address to the UI thread.
         */
        @Override
        protected String doInBackground(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
            Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

            // Get the current location from the input parameter list
            Location location = params[0];

            // Create a list to contain the result address
            List <Address> addresses = null;

            // Try to get an address for the current location. Catch IO or network problems.
            try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
                addresses = geocoder.getFromLocation(location.getLatitude(),
                    location.getLongitude(), 1
                );

                // Catch network or other I/O problems.
                } catch (IOException exception1) {

                    // Log an error and return an error message
                    Log.e(LocationUtils.APPTAG, getString(R.string.IO_Exception_getFromLocation));

                    // print the stack trace
                    exception1.printStackTrace();

                    // Return an error message
                    return (getString(R.string.IO_Exception_getFromLocation));

                // Catch incorrect latitude or longitude values
                } catch (IllegalArgumentException exception2) {

                    // Construct a message containing the invalid arguments
                    String errorString = getString(
                            R.string.illegal_argument_exception,
                            location.getLatitude(),
                            location.getLongitude()
                    );
                    // Log the error and print the stack trace
                    Log.e(LocationUtils.APPTAG, errorString);
                    exception2.printStackTrace();

                    //
                    return errorString;
                }
                // If the reverse geocode returned an address
                if (addresses != null && addresses.size() > 0) {

                    // Get the first address
                    Address address = addresses.get(0);

                    // Format the first line of address
                    String addressText = getString(R.string.address_output_string,

                            // If there's a street address, add it
                            address.getMaxAddressLineIndex() > 0 ?
                                    address.getAddressLine(0) : "",

                            // Locality is usually a city
                            address.getLocality(),

                            // The country of the address
                            address.getCountryName()
                    );

                    // Return the text
                    return addressText;

                // If there aren't any addresses, post a message
                } else {
                  return getString(R.string.no_address_found);
                }
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(String address) {

            // Turn off the progress bar
/*-------------------------------------------------------------------------------------
            mActivityIndicator.setVisibility(View.GONE);
--------------------------------------------------------------------------------------*/
        	
        	Toast.makeText(getApplicationContext(), "GONE", Toast.LENGTH_SHORT).show();

            // Set the address in the UI
/*-------------------------------------------------------------------------------------
            mAddress.setText(address);
--------------------------------------------------------------------------------------*/
        	
        	//longitude.setText(address);
        	
        }
    }


    
    
}
