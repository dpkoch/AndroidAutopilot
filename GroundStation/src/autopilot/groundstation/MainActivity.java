package autopilot.groundstation;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import autopilot.groundstation.R;
import autopilot.shared.*;

public class MainActivity extends Activity
{

	private EditText destination;
	private EditText command;
	private Button sendCmdButton;
	private EditText pGain;
	private EditText iGain;
	private EditText dGain;
	private Button sendGainsButton;
	private Button trimButton;
	private LocationManager locationManager;
	private double currentLatitude;
	private double currentLongitude;
	private boolean gpsRecieved;
	private Button sendWPButton;
	
	private SmsManager smsManager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		destination = (EditText) findViewById(R.id.destination);
		command = (EditText) findViewById(R.id.command);
		sendCmdButton = (Button) findViewById(R.id.send_cmd);
		sendCmdButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				sendCommand();
			}
		});
		
		pGain = (EditText) findViewById(R.id.p_edit);
		iGain = (EditText) findViewById(R.id.i_edit);
		dGain = (EditText) findViewById(R.id.d_edit);
		sendGainsButton = (Button) findViewById(R.id.send_gains);
		sendGainsButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				sendGains();
			}
		});
		trimButton = (Button) findViewById(R.id.trim);
		trimButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				trim();
			}
		});
		gpsRecieved = false;
		sendWPButton = (Button) findViewById(R.id.send_wayPoint);
		sendWPButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				sendWaypoint();				
			}
		});
		smsManager = SmsManager.getDefault();
		//locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	}
	
	private void sendCommand() {
		
		try
		{
			String text = Communicator.sendCommand(command.getText().toString());
			
			smsManager.sendTextMessage(destination.getText().toString(), null, text, null, null);
			Toast.makeText(getApplicationContext(), "Command sent!", Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), "Command failed, please try again", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void sendGains() {
		
		try 
		{
			String text = Communicator.sendGains(pGain.getText().toString(), iGain.getText().toString(), dGain.getText().toString());
			
			smsManager.sendTextMessage(destination.getText().toString(), null, text, null, null);
			Toast.makeText(getApplicationContext(), "Gains sent!", Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), "Gains failed to send, please try again", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void trim() {
		try 
		{
			String text = Communicator.sendTrim();
			
			smsManager.sendTextMessage(destination.getText().toString(), null, text, null, null);
			Toast.makeText(getApplicationContext(), "Trim sent!", Toast.LENGTH_LONG).show();	
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), "Trim failed, please try again", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void sendWaypoint() {
		try 
		{
			if(!gpsRecieved)
				Toast.makeText(getApplicationContext(), "Waypoint failed to send, no GPS signal", Toast.LENGTH_LONG).show();
			else {
				String text = Communicator.sendWaypoint(currentLongitude, currentLatitude);
			
				smsManager.sendTextMessage(destination.getText().toString(), null, text, null, null);
				Toast.makeText(getApplicationContext(), "Waypoint sent!", Toast.LENGTH_LONG).show();
			}	
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), "Waypoint failed to send, please try again", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private LocationListener locationListener = new LocationListener() {

		@Override
		public void onLocationChanged(Location location) {
			currentLatitude = location.getLatitude();
			currentLongitude = location.getLongitude();
			gpsRecieved = true;
		}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}
		
	};
}
