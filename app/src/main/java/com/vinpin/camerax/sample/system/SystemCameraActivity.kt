package com.vinpin.camerax.sample.system

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.vinpin.camerax.sample.R
import com.vinpin.camerax.sample.utils.FileUtils
import com.vinpin.imageloader.ImageLoader
import com.vinpin.livedatapermissions.LiveDataPermissions
import com.vinpin.livedatapermissions.PermissionDeny
import com.vinpin.livedatapermissions.PermissionGrant
import kotlinx.android.synthetic.main.activity_system_camera.*
import java.io.File

/**
 * author : VinPin
 * e-mail : hearzwp@163.com
 * time   : 2020/8/26 11:26
 * desc   : 调用系统相机界面
 */
class SystemCameraActivity : AppCompatActivity() {

    private var pictureFilePath: String? = null

    companion object {

        private const val REQUEST_CODE = 100

        fun newIntent(context: Context): Intent {
            return Intent(context, SystemCameraActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_camera)
        LiveDataPermissions(this).request(Manifest.permission.CAMERA).observe(this) {
            when (it) {
                is PermissionGrant -> goSystemCamera()
                is PermissionDeny -> onBackPressed()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("test", ">>> onActivityResult $requestCode $resultCode data is null ${data == null}")
        if (resultCode != RESULT_OK) return
        if (REQUEST_CODE == requestCode) {
            val photoFile = File(pictureFilePath ?: "")
            ImageLoader.with(this).file(photoFile).into(img_picture)
        }
    }

    /**
     * 调用系统相机
     */
    private fun goSystemCamera() {
        val rootPath = externalCacheDir?.path ?: Environment.getExternalStorageDirectory().path
        pictureFilePath = "$rootPath/picture/${System.currentTimeMillis()}.jpg"
        val pictureFile = File(pictureFilePath!!)
        FileUtils.createOrExistFile(pictureFile)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(this, "${packageName}.fileProvider", pictureFile)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            uri = Uri.fromFile(pictureFile)
        }
        Log.d("test", ">>>>> Uri is $uri")
        //指定ACTION为MediaStore.EXTRA_OUTPUT
        // 问题：拍摄成功后制定的pictureFile文件并没有被写入，且onActivityResult中的data为null。
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
        startActivityForResult(intent, REQUEST_CODE)
    }
}