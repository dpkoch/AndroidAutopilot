package autopilot.opengl;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

public class CPGLRenderer implements GLSurfaceView.Renderer {

	Context mActivityContext;
	Triangle mTriangle;
	Square mSquare;
	Panel mPanel;
	
	public CPGLRenderer(final Context context) {
		mActivityContext = context;
	}
	
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		
		// initialize a triangle
	    mTriangle = new Triangle();
	    // initialize a square
	    mSquare = new Square();
	    
	    mPanel = new Panel(mActivityContext);
	}
	
	@Override
	public void onDrawFrame(GL10 unused) {
		
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
		//mTriangle.draw();
		mPanel.draw();
	}

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		GLES20.glViewport(0, 0, width, height);		
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

}
