package com.example.myapplication

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.Activity


class MyService : Service() {
    companion object {
        lateinit var Main: MainActivity
    }

    override fun onBind(intent: Intent?): IBinder? {
        TODO("Not yet implemented")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO Auto-generated method stub
        System.out.println("--------------flags--------->" + flags);
        System.out.println("--------------startId--------->" + startId);
        System.out.println("----------------------->onStartCommand");
        System.out.println("value = " + intent?.getStringExtra("hello"));
        System.out.println("action = " + intent?.action);


//        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
////        val runningProcesses = am.runningAppProcesses
//        val tasks = am.appTasks
//        for (task in tasks) {
//            System.out.println("taskInfo: " + task.taskInfo.toString())
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                val act = task.taskInfo.topActivity
//                System.out.println(act.toString())
//            }
//        }

        if (application is MyApp) {
            val act = (application as MyApp).currentActivity()
            val main = (act as MainActivity)
            if (intent?.action == "set_alpha_0_5") {
                main?.setFloatWin_(0.5f)
            } else if (intent?.action == "set_alpha_0_8") {
                main?.setFloatWin_(0.8f)
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }
}