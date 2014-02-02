package com.example.yuv2rgbrenderscript;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.AsyncTask;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Type;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private static final String TAG = "CameraPreview";

	private SurfaceHolder mHolder;
	private Camera mCamera;

	private int mBufferSize ;

	private int[] mRgb ;

	//Size mPreviewSize;
	List<Size> mSupportedPreviewSizes;

	private RenderScript mRs ;


	Context mContext ;





	//
	//    private void setupCallback(Camera camera, FrameCatcher catcher, int bufferSize) {
	//        camera.setPreviewCallbackWithBuffer(null);
	//        camera.setPreviewCallbackWithBuffer(catcher);
	//        for (int i = 0; i < NUM_CAMERA_PREVIEW_BUFFERS; i++) {
	//            byte [] cameraBuffer = new byte[bufferSize];
	//            camera.addCallbackBuffer(cameraBuffer);
	//        }
	//    }


	public CameraPreview(Context context, Camera camera) {

		super(context);
		mCamera = camera;
		mContext = context ;

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mRs = RenderScript.create(context); 

	}


	public void surfaceCreated(SurfaceHolder holder) {

		try {

			if (mCamera != null) {

				Camera.Parameters parameters = mCamera.getParameters();

				int format = parameters.getPreviewFormat();
				List<Integer> list =  parameters.getSupportedPictureFormats() ;
				//mCamera.setParameters(parameters);

				Size previewSize = parameters.getPreviewSize();

				int imageFormat = parameters.getPreviewFormat();

				// Assume camera preview format is YUV
				if (imageFormat != ImageFormat.NV21) {
					throw new UnsupportedOperationException();
				}
				mBufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(imageFormat) / 8;

				byte[] mCallbackBuffer = new byte[mBufferSize];

				mCamera.setPreviewCallbackWithBuffer(null);
				mCamera.setPreviewCallbackWithBuffer(this); 
				mCamera.addCallbackBuffer(mCallbackBuffer);


				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			}

		} catch (IOException e) {
			Log.d(TAG, "Error setting camera preview: " + e.getMessage());
		}
	}

	
	  private class ProcessPreviewDataTask extends AsyncTask<byte[], Void, Boolean> {
		  
	        protected Boolean doInBackground(byte[]... args) {
	            byte[] data = args[0];
	            //Size imageSize = args[1];

	            long t1 = java.lang.System.currentTimeMillis();

	            //mFilterYuv.execute(data);
	            
	        	Canvas canvas = null;

	    		canvas = mHolder.lockCanvas();
	            
	            Camera.Parameters parameters = mCamera.getParameters();	    		
	    		Size imageSize = parameters.getPreviewSize() ;
	            Bitmap rgbBitmap = convertYuvToRgb(data,imageSize);

	            long t2 = java.lang.System.currentTimeMillis();
	 

	            mCamera.addCallbackBuffer(data);
	            //mProcessInProgress = false;
	            return true;
	        }

	        protected void onPostExecute(Boolean result) {
	           // mOutputView.invalidate();
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

		canvas = mHolder.lockCanvas();
		try {
			synchronized (mHolder) {

				//int canvasWidth = canvas.getWidth();
				// int canvasHeight = canvas.getHeight();

				// byte[] rgbByteArray = convertYuvToRgb(data,camera);
				final float x = 0 ;
				final float y = 0 ;

				
				new ProcessPreviewDataTask().execute(data);

				//Camera.Parameters parameters = mCamera.getParameters();
				//Size size = parameters.getPreviewSize();

				//mRgb = new int[size.width * size.height];
				//Yuv420.decode(data, mRgb, size.width, size.height);

				//canvas.drawBitmap(rgbBitmap, x, y, null) ;

				//canvas.drawBitmap(mRgb, 0, size.width, x, y, size.width, size.height, false, null);
			}

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
		// If your preview can change or rotate, take care of those events here.
		// Make sure to stop the preview before resizing or reformatting it.

		if (mHolder.getSurface() == null){
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e){
			// ignore: tried to stop a non-existent preview
		}

		// set preview size and make any resize, rotate or
		// reformatting changes here

		// start preview with new settings
		try {


			mCamera.setPreviewDisplay(mHolder);
			mCamera.startPreview();

		} catch (Exception e){
			Log.d(TAG, "Error starting camera preview: " + e.getMessage());
		}
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


		// Allocation aOutBitmap = Allocation.createFromBitmap(mRS, bitmap);
		Allocation aOut = Allocation.createTyped(mRs, tbo.create(), Allocation.USAGE_SCRIPT);
		//aOut.setSurface(null);

		Type.Builder tbOut = new Type.Builder(mRs, Element.RGBA_8888(mRs));
		tbOut.setX(imageWidth); 
		tbOut.setY(imageHeight);


		//			Bitmap inBitmap = BitmapFactory.decodeByteArray(data, 0, data.length) ;
		//	Bitmap outBitmap = inBitmap.copy(inBitmap.getConfig(), true);

		//Create the context and I/O allocations
		//			final Allocation inData = Allocation.createFromBitmap(mRs, inBitmap,
		//					Allocation.MipmapControl.MIPMAP_NONE,
		//					Allocation.USAGE_SCRIPT);
		//			final Allocation outData = Allocation.createTyped(mRs, inData.getType());


		//				Allocation inData = Allocation.createTyped(mRs, tbIn.create(), Allocation.MipmapControl.MIPMAP_NONE,  Allocation.USAGE_SCRIPT);
		//				Allocation outData = Allocation.createTyped(mRs, tbOut.create(), Allocation.MipmapControl.MIPMAP_NONE,  Allocation.USAGE_SCRIPT );

		//Bitmap outputBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

		// Create the script 
		ScriptC_Yuv2Rgb yuvScript = new ScriptC_Yuv2Rgb(mRs); 
		// Bind to script level
		yuvScript.set_gIn(ain);
		yuvScript.set_width(imageWidth);
		yuvScript.set_height(imageHeight);

		//yuvScript.set_frameSize(previewSize);

		//inData.copyFrom(data);
		yuvScript.forEach_yuvToRgb(ain, aOut);
		//outData.copyTo(outBitmap);


		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);

		//byte[] outBuffer = new byte[mBufferSize];
		aOut.copyTo(outBitmap) ;

		return outBitmap ;

		//aOut.copyTo(outBitmap);

		//		ImageView image = new ImageView(getApplicationContext()) ;
		//		image.setImageBitmap(outBitmap) ;
		//		setContentView(image);



	}














	final static class Yuv420 {

		public  static void decode(byte[] yuv, int[] rgb, int width, int height) {
			final int size = width * height;

			//        Preconditions.checkNotNull(yuv, "yuv");
			//        Preconditions.checkArgument(yuv.length >= size, "buffer 'yuv' size < minimum");
			//        Preconditions.checkNotNull(rgb, "rgb");
			//        Preconditions.checkArgument(rgb.length >= size, "Buffer 'rgb' size < minimum");

			int cr = 0;
			int cb = 0;

			for (int j = 0; j < height; j++) {
				int index = j * width;
				final int halfJ = j >> 1;

			for (int i = 0; i < width; i++) {
				int y = yuv[index];

				if (y < 0) {
					y += 255;
				}

				if ((i & 0x1) != 1) {
					final int offset = size + halfJ * width + (i >> 1) * 2;

					cb = yuv[offset];
					if (cb < 0) {
						cb += 127;
					} else {
						cb -= 128;
					}

					cr = yuv[offset + 1];
					if (cr < 0) {
						cr += 127;
					} else {
						cr -= 128;
					}
				}

				int r = r(y, cr);
				int g = g(y, cr, cb);
				int b = b(y, cb);

				rgb[index++] = 0xff000000 + (b << 16) + (g << 8) + r;
			}
			}
		}

		private static int limit(int c) {
			return Math.max(0, Math.min(c, 255));
		}

		private static int r(int y, int cr) {
			return limit(y + cr + (cr >> 2) + (cr >> 3) + (cr >> 5));
		}

		private static int g(int y, int cr, int cb) {
			return limit(y - (cb >> 2) + (cb >> 4) + (cb >> 5) - (cr >> 1) + (cr >> 3) + (cr >> 4) + (cr >> 5));
		}

		private  static int b(int y, int cb) {
			return limit(y + cb + (cb >> 1) + (cb >> 2) + (cb >> 6));
		}

	}

























}