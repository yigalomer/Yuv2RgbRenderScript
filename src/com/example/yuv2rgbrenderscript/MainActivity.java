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
	//private CameraPreview mPreview;
	//private PictureCallback mPicture ;
	private RenderScript mRs ;

	private SurfaceHolder mHolder;

	private int mBufferSize ;

	private SurfaceView mPreview;
	//private ImageView mNormalImage, mBlurImage, mColorImage;



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
				mCamera = Camera.open(); // attempt to get a Camera instance
			}
			catch (Exception e){
				// Camera is not available (in use or does not exist)
				Log.d(TAG, "Camera is not available");
				return ;
			}


			mCamera.setPreviewCallback(null);



		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			//mPreview.getHolder().removeCallback(mPreview);
			mCamera.setPreviewCallbackWithBuffer(null);
			mHolder.removeCallback(this);
			mCamera.release();


			mCamera = null;
			//mPreview = null ;
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

				mBufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(imageFormat) / 8;

				byte[] mCallbackBuffer = new byte[mBufferSize];

				mCamera.setPreviewCallbackWithBuffer(null);
				mCamera.setPreviewCallbackWithBuffer(this); 
				mCamera.addCallbackBuffer(mCallbackBuffer);


				mPreview.setWillNotDraw(false);
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();

				//mCamera.setPreviewCallback(this);
				//mBlurImage = (ImageView) findViewById(R.id.blur_preview);
				//mBlurImage.setVisibility(View.GONE);

			}

		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}


	private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Bitmap> {

		protected Bitmap doInBackground(byte[]... args) {
			byte[] data = args[0];
			//Size imageSize = args[1];

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;

			//Bitmap rgbBitmap = convertYuvToRgb(data,imageSize);
			Bitmap rgbBitmap = convertYuvToRgbIntrinsic(data,imageSize );

			Bitmap blurBitmap = applyBlurEffectIntrinsic(rgbBitmap,imageSize );
			
			Bitmap grayScaleBitmap = applyGrayScaleEffectIntrinsic(blurBitmap, imageSize);


			//canvas.drawBitmap(rgbBitmap, x, y, null) ;

			//mCamera.addCallbackBuffer(data);
			//mProcessInProgress = false;
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
	public void onPreviewFrame(byte[] data, Camera camera) {

		if (data == null || mHolder == null) {
			return;
		}

		int expectedBytes = mBufferSize ;

		if (expectedBytes != data.length) {
			Log.e(TAG, "Mismatched size of buffer! Expected ");
			mCamera.setPreviewCallbackWithBuffer(null);
			return;
		}

		Canvas canvas = null;

		//canvas = mHolder.lockCanvas();
		try {
			//synchronized (mHolder) {
			//canvas = mHolder.lockCanvas();

			Camera.Parameters parameters = mCamera.getParameters();	    		
			Size imageSize = parameters.getPreviewSize() ;
			// Bitmap rgbBitmap = convertYuvToRgb(data,imageSize);

			//Bitmap rgbBitmap = convertYuvToRgbIntrinsic(data,imageSize );

			//TEST TEST
			// Bitmap inBitmap = BitmapFactory.decodeByteArray(data, 0, data.length) ;

			// Bitmap inBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
			//Bitmap mutableBitmap = inBitmap.copy (Bitmap.Config.ARGB_8888, true);

			//	            Bitmap inBitmap=null; 
			//	            ByteArrayInputStream bytes = new ByteArrayInputStream(data); 
			//	            BitmapDrawable bmd = new BitmapDrawable(bytes); 
			//	            inBitmap = bmd.getBitmap(); 

			//ByteArrayInputStream imageStream = new ByteArrayInputStream(data);
			//Bitmap inBitmap = BitmapFactory.decodeStream(imageStream);

			//TEST TEST



			//Bitmap blurBitmap = applyBlurEffectIntrinsic(rgbBitmap,imageSize );



			//Make the image greyscale
			//	            final ScriptIntrinsicColorMatrix scriptColor =ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs));
			//	            scriptColor.setGreyscale();
			//	            scriptColor.forEach(input, output);
			//	            output.copyTo(grayBitmap);


			//ImageView image = new ImageView(this);

			//mBlurImage.setVisibility(View.VISIBLE);


			//mBlurImage.setImageBitmap(blurBitmap) ;

			//			ImageView image = new ImageView(this);
			//			image.setImageBitmap(blurBitmap) ;
			//			setContentView(image) ;


			new ProcessPreviewDataTask().execute(data);

			//Camera.Parameters parameters = mCamera.getParameters();
			//Size size = parameters.getPreviewSize();

			//mRgb = new int[size.width * size.height];
			//Yuv420.decode(data, mRgb, size.width, size.height);

			//canvas.drawBitmap(rgbBitmap, x, y, null) ;

			//canvas.drawBitmap(mRgb, 0, size.width, x, y, size.width, size.height, false, null);
			//}

			// canvas.drawBitmap(rgb, 0, size.width, x, y, size.width, size.height, false, paint);
			// drawBitmap (int[] colors, int offset, int stride, float x, float y, int width, int height, boolean hasAlpha, Paint paint)

		}   finally {
			if (canvas != null) {
				mHolder.unlockCanvasAndPost(canvas);
			}
		}

		camera.addCallbackBuffer(data);

	}





	public void surfaceDestroyed(SurfaceHolder holder) {

		//    	   this.getHolder().removeCallback(this);
		//    	   if (mCamera != null) {
		//    		   mCamera.stopPreview();
		//    		   mCamera.release();
		//    	   }

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
		
		//Make the image greyscale
		final ScriptIntrinsicColorMatrix scriptColor =ScriptIntrinsicColorMatrix.create(mRs, Element.U8_4(mRs));
		scriptColor.setGreyscale();
		scriptColor.forEach(aIn, aOut);
		aOut.copyTo(grayBitmap);
		
		return grayBitmap ;

	}




	private Bitmap applyBlurEffectIntrinsic(Bitmap inBitmap, Size imageSize) {

		Bitmap outBitmap = inBitmap.copy(inBitmap.getConfig(), true);
		// Bitmap grayBitmap = inBitmap.copy(inBitmap.getConfig(), true);

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


	private Bitmap convertYuvToRgbIntrinsic(byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(mRs, Element.RGBA_8888(mRs));


		Type.Builder yuvType = new Type.Builder(mRs, Element.U8(mRs))
		.setX(imageWidth)
		.setY(imageHeight)
		.setYuvFormat(android.graphics.ImageFormat.NV21);
		
		Allocation aIn = Allocation.createTyped(mRs, yuvType.create(), Allocation.USAGE_SCRIPT);


		Type.Builder rgbaType = new Type.Builder(mRs, Element.RGBA_8888(mRs))
		.setX(imageWidth)
		.setY(imageHeight);

		Allocation aOut = Allocation.createTyped(mRs, rgbaType.create(), Allocation.USAGE_SCRIPT);

		aIn.copyFrom(data);

		yuvToRgbIntrinsic.setInput(aIn);
		yuvToRgbIntrinsic.forEach(aOut);

		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

		// Bitmap bmpout = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
		aOut.copyTo(outBitmap);

		return outBitmap ;

	}



	// Convert to RGB using render script
	protected Bitmap convertYuvToRgb(byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		Type.Builder tbIn = new Type.Builder(mRs, Element.U8(mRs));
		tbIn.setX(imageWidth);
		tbIn.setY(imageHeight);
		tbIn.setYuvFormat(ImageFormat.NV21);

		Type.Builder tb = new Type.Builder(mRs, Element.createPixel(mRs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV));
		//Type.Builder tb = new Type.Builder(mRs, Element.U8(mRs));
		tb.setX(imageWidth);
		tb.setY(imageHeight);
		tb.setMipmaps(false);
		tb.setYuvFormat(ImageFormat.NV21);
		Allocation ain = Allocation.createTyped(mRs, tb.create(), Allocation.USAGE_SCRIPT);
		ain.copyFrom(data);


		Type.Builder tbo = new Type.Builder(mRs, Element.RGBA_8888(mRs));
		tbo.setX(imageWidth);
		tbo.setY(imageHeight);
		tbo.setMipmaps(false);

		Allocation aOut = Allocation.createTyped(mRs, tbo.create(), Allocation.USAGE_SCRIPT);

		Type.Builder tbOut = new Type.Builder(mRs, Element.RGBA_8888(mRs));
		tbOut.setX(imageWidth); 
		tbOut.setY(imageHeight);


		// Create the script 
		ScriptC_Yuv2Rgb yuvScript = new ScriptC_Yuv2Rgb(mRs); 
		// Bind to script level
		yuvScript.set_gIn(ain);
		yuvScript.set_width(imageWidth);
		yuvScript.set_height(imageHeight);

		yuvScript.forEach_yuvToRgb(ain, aOut);

		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		aOut.copyTo(outBitmap) ;

		return outBitmap ;

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}



}
