package com.alo7.android.ascsample;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alo7.android.asc.ASCError;
import com.alo7.android.asc.ASCLessonMode;
import com.alo7.android.asc.ASCParam;
import com.alo7.android.asc.ASCPlayerEventListener;
import com.alo7.android.asc.ASCSDK;
import com.alo7.android.asc.ASCServiceListener;
import com.alo7.android.asc.ASCSession;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static long NETWORK_TIME_OUT_MILL = 20000L;
    private final String LOG_TAG = "ASC-DEMO";

    EditText lessonIdEt;
    EditText lessonTypeEt;
    EditText tokenEt;
    EditText userIdEt;
    EditText playUrlEt;
    EditText interactiveH5UrlEt;
    EditText appKeyEt;
    EditText appSecretEt;
    EditText ssAppKeyEt;
    EditText ssAppSecretEt;
    EditText iflyAppIdEt;

    String defaultAppkey = "";
    String defaultAppSecret = "";
    String defaultTokenServerUrl = "";
    ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        lessonIdEt = findViewById(R.id.edit_lesson_id);
        lessonTypeEt = findViewById(R.id.edit_lesson_type);
        tokenEt = findViewById(R.id.edit_token);
        userIdEt = findViewById(R.id.edit_user_id);
        playUrlEt = findViewById(R.id.edit_play_url);
        interactiveH5UrlEt = findViewById(R.id.edit_interactive_layer_url);
        appKeyEt = findViewById(R.id.edit_app_key);
        appSecretEt = findViewById(R.id.edit_app_secret);
        ssAppKeyEt = findViewById(R.id.edit_ss_app_key);
        ssAppSecretEt = findViewById(R.id.edit_ss_app_secret);
        iflyAppIdEt = findViewById(R.id.edit_ifly_app_id);
        findViewById(R.id.btn_go).setOnClickListener(this);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        switch (viewId) {
            case R.id.btn_go:
                joinLesson();
                break;
            default:
                break;
        }
    }

    void setTestEnv() {
        //TODO 设置默认的alo7 key/secret, 设置了之后测试页面上可以不填
        defaultAppkey = "";
        defaultAppSecret = "";
        defaultTokenServerUrl = "https://api.alo7.com/";
    }

    void initAsc() {
        setTestEnv();
        String ssAppKey = ssAppKeyEt.getText().toString().trim();
        String ssAppSecret = ssAppSecretEt.getText().toString().trim();
        String iflyAppId = iflyAppIdEt.getText().toString().trim();
        ASCSDK.initialize(
                this, ASCParam.create().iflytekParam(iflyAppId)
                        .singsoundParam(ssAppKey, ssAppSecret));
    }

    APIService getApiService() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> {
            Log.e(LOG_TAG, message);
        });
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .readTimeout(NETWORK_TIME_OUT_MILL, TimeUnit.MILLISECONDS)
                .connectTimeout(NETWORK_TIME_OUT_MILL, TimeUnit.MILLISECONDS)
                .writeTimeout(NETWORK_TIME_OUT_MILL, TimeUnit.MILLISECONDS).addInterceptor(loggingInterceptor);
        Retrofit.Builder retrofitBuilder =
                new Retrofit.Builder().baseUrl(defaultTokenServerUrl).client(clientBuilder.build());
        retrofitBuilder.addConverterFactory(GsonConverterFactory.create((new GsonBuilder()).disableHtmlEscaping().create()));
        retrofitBuilder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
        return retrofitBuilder.build().create(APIService.class);
    }

    void joinLesson() {
        if (TextUtils.isEmpty(lessonIdEt.getText().toString())) {
            Toast.makeText(this, "没有输入lessonId", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userIdEt.getText().toString())) {
            Toast.makeText(this, "没有输入userId", Toast.LENGTH_SHORT).show();
            return;
        }

        initAsc();
        String token = tokenEt.getText().toString();
        if (TextUtils.isEmpty(token)) {
            fetchTokenAndJoinLesson();
        } else {
            joinLessonInternal(token);
        }
    }

    void fetchTokenAndJoinLesson() {
        String inputAppKey = appKeyEt.getText().toString();
        String appKey = "";
        String appSecret = "";
        if (TextUtils.isEmpty(inputAppKey)) {
            appKey = defaultAppkey;
            appSecret = defaultAppSecret;
        } else {
            appKey = inputAppKey;
            appSecret = appSecretEt.getText().toString();
        }
        if (TextUtils.isEmpty(appKey) || TextUtils.isEmpty(appSecret)) {
            Toast.makeText(this, "此环境需要输入token或者输入key/secret", Toast.LENGTH_LONG).show();
            return;
        }
        loadingDialog = ProgressDialog.show(this, "Loading", "获取token", false, true);
        getApiService().getToken(appKey, appSecret).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<Credential>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(Credential credential) {
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                }
                joinLessonInternal(credential.getToken());
            }

            @Override
            public void onError(Throwable e) {
                if (loadingDialog != null) {
                    loadingDialog.dismiss();
                }
                Toast.makeText(MainActivity.this, "获取token失败, 重试或手动输入", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onComplete() {
            }
        });
    }

    void joinLessonInternal(String token) {
        //ASC_ITG STEP
        long lessonId = 1L;
        try {
            lessonId = Long.parseLong(lessonIdEt.getText().toString().trim());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        int entityType = 0;
        String lessonType = lessonTypeEt.getText().toString();
        if (!TextUtils.isEmpty(lessonType)) {
            entityType = Integer.parseInt(lessonType);
            if (entityType < 0 || entityType > 4) {
                entityType = 0;
            }
        }
        ASCLessonMode mode;
        switch (entityType) {
            case 4:
                mode = ASCLessonMode.PLAN_REPLAY;
                break;
            case 3:
                mode = ASCLessonMode.PLAN;
                break;
            case 2:
                mode = ASCLessonMode.REPLAY;
                break;
            case 1:
                mode = ASCLessonMode.VOD;
                break;
            case 0:
            default:
                mode = ASCLessonMode.NORMAL;
                break;
        }
        ASCSDK.startBySession(this, ASCSession.create(String.valueOf(lessonId)).mode(mode)
                        .userId(userIdEt.getText().toString()).token(token),
                new ASCServiceListener() {
                    @Override
                    public void ascSessionStart(ASCSession ascSession) {
                        Log.e(LOG_TAG, "ascSessionStart");
                    }

                    @Override
                    public void ascSessionExit(ASCSession ascSession) {
                        Log.e(LOG_TAG, "ascSessionExit");
                    }

                    @Override
                    public void onError(ASCError ascError) {
                        Log.e(LOG_TAG, "errorCode: " + ascError.errorCode + " errorMessage: " + ascError.errorMessage);
                    }
                }, new ASCPlayerEventListener() {
                    @Override
                    public void onPlayerPrepareStart() {
                        Log.e(LOG_TAG, "onPlayerPrepareStart");
                    }

                    @Override
                    public void onPlayerPrepared() {
                        Log.e(LOG_TAG, "onPlayerPrepared");
                    }

                    @Override
                    public void onPlayerStart() {
                        Log.e(LOG_TAG, "onPlayerStart");
                    }

                    @Override
                    public void onPlayerRenderStart() {
                        Log.e(LOG_TAG, "onPlayerRenderStart");
                    }

                    @Override
                    public void onPlayerPlayingPositionChange(long currentPosition) {
                        Log.e(LOG_TAG, "onPlayerPlayingPositionChange: " + currentPosition);
                    }

                    @Override
                    public void onPlayerBufferedPositionChange(long bufferedPosition) {
                        Log.e(LOG_TAG, "onPlayerBufferedPositionChange: " + bufferedPosition);
                    }

                    @Override
                    public void onPlayerPause() {
                        Log.e(LOG_TAG, "onPlayerPause");
                    }

                    @Override
                    public void onPlayerLoadingStart() {
                        Log.e(LOG_TAG, "onPlayerLoadingStart");
                    }

                    @Override
                    public void onPlayerLoadingEnd() {
                        Log.e(LOG_TAG, "onPlayerLoadingEnd");
                    }

                    @Override
                    public void onPlayerComplete() {
                        Log.e(LOG_TAG, "onPlayerComplete");
                    }

                    @Override
                    public void onPlayerError(int code, String msg) {
                        Log.e(LOG_TAG, "onPlayerError: code " + code + " msg: " + msg);
                    }

                    @Override
                    public void onPlayerDestroy() {
                        Log.e(LOG_TAG, "onPlayerDestroy");
                    }
                });

    }
}