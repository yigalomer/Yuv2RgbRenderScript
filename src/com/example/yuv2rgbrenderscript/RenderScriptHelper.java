package com.example.yuv2rgbrenderscript;

import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera.Size;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.ScriptIntrinsicBlur;
import android.support.v8.renderscript.ScriptIntrinsicColorMatrix;
import android.support.v8.renderscript.ScriptIntrinsicYuvToRGB;
import android.support.v8.renderscript.Type;

final class RenderScriptHelper {



	// Convert to RGB using Intrinsic render script
	public static Bitmap convertYuvToRgbIntrinsic(RenderScript rs,byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.RGBA_8888(rs));

		// Create the input allocation  memory for Renderscript to work with
		Type.Builder yuvType = new Type.Builder(rs, Element.U8(rs))
		.setX(imageWidth)
		.setY(imageHeight)
		.setYuvFormat(android.graphics.ImageFormat.NV21);

		Allocation aIn = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
		// Set the YUV frame data into the input allocation
		aIn.copyFrom(data);


		// Create the output allocation
		Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs))
		.setX(imageWidth)
		.setY(imageHeight);

		Allocation aOut = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);



		yuvToRgbIntrinsic.setInput(aIn);
		// Run the script for every pixel on the input allocation and put the result in aOut
		yuvToRgbIntrinsic.forEach(aOut);

		// Create an output bitmap and copy the result into that bitmap
		Bitmap outBitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888);
		aOut.copyTo(outBitmap);

		return outBitmap ;

	}



	// Convert to RGB using render script - with Yuv2Rgb.rs
	public static Bitmap convertYuvToRgb(RenderScript rs,byte[] data, Size imageSize) {

		int imageWidth = imageSize.width ;
		int imageHeight = imageSize.height ;

		// Input allocation
		Type.Builder yuvType = new Type.Builder(rs, Element.createPixel(rs, Element.DataType.UNSIGNED_8, Element.DataKind.PIXEL_YUV))
		.setX(imageWidth)
		.setY(imageHeight)
		.setMipmaps(false)
		.setYuvFormat(ImageFormat.NV21);
		Allocation ain = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);
		ain.copyFrom(data);


		// output allocation
		Type.Builder rgbType = new Type.Builder(rs, Element.RGBA_8888(rs))
		.setX(imageWidth)
		.setY(imageHeight)
		.setMipmaps(false);

		Allocation aOut = Allocation.createTyped(rs, rgbType.create(), Allocation.USAGE_SCRIPT);


		// Create the script 
		ScriptC_Yuv2Rgb yuvScript = new ScriptC_Yuv2Rgb(rs); 
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



	


	public static Bitmap applyBlurEffectIntrinsic(RenderScript rs,Bitmap inBitmap, Size imageSize) {

		Bitmap outBitmap = inBitmap.copy(inBitmap.getConfig(), true);

		Allocation aIn = Allocation.createFromBitmap(rs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		Allocation aOut = Allocation.createTyped(rs, aIn.getType());

		//Blur the image
		ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
		// Set the blur radius
		script.setRadius(15f);
		script.setInput(aIn);
		script.forEach(aOut);
		aOut.copyTo(outBitmap);

		return outBitmap ;
	}
	
	
	
	public static Bitmap applyGrayScaleEffectIntrinsic(RenderScript rs,Bitmap inBitmap, Size imageSize) {

		Bitmap grayBitmap = inBitmap.copy(inBitmap.getConfig(), true);

		Allocation aIn = Allocation.createFromBitmap(rs, inBitmap,Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
		Allocation aOut = Allocation.createTyped(rs, aIn.getType());

		//Make the image grey scale
		final ScriptIntrinsicColorMatrix scriptColor = ScriptIntrinsicColorMatrix.create(rs, Element.U8_4(rs));
		scriptColor.setGreyscale();
		scriptColor.forEach(aIn, aOut);
		aOut.copyTo(grayBitmap);

		return grayBitmap ;

	}


}
