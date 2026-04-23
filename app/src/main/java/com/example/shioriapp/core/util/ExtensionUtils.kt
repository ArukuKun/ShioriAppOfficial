package com.example.shioriapp.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import java.io.File

enum class ExtensionStatus {
    NOT_INSTALLED,
    INSTALLED,
    UPDATE_AVAILABLE
}

object ExtensionManager {
    fun getStatus(context: Context, jsonPackageName: String, repoVersion: String, apkFileName: String): ExtensionStatus {
        val packageManager = context.packageManager

        val realPackageName = getRealPackageNameFromApk(context, apkFileName) ?: jsonPackageName

        return try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(realPackageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(realPackageName, 0)
            }

            if (packageInfo.versionName == repoVersion) {
                ExtensionStatus.INSTALLED
            } else {
                ExtensionStatus.UPDATE_AVAILABLE
            }
        } catch (e: PackageManager.NameNotFoundException) {
            ExtensionStatus.NOT_INSTALLED
        }
    }

    private fun getRealPackageNameFromApk(context: Context, apkFileName: String): String? {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "$apkFileName.apk")
        if (!file.exists()) return null

        val packageInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
        return packageInfo?.packageName
    }
}