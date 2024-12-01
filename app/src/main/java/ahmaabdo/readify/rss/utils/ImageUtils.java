package ahmaabdo.readify.rss.utils;

import ahmaabdo.readify.rss.MainApplication;
import ahmaabdo.readify.rss.R;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

public class ImageUtils {

    public static void saveImage(final Bitmap bitmap, final String url) {
        new Thread() {
            @Override
            public void run() {
                Context context = MainApplication.getContext();
                try {
                    String contentType = getContentType(url);
                    Bitmap.CompressFormat format = determineFormat(contentType);

                    String suffix = format == Bitmap.CompressFormat.JPEG ? "jpg" : format.name().toLowerCase();
                    String name = new Date().getTime() + "." + suffix;
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    File file = new File(dir, name);
                    FileOutputStream outputStream = new FileOutputStream(file);
                    bitmap.compress(format, 100, outputStream);
                    outputStream.flush();
                    outputStream.close();

                    // 发送广播通知系统图库更新
                    context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                    ToastUtils.showShort(R.string.action_finished);
                } catch (IOException e) {
                    ToastUtils.showLong(String.format(context.getString(R.string.action_failed), e.getMessage()));
                }
            }
        }.start();
    }

    private static String getContentType(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.connect();
            return connection.getContentType();
        } catch (IOException e) {
            return null;
        }
    }

    private static Bitmap.CompressFormat determineFormat(String contentType) {
        if (contentType == null)
            return Bitmap.CompressFormat.JPEG;

        contentType = contentType.toLowerCase();
        if (contentType.contains("png"))
            return Bitmap.CompressFormat.PNG;
        else if (contentType.contains("gif"))
            return Bitmap.CompressFormat.PNG; // GIF 处理比较复杂，这里简化为 PNG
        else if (contentType.contains("webp"))
            return Bitmap.CompressFormat.WEBP;
        else
            return Bitmap.CompressFormat.JPEG;
    }
}
