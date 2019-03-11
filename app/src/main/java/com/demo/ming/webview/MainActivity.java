package com.demo.ming.webview;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.smtt.export.external.interfaces.IX5WebSettings;
import com.tencent.smtt.sdk.CookieManager;
import com.tencent.smtt.sdk.DownloadListener;
import com.tencent.smtt.export.external.interfaces.SslError;
import com.tencent.smtt.export.external.interfaces.SslErrorHandler;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebSettings;
import com.tencent.smtt.sdk.WebView;
import com.tencent.smtt.sdk.WebViewClient;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    ProgressBar mProgressBarNormal;
    private RelativeLayout ll_contain;

    DownloadManager mDownloadManager;
    private  long mRequestId;

    private ImageView iv_back,iv_close;
    private TextView tv_title;
    private RelativeLayout rl_title;

    //通知栏
    private NotificationManager mNtfManager;
    private Notification.Builder mNtfBuilder;
    private static final int NOTIFICATION_FLAG = 1;
    private Activity mActivity;
    private Subscription mMainSubscription; //延迟

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE" };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    /**
     * 打开的Url
     */
    private String openUrl = "https://h5.chsd.vip";
//    private String openUrl = "https://www.pgyer.com/WpkZ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
//        getSupportActionBar().hide();// 隐藏ActionBar
//        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        //x5 设置键盘
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                | WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        ll_contain= (RelativeLayout) findViewById(R.id.ll_contain);
        iv_back= (ImageView) findViewById(R.id.iv_back);
        iv_close = (ImageView) findViewById(R.id.iv_close);
        tv_title=(TextView) findViewById(R.id.tv_title);
        rl_title= (RelativeLayout) findViewById(R.id.rl_title);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWebView.canGoBack()){
                    mWebView.goBack();
                }
            }
        });
        iv_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWebView.loadUrl(openUrl);
//                mWebView.clearHistory(); // 清除
            }
        });
        mActivity=this;
        initWebView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        try {
            //检测是否有写的权限
            int permission = ActivityCompat.checkSelfPermission(this,
                    "android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // 没有写的权限，去申请写的权限，会弹出对话框
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,REQUEST_EXTERNAL_STORAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    /**
     * 初始化WebView
     */
    private void initWebView() {
        //采用new WebView的方式进行动态的添加WebView
        //WebView 的包一定要注意不要导入错了
        //com.tencent.smtt.sdk.WebView;

        mWebView = new WebView(this);
        mProgressBarNormal=new ProgressBar(this);
        RelativeLayout.LayoutParams layoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);
        RelativeLayout.LayoutParams barLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.WRAP_CONTENT);
        barLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        mWebView.setLayoutParams(layoutParams);
        mProgressBarNormal.setLayoutParams(barLayoutParams);


        WebSettings settings = mWebView.getSettings();


        mWebView.setWebViewClient(new WebViewClient() {

            @Override
            public void doUpdateVisitedHistory(WebView webView, String s, boolean b) {
                super.doUpdateVisitedHistory(webView, s, b);
                if (s.equalsIgnoreCase("https://h5.chsd.vip/#/tabs/home")) {
                    webView.clearHistory();
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //设定加载开始的操作

            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //设定加载结束的操作
                CookieManager cookieManager = CookieManager.getInstance();
                MyApplication.cookie = cookieManager.getCookie(url);
                if(mWebView.canGoBack()){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            iv_back.setVisibility(View.VISIBLE);
                            iv_close.setVisibility(View.VISIBLE);
                            rl_title.setVisibility(View.VISIBLE);
                        }
                    });
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            iv_back.setVisibility(View.GONE);
                            iv_close.setVisibility(View.GONE);
                            rl_title.setVisibility(View.GONE);
                        }
                    });
                }
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                switch (errorCode) {

                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {//处理https请
                //handler.proceed();    //表示等待证书响应
                // handler.cancel();      //表示挂起连接，为默认方式
                // handler.handleMessage(null);    //可做其他处理
            }


        });


        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    String progress = newProgress + "%";

                } else {
                    // to do something...
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                tv_title.setText(title);
            }



        });

        settings.setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new JSInterface(), "native");
        settings.setUseWideViewPort(true); //将图片调整到适合webview的大小


        //设置加载图片
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setDefaultTextEncodingName("utf-8");// 避免中文乱码
        mWebView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        settings.setNeedInitialFocus(false);
        settings.setSupportZoom(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);//适应屏幕
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        settings.setLoadsImagesAutomatically(true);//自动加载图片
        mWebView.getSettings().setBlockNetworkImage(false); // 解决图片不显示
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ){
            mWebView.getSettings().setMixedContentMode(0);
        }
        settings.setCacheMode(WebSettings.LOAD_DEFAULT
                | WebSettings.LOAD_CACHE_ELSE_NETWORK);
        mWebView.setDownloadListener(new MyWebViewDownLoadListener());


        //将WebView添加到底部布局
        ll_contain.removeAllViews();
        ll_contain.addView(mWebView);
        ll_contain.addView(mProgressBarNormal);
        mProgressBarNormal.setVisibility(View.GONE);
        mWebView.loadUrl(openUrl);
