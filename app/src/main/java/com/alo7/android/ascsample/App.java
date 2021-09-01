package com.alo7.android.ascsample;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.alo7.android.asc.ASCParam;
import com.alo7.android.asc.ASCSDK;

/**
 * @author haiyue.meng
 */
public class App extends Application {

    @SuppressLint("StaticFieldLeak")
    private static App instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        DefaultUncaughtExceptionHandler.init(getContext());
        initAsc();
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    private void initAsc() {
        //TODO 替换为真实的key/secret
        ASCSDK.initialize(
                this, ASCParam.create().iflytekParam("iflyAppId")
                        .singsoundParam("ssKey", "ssSecret"));
    }

}
