package com.example.myapplication

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder

class MyService: Service() {
    companion object {
        lateinit var Main:MainActivity
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
        System.out.println("action = " +  intent?.action);
        if (intent?.action == "set_alpha_0_5") {
            MyService.Main?.setFloatWin_(0.5f)
        } else if (intent?.action == "set_alpha_0_8") {
            MyService.Main?.setFloatWin_(0.8f)
        }




        return super.onStartCommand(intent, flags, startId)
    }
}