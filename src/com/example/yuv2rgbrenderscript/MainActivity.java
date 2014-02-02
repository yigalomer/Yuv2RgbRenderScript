package com.example.yuv2rgbrenderscript;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.ScriptIntrinsicColorMatrix;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
//import android.renderscript.Allocation;
//import android.renderscript.Element;
//import android.renderscript.RenderScript;
//import android.renderscript.Type;




public class MainActivity extends Activity implements SurfaceHolder.Callback, Camera.PreviewCallback{


	private static final String TAG = "MainActivity";

	private Camera mCamera;
	private RenderScript mRs ;

	private SurfaceHolder mHolder;

	private int mBufferSize ;

	private SurfaceView mPreview;

	private int mFrameCount;



	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		// Hide the window title.
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mRs = RenderScript.create(this); 

		mPreview = (SurfaceView) findViewById(R.id.camera_preview);

		mHolder = mPreview.getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		//mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//SURFACE_TYPE_NORMAL
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_HARDWARE);//SURFACE_TYPE_NORMAL

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

				mPreview.setWillNotDraw(false);
				mCamera.setPreviewDisplay(holder);
				// Start the camera preview
				mCamera.startPreview();

			}

		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}




	// Method is called for each frame in the camera preview.
	// We'll convert the frame data (byte[] data) from YUV to RGB and apply 
	// some effect like blur or gray scale
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		if (data == null || mHolder == null) {
			return;
		}

		// Skip some preview frames for better performance
		if (mFrameCount % 2 != 0) {
			camera.addCallbackBuffer(data);
			mFrameCount++;
			return;
		}

		int expectedBytes = mBufferSize ;
		if (expectedBytes != data.length) {
			Log.e(TAG, "Mismatched size of buffer!  ");
			return;
		}

		mFrameCount++;
		Canvas canvas = null;

		//canvas = mHolder.lockCanvas();
		try {

			// lockCanvas returns null and throws an exception - might be an android issue - http://stackoverflow.com/search?q=SurfaceHolder.lockCanvas%28%29+returns+null
			// Would be better to draw directly on the preview canvas... instead using an imageView to draw 
			//canvas = mHolder.lockCanvas();

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;

			Bitmap rgbBitmap = convertYuvToRgb(data,imageSize);


			//Bitmap rgbBitmap = convertYuvToRgbIntrinsic(data,imageSize );

			//Bitmap grayScaleBitmap = applyGrayScaleEffectIntrinsic(rgbBitmap, imageSize);

			Bitmap blurBitmap = applyBlurEffectIntrinsic(rgbBitmap,imageSize );


			// Create an ImageView and put the output bitmap after manipulation (blur or gray scale) into it
			// and then display that imageView 
			ImageView image = new ImageView(this);
			image.setImageBitmap(blurBitmap) ;
			setContentView(image) ;

			//new PreviewAsyncTask().execute(data);// didn't see improvements when running in Async task
			//canvas.drawBitmap(rgbBitmap, 0f, 0f, null) ;

		}   finally {
			if (canvas != null) {
				mHolder.unlockCanvasAndPost(canvas);
			}
		}

		camera.addCallbackBuffer(data);

	}



	public void surfaceDestroyed(SurfaceHolder holder) {


		//		if (mCamera != null) {
		//			mCamera.setPreviewCallbackWithBuffer(null);
		//			mCamera.release();
		//			mCamera = null;
		//		}
		//		if (holder != null){
		//			mHolder.removeCallback(this);
		//		}
	}


	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		try {
			// stop preview before making changes
			mCamera.stopPreview();

			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
	}


	private Bitmap applyGrayScaleEffectIntrinsic(Bitmap inBitmap, Size imageSize) {

		Bitmap grayBitmap = inBitmap.copy(inBitmap.getConfig(), true);

		Allocation aIn = Allocation.createFromBitmap(mRs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		Allocation aOut = Allocation.createTyped(mRs, aIn.getType());

		//Make the image grey scale
		final ScriptIntrinsicColorMatrix scriptColor = ScriptIntrinsicColorMatrix.create(mRs, Element.U8_4(mRs));
		scriptColor.setGreyscale();
		scriptColor.forEach(aIn, aOut);
		aOut.copyTo(grayBitmap);

		return grayBitmap ;

	}




	private Bitmap applyBlurEffectIntrinsic(Bitmap inBitmap, Size imageSize) {

		Bitmap outBitmap = inBitmap.copy(inBitmap.getConfig(), true);

		Allocation aIn = Allocation.createFromBitmap(mRs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		Allocation aOut = Allocation.createTyped(mRs, aIn.getType());

		//Blur the image
		ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(mRs, Element.U8_4(mRs));
		script.setRadius(15f);
		script.setInput(aIn);
		script.forEach(aOut);
		aOut.copyTo(outBitmap);

		return outBitmap ;
	}


	// Convert to RGB using Intrinsic render script
	private Bitmap convertYuvToRgbIntrinsic(byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(mRs, Element.RGBA_8888(mRs));

		// Create the input allocation
		Type.Builder yuvType = new Type.Builder(mRs, Element.U8(mRs))
		.setX(imageWidth)
		.setY(imageHeight)
		.setYuvFormat(android.graphics.ImageFormat.NV21);

		Allocation aIn = Allocation.createTyped(mRs, yuvType.create(), Allocation.USAGE_SCRIPT);
		// Set the YUV frame data into the input allocation
		aIn.copyFrom(data);


		// Create the output allocation
		Type.Builder rgbType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
		.setX(imageWidth)
		.setY(imageHeight);

		Allocation aOut = Allocation.createTyped(mRs, rgbType.create(), Allocation.USAGE_SCRIPT);



		yuvToRgbIntrinsic.setInput(aIn);
		// Run the script for every pixel on the input allocation and put the result in aOut
		yuvToRgbIntrinsic.forEach(aOut);

		// Create an output bitmap and copy the result into that bitmap
		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		aOut.copyTo(outBitmap);

		return outBitmap ;

	}



	// Convert to RGB using render script - with Yuv2Rgb.rs
	protected Bitmap convertYuvToRgb(byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		// Input allocation
		Type.Builder yuvType = new Type.Builder(mRs, Element.createPixel(mRs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV))
		.setX(imageWidth)
		.setY(imageHeight)
		.setMipmaps(false)
		.setYuvFormat(ImageFormat.NV21);
		Allocation ain = Allocation.createTyped(mRs, yuvType.create(), Allocation.USAGE_SCRIPT);
		ain.copyFrom(data);


		// output allocation
		Type.Builder rgbType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
		.setX(imageWidth)
		.setY(imageHeight)
		.setMipmaps(false);

		Allocation aOut = Allocation.createTyped(mRs, rgbType.create(), Allocation.USAGE_SCRIPT);


		// Create the script 
		ScriptC_Yuv2Rgb yuvScript = new ScriptC_Yuv2Rgb(mRs); 
		// Bind to script level -  set the allocation input and parameters from the java into the script level (thru JNI)
		yuvScript.set_gIn(ain);
		yuvScript.set_width(imageWidth);
		yuvScript.set_height(imageHeight);

		// invoke the script conversion method
		yuvScript.forEach_yuvToRgb(ain, aOut);

		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		aOut.copyTo(outBitmap) ;

		return outBitmap ;

	}


	// Not using that for now
	private class PreviewAsyncTask extends AsyncTask<byte[], Void, Bitmap> {

		protected Bitmap doInBackground(byte[]... args) {
			byte[] data = args[0];

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;

			//Bitmap rgbBitmap = convertYuvToRgb(data,imageSize);
			Bitmap rgbBitmap = convertYuvToRgbIntrinsic(data,imageSize );
			Bitmap blurBitmap = applyBlurEffectIntrinsic(rgbBitmap,imageSize );
			Bitmap grayScaleBitmap = applyGrayScaleEffectIntrinsic(blurBitmap, imageSize);


			//canvas.drawBitmap(rgbBitmap, x, y, null) ;

			//mCamera.addCallbackBuffer(data);
			return grayScaleBitmap ;
		}

		protected void onPostExecute(Bitmap blurBitmap) {

			// invalidate();
			ImageView image = new ImageView(getApplicationContext());
			image.setImageBitmap(blurBitmap) ;
			setContentView(image) ;

		}

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}



}
