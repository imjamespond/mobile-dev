package com.example.utils

import android.content.ContentResolver
import android.provider.Settings
import android.util.Log
import com.example.myapplication.MainActivity
import java.io.IOException
import java.util.*


class Brightness {

    fun schedule(activity: MainActivity) {
        var timer = Timer()
        var task: TimerTask = object : TimerTask() {
            override fun run() {
                try {
                    setScreenBrightness(activity, 0)
                    Log.println(Log.DEBUG,"foo","setScreenBrightness")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        timer.schedule(task, Date(), 1000)
    }

    /**
     * 设置系统屏幕亮度，影响所有页面和app
     * 注意：这种方式是需要手动权限的（android.permission.WRITE_SETTINGS）
     */
    fun setScreenBrightness(activity: MainActivity, brightness: Int) {
        try {

            //先检测调节模式
            //setScreenManualMode(contentResolver)
            //再设置
            Settings.System.putInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    /**
     * 设置系统亮度调节模式(SCREEN_BRIGHTNESS_MODE)
     * SCREEN_BRIGHTNESS_MODE_MANUAL 手动调节
     * SCREEN_BRIGHTNESS_MODE_AUTOMATIC 自动调节
     */
    private fun setScreenManualMode(contentResolver: ContentResolver) {
        try {
            //获取当前系统亮度调节模式
            val mode =
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
            //如果是自动，则改为手动
            if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.putInt(
                    contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
            }
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
        }
    }

    companion object {
        fun SetBrightness() {
//            activity.window.attributes = activity.window.attributes.apply { screenBrightness = 0f }
        }
    }

}