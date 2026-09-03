package com.john.TreeApp.views;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomableImageView extends ImageView {
    private Matrix matrix;
    private ScaleGestureDetector scaleDetector;
    private PointF lastPoint;
    private float[] matrixValues = new float[9];
    private float minScale = 1.0f;
    private float maxScale = 4.0f;

    public ZoomableImageView(Context context) {
        super(context);
        init();
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setScaleType(ScaleType.MATRIX);
        matrix = new Matrix();
        setImageMatrix(matrix);
        
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float currentScale = getCurrentScale();
                
                // Limit the scale factor
                if (currentScale * scaleFactor < minScale) {
                    scaleFactor = minScale / currentScale;
                } else if (currentScale * scaleFactor > maxScale) {
                    scaleFactor = maxScale / currentScale;
                }
                
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                setImageMatrix(matrix);
                return true;
            }
        });
        
        lastPoint = new PointF();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (getDrawable() != null && (changed || matrix.isIdentity())) {
            fitToScreen();
        }
    }

    private void fitToScreen() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (getDrawable() == null || viewWidth == 0 || viewHeight == 0) return;

        int drawableWidth = getDrawable().getIntrinsicWidth();
        int drawableHeight = getDrawable().getIntrinsicHeight();

        float scale;
        float scaleX = (float) viewWidth / (float) drawableWidth;
        float scaleY = (float) viewHeight / (float) drawableHeight;
        scale = Math.min(scaleX, scaleY);

        matrix.setScale(scale, scale);

        // Center the image
        float redundantXSpace = (float) viewWidth - (scale * (float) drawableWidth);
        float redundantYSpace = (float) viewHeight - (scale * (float) drawableHeight);
        matrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2);

        minScale = scale;
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastPoint.set(event.getX(), event.getY());
                return true;
                
            case MotionEvent.ACTION_MOVE:
                float dx = event.getX() - lastPoint.x;
                float dy = event.getY() - lastPoint.y;
                
                matrix.postTranslate(dx, dy);
                setImageMatrix(matrix);
                
                lastPoint.set(event.getX(), event.getY());
                return true;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                lastPoint.set(0, 0);
                return true;
        }
        
        return super.onTouchEvent(event);
    }

    private float getCurrentScale() {
        matrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    public void resetZoom() {
        fitToScreen();
    }
} 