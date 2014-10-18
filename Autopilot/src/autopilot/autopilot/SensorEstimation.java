package autopilot.autopilot;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;

public class SensorEstimation {
	
	public interface Context {
		
		public void onLocationChanged();
	}
	
	private Context context;
	
	//-----------------------------------------------------------------------
	// sensors
	//-----------------------------------------------------------------------

	private SensorManager sensorManager;
	
	private Sensor gyro = null;
	private Sensor barometer = null;
	private Sensor accel = null;
	private Sensor magnetometer = null;
	
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
	double rollRateRaw = 0;
	double rollRate = 0;
	
	// pitch and pitch rate
	public double pitch = 0.0; // rad
	public double pitchRate = 0.0; // rad/s
	private double rawPitchRate = 0.0; // rad/s
	private double rawPitchIntegrated = 0.0; // rad
	private double rawPitchRotationSensor = 0.0; // rad
	
	// altitude
	private float barometer0 = SensorManager.PRESSURE_STANDARD_ATMOSPHERE;
	private float barometerRaw = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; // hPa
	private float barometerLPF = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; // hPa
	private double altitude0;
	public double altitude = 0; // m
	
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
	
	//-----------------------------------------------------------------------
	// gps
	//-----------------------------------------------------------------------
	
	private LocationManager locationManager;
	private double homeLatitude;
	private double homeLongitude;
	private boolean homeSet;
	public float positionNorth = 0;
	public float positionEast = 0;
	
	//----
	private int currentlyTuning;
	
	public SensorEstimation(Context context, SensorManager sm, LocationManager lm, int ct) {
		
		this.context = context;
		currentlyTuning = ct;
		
		// sensors
		sensorManager = sm;
		
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
        
	    // gps
		homeSet = false;
	    if(currentlyTuning != 0 && currentlyTuning != 2) {
	        locationManager = lm;
	        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	    }
	}
	
	public void registerListeners() {
		
		if (gyro != null)
    		sensorManager.registerListener(sensorListener, gyro, sensorSampleTimeUs);
    	
    	if (barometer != null)
    		sensorManager.registerListener(sensorListener, barometer, sensorSampleTimeUs);
    	
    	if (accel != null)
    		sensorManager.registerListener(sensorListener, accel, sensorSampleTimeUs);
    	
    	if (magnetometer != null)
    		sensorManager.registerListener(sensorListener, magnetometer, sensorSampleTimeUs);		
	}
	
	public void unregisterListeners() {
		sensorManager.unregisterListener(sensorListener);
	}
	
	
	//=========================================================================
	// sensor listeners
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
	// gps listener
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
    		
    		context.onLocationChanged();
    	}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {}

		@Override
		public void onProviderDisabled(String provider) {}

		@Override
		public void onProviderEnabled(String provider) {}
    };
    
    public float[] convertLLtoNE(double longitude, double latitude) {
    	
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
}
