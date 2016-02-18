/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.jr3dL.android;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.multiplyMM;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.opengl.GLSurfaceView.Renderer;
import android.os.SystemClock;

import com.jakaria.android.R;
import com.jr3dL.android.util.LoggerConfig;
import com.jr3dL.android.util.ShaderHelper;
import com.jr3dL.android.util.TextResourceReader;
import com.jr3dL.android.util.TextureHelper;

public class Jr3DLRenderer implements Renderer {                       
    private static final String U_MATRIX = "u_Matrix";
    
    private static final String A_POSITION = "a_Position";
    private static final String A_COLOR = "a_Color";
    
    private static final int BYTES_PER_FLOAT = 4;  
    
    private final FloatBuffer mCubePositions;
    private final FloatBuffer mCubeColors;
    private final FloatBuffer mCubeNormals;
    private final Context context;
    
    private float[] ViewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] mvMatrix = new float[16];
    private final float[] FprojectionMatrix = new float[16];
    
    private final float[] modelMatrix = new float[16];
    private float[] mLightModelMatrix = new float[16];

    private int program;
    private int pointProgram;
    private int uMatrixLocation;
    private int mPositionHandle;
    private int mMVMatrixHandle;
	
    
    //.........TexturePurpose........//
    private final FloatBuffer mCubeTextureCoordinates;
    
    /** This is a handle to our texture data. */
    private int mTextureDataHandle;
    
    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;
    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;
    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;
    //...........TextureEnd........//
    
	/** This will be used to pass in model color information. */
	private int mColorHandle;
	
	/** This will be used to pass in the light position. */
	private int mLightPosHandle;
	
	   /** This will be used to pass in model normal information. */
    private int mNormalHandle;
	
	/** Size of the position data in elements. */
	private final int mPositionDataSize = 3;	
	
	/** Size of the color data in elements. */
	private final int mColorDataSize = 4;	
	
	/** Size of the normal data in elements. */
	private final int mNormalDataSize = 3;
	
    private final float[] mLightPosInModelSpace = new float[] {2.50f, 0.0f, 0.0f, 1.0f};
	
	/** Used to hold the current position of the light in world space (after transformation via model matrix). */
	private final float[] mLightPosInWorldSpace = new float[4];
	
	/** Used to hold the transformed position of the light in eye space (after transformation via modelview matrix) */
	private final float[] mLightPosInEyeSpace = new float[4];


    public Jr3DLRenderer(Context context) {
        this.context = context;
     
        
        final float[] cubePositionData =
    		{
    				// In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
    				// if the points are counter-clockwise we are looking at the "front". If not we are looking at
    				// the back. OpenGL has an optimization where all back-facing triangles are culled, since they
    				// usually represent the backside of an object and aren't visible anyways.
    				
    				// Front face
    				-1.0f, 1.0f, 1.0f,				
    				-1.0f, -1.0f, 1.0f,
    				1.0f, 1.0f, 1.0f, 
    				-1.0f, -1.0f, 1.0f, 				
    				1.0f, -1.0f, 1.0f,
    				1.0f, 1.0f, 1.0f,
    				
    				// Right face
    				1.0f, 1.0f, 1.0f,				
    				1.0f, -1.0f, 1.0f,
    				1.0f, 1.0f, -1.0f,
    				1.0f, -1.0f, 1.0f,				
    				1.0f, -1.0f, -1.0f,
    				1.0f, 1.0f, -1.0f,
    				
    				// Back face
    				1.0f, 1.0f, -1.0f,				
    				1.0f, -1.0f, -1.0f,
    				-1.0f, 1.0f, -1.0f,
    				1.0f, -1.0f, -1.0f,				
    				-1.0f, -1.0f, -1.0f,
    				-1.0f, 1.0f, -1.0f,
    				
    				// Left face
    				-1.0f, 1.0f, -1.0f,				
    				-1.0f, -1.0f, -1.0f,
    				-1.0f, 1.0f, 1.0f, 
    				-1.0f, -1.0f, -1.0f,				
    				-1.0f, -1.0f, 1.0f, 
    				-1.0f, 1.0f, 1.0f, 
    				
    				// Top face
    				-1.0f, 1.0f, -1.0f,				
    				-1.0f, 1.0f, 1.0f, 
    				1.0f, 1.0f, -1.0f, 
    				-1.0f, 1.0f, 1.0f, 				
    				1.0f, 1.0f, 1.0f, 
    				1.0f, 1.0f, -1.0f,
    				
    				// Bottom face
    				1.0f, -1.0f, -1.0f,				
    				1.0f, -1.0f, 1.0f, 
    				-1.0f, -1.0f, -1.0f,
    				1.0f, -1.0f, 1.0f, 				
    				-1.0f, -1.0f, 1.0f,
    				-1.0f, -1.0f, -1.0f,
    		};	
    		
    		// R, G, B, A
    		final float[] cubeColorData =
    		{				
    		 // Front face (red)
                1.0f, 0.0f, 0.0f, 1.0f,             
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,             
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                
                // Right face (green)
                0.0f, 1.0f, 0.0f, 1.0f,             
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,             
                0.0f, 1.0f, 0.0f, 1.0f,
                0.0f, 1.0f, 0.0f, 1.0f,
                
                // Back face (blue)
                0.0f, 0.0f, 1.0f, 1.0f,             
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,             
                0.0f, 0.0f, 1.0f, 1.0f,
                0.0f, 0.0f, 1.0f, 1.0f,
                
                // Left face (yellow)
                1.0f, 1.0f, 0.0f, 1.0f,             
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,             
                1.0f, 1.0f, 0.0f, 1.0f,
                1.0f, 1.0f, 0.0f, 1.0f,
                
             // Bottom face (grayish white)
                0.8f, 1.0f, 1.0f, 0.0f,             
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f,             
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f,
                
                // Bottom face (grayish white)
                0.8f, 1.0f, 1.0f, 0.0f,             
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f,             
                0.8f, 1.0f, 1.0f, 0.0f,
                0.8f, 1.0f, 1.0f, 0.0f
    		};
    		
    		// X, Y, Z
    		// The normal is used in light calculations and is a vector which points
    		// orthogonal to the plane of the surface. For a cube model, the normals
    		// should be orthogonal to the points of each face.
    		final float[] cubeNormalData =
    		{												
    				// Front face
    				0.0f, 0.0f, 1.0f,				
    				0.0f, 0.0f, 1.0f,
    				0.0f, 0.0f, 1.0f,
    				0.0f, 0.0f, 1.0f,				
    				0.0f, 0.0f, 1.0f,
    				0.0f, 0.0f, 1.0f,
    				
    				// Right face 
    				1.0f, 0.0f, 0.0f,				
    				1.0f, 0.0f, 0.0f,
    				1.0f, 0.0f, 0.0f,
    				1.0f, 0.0f, 0.0f,				
    				1.0f, 0.0f, 0.0f,
    				1.0f, 0.0f, 0.0f,
    				
    				// Back face 
    				0.0f, 0.0f, -1.0f,				
    				0.0f, 0.0f, -1.0f,
    				0.0f, 0.0f, -1.0f,
    				0.0f, 0.0f, -1.0f,				
    				0.0f, 0.0f, -1.0f,
    				0.0f, 0.0f, -1.0f,
    				
    				// Left face 
    				-1.0f, 0.0f, 0.0f,				
    				-1.0f, 0.0f, 0.0f,
    				-1.0f, 0.0f, 0.0f,
    				-1.0f, 0.0f, 0.0f,				
    				-1.0f, 0.0f, 0.0f,
    				-1.0f, 0.0f, 0.0f,
    				
    				// Top face 
    				0.0f, 1.0f, 0.0f,			
    				0.0f, 1.0f, 0.0f,
    				0.0f, 1.0f, 0.0f,
    				0.0f, 1.0f, 0.0f,				
    				0.0f, 1.0f, 0.0f,
    				0.0f, 1.0f, 0.0f,
    				
    				// Bottom face 
    				0.0f, -1.0f, 0.0f,			
    				0.0f, -1.0f, 0.0f,
    				0.0f, -1.0f, 0.0f,
    				0.0f, -1.0f, 0.0f,				
    				0.0f, -1.0f, 0.0f,
    				0.0f, -1.0f, 0.0f
    		};
    		
    		//Texture Purpose//
    		final float[] cubeTextureCoordinateData =
            {                                               
                    // Front face
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f,             
                    
                    // Right face 
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f, 
                    
                    // Back face 
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f, 
                    
                    // Left face 
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f, 
                    
                    // Top face 
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f, 
                    
                    // Bottom face 
                    0.0f, 0.0f,                 
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f
            };
    		//TexturePurpose..............//
    		
    		// Initialize the buffers.
    		mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();							
    		mCubePositions.put(cubePositionData).position(0);		
    		
    		mCubeColors = ByteBuffer.allocateDirect(cubeColorData.length * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();							
    		mCubeColors.put(cubeColorData).position(0);
    		
    		mCubeNormals = ByteBuffer.allocateDirect(cubeNormalData.length * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer();							
    		mCubeNormals.put(cubeNormalData).position(0);
    		
    		//TexturePurpose.........//
    		mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * BYTES_PER_FLOAT)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
            //Texture........//
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        
        // Use culling to remove back faces.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
		
		// Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		
		
		// Position the eye in front of the origin.
				final float eyeX = 0.0f;
				final float eyeY = 0.0f;
				final float eyeZ = -.2f;

				// We are looking toward the distance
				final float lookX = 0.0f;
				final float lookY = 0.0f;
				final float lookZ = -20f;

				// Set our up vector. This is where our head would be pointing were we holding the camera.
				final float upX = 0.0f;
				final float upY = 1.0f;
				final float upZ = 0.0f;

				// Set the view matrix. This matrix can be said to represent the camera position.
				// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
				// view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
				Matrix.setLookAtM(ViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
				//Matrix.rotateM(ViewMatrix, 0, -45, 0.0f, 1.0f, 0.0f); 

///////////////////////////////pointProgram/////////////////////////////////	
				
				String vertexShaderSource1 = TextResourceReader
		            .readTextFileFromResource(context, R.raw.point_vertex_shader);
		        String fragmentShaderSource1 = TextResourceReader
		            .readTextFileFromResource(context, R.raw.point_fragment_shader);

		        int vertexShader1 = ShaderHelper.compileVertexShader(vertexShaderSource1);
		        int fragmentShader1 = ShaderHelper
		            .compileFragmentShader(fragmentShaderSource1);

		        pointProgram = ShaderHelper.linkProgram(vertexShader1, fragmentShader1);

		        if (LoggerConfig.ON) {
		            ShaderHelper.validateProgram(pointProgram);
		        }

				
///////////////////////////////////////////////// program////////////				
        String vertexShaderSource = TextResourceReader
            .readTextFileFromResource(context, R.raw.simple_vertex_shader);
        String fragmentShaderSource = TextResourceReader
            .readTextFileFromResource(context, R.raw.simple_fragment_shader);

        int vertexShader = ShaderHelper.compileVertexShader(vertexShaderSource);
        int fragmentShader = ShaderHelper
            .compileFragmentShader(fragmentShaderSource);

        program = ShaderHelper.linkProgram(vertexShader, fragmentShader);

        if (LoggerConfig.ON) {
            ShaderHelper.validateProgram(program);
        }

        glUseProgram(program);
        
        uMatrixLocation = glGetUniformLocation(program, U_MATRIX);
        
        // Load the texture
        mTextureDataHandle = TextureHelper.loadTexture(context, R.drawable.fouga);

    }


    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to fill the entire surface.
        glViewport(0, 0, width, height);
        
        final float ratio = (float) height/ width;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 20.0f;
		
		Matrix.frustumM(projectionMatrix, 0, bottom, top, left, right, near, far);
        
    }

    /**
     * OnDrawFrame is called whenever a new frame needs to be drawn. Normally,
     * this is done at the refresh rate of the screen.
     */
    @Override
    public void onDrawFrame(GL10 glUnused) {
        // Clear the rendering surface.
        glClear(GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        
        long time = SystemClock.uptimeMillis() % 10000L;        
        float angleInDegrees = (2000.0f / 100000.0f) * ((int) time);
        
        glUseProgram(program);
                        
        // Assign the matrix
        
        mMVMatrixHandle = GLES20.glGetUniformLocation(program, "u_MVMatrix"); 
        mLightPosHandle = GLES20.glGetUniformLocation(program, "u_LightPos");
        mNormalHandle = GLES20.glGetAttribLocation(program, "a_Normal");
        
        
        //...........TexturePurpose..........//
        mTextureUniformHandle = GLES20.glGetUniformLocation(program, "u_Texture");
        
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(program, "a_TexCoordinate");
        
        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        
        // Bind the texture to this unit.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, 0); 
        
        //..................TexturePurpose...............//
        
        
        // Calculate position of the light. Rotate and then push into the distance.
        Matrix.setIdentityM(mLightModelMatrix, 0);
        Matrix.translateM(mLightModelMatrix, 0, 1.0f, 1.0f, -5.0f);      
        Matrix.rotateM(mLightModelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);
        Matrix.translateM(mLightModelMatrix, 0, 0.0f, 0.0f, 2.0f);
         
        
        Matrix.multiplyMV(mLightPosInWorldSpace, 0, mLightModelMatrix, 0, mLightPosInModelSpace, 0);
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, ViewMatrix, 0, mLightPosInWorldSpace, 0);  
        
        
       
        
     
        
         //Left wall              
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, -4.0f, 0.0f, -7.0f);     
        Matrix.scaleM(modelMatrix, 0, 0.5f, 2.0f, 2.0f);
        Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 0.0f, 1.0f);
        drawCube();
        
        //fan
       Matrix.setIdentityM(modelMatrix, 0);
       Matrix.translateM(modelMatrix, 0, 0.3f, 1.5f, -7.0f);
       Matrix.scaleM(modelMatrix, 0, 0.5f, 0.2f, 0.5f);
       Matrix.rotateM(modelMatrix, 0, angleInDegrees, 0.0f, 1.0f, 0.0f);  
       drawCube();
       
       
        //Right wall
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 4.0f, 0.0f, -7.0f);
        Matrix.scaleM(modelMatrix, 0, 0.5f, 2.0f, 2.0f);
        Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 0.0f, 1.0f);
        drawCube();
        
        //Back wall
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, -9.0f);
        Matrix.scaleM(modelMatrix, 0, 4.0f, 2.0f, 0.5f);
        Matrix.rotateM(modelMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
        drawCube();
        
        //floor
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, -2f, -7.0f);
        Matrix.scaleM(modelMatrix, 0, 4f, 0.2f, 1.5f);
        Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);  
        drawCube();
        
        //top
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.translateM(modelMatrix, 0, 0.0f, 2.1f, -7.0f);
        Matrix.scaleM(modelMatrix, 0, 4f, 0.2f, 2.5f);
        Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);  
        drawCube();
        
        
        //bed 1
       Matrix.setIdentityM(modelMatrix, 0);
       Matrix.translateM(modelMatrix, 0, 2.9f, -1.5f, -8.0f);
       Matrix.scaleM(modelMatrix, 0, 2f, 0.2f, 2.5f);
       Matrix.rotateM(modelMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
       drawCube();
       
       //bed 2
       Matrix.setIdentityM(modelMatrix, 0);
       Matrix.translateM(modelMatrix, 0, 2.9f, 0.25f, -8.0f);
       Matrix.scaleM(modelMatrix, 0, 2f, 0.2f, 2.5f);
       Matrix.rotateM(modelMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
       drawCube();
       
     //Stair
       Matrix.setIdentityM(modelMatrix, 0);
       Matrix.translateM(modelMatrix, 0, 0.7f, -0.9f, -10.0f);
       Matrix.scaleM(modelMatrix, 0, 0.1f, 1.4f, 2.5f);
       Matrix.rotateM(modelMatrix, 0, 180, 0.0f, 0.0f, 1.0f);
       drawCube();
       
      //Locker
       Matrix.setIdentityM(modelMatrix, 0);
       Matrix.translateM(modelMatrix, 0, -3.0f, 1.5f, -10.0f);
       Matrix.scaleM(modelMatrix, 0, 1.0f, 2.f, 2.5f);
       Matrix.rotateM(modelMatrix, 0, 90, 1.0f, 0.0f, 0.0f);
       drawCube();
       
       //balish
      Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, 2.1f, -1.3f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, 0.5f, 0.2f, .5f);
     Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 1.0f, 0.0f);
     drawCube();
     
   
     
     
     //table-leg1
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -2.0f, -1.5f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.5f, .1f);
     Matrix.rotateM(modelMatrix, 0, 180, 1.0f, 0.0f, 0.0f);
     drawCube();
      //table-leg2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, 0.3f, -1.5f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.5f, .1f); 
     Matrix.rotateM(modelMatrix, 0, 180, 1.0f, 0.0f, 0.0f);
     drawCube();
     
     //table-top
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -0.7f, -.9f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, 1f, 0.03f, 1.5f);
     Matrix.rotateM(modelMatrix, 0, 90, 1.00f, 0.0f, 0.0f);
     drawCube();
     
     //computer
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -0.5f, -.7f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, .4f, 0.3f, .01f);
     Matrix.rotateM(modelMatrix, 0, 90, 0.0f, 1.0f, 0.0f); 
     drawCube();
     
     //keyboard
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -0.5f, -.8f, -6.5f);
     Matrix.scaleM(modelMatrix, 0, .4f, 0.035f, .25f);
     Matrix.rotateM(modelMatrix, 0, 270, 0f, 1.0f, 0.0f); 
     drawCube();
     //cpu
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -1f, -1.6f, -8.0f);
     Matrix.scaleM(modelMatrix, 0, .1f, 0.3f, .5f);
     Matrix.rotateM(modelMatrix, 0, 270, 0f, 0.0f, 1.0f);
     drawCube();
     
     
     //chair
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -.1f, -.8f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .1f, 0.02f, .3f);
     Matrix.rotateM(modelMatrix, 0, 180, 0f, 1.0f, 0.0f);
     drawCube();
     
     //chair-leg1
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -0.1f, -1.5f, -6.0f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.3f, .1f);
     Matrix.rotateM(modelMatrix, 0, 90, 0f, 0.0f, 1.0f);
     drawCube();
     
     //chair-leg2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, -.2f, -1.5f, -6.0f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.3f, .1f);
     Matrix.rotateM(modelMatrix, 0, 90, 0f, 0.0f, 1.0f);
     drawCube();
     
     /*
     //small cube1
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .6f, -.3f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .04f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 90, 1f, 0f, 0f);
     drawCube();
     //small cube2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .6f, -.4f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .04f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 90, 1f, 0f, 0f);
     drawCube();
     
     //folder1
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0,.92f, -.28f, -5.1f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 180, 1f, 0f, 0f);
     drawCube();
   //folder2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0,1f, -.28f, -5.1f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 180, 1f, 0f, 0f);
     drawCube();
   //folder3
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0,1.08f, -.28f, -5.1f);
     Matrix.scaleM(modelMatrix, 0, .02f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 180, 1f, 0f, 0f);
     drawCube();
     
   //sokes
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .1f, -1.2f, -8f);
     Matrix.scaleM(modelMatrix, 0, .3f, 1f, .5f);
     drawCube();
     
   //sokes small cube1
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .07f, -.2f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .04f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 90, 1f, 0f, 0f);
     drawCube();
     //sokes small cube2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .07f, -.4f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .04f, 0.02f, .03f);
     Matrix.rotateM(modelMatrix, 0, 90, 1f, 0f, 0f);
     drawCube();
     
     //sokes small handler cube2
     Matrix.setIdentityM(modelMatrix, 0);
     Matrix.translateM(modelMatrix, 0, .01f, -.7f, -4.0f);
     Matrix.scaleM(modelMatrix, 0, .01f, 0.04f, .03f);
     Matrix.rotateM(modelMatrix, 0, 90, 1f, 0f, 0f);
     drawCube();
     
     */
     
     
     // Draw a point to indicate the light.
        GLES20.glUseProgram(pointProgram);      
        Matrix.setIdentityM(modelMatrix, 0);
       //Matrix.translateM(modelMatrix, 0, 1f, 1.0f, 10f);
        Matrix.translateM(modelMatrix, 0, 1.0f, 0.0f, -5.0f);
        drawLight();
       
  
    }
    
    //................TexturePurpose............//
    private void drawTextureCube()
    {       
        
        mPositionHandle = glGetAttribLocation(program, A_POSITION);
        mColorHandle = glGetAttribLocation(program, A_COLOR);
         
        // Pass in the position information
        mCubePositions.position(0);     
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
                0, mCubePositions);        
                
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
                0, mCubeColors);        
        
     // Pass in the normal information
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
                0, mCubeNormals);
        
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);    
        
        Matrix.multiplyMM(mvMatrix, 0, ViewMatrix, 0, modelMatrix, 0);  
        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        multiplyMM(FprojectionMatrix, 0, projectionMatrix, 0, mvMatrix, 0);        
        glUniformMatrix4fv(uMatrixLocation, 1, false, FprojectionMatrix, 0);
     // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);      
    }
    
    //.................TextureEnd..............//
    private void drawCube()
	{		
    	
    	mPositionHandle = glGetAttribLocation(program, A_POSITION);
        mColorHandle = glGetAttribLocation(program, A_COLOR);
         
		// Pass in the position information
		mCubePositions.position(0);		
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false,
        		0, mCubePositions);        
                
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the color information
        mCubeColors.position(0);
        GLES20.glVertexAttribPointer(mColorHandle, mColorDataSize, GLES20.GL_FLOAT, false,
        		0, mCubeColors);        
        
     // Pass in the normal information
        mCubeNormals.position(0);
        GLES20.glVertexAttribPointer(mNormalHandle, mNormalDataSize, GLES20.GL_FLOAT, false, 
                0, mCubeNormals);
        
        GLES20.glEnableVertexAttribArray(mColorHandle);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);    
        
        Matrix.multiplyMM(mvMatrix, 0, ViewMatrix, 0, modelMatrix, 0);  
        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mvMatrix, 0);
        multiplyMM(FprojectionMatrix, 0, projectionMatrix, 0, mvMatrix, 0);        
        glUniformMatrix4fv(uMatrixLocation, 1, false, FprojectionMatrix, 0);
     // Pass in the light position in eye space.        
        GLES20.glUniform3f(mLightPosHandle, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1], mLightPosInEyeSpace[2]);
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);      
	}
    
    /**
     * Draws a point representing the position of the light.
     */
    private void drawLight()
    {
        final int pointMVPMatrixHandle = GLES20.glGetUniformLocation(pointProgram, "u_MVPMatrix");
        final int pointPositionHandle = GLES20.glGetAttribLocation(pointProgram, "a_Position");
        
        // Pass in the position.
        GLES20.glVertexAttrib3f(pointPositionHandle, mLightPosInModelSpace[0], mLightPosInModelSpace[1], mLightPosInModelSpace[2]);

        // Since we are not using a buffer object, disable vertex arrays for this attribute.
       // GLES20.glDisableVertexAttribArray(pointPositionHandle);  
        
        // Pass in the transformation matrix.
        Matrix.multiplyMM(mvMatrix, 0, ViewMatrix, 0, mLightModelMatrix, 0);
        Matrix.multiplyMM(FprojectionMatrix, 0, projectionMatrix, 0, mvMatrix, 0);
        GLES20.glUniformMatrix4fv(pointMVPMatrixHandle, 1, false, FprojectionMatrix, 0);
        
        // Draw the point.
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, 1);
    }

}