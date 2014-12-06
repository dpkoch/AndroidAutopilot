package autopilot.opengl.Instruments;

import android.opengl.Matrix;
import autopilot.opengl.CPDisplayStates;
import autopilot.opengl.Circle;

public class HeadingIndicator extends Instrument {

	private Circle mCircle;
	
	public HeadingIndicator(CPDisplayStates cpDS) {
		super(cpDS);
		mCircle = new Circle(0.425f);
	}

	@Override
	public void draw(float[] mvpMatrix) {
		
		float[] rotMatrix = new float[16];
		float[] tranMatrix = new float[16];
		float[] tempMatrix = new float[16];
		float[] dialMatrix = new float[16];
		
		Matrix.setIdentityM(tranMatrix, 0);
		Matrix.translateM(tranMatrix, 0, 0, -0.5f, 0);
        Matrix.setRotateM(rotMatrix, 0, mCpDS.headingIndFaceAng(), 0, 0, 1.0f);
		Matrix.multiplyMM(tempMatrix, 0, tranMatrix, 0, rotMatrix, 0);
        Matrix.multiplyMM(dialMatrix, 0, mvpMatrix, 0, tempMatrix, 0);
        
		mCircle.draw(dialMatrix);
	}

}
