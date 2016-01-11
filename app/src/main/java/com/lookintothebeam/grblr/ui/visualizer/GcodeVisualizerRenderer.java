package com.lookintothebeam.grblr.ui.visualizer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.lookintothebeam.grblr.cnc.GcodeCommand;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class GcodeVisualizerRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "GcodeVisualizerRenderer";

    // --- Color Constants ---
    private static final float[] RAPID_COLOR = { 0.2f, 0.709803922f, 0.898039216f, 1.0f };

    // --- Other Constants ---
    static final int COORDS_PER_VERTEX = 3;
    private final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // --- Matrices ---
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mRotationMatrix = new float[16];

    public float theta = 0;

    // --- Drawn Vertex Data ---
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private List<GcodeCommand> mGcodeCommandList;


    // --- Shaders ---
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
            "attribute vec4 vPosition;" +
            "void main() {" +
            "    gl_Position = uMVPMatrix * vPosition;" +
            "}";

    private final String fragmentShaderCode =
            "precision mediump float;" +
            "uniform vec4 vColor;" +
            "void main() {" +
            "    gl_FragColor = vColor;" +
            "}";

    private int mShaderProgram;
    private int mVertexPositionHandle;
    private int mVertexColorHandle;
    private int mMVPMatrixHandle;



    public GcodeVisualizerRenderer() {
        // prepare shaders and OpenGL program
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mShaderProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mShaderProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mShaderProgram, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(mShaderProgram);
        GLES20.glUseProgram(mShaderProgram);

        mVertexPositionHandle = GLES20.glGetAttribLocation(mShaderProgram, "vPosition");
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mShaderProgram, "uMVPMatrix");
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Set the background frame color
        Log.d(TAG, "New surface created.");
    }

    public void onDrawFrame(GL10 unused) {

        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.setRotateM(mRotationMatrix, 0, theta, 0, 0, 1.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, mMVPMatrix, 0, mRotationMatrix, 0);

        // get handle to vertex shader's vPosition member
        GLES20.glEnableVertexAttribArray(mVertexPositionHandle);
        GLES20.glVertexAttribPointer(mVertexPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);


        GLES20.glDrawArrays(GLES20.GL_LINES, 0, );
        Log.d(TAG, "Frame drawn");
    }

    public void update(List<GcodeCommand> gcodeCommands) {
        mGcodeCommandList = gcodeCommands;
    }

    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
    }

    public void setGcodeCommandList(List<GcodeCommand> gcodeCommandList) {
        mGcodeCommandList = gcodeCommandList;
        buildMeshes();
    }

    private void buildMeshes() {

        ArrayList<Float> meshCoords = new ArrayList<>();

        boolean absoluteDistanceMode = false;
        boolean millimetersMode = false;
        boolean continuousMotionMode = false;

        for(int i = 0; i < mGcodeCommandList.size(); i++) {
            GcodeCommand command = mGcodeCommandList.get(i);

            String[] parts = command.getCommand().split(" ");
            boolean insideComment = false;
            for(int j = 0; j < parts.length; j++) {
                // Comments
                if(parts[j].contains("(")) { insideComment = true; }
                if(parts[j].contains(")")) { insideComment = false; }
                if(insideComment) { continue; }

                // Distance Mode
                if(parts[j].matches("^G90$")) { absoluteDistanceMode = true; }
                if(parts[j].matches("^G91$")) { absoluteDistanceMode = false; }

                // Units Mode
                if(parts[j].matches("^G20")) { millimetersMode = false; }
                if(parts[j].matches("^G21")) { millimetersMode = true; }

                // Path control
                if(parts[j].matches("^G61$")) { /* Exact Path */ }
                if(parts[j].matches("^G61.1$")) { /* Exact Stop */ }
                if(parts[j].matches("^G64$")) { /* Continuous */ }

                // Tool compensation
                if(parts[j].matches("^G40$")) { /* Tool compensation off */ }
                if(parts[j].matches("^G41$")) { /* Tool radius compensation left */ }
                if(parts[j].matches("^G42$")) { /* Tool radius compensation right */ }

                // Tool selection
                if(parts[j].matches("^T\d+")) { /* Tool selection */ }

            }
        }

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(meshCoordsArray.length * 4); // (number of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder()); // use the device hardware's native byte order

        vertexBuffer = bb.asFloatBuffer(); // create a floating point buffer from the ByteBuffer
        vertexBuffer.put(meshCoordsArray); // add the coordinates to the FloatBuffer
        vertexBuffer.position(0); // set the buffer to read the first coordinate
    }


    /**
     * Utility method for compiling a OpenGL shader.
     *
     * <p><strong>Note:</strong> When developing shaders, use the checkGlError()
     * method to debug shader coding errors.</p>
     *
     * @param type - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        return shader;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}
