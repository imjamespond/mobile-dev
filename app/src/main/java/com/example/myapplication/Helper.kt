package com.example.myapplication

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.window.OnBackInvokedDispatcher
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.example.myapplication.MainActivity.VolumeReceiver

class Helper {


    companion object {
        private const val TAG = "Helper"

        fun setupWebView(webView: WebView) {
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


        fun createVolumeReceiver(ma: MainActivity): VolumeReceiver {
            // Register the volume receiver if it's still needed, with the EXPORTED flag
            // However, as noted, handling volume keys directly in onKeyDown/onKeyUp is generally preferred.
            // This is primarily for demonstrating the Android 14 broadcast registration change.
            var volumeReceiver = VolumeReceiver()
            val intentFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 (Android 13) and above
                // For targetSdkVersion 34 (Android 14), you must specify the export behavior.
                ContextCompat.registerReceiver(
                    ma,
                    volumeReceiver,
                    intentFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
                )
            } else {
                ma.registerReceiver(volumeReceiver, intentFilter)
            }

            return volumeReceiver
        }

        fun preventBack(ma: MainActivity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ma.onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT
                ) {
                    // 什么都不做，就不会退出 App
                    Log.d("BackHandler", "Back gesture intercepted")
                }
            }
        }

        fun requirePermission(
            ma: MainActivity,
            requestPermissionLauncher: ActivityResultLauncher<String>
        ) {
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
                    ma,
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

        /*
         * 全屏
         * */
        fun hideStatusBar(window: Window) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                // 1. 设置全屏模式
                // 因为 targetSdk >= 35，setDecorFitsSystemWindows(false) 已经默认生效，
                // 应用内容会自动绘制在系统栏后面。
                // 你只需要处理隐藏系统栏即可。
                window.insetsController?.let {
                    // 隐藏状态栏和导航栏
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())

                    // 设置隐藏后的行为：通过边缘滑动短暂显示系统栏
                    it.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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


        fun fullscreenOff(window: Window) {
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

    }
}