package com.apprevelations.myparkingplace;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class FirstActivity extends ActionBarActivity implements OnClickListener, LocationListener{

	
	
	private Button park,gotoparked;
	private LocationManager lManager;
	private SharedPreferences spref;
	int flag=0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.firstxml);
		
		spref= getSharedPreferences("Location_pref", Context.MODE_PRIVATE);


		flag=0;
		Editor ed= spref.edit();
		ed.putInt("flag", flag);
		ed.commit();
		
		park=(Button) findViewById(R.id.bparkhere);
		gotoparked=(Button) findViewById(R.id.bgotolocation);
		park.setOnClickListener(this);
		gotoparked.setOnClickListener(this);

		
		
	}


	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId()) {
		case R.id.bgotolocation:
			Intent i= new Intent(FirstActivity.this, MainActivity.class);
			startActivity(i);
			break;

		case R.id.bparkhere:
			
					
			break;
		}
		
	}
	
	  private void updateLocation(Location location) {
	
	        
	        if (location != null) {
	            String lat = String.valueOf(location.getLatitude());
	            String lng = String.valueOf(location.getLongitude());
	            Editor editor = spref.edit();
	            editor.putString("Lat", lat);
	            editor.putString("Long", lng);
	            editor.commit();
	        } else {
	        }
	        
	    }
	
	  public void showDialog() {
			AlertDialog.Builder aDialog = new AlertDialog.Builder(
					FirstActivity.this);

			aDialog.setTitle("Network Locations SETTINGS");

			aDialog.setMessage("Network Location is not enabled! Want to go to settings menu?");

			aDialog.setPositiveButton("Settings",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							flag=1;
							Editor ed= spref.edit();
							ed.putInt("flag", flag).commit();
							Intent intent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							FirstActivity.this.startActivity(intent);
						}
					});

			aDialog.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});

			aDialog.show();
		}
	  

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		updateLocation(location);
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		updateLocation(null);
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
	

		@Override
		protected void onResume() {
			// TODO Auto-generated method stub
			super.onResume();
			
			Log.d("OnResume", "called");
			Log.d("OnResume flag", String.valueOf(flag));
			
			
			flag=spref.getInt("flag", 0);
			if(flag!=0)
			{
				if(lManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
	            {
					Log.d("OnResume location", "inside");
					
	            	lManager =(LocationManager)getSystemService(Context.LOCATION_SERVICE);
	                lManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L,
	                    500.0f, this);
	                Location network_location = lManager
	                        .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	                if (network_location != null) {
	                	String lat = String.valueOf(network_location.getLatitude());
	    	            String lng = String.valueOf(network_location.getLongitude());
	    	            Editor editor = spref.edit();
	    	            editor.putString("Lat", lat);
	    	            editor.putString("Long", lng);
	    	            editor.commit();
	                }
	            }
	            else
	            {
	              Toast.makeText(getBaseContext(), "Network Location Not enabled!!!!", Toast.LENGTH_LONG+Toast.LENGTH_LONG).show();
	              showDialog();
	            }
			}
			
		}
	    
	    
	
}