//        initDownLoad();
    }

    DownloadObserver mDownloadObserver;
    public void initDownLoad(String url){
        mDownloadObserver = new DownloadObserver(new Handler());
        getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), true, mDownloadObserver);

        mDownloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "测试.pdf");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        mRequestId = mDownloadManager.enqueue(request);


    }

    private class MyWebViewDownLoadListener implements DownloadListener {


        private String tempUrl="";
        @Override
        public void onDownloadStart(String url, String s1, String s2, String s3, long l) {
//            Uri uri = Uri.parse(url);
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//            initDownLoad(url);
//            FileActivity.show(MainActivity.this, url);
//            if(tempUrl.equalsIgnoreCase(url)){
//                return;
//            }
            tempUrl=url;
//            Intent intent = new Intent(MainActivity.this, FileActivity.class);
//            Bundle bundle = new Bundle();
//            bundle.putSerializable("path", url);
//            intent.putExtras(bundle);
//            startActivity(intent);
            mProgressBarNormal.setVisibility(View.VISIBLE);
            downLoadFile(url);


        }

//        @Override
//        public void onDownloadStart(String url, String s1, byte[] bytes, String s2, String s3, String s4, long l, String s5, String s6) {
//            Uri uri = Uri.parse(url);
//            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
//            startActivity(intent);
//        }
//
//        @Override
//        public void onDownloadVideo(String s, long l, int i) {
//
//        }

    }

    private class DownloadObserver extends ContentObserver {

        private DownloadObserver(Handler handler){
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri){
            queryDownloadStatus();
        }
    }

    private void queryDownloadStatus(){
        DownloadManager.Query query = new DownloadManager.Query().setFilterById(mRequestId);
        Cursor cursor = null;
        try {
            cursor = mDownloadManager.query(query);
            if (cursor != null && cursor.moveToFirst()) {
                //已经下载的字节数
                int currentBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                //总需下载的字节数
                int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                //状态所在的列索引
                int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
//                Log.i("downloadUpdate: ", currentBytes + " " + totalBytes + " " + status);
//                mDownloadBtn.setText("正在下载：" + currentBytes + "/" + totalBytes);
                if (DownloadManager.STATUS_SUCCESSFUL == status ) {
//                    mDownloadBtn.setVisibility(View.GONE);
//                    mDownloadBtn.performClick();
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    //使用Webview的时候，返回键没有重写的时候会直接关闭程序，这时候其实我们要其执行的知识回退到上一步的操作
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //这是一个监听用的按键的方法，keyCode 监听用户的动作，如果是按了返回键，同时Webview要返回的话，WebView执行回退操作，因为mWebView.canGoBack()返回的是一个Boolean类型，所以我们把它返回为true
        if(keyCode== KeyEvent.KEYCODE_BACK&&mWebView.canGoBack()){
            mWebView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    //js调用本地
    private class JSInterface {
        @JavascriptInterface
        public void downLoadAndOpenFile(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressBarNormal.setVisibility(View.VISIBLE);
                    downLoadFile(url);
                }
            });
        }

        @JavascriptInterface
        public void canGoback(){
            if(mWebView.canGoBack()){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv_back.setVisibility(View.VISIBLE);
                        iv_close.setVisibility(View.VISIBLE);
                    }
                });
            }else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        iv_back.setVisibility(View.GONE);
                        iv_close.setVisibility(View.GONE);
                    }
                });
            }
        }

        @JavascriptInterface
        public void title(){

        }
    }

    public void downLoadFile(final String url) {

        LoadFileModel.loadPdfFile(url, new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
//                TLog.d(TAG, "下载文件-->onResponse");
                Headers headers = response.headers();
                String disposition = headers.get("Content-disposition");
                String fileName="";
                if(TextUtils.isEmpty(disposition)){
                    String[] split = url.split("/");
                    fileName=split[split.length-1];
                }else {
                    fileName = disposition.split(";")[1].split("=")[1];
                    fileName = fileName.substring(1, fileName.length() - 1);
                }
                try {
                    fileName = URLDecoder.decode(fileName, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                String cacheDir= Environment.getExternalStorageDirectory() +File.separator+"webview";
                File cacheFile = new File(cacheDir + File.separator + fileName);
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }
                if (cacheFile.exists()) {
//                    cacheFile.delete();
//                    mProgressBarNormal.setVisibility(View.GONE);
//                    openFile(cacheFile.getPath());
                    installAPk(cacheFile.getPath());
                } else {
                    InputStream is = null;
                    byte[] buf = new byte[2048];
                    int len = 0;
                    FileOutputStream fos = null;
                    try {
                        ResponseBody responseBody = response.body();
                        is = responseBody.byteStream();
                        long total = responseBody.contentLength();
                        File file1 = new File(cacheDir);
                        if (!file1.exists()) {
                            file1.mkdirs();
                        }
                        final File fileN = new File(cacheDir + File.separator + fileName);
                        if (!fileN.exists()) {
                            boolean mkdir = fileN.createNewFile();
                        }
                        fos = new FileOutputStream(fileN);

//                        mNtfManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//                        mNtfBuilder = new Notification.Builder(mActivity)
//                                .setSmallIcon(R.mipmap.ic_logo)
//                                .setTicker("开始下载")
//                                .setContentTitle(getString(R.string.app_name))
//                                .setContentText("正在下载APK");
//                        Notification ntf = mNtfBuilder.getNotification();
//                        ntf.flags |= Notification.FLAG_AUTO_CANCEL;
//                        mNtfManager.notify(NOTIFICATION_FLAG, ntf);

                        long sum = 0;
                        while ((len = is.read(buf)) != -1) {
                            fos.write(buf, 0, len);
                            sum += len;
                            int progress = (int) (sum * 1.0f / total * 100);
                            mProgressBarNormal.setProgress(progress);
//                            if ((sum == 0)
//                                    || (int) (total * 100 / sum) - 10 > sum){
//                                mNtfBuilder.setTicker("开始下载")
//                                        .setContentTitle("正在下载")
//                                        .setContentText("下载进度"+(int) sum * 100 / total+ "%");
//                                mNtfManager.notify(NOTIFICATION_FLAG, mNtfBuilder.getNotification());
//                            }
//                        TLog.d(TAG, "写入缓存文件" + fileN.getName() + "进度: " + progress);
                        }
                        fos.flush();
                        mProgressBarNormal.setVisibility(View.GONE);
                        installAPk(fileN.getPath());
//                        mMainSubscription= Observable.interval(0, 1, TimeUnit.SECONDS)
//                                .map(new Func1<Long, Long>() {
//                                    @Override public Long call(Long increaseTime) {
//                                        return 1 - increaseTime; } }) .
//                                        take(2)
//                                .observeOn(AndroidSchedulers.mainThread())
//                                .subscribe(new Subscriber<Long>() {
//                                    @Override
//                                    public void onCompleted() {
////                                        Uri uri;
////                                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
////                                            uri = Uri.fromFile(fileN);
////                                        } else {
////                                            /**
////                                             * 7.0 调用系统相机拍照不再允许使用Uri方式，应该替换为FileProvider
////                                             * 并且这样可以解决MIUI系统上拍照返回size为0的情况
////                                             */
////                                            uri = FileProvider.getUriForFile(MyApplication.instanse.getBaseContext(),
////                                                    BuildConfig.APPLICATION_ID + ".fileProvider",
////                                                    fileN);
////                                        }
////                                        Intent installIntent = new Intent(Intent.ACTION_VIEW);
////                                        installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
////                                        installIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                                        installIntent.setDataAndType(uri,"application/vnd.android.package-archive");
////                                        PendingIntent updatePendingIntent=PendingIntent.getActivity(getBaseContext(), 0, installIntent,0);
////                                        mNtfBuilder.setTicker("下载完成")
////                                                .setContentTitle("下载完成,点击安装")
////                                                .setContentText("点击安装")
////                                                .setContentIntent(updatePendingIntent);
////                                        mNtfManager.notify(NOTIFICATION_FLAG, mNtfBuilder.getNotification());
////                                        mNtfManager.cancel(NOTIFICATION_FLAG);
//                                        installAPk(fileN.getPath());
//                                    }
//                                    @Override
//                                    public void onError(Throwable e) {
//                                    }
//                                    @Override
//                                    public void onNext(Long aLong) {
//
//                                    }
//                                });
//                        openFile(fileN.getPath());
                    } catch (Exception e) {
                        mProgressBarNormal.setVisibility(View.GONE);
                        showToast("下载失败");
                    } finally {

                        try {
                            if (is != null)
                                is.close();
                        } catch (IOException e) {

                        }
                        try {
                            if (fos != null)
                                fos.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("报错", Log.getStackTraceString(t));
                mProgressBarNormal.setVisibility(View.GONE);
                showToast("下载失败");
            }
        });
    }

    public void openFile(String filePath) {
        String fFileType = filePath.substring(filePath.lastIndexOf(".")+1);
        if ("apk".equals(fFileType) ||"jpg".equals(fFileType) || "gif".equals(fFileType) || "png".equals(fFileType) || "jpeg".equals(fFileType) || "bmp".equals(fFileType) || "doc".equals(fFileType) || "docx".equals(fFileType) || "xls".equals(fFileType) || "pdf".equals(fFileType) || "ppt".equals(fFileType)) {
            String mimeType = "";
            if (fFileType != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fFileType);
                if (mimeType == null) {
                    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fFileType.toLowerCase());
                }
            }

            Intent intent = OpenFileUtils.opentAttatchmentFile(filePath, mimeType);
            if (OpenFileUtils.isIntentAvailable(this, intent)) {
                startActivity(intent);
            } else {
                showToast("附件打开失败！");

            }
            //打开文件
        } else {
            showToast("该附件不支持在客户端打开！");
            return;
        }
    }

    public void showToast(String message){
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }


    public void installAPk(String path){
        File apkfile = new File(path);
        if (!apkfile.exists()) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(MyApplication.instanse.getBaseContext(), BuildConfig.APPLICATION_ID + ".fileProvider", apkfile);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            //兼容8.0
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                boolean hasInstallPermission = getPackageManager().canRequestPackageInstalls();
                if (!hasInstallPermission) {
                    //请求安装未知应用来源的权限
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, 6666);
                }
            }
        } else {
            // 通过Intent安装APK文件
            intent.setDataAndType(Uri.parse("file://" + apkfile.toString()),
                    "application/vnd.android.package-archive");
        }
        if (getPackageManager().queryIntentActivities(intent, 0).size() > 0) {
           startActivity(intent);
        }

    }
}
