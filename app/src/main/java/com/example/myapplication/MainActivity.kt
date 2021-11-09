package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.view.View.OnTouchListener
import android.widget.RemoteViews
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        MyService.Main = this
        createMusicNotification()
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode === 0) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
//                startService(Intent(this@MainActivity, FloatingButtonService::class.java))
            }
        }
//        else if (requestCode === 1) {
//            if (!Settings.canDrawOverlays(this)) {
//                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
//                startService(Intent(this@MainActivity, FloatingImageDisplayService::class.java))
//            }
//        } else if (requestCode === 2) {
//            if (!Settings.canDrawOverlays(this)) {
//                Toast.makeText(this, "授权失败", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "授权成功", Toast.LENGTH_SHORT).show()
//                startService(Intent(this@MainActivity, FloatingVideoService::class.java))
//            }
//        }
    }

    fun setBrightness(view: android.view.View?) {

        this.window.attributes = this.window.attributes.apply { screenBrightness = 0f }
        //Brightness().schedule(this)
        Toast.makeText(this.applicationContext, "btn is clicked!", Toast.LENGTH_SHORT).show()
    }

    var touchDownY = 0f
    fun setFloatWin(view: View) {
        setFloatWin_(.5f)
    }

    fun setFloatWin_(opacity: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT);
            val intent = Intent();
            intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
        }

        val view = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)
        view.setOnTouchListener(OnTouchListener { v, event ->
            when (event!!.action) {
                MotionEvent.ACTION_DOWN -> System.out.println("起始位置：(" + event.x.toString() + "," + event.y)
                MotionEvent.ACTION_MOVE -> System.out.println("实时位置：(" + event.x.toString() + "," + event.y)
                MotionEvent.ACTION_UP -> {
                    System.out.println("结束位置：(" + event.x.toString() + "," + event.y)
                    touchDownY = event.y
                }
                else -> {
                }
            }
            false
        })


        val layoutParams = WindowManager.LayoutParams().apply {
            alpha = opacity
            format = PixelFormat.RGBA_8888
            flags =
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_FULLSCREEN
            //设置大小 自适应
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }


        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        wm.addView(view, layoutParams)
    }

    fun closeWin(view: android.view.View) {
        if (touchDownY < view.height / 3) {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
        }
    }

    fun onExit(view: android.view.View) {
//        val homeIntent = Intent(Intent.ACTION_MAIN)
//        homeIntent.addCategory(Intent.CATEGORY_HOME)
//        homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//        startActivity(homeIntent)

        android.os.Process.killProcess(android.os.Process.myPid())
    }

    fun createMusicNotification() {

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ncId = "nc-1"
        val ncName = "nc-name-1"
        //android 8.0的判断、需要加入NotificationChannel
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(ncId, ncName, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this.applicationContext, ncId);
        //自定义布局必须加上、否则布局会有显示问题、可以自己try try
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setOngoing(true);//代表是常驻的，主要是配合服务


        val remoteViews =
            RemoteViews(this.applicationContext.getPackageName(), R.layout.notification_layout);

//        val intent = Intent()
//        intent.component = ComponentName("com.example", "com.example.MyExampleActivity")
//        startActivity(intent)
//        自定义点击事件、会在Service. onStartCommand中回调,
        val it1 = Intent(this, MyService::class.java)
        it1.setAction("set_alpha_0_5")
        val it2 = Intent(this, MyService::class.java)
        it2.setAction("set_alpha_0_8")
//        it.putExtra("hello", "我是一个Service");
        remoteViews.setOnClickPendingIntent(R.id.button9, PendingIntent.getService(this, 0, it1, 0));
        remoteViews.setOnClickPendingIntent(R.id.button10, PendingIntent.getService(this, 0, it2, 0));

        builder.setContent(remoteViews);
        val notification = builder.build();
        //0x11 为通知id 自定义可
        notificationManager.notify(0x11, notification);

    }
}