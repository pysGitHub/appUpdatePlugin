package cordova-appupdate-plugin;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.FileProvider;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

/**
 * This class echoes a string called from JavaScript.
 */
public class appUpdatePlugin extends CordovaPlugin {

    public static final String TAG = "AutoUpdatePlugin";
    // 用来请求版本数据的链接
    private String checkVersionUrl = null;
    // 新版本的下载链接
    // private String newVersionUrl = null;
    private Version latestVersion = null;
  
    // 跳转到浏览器的链接
    private String downloadUrl = null;
  
    private Context mContext;
  
  
    private static final int HAS_NEW_VERSION = 0x1111;
  
    // private static final String MSG_ENG = "TSS App is denied from reading your
    // files. Unable to update automatically.\n"
    // + "Would you like to open the download link and install the latest update
    // manually?\n";
    // private static final String MSG_CN = "您未允许TSS App读取该设备上的文件，因此无法自动更新。\n" +
    // "是否打开下载地址手动安装最新版本？\n";
    // private static final String MSG_TW = "因為您未允許TSS App讀取此裝置檔案，所以無法自動更新。\n" +
    // "是否要打開下載連結手動安裝最新版本 ?\n";
    private static final String URL_DOWNLOAD = "/~pts/dispatcher/app/store/index.php";

    private static final String BTN_OK = "YES";
    private static final String BTN_CANCEL = "NO";
    private static final String BTN_OK_CN = "是";
    private static final String BTN_CANCEL_CN = "否";
  
