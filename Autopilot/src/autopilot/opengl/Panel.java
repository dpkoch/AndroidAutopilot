package autopilot.opengl;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.content.Context;
import android.opengl.GLES20;
import autopilot.autopilot.R;

public class Panel {
	
	//private final Context mActivityContext;
	
	private final String vertexShaderCode =
			"uniform mat4 uMVPMatrix;" +
			"attribute vec4 vPosition;" +
		    "varying vec3 vPosVarying;" + 
		    "void main() {" +
		    "  vPosVarying = vec3(vPosition);" +
		    "  gl_Position = uMVPMatrix * vPosition;" +
		    "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "uniform sampler2D uTexture;" +  
            "varying vec3 vPosVarying;" +  
            "void main() {" +
            "  float texX = (-vPosVarying[0] + 1.5)/3.0;" +
            "  float texY = (1.0 - vPosVarying[1])/2.0;" +
            "  vec2 texCoord = vec2(texX,texY);" + 
            "  gl_FragColor = texture2D(uTexture, texCoord);" +
            "}";

	private final FloatBuffer vertexBuffer;
	private final ShortBuffer drawListBuffer;
	private final int mProgram;
    private int mPositionHandle;
    private int mColorHandle;
    private int mMVPMatrixHandle;
    private int mTextureDataHandle;
	private int mTextureUniformHandle;
    
 // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static float panelCoords[] = {1f,0.925f,0f,1.5f,1f,0f,-0.00242786f,0.925f,0f,1.5f,-1f,0f,1.425f,-0.5f,0f,-1.425f,0.5f,0f,-1.5f,1f,0f,-1.425f,-0.5f,0f,-1f,0.925f,0f,0.83685f,0.89388f,0f,0.160723f,0.89388f,0f,-0.165578f,0.89388f,0f,-0.83685f,0.89388f,0f,1.425f,0.5f,0f,-1.39388f,0.33685f,0f,-1.39388f,-0.33685f,0f,1.39388f,0.33685f,0f,1.39388f,-0.33685f,0f,-1.5f,-1f,0f,1f,-0.925f,0f,0.83685f,-0.89388f,0f,0.160723f,-0.89388f,0f,-0.00242786f,-0.925f,0f,-0.165578f,-0.89388f,0f,-0.83685f,-0.89388f,0f,-1f,-0.925f,0f,1.16315f,0.89388f,0f,1.39388f,0.66315f,0f,-1.39388f,0.66315f,0f,-1.16315f,0.89388f,0f,0.69948f,0.80052f,0f,0.298093f,0.80052f,0f,-0.302948f,0.80052f,0f,-0.69948f,0.80052f,0f,1.30052f,-0.19948f,0f,-1.30052f,0.19948f,0f,1.30052f,0.19948f,0f,-1.30052f,-0.19948f,0f,1.39388f,-0.66315f,0f,-1.39388f,-0.66315f,0f,0.69948f,-0.80052f,0f,0.298093f,-0.80052f,0f,-0.302948f,-0.80052f,0f,-0.69948f,-0.80052f,0f,1.16315f,-0.89388f,0f,-1.16315f,-0.89388f,0f,0.60612f,0.66315f,0f,0.391452f,0.66315f,0f,-0.396308f,0.66315f,0f,-0.60612f,0.66315f,0f,0.69948f,0.19948f,0f,0.60612f,0.33685f,0f,0.391452f,0.33685f,0f,0.298093f,0.19948f,0f,-0.302948f,0.19948f,0f,-0.396308f,0.33685f,0f,-0.60612f,0.33685f,0f,-0.69948f,0.19948f,0f,0.83685f,-0.10612f,0f,0.83685f,0.10612f,0f,0.69948f,-0.19948f,0f,-0.165578f,-0.10612f,0f,-0.165578f,0.10612f,0f,-0.302948f,-0.19948f,0f,-1.16315f,0.10612f,0f,1.16315f,0.10612f,0f,1.16315f,-0.10612f,0f,0.160723f,0.10612f,0f,0.160723f,-0.10612f,0f,-1.16315f,-0.10612f,0f,-0.83685f,0.10612f,0f,-0.83685f,-0.10612f,0f,0.298093f,-0.19948f,0f,-0.69948f,-0.19948f,0f,0.60612f,-0.33685f,0f,0.391452f,-0.33685f,0f,-0.396308f,-0.33685f,0f,-0.60612f,-0.33685f,0f,0.60612f,-0.66315f,0f,0.391452f,-0.66315f,0f,-0.396308f,-0.66315f,0f,-0.60612f,-0.66315f,0f,1.30052f,0.80052f,0f,-1.30052f,0.80052f,0f,1.30052f,-0.80052f,0f,-1.30052f,-0.80052f,0f,0.575f,0.5f,0f,0.422572f,0.5f,0f,-0.427428f,0.5f,0f,-0.575f,0.5f,0f,1f,0.075f,0f,-0.00242786f,-0.075f,0f,-0.00242786f,0.075f,0f,-1f,0.075f,0f,1f,-0.075f,0f,-1f,-0.075f,0f,0.575f,-0.5f,0f,0.422572f,-0.5f,0f,-0.427428f,-0.5f,0f,-0.575f,-0.5f,0f};
    private final short drawOrder[] = {0,1,2,3,1,4,5,6,7,2,6,8,1,6,2,9,0,10,11,2,8,0,2,10,11,8,12,1,13,4,14,5,15,16,17,13,17,4,13,6,18,7,5,7,15,19,20,21,19,21,22,22,23,24,22,24,25,3,19,22,3,22,18,22,25,18,26,1,0,13,1,27,28,6,5,8,6,29,30,9,31,9,10,31,32,11,12,32,12,33,17,16,34,35,14,15,16,36,34,35,15,37,3,4,38,39,7,18,20,40,21,40,41,21,23,42,43,23,43,24,3,44,19,25,45,18,46,30,47,30,31,47,48,32,33,48,33,49,50,51,52,50,52,53,54,55,56,54,56,57,58,59,60,61,62,63,62,54,63,64,35,37,65,66,36,66,34,36,53,67,68,64,37,69,70,71,57,59,50,60,53,68,72,57,71,73,74,60,75,60,72,75,76,63,73,76,73,77,40,78,79,40,79,41,42,80,81,42,81,43,82,1,26,27,1,82,83,6,28,29,6,83,3,38,84,85,39,18,3,84,44,45,85,18,86,46,87,46,47,87,88,48,49,88,49,89,51,86,87,51,87,52,55,88,89,55,89,56,72,60,50,58,90,59,91,92,61,92,62,61,93,64,69,58,94,90,90,94,65,94,66,65,67,92,91,95,93,69,54,57,63,71,70,95,91,68,67,70,93,95,53,72,50,63,57,73,96,74,75,96,75,97,98,76,77,98,77,99,78,96,97,78,97,79,80,98,99,80,99,81};
    
