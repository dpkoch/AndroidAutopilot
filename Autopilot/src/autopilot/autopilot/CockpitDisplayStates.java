package autopilot.autopilot;

public class CockpitDisplayStates {
	
	public interface Context {
		
		public float[] onRequestStates();  //roll, pitch, airspeed, heading, altitude
	}

	private Context context;
	private float[] state;
	private float FT_M = (float)3.28084;
	private float MAXSPEEDAng = (float) (2*Math.PI - Math.PI/6); //rad
	private float MAXDisplayedSPEED = 100; //knots
	private float MINSPEEDAng = (float) (Math.PI/6); //rad
	private float MINDisplayedSPEED = 5; //knots
	private float KNOTS_MpS = (float)0.514;
	private float COTurnAng = (float) (Math.PI/8); //rad
	

	public CockpitDisplayStates(Context context) {
		
		this.context = context;
	}
	
	// returns angles from straight and level (up and clockwise are positive rotations but face spins opposite)
	public void attIndFaceAng(float pitch, float roll) {
		
		this.state = context.onRequestStates();
		
		pitch = -state[0];
		roll = -state[1];
	}
	
	// returns angles from vertical (rad)
	public void altIndNeedleAng(float thousand, float unit) {
		
		float altitude = state[4] * FT_M;
		thousand = (float) (altitude / 10000 * 2 * Math.PI);
		unit = (float) ((altitude % 1000) / 1000 * 2 * Math.PI);
	}
	
	// returns angles from vertical between MINSPEEDAng to MAXSPEEDAng (rad)
	public float airSpeedIndNeedleAng() {
		
		float speedRange = MAXDisplayedSPEED - MINDisplayedSPEED;
		float speed = state[2] * KNOTS_MpS; //knots
		float angleRange = MAXSPEEDAng - MINSPEEDAng;
		return (float)(MINSPEEDAng + speed/(speedRange) * angleRange);
	}
	
	// returns angles from vertical (face rotates opposite of heading angle
	public float headingIndFaceAng() {
		
		return -state[3];
	}
	
	// returns angle from left horizontal (rad)
	public float vSpeedIndNeedleAng() {
		// todo: need to do a dirty derivative of altitude to get this
		return 0;
	}
	
	// returns angle from vertical (rad)
	public float coTurnIndNeedleAng() {
		// todo: there is an actual equation for this that relates the actual coordinated turn angle to airspeed
		float coordinatedTurnAngle = (float) (10 * (Math.PI / 180));
		float rollAngle = state[1];
		return rollAngle/coordinatedTurnAngle * COTurnAng;
	}
}
