package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService

// Assuming you have an ActivityMainBinding generated from your layout XML.
// If not, you'll need to define it or remove references to viewBinding and use find
// ViewById directly.
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {


    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10

        // For Android 10 (API 29) and above, WRITE_EXTERNAL_STORAGE is deprecated for media.
        // We only need CAMERA permission.
        // On Android 14+, you generally don't need READ_EXTERNAL_STORAGE for media files
        // that your app creates or for using the photo picker.
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).toTypedArray()
    }

    private lateinit var webView: WebView
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService // Declare cameraExecutor here

    // VolumeReceiver adaptation for Android 14+ broadcast changes.
    // For runtime-registered receivers, you must specify RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED.
    // However, handling hardware key events like volume buttons is usually done via onKeyDown/onKeyUp,
    // which you already have. A BroadcastReceiver for volume changes is less common and might
    // not function as expected due to stricter background broadcast restrictions.
    // If you specifically need to observe volume changes when your app is not in the foreground,
    // consider using `AudioManager` and its `registerAudioPlaybackCallback` (for audio state)
    // or explore alternative background task mechanisms (like WorkManager) if a persistent
    // listener is truly required, rather than a BroadcastReceiver for general volume key presses.
    // For now, I'm keeping the BroadcastReceiver for demonstrative purposes of the API change,
    // but the `onKeyDown` method is the primary way you're handling volume keys.
    private var volumeReceiver: VolumeReceiver? = null

    // Register an ActivityResultLauncher for the overlay permission
    private val requestOverlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Permission granted, try setting up the float window again
            // You might want to pass the opacity value here if it's dynamic
            // For simplicity, calling with a default value.
            // Note: Floating windows are a special permission and have specific
            // behavior and limitations on newer Android versions.
            setFloatWinImpl(.9f)
        } else {
            Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, you can safely initialize the camera
            // startCamera()
        } else {
            // Permission denied, show a message to the user
            Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_LONG).show()
            Log.e("CameraXApp", "Camera permission not granted by the user.")
            // You might want to close the app or disable camera-related features
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

         hideStatusBar()


        /* 主界面 */
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Initialize cameraExecutor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Set up the listeners for take photo and video capture buttons
        // viewBinding.imageCaptureButton.setOnClickListener { takePhoto() }
        viewBinding.closeButton.setOnClickListener { exitApp() }
        viewBinding.toBingButton.setOnClickListener {
            setFloatWinImpl(.9f)
        }

        // Request permissions on create, as it's typically needed early.
        requirePermission()

        // Initialize WebView (assuming you have a WebView in your layout)
        webView = viewBinding.wvWebview // Replace with your WebView ID if different
        setupWebView()

        // Register the volume receiver if it's still needed, with the EXPORTED flag
        // However, as noted, handling volume keys directly in onKeyDown/onKeyUp is generally preferred.
        // This is primarily for demonstrating the Android 14 broadcast registration change.
        volumeReceiver = VolumeReceiver()
        val intentFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 (Android 13) and above
            // For targetSdkVersion 34 (Android 14), you must specify the export behavior.
            ContextCompat.registerReceiver(
                this,
                volumeReceiver,
                intentFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(volumeReceiver, intentFilter)
        }
    }

    private fun setupWebView() {
        // Configure WebView settings as needed
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url != null) {
                    view?.loadUrl(url)
                }
                return true
            }
        }
        webView.loadUrl("https://cn.bing.com") // Load a default URL
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Shut down the camera executor when the activity is destroyed

        // Unregister the volume receiver
        volumeReceiver?.let {
            unregisterReceiver(it)
        }
    }

    private fun requirePermission() {
        // 拍照初始,权限申请
//        if (allPermissionsGranted()) {
//            // startCamera()
//        } else {
//            ActivityCompat.requestPermissions(
//                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
//            )
//        }
        // Check for camera permission at startup
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, proceed with camera initialization
            // startCamera()
        } else {
            // Permission not granted, request it from the user
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // startCamera()
                Log.d(TAG, "startCamera is capable")
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview is commented out, assuming you are not displaying a live preview
            // If you intend to show a preview, uncomment this and ensure viewBinding.viewFinder is a PreviewView
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
                    this as LifecycleOwner, cameraSelector, /*preview,*/ imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            // viewBinding.imageCaptureButton.text = "Ready!" // Use .text instead of setText()
            vibratePhone(this, 100L)

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Log.d(TAG, "KEYCODE_BACK")
            if (webView.canGoBack()) { // Check if WebView can go back
                webView.goBack()
                return true // Consume the back event
            } else {
                // If WebView can't go back, let the system handle it (e.g., exit app)
                return super.onKeyDown(keyCode, event)
            }
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            takePhoto()
            return true // Consume the event to prevent system volume change
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true // Consume the event to prevent system volume change
        } else if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_MOVE_HOME) {
            Log.d(
                TAG,
                "KEYCODE_HOME $keyCode, ${KeyEvent.KEYCODE_HOME}, ${KeyEvent.KEYCODE_MOVE_HOME}"
            )
            // HOME key is typically handled by the system and cannot be reliably intercepted.
            // Returning false might allow the system to handle it, but true might prevent it.
            // For HOME, it's generally best to let the system handle it.
            return false
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true // Consume the key up event as well if you consumed key down
        }
        return super.onKeyUp(keyCode, event)
    }

    //拍照
    private fun takePhoto() {
        // Toast.makeText(this, "...", Toast.LENGTH_SHORT).show()

        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 (API 29) and above
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }

        // It's generally better to save to MediaStore directly rather than externalCacheDir
        // if you want the image to be immediately visible in the gallery and accessible by other apps.
        // If it's a temporary file only for your app, externalCacheDir is fine.
        // For demonstration, I'll show saving to MediaStore.
