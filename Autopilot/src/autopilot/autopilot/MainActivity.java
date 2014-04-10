package autopilot.autopilot;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOActivity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.LineGraphView;


public class MainActivity extends IOIOActivity {

	//=========================================================================
	// private data members
	//=========================================================================
	
	// sensors
	private SensorManager sensorManager;
	private Sensor gyro = null;
	private Sensor rotationSensor = null;
	
	// UI elements
	private ToggleButton button;
	
	// graphs
	private final Handler graphHandler = new Handler();
	private Runnable graphTimer;
	
	private LineGraphView pitchGraph;
	private GraphViewSeries pitchGraphSeries;
	
	// tuning parameters
	private double Ts = 0.01; // (estimate)
	private double alphaGyro = Math.exp(-5 * 2*Math.PI * Ts);
	private double sigmaPitchFilter = 0.5;
	
	// values
	private double pitch;
	private double pitchRate;
	private double rawPitchRate;
	private double rawIntegratedPitch;
	private double rawRotationSensorPitch;
	
	//=========================================================================
	// Android activity
	//=========================================================================
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		button = (ToggleButton) findViewById(R.id.toggle_button);
		
		// sensors
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null)
        	gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		
		if (sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null)
        	rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		
		// graphs
		pitchGraphSeries = new GraphViewSeries(new GraphViewData[] {});;
		pitchGraph = new LineGraphView(this, "Pitch");
		pitchGraph.addSeries(pitchGraphSeries);
		pitchGraph.setViewPort(0, 30);
		pitchGraph.setScalable(true);
		pitchGraph.getGraphViewStyle().setNumHorizontalLabels(2);
		pitchGraph.getGraphViewStyle().setVerticalLabelsWidth(30);
		
		LinearLayout pitchGraphLayout = (LinearLayout) findViewById(R.id.pitch_graph);
		pitchGraphLayout.addView(pitchGraph);
	}
	
	@Override
    protected void onResume()
    {
    	super.onResume();
    	
    	// sensors
    	if (gyro != null)
    		sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST);
    	
    	if (rotationSensor != null)
    		sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_FASTEST);
    	
    	// graphs
    	graphTimer = new Runnable() {
    		@Override
    		public void run() {
    			pitchGraphSeries.appendData(new GraphViewData(SystemClock.currentThreadTimeMillis() / 100, pitch * 180 / Math.PI), true, 1000);
    			graphHandler.postDelayed(this, 100);
    		}
    	};
    	graphHandler.post(graphTimer);
    }
    
    @Override
    protected void onPause()
    {	
    	// sensors
    	sensorManager.unregisterListener(listener);
    	
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
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {}

		@Override
		public void onSensorChanged(SensorEvent event) {

			if (event.sensor == gyro)
			{
				// TODO process
			}
			else if (event.sensor == rotationSensor)
			{
				float[] R = new float[9];
				SensorManager.getRotationMatrixFromVector(R, event.values);

				float values[] = new float[3];
				SensorManager.getOrientation(R, values);

				pitch = -values[1];
			}
		}
	};
}
