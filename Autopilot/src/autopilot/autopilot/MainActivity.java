package autopilot.autopilot;

import autopilot.shared.*;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
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
import android.widget.Toast;
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
	private Sensor barometer = null;
	private Sensor accel = null;
	private Sensor magnetometer = null;
	
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
	
	// roll and roll rate
	private double rollRateRaw;
	private double rollRate;
	
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
	
	
	private boolean trustAccel = true;
	private float[] valuesAccelerometer;
	private float[] valuesMagneticField;
	
	double gyroXValue;
	double gyroYValue;
	double gyroZValue;
	
	double yawRawValue = 0;
	double pitchRawValue = 0;
	double rollRawValue = 0;
	
	private long time = 0;
	
	//-----------------------------------------------------------------------
	// PID loops
	//-----------------------------------------------------------------------
	
	private PID rollToRudder;
	private PID headingToRoll;
	private PID pitchToElivator;
	private PID altitudeToPitch;
	private PID airspeedToThrottle;
	
	private double tuningCommand = 0;
	private int currentlyTuning = 0;
	
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
		
		// pid loops
		rollToRudder = new PID(2.8, 0, 0, 1);
		/*headingToRoll = new PID(1.0, 0, 0);
		pitchToElivator = new PID(1.0, 0, 0);
		altitudeToPitch = new PID(1.0, 0, 0);
		airspeedToThrottle = new PID(1.0, 0, 0);*/
		
		// sms
		IntentFilter filter = new IntentFilter(SMS_ACTION);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
		registerReceiver(smsReceiver, filter);
		
		button = (ToggleButton) findViewById(R.id.toggle_button);
		
		// sensors
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
		{
        	gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        	Log.i("Sensors", String.format("Gyroscope found: %s", gyro.getName()));
		}
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null)
		{
			barometer = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
			Log.i("Sensors", String.format("Barometer found: %s", barometer.getName()));
		}
		else
		{
			Log.w("Sensors", "Barometer not found!");
		}
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
        	accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		
        if (sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
        	magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
        valuesAccelerometer = new float[3];
        valuesMagneticField = new float[3];
		
		// pitch graph
		pitchGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
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
		/*altitudeGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
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
    		sensorManager.registerListener(sensorListener, gyro, sensorSampleTimeUs);
    	
    	if (barometer != null)
    		sensorManager.registerListener(sensorListener, barometer, sensorSampleTimeUs);
    	
    	if (accel != null)
    		sensorManager.registerListener(sensorListener, accel, sensorSampleTimeUs);
    	
    	if (magnetometer != null)
    		sensorManager.registerListener(sensorListener, magnetometer, sensorSampleTimeUs);
    	
    	// graphs
    	graphTimer = new Runnable() {
    		@Override
    		public void run() {
    			pitchGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, Math.toDegrees(rollRawValue)), true, 1000);
    			pitchRateGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, Math.toDegrees(rollRate)), true, 1000);
//    			altitudeGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, altitude), true, 1000);
    			graphHandler.postDelayed(this, 100);
    		}
    	};
    	graphHandler.post(graphTimer);
    }
    
    @Override
    protected void onPause()
    {	
    	// sensors
    	sensorManager.unregisterListener(sensorListener);
    	
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
		private PwmOutput rudderOutput;
		private double dT = .1;
		
		/**
		 * Called once each time a connection is made to the IOIO board 
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			led = ioio_.openDigitalOutput(0, true);
			rudderOutput = ioio_.openPwmOutput(12, 100);
		}
		
		/**
		 * Called continuously as long as a connection with the IOIO board exists
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {
			led.write(!button.isChecked());
//			rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(tuningCommand, rollRawValue, dT)));
			rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(tuningCommand, rollRawValue, dT, rollRate)));
			Thread.sleep((long)(dT*1000));
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

	private SensorEventListener sensorListener = new SensorEventListener()
	{
		private long lastGyroTime = SystemClock.elapsedRealtime();
		
		private boolean barometerInitialized = false;
		
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor == gyro)
			{
				Log.d("Sensors", "Gyro event");
				rollRateRaw = event.values[1];
				rollRate = alphaGyroLPF*rollRate + (1 - alphaGyroLPF)*rollRateRaw;
				
				rawPitchRate = event.values[0];
				pitchRate = alphaGyroLPF*pitchRate + (1 - alphaGyroLPF)*rawPitchRate;
				
				long time = SystemClock.elapsedRealtime();
				double delta_t = (time - lastGyroTime) / 1.0e3;
				lastGyroTime = time;
				
				rawPitchIntegrated = rawPitchIntegrated + delta_t * rawPitchRate;
				
				gyroXValue = event.values[0];
				gyroYValue = event.values[1];
				gyroZValue = event.values[2];
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
			else if (event.sensor == accel)
			{
				double total = Math.sqrt(Math.pow(event.values[0], 2.0) + Math.pow(event.values[1], 2.0) + Math.pow(event.values[2], 2.0));
				
				for(int i =0; i < 3; i++)
				    valuesAccelerometer[i] = event.values[i];
				
				if(total > 9.5 && total < 10.2)
					trustAccel = true;
				else
					trustAccel = false;
			}
			else if (event.sensor == magnetometer)
			{
				for(int i =0; i < 3; i++)
				    valuesMagneticField[i] = event.values[i];
								
				updateOrientation();
			}
			
			pitch = sigmaPitchFilter*rawPitchRotationSensor + (1-sigmaPitchFilter)*rawPitchIntegrated;
		}
	};
	
	private void updateOrientation() {
		
		double Ts = (SystemClock.currentThreadTimeMillis() - time)/1000.0;
		time = SystemClock.currentThreadTimeMillis();
		
		if(trustAccel) {
			float[] matrixR = new float[9];
		    float[] matrixI = new float[9];
		    
			boolean success = SensorManager.getRotationMatrix(
					matrixR,
					matrixI,
					valuesAccelerometer,
					valuesMagneticField);
			
			if(success){
				float[] matrixValues = new float[3];
				SensorManager.getOrientation(matrixR, matrixValues);
	     
				yawRawValue = matrixValues[0];
				pitchRawValue = matrixValues[1];
				rollRawValue = matrixValues[2];
			}
		}
		else {
			yawRawValue = yawRawValue + gyroZValue*Ts;
			rollRawValue = rollRawValue + gyroYValue*Ts;//Integrate the gyros instead
			pitchRawValue = pitchRawValue + gyroXValue*Ts;
		}	
	}
	
	//=========================================================================
	// SMS receiver
	//=========================================================================
	
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

				if (Communicator.isAutopilotMSG(data))
				{
					int type = Communicator.getMsgType(data);
					abortBroadcast();
					
					switch(type) {
					case Communicator.COMMAND_MSG_TYPE:
						tuningCommand = Communicator.getCommand(data);
						Toast.makeText(getApplicationContext(), "command received", Toast.LENGTH_LONG).show();
						break;
					case Communicator.GAINS_MSG_TYPE:
						
						double[] gains = {0,0,0};
						Communicator.getGains(data, gains);
						
						switch(currentlyTuning) {
						case(0):
							rollToRudder.setGains(gains);
							break;
						case(1):
							headingToRoll.setGains(gains);
							break;
						case(2):
							pitchToElivator.setGains(gains);
							break;
						case(3):
							altitudeToPitch.setGains(gains);
							break;
						case(4):
							airspeedToThrottle.setGains(gains);
							break;
						default:
							break;
						}
												
						Toast.makeText(getApplicationContext(), "gains received", Toast.LENGTH_LONG).show();
						break;
					case Communicator.WAYPOINT_MSG_TYPE:
						Toast.makeText(getApplicationContext(), "waypoint received", Toast.LENGTH_LONG).show();
						break;
					default:
						Toast.makeText(getApplicationContext(), "unknown message!", Toast.LENGTH_LONG).show();
						break;
					}
				}
			}
		}
	};
}
