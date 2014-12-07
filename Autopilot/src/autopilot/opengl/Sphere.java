package autopilot.opengl;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import android.content.Context;
import android.opengl.GLES20;
import autopilot.autopilot.R;

public class Sphere {

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
            "  float texX = (vPosVarying[0] + 1.5)/3.0;" +
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
    private final int COORDS_PER_VERTEX = 3;
    private float sphereCoords[];
    private short drawOrder[];

    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    float color[] = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    /**
     * Sets up the drawing object data for use in an OpenGL ES context.
     * @param context 
     * @param rad 
     */
    public Sphere(float radius, Context context) {
    	createSphereCoord(radius);
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(
        // (# of coordinate values * 4 bytes per float)
                sphereCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(sphereCoords);
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
                GLES20.GL_VERTEX_SHADER,
                vertexShaderCode);
        int fragmentShader = CPGLRenderer.loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
        
        mTextureDataHandle = TextureHelper.loadTexture(context, R.drawable.exampleimage);
    }

    /**
     * Encapsulates the OpenGL ES instructions for drawing this shape.
     *
     * @param mvpMatrix - The Model View Project matrix in which to draw
     * this shape.
     */
    public void draw(float[] mvpMatrix) {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
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
        
        // Draw the circle
        GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
    }
    
    private void createSphereCoord(float radius) {
    	
    	int slices = 20;
    	int cuts = 10;
    	ArrayList<Float> coords = new ArrayList<Float>();
    	ArrayList<Short> order = new ArrayList<Short>();
    	
    	for(int j=0;j<=cuts;j++)
    	{
    		float phi = (float)((j * Math.PI /(float)cuts) - (Math.PI/2.0f));
    		float y = (float) (radius*Math.sin(phi));
    		double innerRad = radius*Math.cos(phi);
	    	for(int i=0;i<=slices;i++) {
	        	float theta = (float)(i / (float)slices * 2.0 * Math.PI);
	        	float x = (float) (innerRad * Math.sin(theta));
	        	float z = (float) (innerRad * Math.cos(theta));
	        	coords.add(x); coords.add(y); coords.add(z);
	        }
    	}
    	for(int j=0;j<cuts;j++)
    	{
	    	for(int i=0;i<slices;i++) {	
	        	order.add((short)((slices+1)*j + i)); 
	        	order.add((short)((slices+1)*j + i + 1)); 
	        	order.add((short)((slices+1)*(j+1) + i));
	        	
	        	order.add((short)((slices+1)*j + i + 1)); 
	        	order.add((short)((slices+1)*(j+1) + i)); 
	        	order.add((short)((slices+1)*(j+1) + i + 1));
	        }
    	}
    	
    	sphereCoords = new float[coords.size()];
        drawOrder = new short[order.size()];
        for(int i=0;i<coords.size();i++) {
        	sphereCoords[i] = coords.get(i).floatValue();
        }
        
        for(int i=0;i<order.size();i++) {
        	drawOrder[i] = order.get(i).shortValue();
        }
    }
}
