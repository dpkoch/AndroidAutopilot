package autopilot.autopilot;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;
import autopilot.shared.Communicator;

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
	
	private ToggleButton graphsButton;
	
	//-----------------------------------------------------------------------
	// graphs
	//-----------------------------------------------------------------------
	
	private final Handler graphHandler = new Handler();
	private Runnable graphTimer;
	
	private LineGraphView northGraph;
	private GraphViewSeries northGraphSeries;
	
	private LineGraphView eastGraph;
	private GraphViewSeries eastGraphSeries;
	
	private LineGraphView courseGraph;
	private GraphViewSeries courseGraphSeries;
	
	private LineGraphView altitudeGraph;
	private GraphViewSeries altitudeGraphSeries;
	
	private LineGraphView rollGraph;
	private GraphViewSeries rollGraphSeries;
	
	private LineGraphView rollRateGraph;
	private GraphViewSeries rollRateGraphSeries;
	
	private LineGraphView pitchGraph;
	private GraphViewSeries pitchGraphSeries;
	
	private LineGraphView pitchRateGraph;
	private GraphViewSeries pitchRateGraphSeries;
	
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
	private double rollRateRaw = 0;
	private double rollRate = 0;
	
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
	private double altitude = 0; // m
	
	// oriantation
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
	
	// trims
	private double pitchTrim = 0;
	private double rollTrim = 0;
	private double altitudeTrim = 30; // m
	
	//-----------------------------------------------------------------------
	// PID loops
	//-----------------------------------------------------------------------
	
	private PID rollToRudder;
	private PID headingToRoll;
	private PID pitchToElevator;
	private PID altitudeToPitch;
	private PID airspeedToThrottle;
	
	private double tuningCommand = 0;
	private int currentlyTuning = 0;
	
	//-----------------------------------------------------------------------
	// sms receiver
	//-----------------------------------------------------------------------
	private String SMS_ACTION = android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
	
	//-----------------------------------------------------------------------
	// gps
	//-----------------------------------------------------------------------
	private LocationManager locationManager;
	private double homeLatitude;
	private double homeLongitude;
	private boolean homeSet;
	private float positionNorth = 0;
	private float positionEast = 0;
	private float waypointNorth = 0;
	private float waypointEast = 0;
	
	private double heading = 0;
	
	//=========================================================================
	// Android activity
	//=========================================================================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// pid loops
		rollToRudder = new PID(.9, 0, .1, 1);
		headingToRoll = new PID(1.0, 0, 0, Math.toRadians(25));
		pitchToElevator = new PID(5.5, 0, 0.2, 1);
		altitudeToPitch = new PID(1.0, 0, 0, Math.toRadians(30));
