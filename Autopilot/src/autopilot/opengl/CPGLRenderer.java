package autopilot.opengl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import autopilot.opengl.Instruments.AirSpeedIndicator;
import autopilot.opengl.Instruments.AltitudeIndicator;
import autopilot.opengl.Instruments.AttitudeIndicator;
import autopilot.opengl.Instruments.HeadingIndicator;
import autopilot.opengl.Instruments.Instrument;
import autopilot.opengl.Instruments.VSpeedIndicator;

public class CPGLRenderer implements GLSurfaceView.Renderer {

	Context mActivityContext;
	Triangle mTriangle;
	Panel mPanel;
	private Instrument mInstruments[];
	private CPDisplayStates mCpDS;
	
	private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];

    private final float[] mRotationMatrix = new float[16]; //<<----
    
    private float mAngle = 0;
    private float mPan = 0;
	
    public CPGLRenderer(final Context context) {
		mActivityContext = context;
	}
    
	public CPGLRenderer(final Context context, CPDisplayStates cpDS) {
		mActivityContext = context;
		mCpDS = cpDS;
	}
	
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		// initialize a triangle
	    mTriangle = new Triangle();
	    
	    mInstruments = new Instrument[6];
	    mInstruments[0] = new AirSpeedIndicator(mCpDS);
	    mInstruments[1] = new AttitudeIndicator(mCpDS, mActivityContext);
	    mInstruments[2] = new AltitudeIndicator(mCpDS);
	    mInstruments[4] = new HeadingIndicator(mCpDS, mActivityContext);
	    mInstruments[5] = new VSpeedIndicator(mCpDS);
	    
	    mPanel = new Panel(mActivityContext);
	}
	
	@Override
	public void onDrawFrame(GL10 unused) {
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		
		// Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        
        float[] trans = new float[16];
        Matrix.setIdentityM(trans, 0);
        Matrix.translateM(trans, 0, mPan, 0, 0);
        float[] scratch = new float[16];
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, trans, 0);
        mPanel.draw(scratch);
        mInstruments[0].draw(scratch);
        mInstruments[1].draw(scratch);
        mInstruments[2].draw(scratch);
        mInstruments[4].draw(scratch);
        mInstruments[5].draw(scratch);
        /*Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 1.0f, 0); //<<----
        float[] scratch = new float[16];
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, mRotationMatrix, 0);
        mPanel.draw(scratch);
        mInstruments[0].draw(scratch);
        mInstruments[1].draw(scratch);
        mInstruments[2].draw(scratch);
        mInstruments[4].draw(scratch);
        mInstruments[5].draw(scratch);
        
        mPanel.draw(mMVPMatrix);
        mInstruments[0].draw(mMVPMatrix);
        mInstruments[1].draw(mMVPMatrix);
        mInstruments[2].draw(mMVPMatrix);
        mInstruments[4].draw(mMVPMatrix);
        mInstruments[5].draw(mMVPMatrix);*/
		//mTriangle.draw();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);		
		
		float ratio = (float) width / height;

        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
	}
	
	public static int loadShader(int type, String shaderCode){

	    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
	    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
	    int shader = GLES20.glCreateShader(type);

	    // add the source code to the shader and compile it
	    GLES20.glShaderSource(shader, shaderCode);
	    GLES20.glCompileShader(shader);

	    return shader;
	}

	public float getAngle() {
        return mAngle;
    }

    public void setAngle(float angle) {
        mAngle = angle;
    }
    
    public float getPan() {
        return mPan;
    }

    private float MAXPAN = 0.4f;
    public void setPan(float pan) {
    	if(pan > MAXPAN)
    		mPan = MAXPAN;
    	else if(pan < -MAXPAN)
    		mPan = -MAXPAN;
    	else
    		mPan = pan;
    }
}
