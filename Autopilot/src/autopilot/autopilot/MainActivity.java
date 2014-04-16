package autopilot.autopilot;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;


public class MainActivity extends IOIOActivity {

	//=========================================================================
	// private data members
	//=========================================================================
	
	//-----------------------------------------------------------------------
	// sensors
	//-----------------------------------------------------------------------
	
	private SensorManager sensorManager;
	
	private Sensor gyro = null;
	private Sensor rotationSensor = null;
	private Sensor barometer = null;
	
	//-----------------------------------------------------------------------
	// UI elements
	//-----------------------------------------------------------------------
	
	private ToggleButton button;
	
	//-----------------------------------------------------------------------
	// graphs
	//-----------------------------------------------------------------------
	
	private final Handler graphHandler = new Handler();
	private Runnable graphTimer;
	
	private LineGraphView pitchGraph;
	private GraphViewSeries pitchGraphSeries;
	
	private LineGraphView pitchRateGraph;
	private GraphViewSeries pitchRateGraphSeries;
	
	private LineGraphView altitudeGraph;
	private GraphViewSeries altitudeGraphSeries;
	
	//-----------------------------------------------------------------------
	// tuning parameters
	//-----------------------------------------------------------------------
	
	// common
	private int sensorSampleTimeUs = 10000;
	private double sensorSampleTimeS = sensorSampleTimeUs / 1.0e6; 
	
	// pitch and pitch rate
	private double gyroLPFCuttoffFrequency = 2.0;
	private double alphaGyroLPF = Math.exp(-gyroLPFCuttoffFrequency * 2*Math.PI * sensorSampleTimeS);
	private double sigmaPitchFilter = 0.7;
	
	// altitude
	private float barometerLPFCuttoffFrequency = 1.0f;
	private float alphaBarometerLPF = (float) Math.exp(-barometerLPFCuttoffFrequency * 2*Math.PI * sensorSampleTimeS);
	
	//-----------------------------------------------------------------------
	// values
	//-----------------------------------------------------------------------
	
	// pitch and pitch rate
	private double pitch = 0.0; // rad
	private double pitchRate = 0.0; // rad/s
	private double rawPitchRate = 0.0; // rad/s
	private double rawPitchIntegrated = 0.0; // rad
	private double rawPitchRotationSensor = 0.0; // rad
	
	// altitude
	private float barometer0 = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
	private float barometerRaw = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; // hPa
	private float barometerLPF = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; // hPa
	private double altitude0;
	private double altitude; // m
	
	//-----------------------------------------------------------------------
	// sms receiver
	//-----------------------------------------------------------------------
	private String SMS_ACTION = android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
	
	//=========================================================================
	// Android activity
	//=========================================================================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// SMS
		IntentFilter filter = new IntentFilter(SMS_ACTION);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
		registerReceiver(smsReceiver, filter);
		
		button = (ToggleButton) findViewById(R.id.toggle_button);
		
