package autopilot.groundstation;

import android.app.Activity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import autopilot.groundstation.R;

public class MainActivity extends Activity
{

	private EditText destination;
	private EditText command;
	private Button sendCmdButton;
	private EditText pGain;
	private EditText iGain;
	private EditText dGain;
	private Button sendGainsButton;
	private Button sendWPButton;
	
	private SmsManager manager;
	
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
		
		sendWPButton = (Button) findViewById(R.id.send_wayPoint);
		sendWPButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				sendWaypoint();				
			}
		});
		manager = SmsManager.getDefault();
	}
	
	private void sendCommand() {
		
		try
		{
			String text = command.getText().toString();
			text = getString(R.string.prefix) + "\n" + text;
			
			manager.sendTextMessage(destination.getText().toString(), null, text, null, null);
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
			String text = getString(R.string.prefix) + "\n" + pGain.getText().toString() + "," +
															  iGain.getText().toString() + "," +
															  dGain.getText().toString();
			
			manager.sendTextMessage(destination.getText().toString(), null, text, null, null);
			Toast.makeText(getApplicationContext(), "Gains sent!", Toast.LENGTH_LONG).show();
		}
		catch (Exception e)
		{
			Toast.makeText(getApplicationContext(), "Gains failed to send, please try again", Toast.LENGTH_LONG).show();
			e.printStackTrace();
		}
	}
	
	private void sendWaypoint() {
		
	}
}
