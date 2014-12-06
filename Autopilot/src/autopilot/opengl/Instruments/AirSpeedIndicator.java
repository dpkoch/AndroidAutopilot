package autopilot.opengl.Instruments;

import android.opengl.Matrix;
import autopilot.opengl.CPDisplayStates;

public class AirSpeedIndicator extends Instrument {

	public AirSpeedIndicator(CPDisplayStates cpDS) {
		super(cpDS);
	}

	@Override
	public void draw(float[] mvpMatrix) {

		float[] rotMatrix = new float[16];
		float[] tranMatrix = new float[16];
		float[] tempMatrix = new float[16];
		float[] needleMatrix = new float[16];
		
		Matrix.setIdentityM(tranMatrix, 0);
		Matrix.translateM(tranMatrix, 0, 1.0f, 0.5f, 0);
        Matrix.setRotateM(rotMatrix, 0, mCpDS.airSpeedIndNeedleAng(), 0, 0, 1.0f);
		Matrix.multiplyMM(tempMatrix, 0, tranMatrix, 0, rotMatrix, 0);
        Matrix.multiplyMM(needleMatrix, 0, mvpMatrix, 0, tempMatrix, 0);

		super.drawNeedle(needleMatrix);
	}
}