    private final int vertexCount = panelCoords.length / COORDS_PER_VERTEX;
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // Set color with red, green, blue and alpha (opacity) values
    float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    public Panel(final Context context) {
    	//mActivityContext = context;
    	
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
                // (number of coordinate values * 4 bytes per float)
                panelCoords.length * 4);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());

        // create a floating point buffer from the ByteBuffer
        vertexBuffer = bb.asFloatBuffer();
        // add the coordinates to the FloatBuffer
        vertexBuffer.put(panelCoords);
        // set the buffer to read the first coordinate
        vertexBuffer.position(0);
        
        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(
                // (# of coordinate values * 2 bytes per short)
                drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);
        
     // prepare shaders and OpenGL program
        int vertexShader = CPGLRenderer.loadShader(
                GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = CPGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
        
        mTextureDataHandle = TextureHelper.loadTexture(context, R.drawable.examplecp);
    }
    
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                                     GLES20.GL_FLOAT, false,
                                     vertexStride, vertexBuffer);

        // get handle to fragment shader's vColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
        
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, color, 0);
        
        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        //CPGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);
        //CPGLRenderer.checkGlError("glUniformMatrix4fv");
        
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
     
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0);

        // Draw the triangle
        //GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }

    /*public void loadTexture() {
    	
    	final int[] textureHandle = new int[1];
    	GLES20.glGenTextures(1, textureHandle, 0);
        
    	final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;   // No pre-scaling
 
        // Read in the resource
        final Bitmap bitmap = BitmapFactory.decodeResource(mActivityContext.getResources(), R.drawable.exampleimage, options);
 
        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);
 
        // Set filtering
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
 
        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
 
        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bitmap.recycle();
    	
        mTextureDataHandle = textureHandle[0];
    }*/
}
