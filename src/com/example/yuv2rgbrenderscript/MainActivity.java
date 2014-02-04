package com.example.yuv2rgbrenderscript;

import java.io.IOException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v8.renderscript.RenderScript;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;


public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback, 
	SharedPreferences.OnSharedPreferenceChangeListener {


	private static final String TAG = "MainActivity";

	private Camera mCamera;
	private RenderScript mRs ;
	private SurfaceHolder mHolder;
	private int mBufferSize ;
	private SurfaceView mPreview;
	private int mFrameCount;
	private String mViewType  ;



	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Hide the window title.
		final Window window = getWindow();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mCamera = Camera.open();
		mRs = RenderScript.create(this); 

		mPreview = (SurfaceView) findViewById(R.id.camera_preview);

		mHolder = mPreview.getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		//mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
		//mHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);//SURFACE_TYPE_NORMAL
	}



	@Override
	protected void onResume() {
		super.onResume();

		if (mCamera == null) {

			try {
				// get the Camera instance
				mCamera = Camera.open(); 
			}
			catch (Exception e){
				// Camera is not available (in use or does not exist)
				Log.d(TAG, "Camera is not available");
				return ;
			}
			
			//mCamera.setPreviewCallback(null);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mCamera != null) {
			//mCamera.setPreviewCallback(null);
			mCamera.setPreviewCallbackWithBuffer(null);
			mHolder.removeCallback(this);
			mCamera.release();
			mCamera = null;
		}
	}




	public void surfaceCreated(SurfaceHolder holder) {

		try {

			if (mCamera != null) {

				Camera.Parameters parameters = mCamera.getParameters();

//				List<int[]> list = parameters.getSupportedPreviewFpsRange();
//				int minFps = list.get(0)[0] ;
//				int maxFps = list.get(0)[1] ;
//				parameters.setPreviewFpsRange (minFps, maxFps) ;
//				mCamera.setParameters(parameters) ;

				
				
				//List<Integer> list =  parameters.getSupportedPictureFormats() ;
				Size previewSize = parameters.getPreviewSize();

				int imageFormat = parameters.getPreviewFormat();
				// Assume camera preview format is YUV!!
				if (imageFormat != ImageFormat.NV21) {
					throw new UnsupportedOperationException();
				}

				// Buffer size in bytes W * H * 4
				mBufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(imageFormat) / 8;
				// Allocate one buffer for the preview usage
				byte[] mCallbackBuffer = new byte[mBufferSize];

				mCamera.setPreviewCallbackWithBuffer(null);
				mCamera.setPreviewCallbackWithBuffer(this); 
				mCamera.addCallbackBuffer(mCallbackBuffer);

				//mPreview.setWillNotDraw(false);
				mCamera.setPreviewDisplay(holder);
				// Start the camera preview
				mCamera.startPreview();

			}

		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}




	// onPreviewFrame is called for each frame at the camera preview.
	// We'll convert each frame data (byte[] data) from YUV to RGB and apply 
	// some effect like blur or gray scale

	@Override
	public void onPreviewFrame(byte[] yuvFrame, Camera camera) {

		if (yuvFrame == null || mHolder == null) {
			return;
		}
		// Skip some preview frames for better performance in preview
		if (mFrameCount % 2 != 0) {
			camera.addCallbackBuffer(yuvFrame);
			mFrameCount++;
			return;
		}
		int expectedBytes = mBufferSize ;
		if (expectedBytes != yuvFrame.length) {
			Log.e(TAG, "Mismatched size of buffer!  ");
			return;
		}

		mFrameCount++;
		Canvas canvas = null;

		try {

			// lockCanvas returns null and throws an exception - might be an android issue - http://stackoverflow.com/search?q=SurfaceHolder.lockCanvas%28%29+returns+null
			// Would be better to draw directly on the preview canvas instead of using an imageView ...
			//canvas = mHolder.lockCanvas(null);

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;


			SharedPreferences prefs = getPreferences(0); 
			mViewType = prefs.getString("ViewType", "BlurView");
			 
//			if ( mViewType.equals("NormalView") ){
//				
//				mCamera.stopPreview();
//				//mCamera.setPreviewDisplay(mHolder);
//				mCamera.startPreview();
//				camera.addCallbackBuffer(yuvFrame);
//				return ;
//				
//			}


			//Bitmap rgbBitmap = RenderScriptHelper.convertYuvToRgb(mRs,data,imageSize);
			Bitmap rgbBitmap = RenderScriptHelper.convertYuvToRgbIntrinsic(mRs,yuvFrame,imageSize );
			
			Bitmap outBitmap  = null; 
			if ( mViewType.equals("BlurView") ){
				outBitmap = RenderScriptHelper.applyBlurEffectIntrinsic(mRs,rgbBitmap,imageSize );

			}
			else {
				outBitmap = RenderScriptHelper.applyGrayScaleEffectIntrinsic(mRs,rgbBitmap, imageSize);
				
			}

			

			// Create an ImageView and set the output bitmap after manipulation (blur or gray scale) into it
			ImageView image = new ImageView(this);
			image.setImageBitmap(outBitmap) ;
			// display the imageView 
			setContentView(image) ;

			//new PreviewAsyncTask().execute(data);// didn't see any improvement when running in Async task

			//canvas.drawBitmap(rgbBitmap, 0f, 0f, null) ;

		}   finally {
			if (canvas != null) {
				mHolder.unlockCanvasAndPost(canvas);
			}
		}

		camera.addCallbackBuffer(yuvFrame);

	}



	public void surfaceDestroyed(SurfaceHolder holder) {

		// is called when calling setContentView, so for now disable it
		//		if (mCamera != null) {
		//			mCamera.setPreviewCallbackWithBuffer(null);
		//			mCamera.release();
		//			mCamera = null;
		//		}
	}


	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

//		try {
//			
//			
//			
//			// stop preview before making changes
//			mCamera.stopPreview();
//			mCamera.setPreviewDisplay(mHolder);
//			mCamera.startPreview();
//
//		} catch (Exception e){
//			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
//		}
	}




	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		OptionsMenu.createMenu(this, menu);
		return super.onCreateOptionsMenu(menu);
		// Inflate the menu; this adds items to the action bar if it is present.
		//getMenuInflater().inflate(R.menu.main, menu);

		//return true;
	}



	// Not using that for now
	private class PreviewAsyncTask extends AsyncTask<byte[], Void, Bitmap> {

		protected Bitmap doInBackground(byte[]... args) {
			byte[] data = args[0];

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;

			//Bitmap rgbBitmap = RenderScriptHelper.convertYuvToRgb(mRs,data,imageSize);
			Bitmap rgbBitmap = RenderScriptHelper.convertYuvToRgbIntrinsic(mRs,data,imageSize );
			Bitmap blurBitmap = RenderScriptHelper.applyBlurEffectIntrinsic(mRs,rgbBitmap,imageSize );
			Bitmap grayScaleBitmap = RenderScriptHelper.applyGrayScaleEffectIntrinsic(mRs,blurBitmap, imageSize);
			return grayScaleBitmap ;
		}

		protected void onPostExecute(Bitmap blurBitmap) {

			ImageView image = new ImageView(getApplicationContext());
			image.setImageBitmap(blurBitmap) ;
			setContentView(image) ;

		}

	}

	
	
	   @Override
	    public void onSharedPreferenceChanged(SharedPreferences preferences, String s) {
	        //configure(preferences);
	        
	        String mViewType = preferences.getString("ViewType", "BlureView");
	    }

	 




}
