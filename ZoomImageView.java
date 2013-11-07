package com.sukohi.lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.v4.widget.DrawerLayout.LayoutParams;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class ZoomImageView extends View implements OnTouchListener {

	private final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";
	private final int DRAW_MODE_FIT = 0;
	private final int DRAW_MODE_MAX = 1;
	private final int DRAW_MODE_MIN = 2;
	private Context context;
	private Bitmap bitmap;
	private Paint paint;
	private Rect rect;
	private int layoutWidthParam = 0;
	private int oldPointsDistance, viewLength, viewWidth, viewHeight, drawMode;
	private int doubleTapDuration = 300;
	private float scale = 1F;
	private float maxScale = 2F;
	private float minScale = 0.5F;
	private Pair<Long, Long> actionDownTimes;
	
	public ZoomImageView(Context c) {
		super(c);
		context = c;
		init();
	}
	
	public ZoomImageView(Context c, AttributeSet attrs){
		super(c,attrs);
		context = c;
		init();

		int layoutWidthParam = attrs.getAttributeIntValue(ANDROID_NAMESPACE, "layout_width", 0);
		setWidth(layoutWidthParam);
		int srcResourceId = attrs.getAttributeResourceValue(ANDROID_NAMESPACE, "src", 0);
		
		if(srcResourceId > 0) {
			
			setImageResource(srcResourceId);
			
		}
		
	}
	
	public void setWidth(int width) {
		
		layoutWidthParam = width;
		
	}
	
	private void init() {
		
		rect = new Rect();
		paint = new Paint();
		actionDownTimes = new Pair<Long, Long>(0L, 0L);
		setOnTouchListener(this);
		
	}
	
	public void setImageBitmap(Bitmap bm) {
		
		bitmap = bm;
		
	}
	
	public void setImageResource(int resourceId) {
		
		Resources resources = context.getResources();
		bitmap = BitmapFactory.decodeResource(resources, resourceId);
		
	}

	public void setMaxScale(float scale) {
		
		maxScale = scale;
		
	}

	public void setMinScale(float scale) {
		
		minScale = scale;
		
	}
	
	public void setDoubleTapDuration(int duration) {
		
		doubleTapDuration = duration;
		
	}
	
	private int bitmapX = 0;
	private int bitmapY = 0;
	private float oldX = 0;
	private float oldY = 0;
	private float lastMultiX = 0;
	private float lastMultiY = 0;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		int pointCount = event.getPointerCount();
		float x = event.getRawX();
		float y = event.getRawY();

		if(oldX == 0 && oldY == 0) {

			oldX = x;
			oldY = y;

		}
		
		if(lastMultiX != 0 || lastMultiY != 0) {
			
			x = x - (x - lastMultiX);
			y = y - (y - lastMultiY);
			
		}
		
		switch(event.getAction()) {

		case MotionEvent.ACTION_MOVE:
	    	
	    	if(pointCount == 1) {
	    		
				bitmapX += (int) (x - oldX);
				bitmapY += (int) (y - oldY);
   
				clearPinchDistance();
	    		   
	    	} else if(pointCount == 2) {
				
				int pointsdefference = getPointsDifference(
						new PointF(event.getX(0), event.getY(0)), 
						new PointF(event.getX(1), event.getY(1))
				);
	    		
				if(oldPointsDistance == 0) {
					
					oldPointsDistance = pointsdefference;
					
				}
				
				float moveDifference = (pointsdefference - oldPointsDistance);
				scale += moveDifference / viewLength;
				oldPointsDistance = pointsdefference;
				
				if(scale < minScale) {
					
					scale = minScale;
					
				}
				
	    	} else {
	    		   
	    		clearPinchDistance();
	    		   
	    	}
	    	   
	    	invalidate();
	    	break;
	    	
		case MotionEvent.ACTION_POINTER_UP:
			lastMultiX = x;
			lastMultiY = y;
			break;
			
		case MotionEvent.ACTION_UP:
			clearLastMultiXY();
			
			long firstTime = actionDownTimes.first;
			long secondTime = actionDownTimes.second;		
			
			if(firstTime > 0 && secondTime > 0
					&& (secondTime-firstTime) < doubleTapDuration
					&& (System.currentTimeMillis()-secondTime) < doubleTapDuration) {
				
				onDoubleTap();
				invalidate();
				
			}
			
			break;
			
		case MotionEvent.ACTION_DOWN:
			clearLastMultiXY();
			actionDownTimes = new Pair<Long, Long>(actionDownTimes.second, System.currentTimeMillis());
			break;
			
		}
		
		oldX = x;
		oldY = y;
		return true;
	}
	
	private void clearPinchDistance() {
		
		oldPointsDistance = 0;
		
	}
	
	private void clearLastMultiXY() {
		
		lastMultiX = lastMultiY = 0;
		
	}
	
	private int getPointsDifference(PointF point1, PointF point2) {
		
		return (int) Math.sqrt((point2.x - point1.x) * (point2.x - point1.x) + (point2.y - point1.y) * (point2.y - point1.y));
		
	}
	
    @SuppressLint("DrawAllocation")
	@Override
    protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec){
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();
        
        if(layoutWidthParam == LayoutParams.MATCH_PARENT || bitmapWidth > measureWidth) {
        	
        	float ratio = (float)bitmapHeight / (float)bitmapWidth;
        	viewWidth = measureWidth;
        	viewHeight = (int) ((float)viewWidth * ratio);
        	
        	if(bitmapWidth > measureWidth) {
            	
            	bitmap = Bitmap.createScaledBitmap(bitmap, viewWidth, viewHeight, false);
            	
            }
        	
        } else {
        	
        	viewWidth = bitmapWidth;
        	viewHeight = bitmapHeight;
        	
        }
        
        viewLength = (int) (((float)viewWidth + (float)viewHeight) / 2F);
        setMeasuredDimension(viewWidth, viewHeight);
        
    }
    
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		float correctedWidth = viewWidth * scale - viewWidth;
		float correctedHeight = viewHeight * scale - viewHeight;
		int correctedWidthHalf = 0;
		int correctedHeightHalf = 0;
		
		if(correctedWidth != 0) {
			
			correctedWidthHalf = (int) (correctedWidth / 2);
			
		}
		
		if(correctedHeight != 0) {
			
			correctedHeightHalf = (int) (correctedHeight / 2);
			
		}
		
		int startX1 = bitmapX - correctedWidthHalf;
		int startY1 = bitmapY - correctedHeightHalf;
		int startX2 = canvas.getWidth() + bitmapX + correctedWidthHalf;
		int startY2 = canvas.getHeight() + bitmapY + correctedHeightHalf;
		rect.set(startX1, startY1, startX2, startY2);
		canvas.drawBitmap(bitmap, null, rect, paint);
		    
	}
	
	private void onDoubleTap() {
		
		switch (drawMode) {
		
		case DRAW_MODE_FIT:
			bitmapX = bitmapY = 0;
			scale = 1F;
			break;
		case DRAW_MODE_MAX:
			scale = maxScale;
			break;
		case DRAW_MODE_MIN:
			scale = minScale;
			break;
		default:
			break;
			
		}
		
		drawMode++;
		
		if(drawMode > DRAW_MODE_MIN) {
			
			drawMode = 0;
			
		}
		
	}
	
}
/***Sample

	// Xml

	<com.sukohi.lib.ZoomImageView
	    android:layout_width="match_parent"
	    android:layout_height="wrap_content"
	    android:src="@drawable/drawable"
	    android:background="@color/black" />
    
    
    // Code
    
	ZoomImageView zoomImageView = (ZoomImageView) findViewById(R.id.zoomimageview);
	zoomImageView.setImageResource(R.drawable.drawable);	// or setImageBitmap(bitmap);
	zoomImageView.setMaxScale(5F);							// skippable(Default:2F)
	zoomImageView.setMinScale(0.1F);						// skippable(Default:0.5F)
    
***/