    // 需要动态申请的权限
    private static String[] PERMISSIONS_STORAGE = { "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE" };
  
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
      if ("autoUpdateVersion".equals(action)) {
        // 支持自带参数
        String arg = args.getString(0);
        if (null != arg && !arg.isEmpty()) {
          Log.i(TAG, "autoUpdateVersion: " + arg);
          this.checkVersionUrl = arg + UPDATE_URL;
          this.downloadUrl = arg + URL_DOWNLOAD;
        }
        initBroadcastReceiver();
        checkNewVersion();
        return true;
      }
      return super.execute(action, args, callbackContext);
    }
  
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
      super.initialize(cordova, webView);
      this.checkVersionUrl = UPDATE_SERVER_URL_PTS;
      this.mContext = this.cordova.getActivity();
      Log.d(TAG, "initialize");
      initBroadcastReceiver();
      // checkNewVersion();
      // Android系统在6.0以上进行动态申请权限
      /*
       * int permission = ActivityCompat.checkSelfPermission(mContext,
       * "android.permission.WRITE_EXTERNAL_STORAGE"); if
       * (PackageManager.PERMISSION_GRANTED != permission) { // 没有文件存储权限，动态申请
       * ActivityCompat.requestPermissions((Activity) mContext, PERMISSIONS_STORAGE,
       * 1); }
       */
  
    }
  
    private void initBroadcastReceiver() {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction("android.intent.action.DOWNLOAD_COMPLETE");
      mContext.registerReceiver(broadcastReceiver, intentFilter);
    }
  
    @Override
    protected void pluginInitialize() {
      super.pluginInitialize();
    }
  
    private void downLoadFile(String url) {
      Log.i(TAG, "downloadFile");
      File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PTSAPP/ptsApp.apk");
      if (file.exists())
        file.delete();
  
      DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
      DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
      request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
      request.setDestinationInExternalPublicDir("PTSAPP", "/ptsApp.apk");
      request.setVisibleInDownloadsUi(true);
      long id = manager.enqueue(request);
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
      sharedPreferences.edit().putLong(DownloadManager.EXTRA_DOWNLOAD_ID, id).apply();
      Log.i(TAG, "started DownloadManager");
    }
  
    /**
     * 向服务器请求判断是否有新版本
     */
    private boolean checkNewVersion() {
      final String requestUrl = checkVersionUrl;
      Log.i(TAG, "requestUrl : " + requestUrl);
      new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            BufferedReader reader;
            String line;
            StringBuffer buffer = new StringBuffer();
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int responseCode = connection.getResponseCode();
            Log.d(TAG, "responseCode = " + responseCode);
            if (responseCode == 200) {
              // 请求成功，对get的数据进行解析
              reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
              while (null != (line = reader.readLine())) {
                buffer.append(line);
              }
              String data = buffer.toString();
              Log.d(TAG, "returned data :" + data);
              parseDownloadUrl(data);
              // 解析成功，获取到当前的最新版本信息
              if (null != latestVersion && null != getVersionCode()) {
                if (!latestVersion.versionCode.equals(getVersionCode())) {
                  mHandler.sendEmptyMessage(HAS_NEW_VERSION);
                } else
                  Log.d(TAG, "currentVersionCode:" + getVersionCode() + " \nlatestVersionCode:"
                      + latestVersion.getVersionCode());
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }).start();
      return false;
    }
  
    /**
     * 展示提示更新的弹窗
     */
    private void showUpdateDialog() {
      Log.i(TAG, "showUpdateDialog");
  
      String msg = latestVersion.description;
      String btn_ok = BTN_OK;
      String btn_cancel = BTN_CANCEL;
  
      Locale locale = Locale.getDefault();
      if (locale.getDisplayLanguage(Locale.CHINA).equals("中文")) {
        btn_ok = BTN_OK_CN;
        btn_cancel = BTN_CANCEL_CN;
        if (locale.getCountry().equals("CN")) {
          // 简中
          msg = latestVersion.description;
        } else if (locale.getCountry().equals("TW") || locale.getCountry().equals("HK")) {
          // 繁中
          msg = latestVersion.description;
        }
      }
      msg = msg.replace("VERSION_NUMBER", latestVersion.getVersion());
  
      AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
      alert.setTitle(latestVersion.getSummary()).setMessage(msg)
          .setPositiveButton(btn_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog,
                int which) {/*
                             * // 确认下载的逻辑 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //
                             * Android系统在6.0以上进行动态申请权限 int permission =
                             * ActivityCompat.checkSelfPermission(mContext,
                             * "android.permission.WRITE_EXTERNAL_STORAGE"); if
                             * (PackageManager.PERMISSION_GRANTED != permission) { showNoPermissionMsg(); }
                             * else { // 有权限，开始下载 new Thread(new Runnable() {
                             * 
                             * @Override public void run() { downLoadFile(latestVersion.getUrl()); }
                             * }).start(); } }
                             */
              // 直接跳转浏览器下载
              Uri uri = Uri.parse(latestVersion.url);
              Intent intent = new Intent(Intent.ACTION_VIEW, uri);
              mContext.startActivity(intent);
              dialog.dismiss();
            }
          }).setNegativeButton(btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              // 取消下载的逻辑
              dialog.dismiss();
            }
          }).setCancelable(false);
      alert.create().show();
    }
  
    /*
     * /** 没有给权限时跳转到浏览器打开apk下载链接
     *//*
        * private void showNoPermissionMsg() { Log.i(TAG, "showNoPermissionMsg");
        * AlertDialog.Builder builder = new AlertDialog.Builder(mContext); // 默认英语
        * String msg = MSG_ENG; String btn_ok = BTN_OK; String btn_cancel = BTN_CANCEL;
        * Locale locale = Locale.getDefault(); if
        * (locale.getDisplayLanguage(Locale.CHINA).equals("中文")) { btn_ok = BTN_OK_CN;
        * btn_cancel = BTN_CANCEL_CN; if (locale.getCountry().equals("CN")) { // 简中 msg
        * = MSG_CN; } else if (locale.getCountry().equals("TW") ||
        * locale.getCountry().equals("HK")) { // 繁中 msg = MSG_TW; } }
        * builder.setMessage(msg); builder.setPositiveButton(btn_ok, new
        * DialogInterface.OnClickListener() {
        * 
        * @Override public void onClick(DialogInterface dialog, int which) { Uri uri =
        * Uri.parse(downloadUrl); Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        * mContext.startActivity(intent); dialog.dismiss(); } });
        * builder.setNegativeButton(btn_cancel, new DialogInterface.OnClickListener() {
        * 
        * @Override public void onClick(DialogInterface dialog, int which) {
        * dialog.dismiss(); } }); builder.create().show(); }
        */
  
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults)
        throws JSONException {
      Log.d(TAG, "onRequestPermissionResult: requestCode: " + requestCode);
      super.onRequestPermissionResult(requestCode, permissions, grantResults);
      if (requestCode == 1) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
          new Thread(new Runnable() {
            @Override
            public void run() {
              downLoadFile(latestVersion.getUrl());
            }
          }).start();
      } else
        Log.d(TAG, "" + requestCode);
    }
  
    @Override
    public void onDestroy() {
      mContext.unregisterReceiver(broadcastReceiver);
      super.onDestroy();
    }
  
    /**
     * 从json数据中解析出apk的下载链接
     */
    private void parseDownloadUrl(String data) {
      Log.i(TAG, "parseDownloadUrl");
      latestVersion = null;
      try {
        JSONObject jsonObject = new JSONObject(data);
        JSONObject android = new JSONObject(jsonObject.getString("android"));
        latestVersion = new Version();
        latestVersion.description = android.getString("description");
        latestVersion.summary = android.getString("summary");
        latestVersion.versionCode = android.getString("versionCode");
        latestVersion.version = android.getString("version");
        latestVersion.url = android.getString("package");
        Log.d(TAG, "parseNewVersion successful!" + latestVersion.getUrl());
      } catch (JSONException e) {
        e.printStackTrace();
      }
    }
  
    /**
     * 获取versionName
     *
     * @return 返回versionName
     */
    private String getVersionCode() {
      PackageInfo packageInfo = null;
      try {
        packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
        return packageInfo.versionName;
  
      } catch (PackageManager.NameNotFoundException e) {
        e.printStackTrace();
      }
      return packageInfo.versionName;
    }
  
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (null != intent.getAction() && intent.getAction().equals("android.intent.action.DOWNLOAD_COMPLETE")) {
          installApk(context);
        }
      }
    };
  
    private void installApk(Context context) {
      Log.i(TAG, "installApk");
      File apkFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/PTSAPP/ptsApp.apk");
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Uri contentUri = FileProvider.getUriForFile(context,
            context.getApplicationContext().getPackageName() + ".fileprovider", apkFile); // 与manifest中定义的provider中的authorities="com.wistron.ptsApp.fileprovider"保持一致
        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
      } else {
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
      }
      mContext.startActivity(intent);
    }
  
    /**
     * 版本信息类
     */
    class Version {
      private String summary;
      private String description;
  
      private String version;
      private String versionCode;
      private String url;
  
      public String getSummary() {
        return summary;
      }
  
      public void setSummary(String summary) {
        this.summary = summary;
      }
  
      public String getDescription() {
        return description;
      }
  
      public void setDescription(String description) {
        this.description = description;
      }
  
      public String getVersionCode() {
        return versionCode;
      }
  
      public void setVersionCode(String versionCode) {
        this.versionCode = versionCode;
      }
  
      public String getUrl() {
        return url;
      }
  
      public void setUrl(String url) {
        this.url = url;
      }
  
      public String getVersion() {
        return version;
      }
  
      public void setVersion(String version) {
        this.version = version;
      }
    }
  
    private Handler mHandler = new Handler() {
      @Override
      public void handleMessage(Message msg) {
        super.handleMessage(msg);
        switch (msg.what) {
        case HAS_NEW_VERSION:
          showUpdateDialog();
          break;
        default:
          break;
        }
      }
    };
}
