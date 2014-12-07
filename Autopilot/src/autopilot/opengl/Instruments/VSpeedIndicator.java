package autopilot.opengl.Instruments;

import android.opengl.Matrix;
import autopilot.opengl.CPDisplayStates;

public class VSpeedIndicator extends Instrument {

	public VSpeedIndicator(CPDisplayStates cpDS) {
		super(cpDS);
	}

	@Override
	public void draw(float[] mvpMatrix) {
		
		float[] rotMatrix = new float[16];
		float[] tranMatrix = new float[16];
		float[] tempMatrix = new float[16];
		float[] needleMatrix = new float[16];
		
		super.needleCoords = new float[]{
				 0.25f,  0.0f, 0.0f,     // point
	             0.235f, 0.015f, 0.0f,   // top point
	             0.235f,-0.015f, 0.0f,   // bottom point
	            -0.015f, 0.015f, 0.0f,   // top blunt
	            -0.015f,-0.015f, 0.0f }; // bottom blunt
		super.drawOrder = new short[]{0,1,2,1,2,3,2,3,4};
		super.makeBuffers();
		
		Matrix.setIdentityM(tranMatrix, 0);
		Matrix.translateM(tranMatrix, 0, -1.0f, -0.5f, 0);
        Matrix.setRotateM(rotMatrix, 0, mCpDS.vSpeedIndNeedleAng(), 0, 0, 1.0f);
		Matrix.multiplyMM(tempMatrix, 0, tranMatrix, 0, rotMatrix, 0);
		Matrix.multiplyMM(needleMatrix, 0, mvpMatrix, 0, tempMatrix, 0);
		super.drawNeedle(needleMatrix);
		
	}

}
