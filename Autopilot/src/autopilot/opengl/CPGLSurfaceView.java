package autopilot.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CPGLSurfaceView extends GLSurfaceView {

	private CPGLRenderer mRenderer;
	
	public CPGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
				
		setEGLContextClientVersion(2);
		
		//mRenderer = new CPGLRenderer(context);
		//setRenderer(mRenderer);
		
		//setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
	}
	
	public void setCPRenderer(Renderer renderer) {
		mRenderer = (CPGLRenderer) renderer;
		setRenderer(mRenderer);
	}
	
	private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mPreviousX;
    private float mPreviousY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - mPreviousX;
                float dy = y - mPreviousY;

                // reverse direction of rotation above the mid-line
                //if (y > getHeight() / 2) {
                    dx = dx * -1 ;
                //}

                // reverse direction of rotation to left of the mid-line
                //if (x < getWidth() / 2) {
                    //dy = dy * -1 ;
                //}

                mRenderer.setPan(mRenderer.getPan() + dx / 100f);
                        //((dx /*+ dy*/) * TOUCH_SCALE_FACTOR));  // = 180.0f / 320
                requestRender();
        }

        mPreviousX = x;
        mPreviousY = y;
        return true;
    }

}