		// sensors
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
        	gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
        	rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null)
		{
			barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			Log.i("Sensors", String.format("Barometer found: %s", barometer.getName()));
		}
		else
		{
			Log.w("Sensors", "Barometer not found!");
		}
		
		// pitch graph
		/*pitchGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
		pitchGraph = new LineGraphView(this, "Pitch");
		pitchGraph.addSeries(pitchGraphSeries);
		pitchGraph.setViewPort(0, 30);
		pitchGraph.setScalable(true);
		pitchGraph.getGraphViewStyle().setNumHorizontalLabels(2);
		pitchGraph.getGraphViewStyle().setVerticalLabelsWidth(60);
		
		LinearLayout pitchGraphLayout = (LinearLayout) findViewById(R.id.pitch_graph);
		pitchGraphLayout.addView(pitchGraph);
		
		// pitch rate graph
		pitchRateGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
		pitchRateGraph = new LineGraphView(this, "Pitch Rate");
		pitchRateGraph.addSeries(pitchRateGraphSeries);
		pitchRateGraph.setViewPort(0, 30);
		pitchRateGraph.setScalable(true);
		pitchRateGraph.getGraphViewStyle().setNumHorizontalLabels(2);
		pitchRateGraph.getGraphViewStyle().setVerticalLabelsWidth(60);
		
		LinearLayout pitchRateGraphLayout = (LinearLayout) findViewById(R.id.pitch_rate_graph);
		pitchRateGraphLayout.addView(pitchRateGraph);
		
		// altitude graph
		altitudeGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
		altitudeGraph = new LineGraphView(this, "Altitude");
		altitudeGraph.addSeries(altitudeGraphSeries);
		altitudeGraph.setViewPort(0, 30);
		altitudeGraph.setScalable(true);
		altitudeGraph.getGraphViewStyle().setNumHorizontalLabels(2);
		altitudeGraph.getGraphViewStyle().setVerticalLabelsWidth(60);

		LinearLayout altitudeGraphLayout = (LinearLayout) findViewById(R.id.altitude_graph);
		altitudeGraphLayout.addView(altitudeGraph);*/
	}
	
	@Override
    protected void onResume()
    {
    	super.onResume();
    	
    	// sensors
    	if (gyro != null)
    		sensorManager.registerListener(listener, gyro, sensorSampleTimeUs);
    	
    	if (rotationSensor != null)
    		sensorManager.registerListener(listener, rotationSensor, sensorSampleTimeUs);
    	
    	if (barometer != null)
    		sensorManager.registerListener(listener, barometer, sensorSampleTimeUs);
    	
    	// graphs
    	/*graphTimer = new Runnable() {
    		@Override
    		public void run() {
    			pitchGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, pitch * 180 / Math.PI), true, 1000);
    			pitchRateGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, pitchRate * 180 / Math.PI), true, 1000);
    			altitudeGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, altitude), true, 1000);
    			graphHandler.postDelayed(this, 100);
    		}
    	};
    	graphHandler.post(graphTimer);*/
    }
    
    @Override
    protected void onPause()
    {	
    	// sensors
    	//sensorManager.unregisterListener(listener);
    	
    	// graphs
    	graphHandler.removeCallbacks(graphTimer);

    	super.onPause();
    }
	
	//=========================================================================
	// IOIO Board
	//=========================================================================
	
	/**
	 * Handles interface with IOIO board
	 */
	class Looper extends BaseIOIOLooper {
		
		private DigitalOutput led;
		
		/**
		 * Called once each time a connection is made to the IOIO board 
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led = ioio_.openDigitalOutput(0, true);
		}
		
		/**
		 * Called continuously as long as a connection with the IOIO board exists
		 */
		@Override
		public void loop() throws ConnectionLostException {
			led.write(!button.isChecked());
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
		}
	}
	
	/**
	 * Returns an instance of our IOIOLooper implementation. Required for
	 * extending the IOIOActivity base class
	 */
	@Override
	protected IOIOLooper createIOIOLooper() {
		return new Looper();
	}
	
	//=========================================================================
	// listeners
	//=========================================================================

	private SensorEventListener listener = new SensorEventListener()
	{
		private long lastGyroTime = SystemClock.elapsedRealtime();
		
		private boolean barometerInitialized = false;
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor == gyro)
			{
				rawPitchRate = event.values[0];
				pitchRate = alphaGyroLPF*pitchRate + (1 - alphaGyroLPF)* rawPitchRate;
				
				long time = SystemClock.elapsedRealtime();
				double delta_t = (time - lastGyroTime) / 1.0e3;
				lastGyroTime = time;
				
				rawPitchIntegrated = rawPitchIntegrated + delta_t * rawPitchRate;
			}
			else if (event.sensor == rotationSensor)
			{
				float[] R = new float[9];
				SensorManager.getRotationMatrixFromVector(R, event.values);

				float values[] = new float[3];
				SensorManager.getOrientation(R, values);

				rawPitchRotationSensor = -values[1];
			}
			else if (event.sensor == barometer)
			{
				if (!barometerInitialized)
				{
					barometer0 = event.values[0];
					barometerLPF = event.values[0];
					
					altitude0 = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, barometer0);
					
					barometerInitialized = true;
				}
				
				barometerRaw = event.values[0];
				barometerLPF = alphaBarometerLPF*barometerLPF + (1 - alphaBarometerLPF)*barometerRaw;
				
				double currAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, barometerLPF);
				altitude = currAltitude - altitude0;
			}
			
			pitch = sigmaPitchFilter*rawPitchRotationSensor + (1-sigmaPitchFilter)*rawPitchIntegrated;
		}
	};
	
	
	private final BroadcastReceiver smsReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(SMS_ACTION))
			{
				Bundle pudsBundle = intent.getExtras();
				Object[] pdus = (Object[]) pudsBundle.get("pdus");
				SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[0]);
				String data = message.getMessageBody();

				if (data != null)
				{
					String[] lines = data.split("\n");

					if (lines[0].equals(context.getString(R.string.prefix)))
					{
						abortBroadcast();
						String body = data.substring(lines[0].length() + 1);
						displaySMSAlert(body);
					}
				}
			}
		}
	};
	
	private void displaySMSAlert(String data)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Message Intercepted!");
		builder.setMessage(data);

		AlertDialog dialog = builder.create();
		dialog.show();
	}
}