//        val outputOptions = ImageCapture.OutputFileOptions.Builder(
//            contentResolver,
//            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//            contentValues
//        ).build()

        val filePath = "${externalCacheDir?.absolutePath}/${name}.jpg"
        val file = File(filePath)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    Toast.makeText(
                        baseContext,
                        "Photo capture failed: ${exc.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                @RequiresApi(Build.VERSION_CODES.R) // MediaStore.Images.Media.RELATIVE_PATH behavior
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }
            }
        )
    }

    /*
     * 全屏
     * */
    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            // 1. 设置全屏模式
            // 因为 targetSdk >= 35，setDecorFitsSystemWindows(false) 已经默认生效，
            // 应用内容会自动绘制在系统栏后面。
            // 你只需要处理隐藏系统栏即可。
            window.insetsController?.let {
                // 隐藏状态栏和导航栏
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

                // 设置隐藏后的行为：通过边缘滑动短暂显示系统栏
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

            // 2. 处理刘海屏
            // 对于 targetSdk >= 35 的应用，刘海屏区域的渲染也是默认行为，
            // 即 LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS。
            // 因此，这行代码在大多数情况下也是非必需的。
            // 如果你想确保兼容性，可以保留，但它已经是默认值。
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS

            // 3. 处理内容内边距
            // 因为你调用了 setDecorFitsSystemWindows(false) (或系统自动调用了它)，
            // 你的内容 View 会被扩展到整个屏幕。
            // 你需要为你的 UI 元素手动添加内边距，以避免它们被系统栏覆盖。
            // 你可以通过监听 WindowInsets 来动态获取内边距并应用。
            // 例如，使用 ViewCompat.setOnApplyWindowInsetsListener
            // 或在 Jetpack Compose 中使用 Modifier.windowInsetsPadding
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

    }

    private fun fullscreenOff() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            /* 刘海周围显示 */
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            if (window.insetsController != null) {
                window.insetsController!!.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
            window.setDecorFitsSystemWindows(true)
        }
    }

    private fun exitApp() {
        finishAndRemoveTask() // More graceful way to exit activity and remove from recent tasks
        // Or to kill the process (less common for typical app exit):
        // android.os.Process.killProcess(android.os.Process.myPid())
    }


    /*
    * Volume key BroadcastReceiver (for demonstration of Android 14 changes)
    * In most cases, `onKeyDown` and `onKeyUp` are sufficient for handling volume keys.
    * */
    class VolumeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "android.media.VOLUME_CHANGED_ACTION") {
                // This broadcast is not reliable for intercepting volume button presses
                // for app-specific actions, especially in background.
                // onKeyDown/onKeyUp are the correct way to handle direct key presses.
                Log.d(TAG, "Volume changed via broadcast (unreliable for key press detection)")
                // If you were to do something specific here, remember the limitations.
            }
        }
    }

    // This method is correctly overridden from ComponentActivity for key events.
    // The previous `dispatchKeyEvent` was a standalone function, which is not how
    // Android handles key events for activities.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        Log.d("dispatchKeyEvent", event.toString())

        if (event != null) {
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && event.action == KeyEvent.ACTION_UP) {
                vibratePhone(this, 100L)
                takePhoto()
            }
        }

        return super.dispatchKeyEvent(event)
    }


    /*
    * 浮窗层
    * */
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var preScreenBrightness = 0f
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
            }
        }

    private fun setFloatWinImpl(opacity: Float) {
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
        // Create the floating view, need to bind closeWin manually
        //        val view = FrameLayout(this).apply {
        //            setBackgroundColor(0x80000000.toInt()) // Semi-transparent black background
        //            // Add your content here, e.g., a TextView
        //        }

        view.setOnTouchListener(View.OnTouchListener { v, event ->
            when (event!!.action) {
//                MotionEvent.ACTION_DOWN -> System.out.println("起始位置：(" + event.x.toString() + "," + event.y)
//                MotionEvent.ACTION_MOVE -> System.out.println("实时位置：(" + event.x.toString() + "," + event.y)
                MotionEvent.ACTION_UP -> {
                    Log.d(TAG, "结束位置：(" + event.x.toString() + "," + event.y)
                    touchDownX = event.x
                    touchDownY = event.y
                    // view onClick bind closeWin
                }

                else -> {
                }
            }
            false
        })

        val layoutParams = WindowManager.LayoutParams().apply {
            alpha = opacity
            format = PixelFormat.RGBA_8888
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN
            //设置大小 自适应
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            // Set the window type based on API level
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT // Requires SYSTEM_ALERT_WINDOW permission
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutParams.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        view.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE


        val wm = getSystemService(AppCompatActivity.WINDOW_SERVICE) as WindowManager
        wm.addView(view, layoutParams)

        startCamera()
    }

    fun closeWin(view: android.view.View) {
        if (touchDownX < 100 && touchDownY < 100 && touchDownY < view.height / 3) {
            this.window.attributes =
                this.window.attributes.apply { screenBrightness = preScreenBrightness }
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(view)

            // fullscreenOff()
            exitApp()
        }
    }

    /**
     * Vibrates the phone for a specified duration using the recommended APIs for each Android version.
     * @param context The context to retrieve the Vibrator service from.
     * @param durationMillis The duration of the vibration in milliseconds.
     */
    private fun vibratePhone(context: Context, durationMillis: Long) {
        // Get the Vibrator service using the recommended approach for the current SDK level.
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use VibratorManager for API 31+
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            // Use the old method for API < 31
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Check if the device has a vibrator
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For API 26 and above, use VibrationEffect
                val vibrationEffect = VibrationEffect.createOneShot(
                    durationMillis,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                // For older APIs (< 26), use the deprecated vibrate method
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        }
    }
}