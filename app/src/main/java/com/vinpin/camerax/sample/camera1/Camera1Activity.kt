package com.vinpin.camerax.sample.camera1

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.vinpin.camerax.sample.R
import com.vinpin.camerax.sample.utils.FileUtils
import com.vinpin.imageloader.ImageLoader
import com.vinpin.livedatapermissions.LiveDataPermissions
import com.vinpin.livedatapermissions.PermissionDeny
import com.vinpin.livedatapermissions.PermissionGrant
import kotlinx.android.synthetic.main.activity_camera_1.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
        img_take_photo.setOnClickListener { takePicture() }
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
        return listFocus.contains(focusMode)
    }

    /**
     * 判断是否支持某个相机
     */
    private fun isSupport(cameraId: Int): Boolean {
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == cameraId) return true
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
            setPreviewSize(mParameters!!, surfaceView.measuredWidth, surfaceView.measuredHeight)
            setPictureSize(mParameters!!)
            mCamera?.parameters = mParameters
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "设置相机具体参数失败~", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 设置预览界面尺寸
     */
    private fun setPreviewSize(parameters: Camera.Parameters, width: Int, height: Int) {
        val supportedPictureSizes = parameters.supportedPictureSizes
        // 注意点：supportedPictureSizes中的Camera.Size的w是大于h的。比如：1920*1080
        if (width.toFloat() / height == 3.0f / 4) {
            for (i in 0 until supportedPictureSizes.size) {
                val size: Camera.Size = supportedPictureSizes[i]
                if (size.width.toFloat() / size.height == 4.0f / 3) {
                    try {
                        mParameters?.setPreviewSize(size.width, size.height)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    break
                }
            }
            return
        }

        var biggestSize: Camera.Size? = null //最大分辨率
        var fitSize: Camera.Size? = null //最合适的分辨率
        var targetSize: Camera.Size? = null//没有屏幕分辨率就取跟屏幕分辨率相近(大)的size
        var targetSize2: Camera.Size? = null//没有屏幕分辨率就取跟屏幕分辨率相近(小)的size

        Log.i("test", ">>> SurfaceView尺寸: ${height}*${width}")
        for (size in supportedPictureSizes) {
            Log.i("test", ">>> 系统支持的尺寸: ${size.width}*${size.height}")
            // 找出支持的分辨率中最大的分辨率
            if (biggestSize == null || (size.width > biggestSize.width && size.height > biggestSize.height)) {
                biggestSize = size
            }

            if (size.width == height && size.height == width) {
                fitSize = size
            } else if (size.width == height || size.height == width) {
                if (targetSize == null) {
                    targetSize = size
                } else if (size.width < height || size.height < width) {
                    targetSize2 = size
                }
            }
        }

        if (fitSize == null) fitSize = targetSize
        if (fitSize == null) fitSize = targetSize2
        if (fitSize == null) fitSize = biggestSize

        Log.i("test", ">>> 最佳预览尺寸:：${fitSize!!.width}*${fitSize.height}")
        try {
            parameters.setPreviewSize(fitSize!!.width, fitSize.height)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置保存图片的尺寸
     */
    private fun setPictureSize(parameters: Camera.Parameters) {
        val previewSize = parameters.previewSize
        val previewSizeScale =
            if (previewSize != null) previewSize.width.toFloat() / previewSize.height else 0
        val supportedPictureSizes = parameters.supportedPictureSizes

        var biggestSize: Camera.Size? = null //最大分辨率
        var fitSize: Camera.Size? = null //最合适的分辨率

        for (size in supportedPictureSizes) {
            // 找出支持的分辨率中最大的分辨率
            if (biggestSize == null || (size.width > biggestSize.width && size.height > biggestSize.height)) {
                biggestSize = size
            }
            //选出与预览界面等比的最高分辨率
            if (size.width >= previewSize?.width!! && size.height >= previewSize?.height!!) {
                val sizeScale: Float = size.width / size.height.toFloat()
                if (sizeScale == previewSizeScale) {
                    if (fitSize == null) {
                        fitSize = size
                    } else if (size.width >= fitSize.width && size.height >= fitSize.height) {
                        fitSize = size
                    }
                }
            }
        }
        if (fitSize == null) fitSize = biggestSize
        try {
            parameters.setPictureSize(fitSize!!.width, fitSize.height)
        } catch (e: Exception) {
            e.printStackTrace()
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

    /**
     * 点击拍照
     */
    private fun takePicture() {
        mCamera?.takePicture({
            //点击拍照时回调
        }, { data, camera ->
            //回调没压缩的原始数据
        }, { data, camera ->
            //回调图片数据，点击拍照后相机返回的照片byte数组
            Toast.makeText(this, "拍摄成功~", Toast.LENGTH_SHORT).show()
            //拍照后记得调用预览方法，不然会停在拍照图像的界面
            mCamera?.startPreview()
            //保存图片
            savePicture(data)
        })
    }

    /**
     * 保存图片
     */
    private fun savePicture(data: ByteArray?) {
        if (data == null) return
        lifecycleScope.launch(Dispatchers.IO) {
            val rootPath = externalCacheDir?.path ?: Environment.getExternalStorageDirectory().path
            val pictureFilePath = "$rootPath/picture/${System.currentTimeMillis()}.jpg"
            val pictureFile = File(pictureFilePath)
            FileUtils.createOrExistFile(pictureFile)

            var fos: FileOutputStream? = null
            try {
                fos = FileOutputStream(pictureFile)
                //将数据写入文件
                fos.write(data)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    fos?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            // 旋转图片
            rotateImageView(mCameraId, mDisplayOrientation, pictureFilePath)

            withContext(Dispatchers.Main) {
                ImageLoader.with(this@Camera1Activity).file(pictureFile).into(img_photo)
            }
        }
    }

    /**
     * 旋转图片
     */
    private fun rotateImageView(cameraId: Int, displayOrientation: Int, path: String) {
        val bitmap = BitmapFactory.decodeFile(path)
        val matrix = Matrix().apply {
            if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (displayOrientation == 90) postRotate(90f)
            } else if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                postRotate(270f)
            }
        }
        //旋转后的Bitmap
        val rotateBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        var resultBitmap: Bitmap = rotateBitmap
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            //若是前置摄像头，需要做镜面翻转
            resultBitmap = Bitmap.createBitmap(
                rotateBitmap,
                0,
                0,
                rotateBitmap.width,
                rotateBitmap.height,
                Matrix().apply { postScale(-1f, 1f) },
                true
            )
        }

        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(File(path))
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            bitmap.recycle()
            rotateBitmap.recycle()
            resultBitmap.recycle()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                fos?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}