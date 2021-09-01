

package com.alo7.android.ascsample;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.Looper;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局捕获应用程序异常 保存文件
 *
 */
public class DefaultUncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static final String PATH = getCrashLogPath();
    private static UncaughtExceptionHandler mDefaultHandler;// 系统默认的UncaughtException处理类
    private static DefaultUncaughtExceptionHandler INSTANCE = new DefaultUncaughtExceptionHandler();// CrashHandler实例
    private static Context mContext;// 程序的Context对象
    private static Map<String, String> info = new LinkedHashMap<>();// 用来存储设备信息和异常信息
    private static SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");// 用于格式化日期,作为日志文件名的一部分

    /**
     * 保证只有一个CrashHandler实例
     */
    private DefaultUncaughtExceptionHandler() {

    }

    private static String getCrashLogPath() {
        String crashLogDirName = "crash";
        File file = App.getContext().getExternalFilesDir(crashLogDirName);
        if (file != null) {
            return file.getPath();
        } else {
            // requires WRITE_EXTERNAL_STORAGE permission
            return Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + crashLogDirName;
        }
    }

    /**
     * 获取CrashHandler实例 ,单例模式
     */
    public static DefaultUncaughtExceptionHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 应该尽量在主线程最开始的时候做初始化
     */
    public static void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();// 获取系统默认的UncaughtException处理器
        Thread.setDefaultUncaughtExceptionHandler(INSTANCE);// 设置该CrashHandler为程序的默认处理器
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        handleException(ex);
        if (mDefaultHandler != null) {
            // 如果自定义的没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }

    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param throwable 异常信息
     */
    public void handleException(Throwable throwable) {
        try {
            new Thread() {
                public void run() {
                    Looper.prepare();
                    Toast.makeText(mContext, "崩溃了, 即将退出",
                            Toast.LENGTH_SHORT).show();
                    Looper.loop();
                }
            }.start();
            collectSoftWareInfo(mContext);
            // 保存日志文件
            saveCrashInfo2File(throwable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 收集软件信息
     *
     * @param context
     */
    public void collectSoftWareInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();// 获得包管理器
            PackageInfo pi =
                    pm.getPackageInfo(context.getPackageName(), PackageManager.GET_GIDS);// 得到该应用的信息，即主Activity
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                info.put("versionName", versionName);
                info.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

    }

    /**
     * 保存信息到文件
     *
     * @param ex
     */
    private void saveCrashInfo2File(Throwable ex) {
        if (PATH == null || PATH.length() == 0) {
            return;
        }
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : info.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                sb.append(key).append("=").append(value).append("\r\n");
            }
            Writer writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            ex.printStackTrace(pw);
            Throwable cause = ex.getCause();
            // 循环着把所有的异常信息写入writer中
            while (cause != null) {
                cause.printStackTrace(pw);
                cause = cause.getCause();
            }
            pw.close();// 记得关闭
            String result = writer.toString();
            sb.append(result);
            // 保存文件
            long timestamp = System.currentTimeMillis();
            String time = format.format(new Date());
            String fileName = "crash-" + time + "_" + timestamp + ".log";
            File dir = new File(PATH);
            if (!dir.exists()) {
                dir.mkdir();
            }
            writeToFile(new File(dir, fileName), new ByteArrayInputStream(sb.toString().getBytes("UTF-8")));
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public static boolean writeToFile(File file, InputStream is) {
        FileOutputStream fos = null;
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            fos = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int length = 0;
            while ((length = is.read(buffer, 0, 1024)) != -1) {
                fos.write(buffer, 0, length);
                fos.flush();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
