package autopilot.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class CPGLSurfaceView extends GLSurfaceView {

	private final CPGLRenderer mRenderer;
	
	public CPGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
				
		setEGLContextClientVersion(2);
		
		mRenderer = new CPGLRenderer(context);
		setRenderer(mRenderer);
		
		//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}

}
