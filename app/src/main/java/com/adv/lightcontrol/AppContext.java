package com.adv.lightcontrol;

import android.app.Application;
import android.content.Context;

public class AppContext extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContextObject() {
        return context;
    }
}
