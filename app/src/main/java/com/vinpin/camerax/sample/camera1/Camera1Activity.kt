package com.vinpin.camerax.sample.camera1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.ImageFormat
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.vinpin.camerax.sample.R
import com.vinpin.livedatapermissions.LiveDataPermissions
import com.vinpin.livedatapermissions.PermissionDeny
import com.vinpin.livedatapermissions.PermissionGrant
import kotlinx.android.synthetic.main.activity_camera_1.*

/**
 * author : VinPin
 * e-mail : hearzwp@163.com
 * time   : 2020/8/26 14:02
 * desc   : 基于Camera1自定义相机拍摄
 */
class Camera1Activity : AppCompatActivity() {

    private var mSurfaceHolder: SurfaceHolder? = null

    private var mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK

    private var mCamera: Camera? = null

    private var mParameters: Camera.Parameters? = null

    /** 预览旋转的角度 */
    private var mDisplayOrientation: Int = 0

    /** 开启闪光灯 */
    private var mLightOpen: Boolean = false

    companion object {

        fun newIntent(context: Context): Intent {
            return Intent(context, Camera1Activity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_1)
        LiveDataPermissions(this).request(Manifest.permission.CAMERA).observe(this) {
            when (it) {
                is PermissionGrant -> initSurfaceHolder()
                is PermissionDeny -> onBackPressed()
            }
        }

        img_flash.setOnClickListener {
            val newLightOpen = !mLightOpen
            if (switchLight(newLightOpen)) {
                mLightOpen = newLightOpen
                img_flash.setImageResource(if (mLightOpen) R.drawable.ic_flash_on else R.drawable.ic_flash_off)
            } else Toast.makeText(this, "切换闪光灯失败~", Toast.LENGTH_SHORT).show()
        }
        img_flip.setOnClickListener { switchCamera() }
    }

    override fun onDestroy() {
        releaseCamera()
        super.onDestroy()
    }

    private fun initSurfaceHolder() {
        mSurfaceHolder = surfaceView.holder
        mSurfaceHolder?.addCallback(object : SurfaceHolder.Callback {

            override fun surfaceCreated(holder: SurfaceHolder) {
                //surface创建时执行
                //打开相机并设置参数
                openCamera(mCameraId)
                //开始预览
                startPreview()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                //surface绘制时执行
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                //surface销毁时执行
                releaseCamera()
            }
        })
    }

    /**
     * 打开相机
     */
    private fun openCamera(cameraId: Int) {
        if (!isSupport(cameraId)) {
            Toast.makeText(this, "当前手机不支持该摄像头~", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            mCamera = Camera.open(cameraId)
            // 设置相机具体参数
            initParameters(mCamera)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "打开相机失败~", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 开始预览
     */
    private fun startPreview() {
        try {
            //根据所传入的SurfaceHolder对象来设置实时预览
            mCamera?.setPreviewDisplay(mSurfaceHolder)
            //设置正确的预览显示方向
            setCameraDisplayOrientation(this, mCameraId, mCamera)
            //开始预览
            mCamera?.startPreview()
            //开启人脸检测，执行在startPreview之后。
            startFaceDetection()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 开启人脸检测
     */
    private fun startFaceDetection() {
        try {
            mCamera?.startFaceDetection()
            mCamera?.setFaceDetectionListener { faces, camera ->
                Log.d("test", ">>> 检测到${faces?.size}个人脸")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 释放资源
     */
    private fun releaseCamera() {
        mCamera?.stopPreview()
        mCamera?.setPreviewCallback(null)
        mCamera?.release()
        mCamera = null
    }

    /**
     * 判断是否支持对焦模式
     */
    private fun isSupportFocus(parameters: Camera.Parameters, focusMode: String): Boolean {
        //获取所支持对焦模式
        val listFocus: List<String> = parameters.supportedFocusModes
        for (element in listFocus) {
            if (element == focusMode) {
                return true
            }
        }
        return false
    }

    /**
     * 判断是否支持某个相机
     */
    private fun isSupport(cameraId: Int): Boolean {
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == cameraId) {
                return true
            }
        }
        return false
    }

    /**
     * 设置相机具体参数
     */
    private fun initParameters(camera: Camera?) {
        try {
            mParameters = camera?.parameters
            //设置预览格式
            mParameters?.previewFormat = ImageFormat.NV21
            //连续自动对焦图像
            if (isSupportFocus(mParameters!!, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mParameters?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else if (isSupportFocus(mParameters!!, Camera.Parameters.FOCUS_MODE_AUTO)) {
                //自动对焦(单次)
                mParameters?.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            mCamera?.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置相机具体参数失败~", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置正确的预览显示方向
     */
    private fun setCameraDisplayOrientation(
        appCompatActivity: AppCompatActivity,
        cameraId: Int,
        camera: Camera?
    ) {
        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)
        val degree = when (appCompatActivity.windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        var result: Int
        //计算图像所要旋转的角度
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (cameraInfo.orientation + degree) % 360
            result = (360 - result) % 360
        } else {
            result = (cameraInfo.orientation - degree + 360) % 360
        }
        mDisplayOrientation = result
        //调整预览图像旋转角度
        camera?.setDisplayOrientation(result)
    }

    // ======================================

    /**
     * 前后摄像切换
     */
    private fun switchCamera() {
        releaseCamera()
        mCameraId = (mCameraId + 1) % Camera.getNumberOfCameras()
        openCamera(mCameraId)
        startPreview()
    }

    /**
     * 闪光灯切换
     */
    private fun switchLight(open: Boolean): Boolean {
        return try {
            val parameters = mCamera?.parameters
            parameters?.flashMode =
                if (open) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
            mCamera?.parameters = parameters
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}