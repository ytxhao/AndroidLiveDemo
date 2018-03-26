package com.ytx.live.pusher;

import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PreviewCallback;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.WindowManager;

import com.ytx.live.jni.PushNative;
import com.ytx.live.params.VideoParam;

public class VideoPusher extends Pusher implements Callback, PreviewCallback{

	private SurfaceHolder surfaceHolder;
	private Camera mCamera;
	private VideoParam videoParams;
	private byte[] buffers;
	private boolean isPushing = false;
	private PushNative pushNative;
	private WeakReference<Activity> mWeakReferenceActivity;
	public VideoPusher(SurfaceHolder surfaceHolder, VideoParam videoParams, PushNative pushNative,WeakReference<Activity> mWeakReferenceActivity) {
		this.surfaceHolder = surfaceHolder;
		this.videoParams = videoParams;
		this.pushNative = pushNative;
		this.mWeakReferenceActivity = mWeakReferenceActivity;
		surfaceHolder.addCallback(this);
	}

	@Override
	public void startPush() {
		//设置视频参数
		pushNative.setVideoOptions(videoParams.getWidth(), 
				videoParams.getHeight(), videoParams.getBitrate(), videoParams.getFps());
		isPushing = true;
	}

	@Override
	public void stopPush() {
		isPushing = false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		startPreview();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		
	}
	
	@Override
	public void release() {
		stopPreview();
	}


	/**
	 * 切换摄像头
	 */
	public void switchCamera() {
		if(videoParams.getCameraId() == CameraInfo.CAMERA_FACING_BACK){
			videoParams.setCameraId(CameraInfo.CAMERA_FACING_FRONT);
		}else{
			videoParams.setCameraId(CameraInfo.CAMERA_FACING_BACK);
		}
		//重新预览
		stopPreview();
		startPreview();
	}

	// 获取当前窗口管理器显示方向
	private int getDisplayOrientation(){
		Activity mActivity = mWeakReferenceActivity.get();
		if(mActivity == null){
			return 0;
		}
		WindowManager windowManager = mActivity.getWindowManager();
		Display display = windowManager.getDefaultDisplay();
		int rotation = display.getRotation();
		int degrees = 0;
		switch (rotation){
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

		android.hardware.Camera.CameraInfo camInfo =
				new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, camInfo);

		// 这里其实还是不太懂：为什么要获取camInfo的方向呢？相当于相机标定？？
		int result = (camInfo.orientation - degrees + 360) % 360;

		return result;
	}

	/**
	 * 开始预览
	 */
	private void startPreview() {
		try {
			//SurfaceView初始化完成，开始相机预览
			mCamera = Camera.open(videoParams.getCameraId());
			Camera.Parameters parameters = mCamera.getParameters();
			//设置相机参数
			parameters.setPreviewFormat(ImageFormat.NV21); //YUV 预览图像的像素格式
			parameters.setPreviewSize(videoParams.getWidth(), videoParams.getHeight()); //预览画面宽高
			parameters.setPictureSize(videoParams.getWidth(),videoParams.getHeight());
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //连续自动对焦
			mCamera.setParameters(parameters);
			int rotation = getDisplayOrientation(); //获取当前窗口方向

			mCamera.setDisplayOrientation(rotation); //设定相机显示方向
			//parameters.setPreviewFpsRange(videoParams.getFps()-1, videoParams.getFps());
			mCamera.setPreviewDisplay(surfaceHolder);
			//获取预览图像数据
			buffers = new byte[videoParams.getWidth() * videoParams.getHeight() * 4];
			mCamera.addCallbackBuffer(buffers);
			mCamera.setPreviewCallbackWithBuffer(this);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 停止预览
	 */
	private void stopPreview() {
		if(mCamera != null){			
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if(mCamera != null){
			mCamera.addCallbackBuffer(buffers);
		}
		
		if(isPushing){
			//回调函数中获取图像数据，然后给Native代码编码
			pushNative.fireVideo(data);
		}
	}


}
