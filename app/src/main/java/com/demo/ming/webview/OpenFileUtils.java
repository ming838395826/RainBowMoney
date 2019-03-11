package com.demo.ming.webview;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.widget.Toast;


import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Created by Administrator on 2018/2/26.
 */

public class OpenFileUtils {

    /**声明各种类型文件的dataType**/
    private static final String DATA_TYPE_ALL = "*/*";//未指定明确的文件类型，不能使用精确类型的工具打开，需要用户选择
    private static final String DATA_TYPE_APK = "application/vnd.android.package-archive";
    private static final String DATA_TYPE_VIDEO = "video/*";
    private static final String DATA_TYPE_AUDIO = "audio/*";
    private static final String DATA_TYPE_HTML = "text/html";
    private static final String DATA_TYPE_IMAGE = "image/*";
    private static final String DATA_TYPE_PPT = "application/vnd.ms-powerpoint";
    private static final String DATA_TYPE_EXCEL = "application/vnd.ms-excel";
    private static final String DATA_TYPE_WORD = "application/msword";
    private static final String DATA_TYPE_CHM = "application/x-chm";
    private static final String DATA_TYPE_TXT = "text/plain";
    private static final String DATA_TYPE_PDF = "application/pdf";

    private Context mContext;

    /**
     * 获取对应文件的Uri
     * @param intent 相应的Intent
     * @param file 文件对象
     * @return
     */
    private static Uri getUri(Intent intent, File file) {
        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //判断版本是否在7.0以上
            uri = FileProvider.getUriForFile(MyApplication.instanse,
                    MyApplication.instanse.getPackageName() + ".fileprovider",
                            file);
            //添加这一句表示对目标应用临时授权该Uri所代表的文件
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }else {
            uri = Uri.fromFile(file);
        }
        return uri;
    }

    /**
     * 打开文件
     * @param filePath 文件的全路径，包括到文件名
     */
    public static void openFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()){
            //如果文件不存在
            Toast.makeText(MyApplication.instanse, "打开失败，原因：文件已经被移动或者删除", Toast.LENGTH_SHORT).show();
            return;
        }
        /* 取得扩展名 */
        String end = file.getName().substring(file.getName().lastIndexOf(".") + 1, file.getName().length()).toLowerCase(Locale.getDefault());
        /* 依扩展名的类型决定MimeType */
        Intent intent = null;
        if (end.equals("m4a") || end.equals("mp3") || end.equals("mid") || end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
            intent =  generateVideoAudioIntent(filePath,DATA_TYPE_AUDIO);
        }else if (end.equals("3gp") || end.equals("mp4")) {
            intent = generateVideoAudioIntent(filePath,DATA_TYPE_VIDEO);
        } else if (end.equals("jpg") || end.equals("gif") || end.equals("png") || end.equals("jpeg") || end.equals("bmp")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_IMAGE);
        } else if (end.equals("apk")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_APK);
        }else if (end.equals("html") || end.equals("htm")){
            intent = generateHtmlFileIntent(filePath);
        } else if (end.equals("ppt")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_PPT);
        } else if (end.equals("xls")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_EXCEL);
        } else if (end.equals("doc")||end.equals("docx")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_WORD);
        } else if (end.equals("pdf")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_PDF);
        } else if (end.equals("chm")) {
            intent = generateCommonIntent(filePath,DATA_TYPE_CHM);
        } else if (end.equals("txt")) {
            intent = generateCommonIntent(filePath, DATA_TYPE_TXT);
        } else {
            intent = generateCommonIntent(filePath,DATA_TYPE_ALL);
        }
        MyApplication.instanse.startActivity(intent);

    }

    /** * 产生打开视频或音频的Intent * @param filePath 文件路径 * @param dataType 文件类型 * @return */
    private static Intent generateVideoAudioIntent(String filePath, String dataType){
        Intent intent = new Intent(Intent.ACTION_VIEW); intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0); intent.putExtra("configchange", 0);
        File file = new File(filePath);
        intent.setDataAndType(getUri(intent,file), dataType);
        return intent;
    }

    /** * 产生打开网页文件的Intent * @param filePath 文件路径 * @return */
    private static Intent generateHtmlFileIntent(String filePath) {
        Uri uri = Uri.parse(filePath) .buildUpon() .encodedAuthority("com.android.htmlfileprovider") .scheme("content") .encodedPath(filePath) .build();
        Intent intent = new Intent(Intent.ACTION_VIEW); intent.setDataAndType(uri, DATA_TYPE_HTML);
        return intent;
    }

    /** * 产生除了视频、音频、网页文件外，打开其他类型文件的Intent
     * * @param filePath 文件路径 * @param dataType 文件类型
     * * @return */
    private static Intent generateCommonIntent(String filePath, String dataType) {
        Intent intent = new Intent(); intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(Intent.ACTION_VIEW); File file = new File(filePath);
        Uri uri = getUri(intent, file); intent.setDataAndType(uri, dataType);
        return intent;
    }


    /*
     *weifubin
     *
     */

    // 打开音频，视频以外的附件
    public static Intent opentAttatchmentFile(String filePath, String mimeType) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addCategory("android.intent.category.DEFAULT");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
            uri = Uri.fromFile(new File(filePath));
        } else {
            /**
             * 7.0 调用系统相机拍照不再允许使用Uri方式，应该替换为FileProvider
             * 并且这样可以解决MIUI系统上拍照返回size为0的情况
             */
            uri = FileProvider.getUriForFile(MyApplication.instanse.getBaseContext(),
                    BuildConfig.APPLICATION_ID + ".fileProvider",
                    new File(filePath));
        }
        if (!TextUtils.isEmpty(mimeType)) {
            mimeType = mimeType.toLowerCase().trim();
            if (mimeType.startsWith("image/")) {
                if(mimeType.indexOf("tiff")!=-1){
                    intent.setDataAndType(uri, "image/tiff");
                }else {
                    intent.setDataAndType(uri, "image/*");
                }
            } else if (mimeType.compareTo("application/pdf") == 0
                    || mimeType.compareTo("application/x-pdf") == 0) {
                intent.setDataAndType(uri, "application/pdf");
            } else if (mimeType.compareTo("application/vnd.ms-excel") == 0) {
                intent.setDataAndType(uri, "application/vnd.ms-excel");
            } else if (mimeType.compareTo("application/x-chm") == 0) {
                intent.setDataAndType(uri, "application/x-chm");
            } else if (mimeType.compareTo("application/msword") == 0) {
                intent.setDataAndType(uri, "application/msword");
            } else if (mimeType.compareTo("application/vnd.ms-powerpoint") == 0) {
                intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
            } else if (mimeType.compareTo("application/x-chm") == 0) {
                intent.setDataAndType(uri, "application/x-chm");
            } else if (mimeType.indexOf("rar")!=-1||mimeType.indexOf("zip")!=-1) {
                intent.setDataAndType(uri, "application/x-gzip");
            }else if (mimeType.compareTo("text/plain") == 0) {
                intent.setDataAndType(uri, mimeType);
            }else {
                intent.setDataAndType(uri, mimeType);
            }
        } else {
            intent.setDataAndType(uri, mimeType);
        }
        return intent;
    }

    // android获取一个用于打开音频文件的intent
    public static Intent getAudioFileIntent(String filePath) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(filePath));
        intent.setDataAndType(uri, "audio/*");
        return intent;
    }

    // android获取一个用于打开视频文件的intent
    public static Intent getVideoFileIntent(String filePath) {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("oneshot", 0);
        intent.putExtra("configchange", 0);
        Uri uri = Uri.fromFile(new File(filePath));
        intent.setDataAndType(uri, "video/*");
        return intent;
    }
//    // 判断Internet 是否有效
    public static boolean isIntentAvailable(Context context, Intent intent) {
        final PackageManager packageManager = context.getPackageManager();
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
                PackageManager.GET_ACTIVITIES);
        return list.size() > 0;
    }
}
