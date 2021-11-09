package com.example.myapplication;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MyApp extends Application {

    MyActivityLifecycleCallbacks lifecycleCallbacks = new MyActivityLifecycleCallbacks();

    public void onCreate() {
        super.onCreate();

        this.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    Activity currentActivity() {
        return lifecycleCallbacks.currentActivity;
    }
}


class MyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
    Activity currentActivity;

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {

    }
}