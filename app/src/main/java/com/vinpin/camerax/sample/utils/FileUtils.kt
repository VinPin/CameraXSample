package com.vinpin.camerax.sample.utils

import java.io.File

/**
 * author : VinPin
 * e-mail : hearzwp@163.com
 * time   : 2020/8/27 10:05
 * desc   :
 */
object FileUtils {

    fun createOrExistFile(file: File) {
        if (file.exists()) {
            return
        }
        if (file.isDirectory) {
            file.mkdirs()
            return
        }
        val parentFile = file.parentFile
        if (parentFile?.exists() == false) {
            parentFile.mkdirs()
        }
        try {
            file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}