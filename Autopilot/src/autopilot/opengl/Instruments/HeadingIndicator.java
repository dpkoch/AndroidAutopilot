package autopilot.opengl.Instruments;

import android.content.Context;
import android.opengl.Matrix;
import autopilot.opengl.CPDisplayStates;
import autopilot.opengl.Circle;

public class HeadingIndicator extends Instrument {

	private Circle mCircle;
	
	public HeadingIndicator(CPDisplayStates cpDS, Context context) {
		super(cpDS);
		mCircle = new Circle(0.425f, context);
	}

	@Override
	public void draw(float[] mvpMatrix) {
		
		float[] rotMatrix = new float[16];
		float[] tranMatrix = new float[16];
		float[] tempMatrix = new float[16];
		float[] dialMatrix = new float[16];
		float[] needleMatrix = new float[16];
		
		Matrix.setIdentityM(tranMatrix, 0);
		Matrix.translateM(tranMatrix, 0, 0, -0.5f, 0);
        Matrix.setRotateM(rotMatrix, 0, mCpDS.headingIndFaceAng(), 0, 0, 1.0f);
		Matrix.multiplyMM(tempMatrix, 0, tranMatrix, 0, rotMatrix, 0);
        Matrix.multiplyMM(dialMatrix, 0, mvpMatrix, 0, tempMatrix, 0);
        
		mCircle.draw(dialMatrix);
		
		super.needleCoords = new float[]{-0.0421539f,0f,0f,-0.25f,-0.035f,0f,-0.25f,0f,0f,0.25f,-0.035f,0f,0.0421539f,0f,0f,0.25f,0f,0f,0f,-0.175f,0f,-0.120349f,-0.200505f,0f,-0.120349f,-0.175324f,0f,0.120349f,-0.200505f,0f,0.120349f,-0.175324f,0f,-0.0421539f,0.12f,0f,-0.0297932f,-0.123042f,0f,0.0421539f,-0.085f,0f,0.0297932f,-0.123042f,0f,-0.0421539f,-0.085f,0f,0.0421539f,0.219863f,0f,-0.0421539f,0.219863f,0f,0.0316154f,0.257319f,0f,-0.0316154f,0.257319f,0f,0.0421539f,0.12f,0f,0f,0.28f,0f};
		super.drawOrder = new short[]{0,1,2,3,4,5,6,7,8,9,6,10,0,2,11,6,8,12,13,14,12,15,13,12,6,14,10,16,17,18,17,19,18,4,20,5,13,15,0,4,0,20,0,11,20,11,17,20,20,17,16,4,13,0,21,18,19,14,6,12};
		super.makeBuffers();
		Matrix.multiplyMM(needleMatrix, 0, mvpMatrix, 0, tranMatrix, 0);
		super.drawNeedle(needleMatrix);
	}

}
