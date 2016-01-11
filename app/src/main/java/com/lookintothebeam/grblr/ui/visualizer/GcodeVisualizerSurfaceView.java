package com.lookintothebeam.grblr.ui.visualizer;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.util.Log;

public class GcodeVisualizerSurfaceView extends GLSurfaceView {


    private static final String TAG = "GcodeVisualizerSurface";

    private final GcodeVisualizerRenderer renderer;

    private float previousX;
    private float previousY;

    public GcodeVisualizerSurfaceView(Context context){
        this(context, null);
    }

    public GcodeVisualizerSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.d(TAG, "New Surface View.");
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2);

        Log.d(TAG, "Creating renderer.");
        renderer = new GcodeVisualizerRenderer();

        Log.d(TAG, "Setting renderer");
        // Set the Renderer for drawing on the GLSurfaceView
        setRenderer(renderer);

        Log.d(TAG, "Setting render mode.");
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float prevTouchX;
    private float prevToucyY;

    @Override
    public boolean onTouchEvent(MotionEvent e) {

        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:

                float dx = x - previousX;
                float dy = y - previousY;

                // reverse direction of rotation above the mid-line
                if (y > getHeight() / 2) {
                    dx = dx * -1 ;
                }

                // reverse direction of rotation to left of the mid-line
                if (x < getWidth() / 2) {
                    dy = dy * -1 ;
                }


                renderer.theta = renderer.theta + ((dx + dy) * TOUCH_SCALE_FACTOR);
                requestRender();
        }

        previousX = x;
        previousY = y;
        return true;
    }

}