//		airspeedToThrottle = new PID(1.0, 0, 0, 1);
		
		// sms
		IntentFilter filter = new IntentFilter(SMS_ACTION);
		filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY - 1);
		registerReceiver(smsReceiver, filter);
		
		graphsButton = (ToggleButton) findViewById(R.id.graphs_button);
		graphsButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
				{
					startGraphs();
				}
				else
				{
					stopGraphs();
				}
			}
		});
		
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
		
        // graphs
        northGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        northGraph = new LineGraphView(this, "North");
        initGraph(northGraph, northGraphSeries, R.id.north_graph);
        
        eastGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        eastGraph = new LineGraphView(this, "East");
        initGraph(eastGraph, eastGraphSeries, R.id.east_graph);
        
        altitudeGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        altitudeGraph = new LineGraphView(this, "Altitude");
        initGraph(altitudeGraph, altitudeGraphSeries, R.id.altitude_graph);
        
        courseGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        courseGraph = new LineGraphView(this, "Course");
        initGraph(courseGraph, courseGraphSeries, R.id.course_graph);
        
        rollGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        rollGraph = new LineGraphView(this, "Roll");
        initGraph(rollGraph, rollGraphSeries, R.id.roll_graph);
        
        rollRateGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        rollRateGraph = new LineGraphView(this, "Roll Rate");
        initGraph(rollRateGraph, rollRateGraphSeries, R.id.roll_rate_graph);
        
        pitchGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        pitchGraph = new LineGraphView(this, "Pitch");
        initGraph(pitchGraph, pitchGraphSeries, R.id.pitch_graph);
        
        pitchRateGraphSeries = new GraphViewSeries(new GraphViewData[] {});
        pitchRateGraph = new LineGraphView(this, "Pitch Rate");
        initGraph(pitchRateGraph, pitchRateGraphSeries, R.id.pitch_rate_graph);
		
		// gps
		homeSet = false;
        if(currentlyTuning != 0 && currentlyTuning != 2) {
	        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
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
    	if (graphsButton.isChecked())
    	{
    		startGraphs();
    	}
    }
    
    @Override
    protected void onPause()
    {	
    	// sensors
    	sensorManager.unregisterListener(sensorListener);
    	
    	// graphs
    	stopGraphs();

    	super.onPause();
    }
	
	//=========================================================================
	// IOIO Board
	//=========================================================================
	
	/**
	 * Handles interface with IOIO board
	 */
	class Looper extends BaseIOIOLooper {
		
		private PwmOutput throttleOutput;
		private PwmOutput elevatorOutput;
		private PwmOutput rudderOutput;
		
		private double dT = .1;
		
		/**
		 * Called once each time a connection is made to the IOIO board 
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			//throttleOutput = ioio_.openPwmOutput(10, 100);
			elevatorOutput = ioio_.openPwmOutput(11, 100);
			rudderOutput = ioio_.openPwmOutput(10, 100);
		}
		
		/**
		 * Called continuously as long as a connection with the IOIO board exists
		 */
		@Override
		public void loop() throws ConnectionLostException, InterruptedException {

			double rollCommand;
			double pitchCommand;
			
			switch (currentlyTuning)
			{
			case 0:
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + tuningCommand, rollRawValue, dT, rollRate)));
				//elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + tuningCommand, pitch, dT, pitchRate)));
				break;
			case 1:
				rollCommand = headingToRoll.control(tuningCommand, yawRawValue, dT);
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + rollCommand, rollRawValue, dT, rollRate)));
				break;
			case 2:
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + tuningCommand, pitch, dT, pitchRate)));
				break;
			case 3:
				pitchCommand = altitudeToPitch.control(tuningCommand, altitudeTrim, dT);
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + pitchCommand, pitch, dT, pitchRate)));
				break;
			case 4:
				// throttleOutput.setPulseWidth(PID.toThrottleCommand(airspeedToThrottle.control(tuningCommand, airspeed, dT)));
				break;
			default:
				rollCommand = headingToRoll.control(heading, yawRawValue, dT);
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + rollCommand, rollRawValue, dT, rollRate)));

				pitchCommand = altitudeToPitch.control(altitude, altitudeTrim, dT);
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + pitchCommand, pitch, dT, pitchRate)));

				// throttleOutput.setPulseWidth(PID.toThrottleCommand(airspeedToThrottle.control(airspeedTrim, airspeed, dT)));
				break;
			}
			
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
							pitchToElevator.setGains(gains);
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
						double[] longiLati = new double[2];
						Communicator.getWaypoint(data, longiLati);
						float[] results = convertLLtoNE(longiLati[0], longiLati[1]);
						waypointNorth = results[0];
						waypointEast = results[1];
						Toast.makeText(getApplicationContext(), "waypoint received", Toast.LENGTH_LONG).show();
						break;
					case Communicator.TRIM_MSG_TYPE:
						trim();
						Toast.makeText(getApplicationContext(), "Trim requested", Toast.LENGTH_LONG).show();
						break;
					default:
						Toast.makeText(getApplicationContext(), "unknown message!", Toast.LENGTH_LONG).show();
						break;
					}
				}
			}
		}
	};
	
	//=========================================================================
	// trim
	//=========================================================================
	
	private void trim() {
		pitchTrim = pitchRawValue;
		rollTrim = rollRawValue;
		altitudeTrim = altitude;
	}
	
	//=========================================================================
	// gps
	//=========================================================================
	
	private LocationListener locationListener = new LocationListener()
    {
    	    	
    	public void onLocationChanged(Location location)
    	{
    		if(!homeSet) {
    			homeSet = true;
    			homeLatitude = location.getLatitude();
    			homeLongitude = location.getLongitude();
    			positionNorth = 0;
    			positionEast = 0;
    		}
    		else {
    			float[] results = convertLLtoNE(location.getLongitude(), location.getLatitude());
    			positionNorth = results[0];
    			positionEast = results[1];
    		}
    		
    		heading = Math.atan2(waypointEast-positionEast, waypointNorth-positionNorth);
    	}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}
    };
    
    private float[] convertLLtoNE(double longitude, double latitude) {
    	
    	float[] calc = new float[3];
    	float[] results = new float[2];
		Location.distanceBetween(homeLatitude, longitude, latitude, longitude, calc);
		if(latitude-homeLatitude < 0)
			calc[0] = -calc[0];
		results[0] = calc[0];
		Location.distanceBetween(latitude, homeLongitude, latitude, longitude, calc);
		if(longitude-homeLongitude < 0)
			calc[0] = -calc[0];
		results[1] = calc[0];
		return results;
    }
    
	//=========================================================================
    // graphing
	//=========================================================================
    
    private void initGraph(LineGraphView graph, GraphViewSeries series, int view)
    {
		graph.addSeries(series);
		graph.setViewPort(0, 30);
		graph.setScalable(true);
		graph.getGraphViewStyle().setNumHorizontalLabels(2);
		graph.getGraphViewStyle().setVerticalLabelsWidth(60);
		
		LinearLayout layout = (LinearLayout) findViewById(view);
		layout.addView(graph);
    }
    
    private void startGraphs()
    {
    	graphTimer = new Runnable() {
			
			private static final int max_count = 1000;
			
			@Override
			public void run() {
				double time = SystemClock.currentThreadTimeMillis() / 100;
				northGraphSeries.appendData(new GraphViewData(time, positionNorth), true, max_count);
				eastGraphSeries.appendData(new GraphViewData(time, positionEast), true, max_count);
				courseGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(yawRawValue)), true, max_count);
				altitudeGraphSeries.appendData(new GraphViewData(time, altitude), true, max_count);
				rollGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(rollRawValue)), true, max_count);
				rollRateGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(rollRate)), true, max_count);
				pitchGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(pitch)), true, max_count);
				pitchRateGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(pitchRate)), true, max_count);
				graphHandler.postDelayed(this, 100);
			}
		};
		graphHandler.post(graphTimer);
    }
    
    private void stopGraphs()
    {
    	graphHandler.removeCallbacks(graphTimer);
    }
}