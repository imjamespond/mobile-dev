package com.example.testcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

public class VolumeReceiver extends BroadcastReceiver {

//    public void init(Context mContext) {
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
//        mContext.registerReceiver(this, filter);
//    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
            Log.d("onReceive", "KEYCODE_VOLUME_DOWN");

//            Context myapp = context.getApplicationContext();
//            MainActivity activity = (MainActivity)context;
//            activity.takePhoto();
        }
    }
}