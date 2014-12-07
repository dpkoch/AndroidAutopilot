package autopilot.opengl.Instruments;

import android.content.Context;
import android.opengl.Matrix;
import autopilot.opengl.CPDisplayStates;
import autopilot.opengl.Sphere;

public class AttitudeIndicator extends Instrument {

	private Sphere mSphere;
	
	public AttitudeIndicator(CPDisplayStates cpDS, Context context) {
		super(cpDS);
		mSphere = new Sphere(0.45f, context);
	}

	@Override
	public void draw(float[] mvpMatrix) {
		
		float[] rotMatrix = new float[16];
		float[] tranMatrix = new float[16];
		float[] tempMatrix = new float[16];
		float[] temp1Matrix = new float[16];
		float[] dialMatrix = new float[16];
		//float[] needleMatrix = new float[16];
		float[] attitudeAngles = mCpDS.attIndFaceAng(); //pitch, roll
		
		Matrix.setIdentityM(tranMatrix, 0);
		Matrix.translateM(tranMatrix, 0, 0, 0.5f, 0.0f);
        Matrix.setRotateM(rotMatrix, 0, attitudeAngles[1], 0, 1.0f, 0);
		Matrix.multiplyMM(tempMatrix, 0, tranMatrix, 0, rotMatrix, 0);
        Matrix.setRotateM(rotMatrix, 0, attitudeAngles[0], 1.0f, 0, 0);
        Matrix.multiplyMM(temp1Matrix, 0, tempMatrix, 0, rotMatrix, 0);
        Matrix.multiplyMM(dialMatrix, 0, mvpMatrix, 0, temp1Matrix, 0);
        
		mSphere.draw(dialMatrix);
	}

}
