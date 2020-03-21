### flutter webview plugin, clear text not permitted
https://stackoverflow.com/questions/45940861/android-8-cleartext-http-traffic-not-permitted
``<uses-permission android:name="android.permission.INTERNET" />``
``android:usesCleartextTraffic="true"``

### 初次debug会卡在gradle
重启几次好了，估计是网络慢


### Hot reload
It counts torward anything that is not returned by build. So routes...
必须build里的改动才会热更新
