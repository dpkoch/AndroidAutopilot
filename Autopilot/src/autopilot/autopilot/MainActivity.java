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
	
	private SensorEstimation senEst;
	private CockpitDisplayStates cpDisplayStates;
	
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
	// values
	//-----------------------------------------------------------------------
	
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
	// Commands
	//-----------------------------------------------------------------------

	private float waypointNorth = 0;
	private float waypointEast = 0;
	
	private double headingCommand = 0;
	
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
		
		senEst = new SensorEstimation(senEstContext, (SensorManager) getSystemService(Context.SENSOR_SERVICE),
												(LocationManager) this.getSystemService(Context.LOCATION_SERVICE),
												currentlyTuning);
		
		cpDisplayStates = new CockpitDisplayStates(cpDisplayStatesContext);
		
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
	
	}
	
	@Override
    protected void onResume()
    {
    	super.onResume();
    	
    	senEst.registerListeners();
    	
    	// graphs
    	if (graphsButton.isChecked())
    	{
    		startGraphs();
    	}
    }
    
    @Override
    protected void onPause()
    {
    	senEst.unregisterListeners();
    	
    	// graphs
    	stopGraphs();

    	super.onPause();
    }
    
    private SensorEstimation.Context senEstContext = new SensorEstimation.Context() {
    	
    	@Override
    	public void onLocationChanged() {
    		headingCommand = Math.atan2(waypointEast-senEst.positionEast, waypointNorth-senEst.positionNorth);
    	}
    };
    
    private CockpitDisplayStates.Context cpDisplayStatesContext = new CockpitDisplayStates.Context() {
		
		@Override
		public float[] onRequestStates() {
			float[] states = {(float)senEst.pitch, (float)senEst.rollRawValue, 10, (float)senEst.yawRawValue, (float)senEst.altitude};
			return states;
		}
	};
	
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
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + tuningCommand, senEst.rollRawValue, dT, senEst.rollRate)));
				//elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + tuningCommand, pitch, dT, pitchRate)));
				break;
			case 1:
				rollCommand = headingToRoll.control(tuningCommand, senEst.yawRawValue, dT);
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + rollCommand, senEst.rollRawValue, dT, senEst.rollRate)));
				break;
			case 2:
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + tuningCommand, senEst.pitch, dT, senEst.pitchRate)));
				break;
			case 3:
				pitchCommand = altitudeToPitch.control(tuningCommand, altitudeTrim, dT);
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + pitchCommand, senEst.pitch, dT, senEst.pitchRate)));
				break;
			case 4:
				// throttleOutput.setPulseWidth(PID.toThrottleCommand(airspeedToThrottle.control(tuningCommand, airspeed, dT)));
				break;
			default:
				rollCommand = headingToRoll.control(headingCommand, senEst.yawRawValue, dT);
				rudderOutput.setPulseWidth(PID.toServoCommand(rollToRudder.control(rollTrim + rollCommand, senEst.rollRawValue, dT, senEst.rollRate)));

				pitchCommand = altitudeToPitch.control(senEst.altitude, altitudeTrim, dT);
				elevatorOutput.setPulseWidth(PID.toServoCommand(-pitchToElevator.control(pitchTrim + pitchCommand, senEst.pitch, dT, senEst.pitchRate)));

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
						float[] results = senEst.convertLLtoNE(longiLati[0], longiLati[1]);
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
		pitchTrim = senEst.pitchRawValue;
		rollTrim = senEst.rollRawValue;
		altitudeTrim = senEst.altitude;
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
				northGraphSeries.appendData(new GraphViewData(time, senEst.positionNorth), true, max_count);
				eastGraphSeries.appendData(new GraphViewData(time, senEst.positionEast), true, max_count);
				courseGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(senEst.yawRawValue)), true, max_count);
				altitudeGraphSeries.appendData(new GraphViewData(time, senEst.altitude), true, max_count);
				rollGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(senEst.rollRawValue)), true, max_count);
				rollRateGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(senEst.rollRate)), true, max_count);
				pitchGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(senEst.pitch)), true, max_count);
				pitchRateGraphSeries.appendData(new GraphViewData(time, Math.toDegrees(senEst.pitchRate)), true, max_count);
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