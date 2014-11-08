package com.sobin.yuvrecorder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;

public class Activity_RGB extends Activity implements SurfaceHolder.Callback {

	private SurfaceView mSurfaceView = null;
	private SurfaceHolder mSurfaceHolder = null;
	private Camera mCamera = null;
	// 旋转角度
	static int mRotation = 0;
	private boolean mPreviewRunning = false;

	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.activity_realtime_face);

		mSurfaceView = (SurfaceView) findViewById(R.id.infoSurfaceView);

		LayoutParams lp = mSurfaceView.getLayoutParams();
		lp.height = PhoneUtils.getScreenSizeArray(this)[0];
		lp.width = lp.height * 480 / 640;
		mSurfaceView.setLayoutParams(lp);
		mSurfaceHolder = mSurfaceView.getHolder();
		mSurfaceHolder.addCallback(this);
		mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (mPreviewRunning) {
			mCamera.stopPreview();
		}

		Camera.Parameters p = mCamera.getParameters();

		p.setPreviewSize(640, 480);
		p.setPictureSize(640, 480);
		p.setPreviewFrameRate(15);
		mCamera.setPreviewCallback(new VideoData(640, 480));
		mCamera.setParameters(p);
		setCameraPreviewOrientation((Activity) this, 1, mCamera); // FIXME:
																	// remove

		try {
			mCamera.setPreviewDisplay(holder);
		} catch (Exception e) {
			e.printStackTrace();
		}

		mCamera.startPreview();
		mPreviewRunning = true;

	}

	private void setCameraPreviewOrientation(Activity activity, int cameraId,
			android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
		mRotation = info.orientation;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open(1);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v("AndroidCamera", "surfaceDestroyed");
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mPreviewRunning = false;
			mCamera.release();
			mCamera = null;
		}

	}
}

class VideoData implements Camera.PreviewCallback {

	RandomAccessFile raf = null;
	byte[] h264Buff = null;
	private static int PIXEL_WIDTH = 640;// 1280,800,768,720,640,576,480,384,352
	private static int PIXEL_HEIGHT = 480;// 720,480,432,480,480,432,320,288,288
	// 旋转后的数据
	private byte[] rotateData;
	private Bitmap bmp;
	public VideoData(int width, int height) {
		Log.v("androidCamera", "new VideoData");
		h264Buff = new byte[width * height * 8];
		
		try {
			Log.v("androidCamera", "Create File: /sdcard/camera.dat start");
			File file = new File("/sdcard/camera.rbg");
			Log.v("androidCamera", "Create File: /sdcard/camera.dat end");
			raf = new RandomAccessFile(file, "rw");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if (data == null) {
			return;
		}
		int width = PIXEL_WIDTH;
		int height = PIXEL_HEIGHT;
		rotateData = new byte[PIXEL_WIDTH * PIXEL_HEIGHT * 2];
		// 校正旋转相片
		switch (Activity_RGB.mRotation) {
		case 0:
			width = PIXEL_WIDTH;
			height = PIXEL_HEIGHT;
			System.arraycopy(data, 0, rotateData, 0, data.length);
			break;
		case 90:
			width = PIXEL_HEIGHT;
			height = PIXEL_WIDTH;
			PhoneUtils.rotateYuvData(rotateData, data, PIXEL_WIDTH,
					PIXEL_HEIGHT, 0);
			break;
		case 270:
			width = PIXEL_HEIGHT;
			height = PIXEL_WIDTH;
			PhoneUtils.rotateYuvData(rotateData, data, PIXEL_WIDTH,
					PIXEL_HEIGHT, 1);
			break;
		}
		
//		savenv21topic();
		int previewWidth = camera.getParameters().getPreviewSize().width;
		int previewHeight = camera.getParameters().getPreviewSize().height;
		System.out.println(previewHeight);
		System.out.println(previewWidth);
		byte[] rgbBuffer = new byte[previewWidth * previewHeight * 3];

		decodeYUV420SP(rgbBuffer, rotateData, previewWidth, previewHeight);

		try {
			raf.write(rgbBuffer, 0, rotateData.length);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	protected void finalize() {
		if (null != raf) {
			try {
				raf.close();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		try {
			super.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void decodeYUV420SP(byte[] rgbBuf, byte[] yuv420sp, int width,
			int height) {
		final int frameSize = width * height;
		if (rgbBuf == null)
			throw new NullPointerException("buffer 'rgbBuf' is null");
		if (rgbBuf.length < frameSize * 3)
			throw new IllegalArgumentException("buffer 'rgbBuf' size "
					+ rgbBuf.length + " < minimum " + frameSize * 3);

		if (yuv420sp == null)
			throw new NullPointerException("buffer 'yuv420sp' is null");

		if (yuv420sp.length < frameSize * 3 / 2)
			throw new IllegalArgumentException("buffer 'yuv420sp' size "
					+ yuv420sp.length + " < minimum " + frameSize * 3 / 2);

		int i = 0, y = 0;
		int uvp = 0, u = 0, v = 0;
		int y1192 = 0, r = 0, g = 0, b = 0;

		for (int j = 0, yp = 0; j < height; j++) {
			uvp = frameSize + (j >> 1) * width;
			u = 0;
			v = 0;
			for (i = 0; i < width; i++, yp++) {
				y = (0xff & ((int) yuv420sp[yp])) - 16;
				if (y < 0)
					y = 0;
				if ((i & 1) == 0) {
					v = (0xff & yuv420sp[uvp++]) - 128;
					u = (0xff & yuv420sp[uvp++]) - 128;
				}

				y1192 = 1192 * y;
				r = (y1192 + 1634 * v);
				g = (y1192 - 833 * v - 400 * u);
				b = (y1192 + 2066 * u);

				if (r < 0)
					r = 0;
				else if (r > 262143)
					r = 262143;
				if (g < 0)
					g = 0;
				else if (g > 262143)
					g = 262143;
				if (b < 0)
					b = 0;
				else if (b > 262143)
					b = 262143;

				rgbBuf[yp * 3] = (byte) (r >> 10);
				rgbBuf[yp * 3 + 1] = (byte) (g >> 10);
				rgbBuf[yp * 3 + 2] = (byte) (b >> 10);
			}
		}
	}
	
	//nv21 to jpg
		protected void savenv21topic() {
			
			YuvImage yuvimage = new YuvImage(rotateData,
					ImageFormat.NV21, 480, 640, null);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			yuvimage.compressToJpeg(new Rect(0, 0, 480, 640),
					100, baos);
			bmp = BitmapFactory.decodeByteArray(
					baos.toByteArray(), 0,
					baos.toByteArray().length);
			
			try {
				baos.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
			String strCapImg = Environment
					.getExternalStorageDirectory()
					.getAbsolutePath()
					+ "/12345.jpg";
			File myCaptureFile = new File(strCapImg);
			FileOutputStream outStream = null;
			try {
				outStream = new FileOutputStream(myCaptureFile);
			} catch (FileNotFoundException e1) {

				e1.printStackTrace();
			}
			if (bmp != null) {
				bmp.compress(Bitmap.CompressFormat.JPEG, 100,
						outStream);
				try {

					outStream.flush();
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
		}
}
