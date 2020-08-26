package com.vinpin.camerax.sample

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.vinpin.camerax.sample.system.SystemCameraActivity
import com.vinpin.livedatapermissions.LiveDataPermissions
import com.vinpin.livedatapermissions.PermissionDeny
import com.vinpin.livedatapermissions.PermissionGrant
import kotlinx.android.synthetic.main.activity_main.*

/**
 * author : VinPin
 * e-mail : hearzwp@163.com
 * time   : 2020/8/26 11:26
 * desc   : 主界面
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // 申请权限
        LiveDataPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).observe(this) {
            when (it) {
                is PermissionGrant -> setClickListeners()
                is PermissionDeny -> onBackPressed()
            }
        }
    }

    private fun setClickListeners() {
        txt_system_camera.setOnClickListener { startActivity(SystemCameraActivity.newIntent(this)) }
        txt_camera_1.setOnClickListener { }
        txt_camera_x.setOnClickListener { }
    }
}