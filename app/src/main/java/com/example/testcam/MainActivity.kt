package com.example.testcam

//import androidx.camera.core.Preview

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testcam.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
//                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//    }

    private lateinit var webView: WebView
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null
//    private lateinit var cameraExecutor: ExecutorService


    private var volumeReceiver: VolumeReceiver? = null


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /* 主界面 */
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // 音量全局接收：
//        volumeReceiver = VolumeReceiver()
//        registerReceiver(volumeReceiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION") )
//        registerReceiver(HomeReceiver(),IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))

        // 拍照初始,权限申请
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up the listeners for take photo and video capture buttons
        viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
//        viewBinding.videoCaptureButton.setOnClickListener { captureVideo() }
        viewBinding.closeButton.setOnClickListener { exitApp() }
//        cameraExecutor = Executors.newSingleThreadExecutor()
//        enterPictureInPictureMode(PictureInPictureParams.Builder().setActions().build())


        /*
        * webpage for camerflag
        * */
        webView = viewBinding.wvWebview
        webView.getSettings().setJavaScriptEnabled(true)
        webView.loadUrl("https://www.sina.com.cn")
        webView.webViewClient = object : WebViewClient() {
            //设置在webView点击打开的新网页在当前界面显示,而不跳转到新的浏览器中
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }
        viewBinding.toBingButton.setOnClickListener {
            webView.loadUrl("https://cn.bing.com/")
            setFloatWinImpl(.9f)
        }
        viewBinding.toSinaButton.setOnClickListener {
            webView.loadUrl("https://www.sina.com.cn/")
        }


        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "KEYCODE_BACK")
            webView.goBack()
            return false
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            takePhoto()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true
        } else if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_HOME) {
            Log.d(TAG, "KEYCODE_HOME ${keyCode}, ${KeyEvent.KEYCODE_HOME}, ${KeyEvent.KEYCODE_MOVE_HOME}")
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    inner class HomeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if(intent.action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)){
                    Log.d(TAG, "ACTION_CLOSE_SYSTEM_DIALOGS")
                }
            }
        }
    }
    /*
    * 拍照
    * */
    private fun takePhoto() {
        // Toast.makeText(this, "...", Toast.LENGTH_SHORT).show()

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG,"" /*"Photo capture failed: ${exc.message}"*/, exc)
                }

                @RequiresApi(Build.VERSION_CODES.R)
                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "" //"Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()


//                    if (output.savedUri != null) {
//                         val file = DocumentFile.fromSingleUri(applicationContext, output.savedUri!!)
//                        if (file != null) {
//                            Log.d(TAG, "${file.getName()}, dir:${file.isDirectory}")
//                            file.delete()
//                        }
//                    }
                }
            }
        )

    }
//    private fun captureVideo() {}
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            /*val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }*/

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, /*preview,*/ imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            viewBinding.imageCaptureButton.setText("Ready!")

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
//        cameraExecutor.shutdown()
//        unregisterReceiver(volumeReceiver)
    }



    private fun exitApp() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }



    /*
    * 音量键
    * */
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        Log.d("dispatchKeyEvent", event.toString())

        if (event != null) {
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_UP) {
//                takePhoto()
            }
        }

        return super.dispatchKeyEvent(event)
    }


    /*
    * 浮窗层
    * */
    var touchDownX = 0f
    var touchDownY = 0f
    var preScreenBrightness = 0f
    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
        }
    }
    fun setFloatWinImpl(opacity: Float) {

        preScreenBrightness = this.window.attributes.screenBrightness
        this.window.attributes = this.window.attributes.apply { screenBrightness = 0f }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "当前无权限，请授权", Toast.LENGTH_SHORT).show()
            val intent = Intent()
            intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.setData(Uri.parse("package:" + getPackageName()))

            resultLauncher.launch(intent)
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.floating_layout, null)
        view.setOnTouchListener(View.OnTouchListener { v, event ->
            when (event!!.action) {
//                MotionEvent.ACTION_DOWN -> System.out.println("起始位置：(" + event.x.toString() + "," + event.y)
//                MotionEvent.ACTION_MOVE -> System.out.println("实时位置：(" + event.x.toString() + "," + event.y)
                MotionEvent.ACTION_UP -> {
                    System.out.println("结束位置：(" + event.x.toString() + "," + event.y)
                    touchDownX = event.x
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
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            //设置大小 自适应
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE
        }


        val wm = getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        wm.addView(view, layoutParams)

        fullscreen()
    }
    fun closeWin(view: android.view.View) {
        if (touchDownX < 100 && touchDownY < 100 && touchDownY < view.height / 3) {
            this.window.attributes = this.window.attributes.apply { screenBrightness = preScreenBrightness }
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(view)

            fullscreenOff()
        }
    }

    /*
    * 全屏
    * */
    private fun fullscreen(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            /* 刘海周围显示 */
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            if (window.insetsController != null) {
                window.insetsController!!.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                window.insetsController!!.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

    }
    private fun fullscreenOff(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            /* 刘海周围显示 */
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            if (window.insetsController != null) {
                window.insetsController!!.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
            window.setDecorFitsSystemWindows(true)
        }
    }
}

