/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

import xyz.klinker.messenger.shared.R

/**
 * Helper class for working with permissions and making sure that they are granted.
 */
object PermissionsUtils {

    private val REQUEST_MAIN_PERMISSIONS = 1

    fun checkRequestMainPermissions(activity: Activity): Boolean {
        return !checkPermissionGranted(activity, Manifest.permission.READ_CONTACTS) ||
                !checkPermissionGranted(activity, Manifest.permission.READ_SMS) ||
                !checkPermissionGranted(activity, Manifest.permission.READ_PHONE_STATE)
    }

    private fun checkPermissionGranted(activity: Activity, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun startMainPermissionRequest(activity: Activity) {
        ActivityCompat.requestPermissions(activity,
                arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECEIVE_MMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET), REQUEST_MAIN_PERMISSIONS)
    }

    fun processPermissionRequest(activity: Activity, requestCode: Int,
                                 permissions: Array<String>, grantResults: IntArray): Boolean {
        if (requestCode == REQUEST_MAIN_PERMISSIONS) {
            if (grantResults.size > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                return true
            } else {
                AlertDialog.Builder(activity)
                        .setMessage(R.string.permissions_needed)
                        .setPositiveButton(android.R.string.ok) { dialogInterface, i -> startMainPermissionRequest(activity) }
                        .show()
                return false
            }
        }

        return false
    }

    fun isDefaultSmsApp(context: Context): Boolean {
        try {
            return context.packageName == Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: NullPointerException) {
            // thrown by robolectric...
            return true
        }

    }

    /**
     * Sets the app as the default on the device if it isn't already. If it is, then we don't need
     * to do anything.
     *
     * @param context the current application context.
     */
    fun setDefaultSmsApp(context: Context) {
        val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT)
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Android TV trying to get set as the default app
            e.printStackTrace()
        }

    }


}