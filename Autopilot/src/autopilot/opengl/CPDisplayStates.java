package autopilot.opengl;

public class CPDisplayStates {
	
	public interface Context {
		
		public float[] onRequestStates();  //pitch, roll, airspeed, heading, altitude
	}

	private Context context;
	private float[] state;
	private float FT_M = (float)3.28084;
	private float MAXSPEEDAng = (float) (2*Math.PI - Math.PI/12); //rad
	private float MAXDisplayedSPEED = 100; //knots
	private float MINSPEEDAng = (float) (Math.PI/9); //rad
	private float MINDisplayedSPEED = 5; //knots
	private float KNOTS_MpS = (float)0.514;
	private float COTurnAng = (float) (Math.PI/9); //rad
	// --- vSpeed dirty derivative ---
	private double mdT;
	private long mTime_d1;
	private float mAlt_d1;
	private float mAltDerivative = 0;
	private final float TAU = 2;
	private float altZero = 0;

	public CPDisplayStates(Context context) {
		mTime_d1 = System.currentTimeMillis();
		this.context = context;
	}
	
	// returns angles from straight and level (up and clockwise are positive rotations but face spins opposite)
	public float[] attIndFaceAng() {
		
		//this.state = context.onRequestStates();
		float angles[] = new float[2]; //pitch, roll
		angles[0] = (float) Math.toDegrees(state[0]);
		angles[1] = (float) -Math.toDegrees(state[1]);
		return angles;
	}
	
	// returns angles from vertical (deg) 
	public float[] altIndNeedleAng() {
		float angles[] = new float[2]; //unit, thousand
		float altitude = state[4] * FT_M;
		angles[1] = new Float(Math.toDegrees(altitude / 10000 * 2 * Math.PI));
		angles[0] = new Float(Math.toDegrees((altitude % 1000) / 1000 * 2 * Math.PI));
		return angles;
	}
	
	// returns angles from vertical between MINSPEEDAng to MAXSPEEDAng (deg)
	public float airSpeedIndNeedleAng() {
		this.state = context.onRequestStates();
		float speedRange = MAXDisplayedSPEED - MINDisplayedSPEED;
		float speed = state[2] * KNOTS_MpS; //knots
		float angleRange = MAXSPEEDAng - MINSPEEDAng;
		return (float)Math.toDegrees(MINSPEEDAng + speed/(speedRange) * angleRange);
	}
	
	// returns angles from vertical (face rotates opposite of heading angle
	public float headingIndFaceAng() {
		
		return (float) -Math.toDegrees(state[3]);
	}
	
	// returns angle from left horizontal (deg)
	public float vSpeedIndNeedleAng() {
		// todo: need to do a dirty derivative of altitude to get this
		long time = System.currentTimeMillis();
		mdT = (time - mTime_d1)/1000;
		mTime_d1 = time;
		float altitude = state[4] * FT_M;
		mAltDerivative = (float)((2*TAU-mdT)/(2*TAU+mdT)*mAltDerivative + (2/(2*TAU+mdT))*(altitude - mAlt_d1));
		mAlt_d1 = altitude;
		//return mAltDerivative*60f*90f/10f;
		//For demo show change in altitude instead of altitude rate
		if(altZero == 0)
			altZero = altitude;
		return (altitude - altZero)*90f/10f;
	}
	
	// returns angle from vertical (rad)
	public float coTurnIndNeedleAng() {
		// todo: there is an actual equation for this that relates the actual coordinated turn angle to airspeed
		float coordinatedTurnAngle = (float) (10 * (Math.PI / 180));
		float rollAngle = state[1];
		return rollAngle/coordinatedTurnAngle * COTurnAng;
	}
}
