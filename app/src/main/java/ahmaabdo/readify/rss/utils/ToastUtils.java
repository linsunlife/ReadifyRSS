package ahmaabdo.readify.rss.utils;

import ahmaabdo.readify.rss.MainApplication;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.StringRes;
import android.widget.Toast;

public class ToastUtils {

    public static void showShort(@StringRes int resId) {
        show(resId, Toast.LENGTH_SHORT);
    }

    public static void showShort(CharSequence text) {
        show(text, Toast.LENGTH_SHORT);
    }

    public static void showLong(@StringRes int resId) {
        show(resId, Toast.LENGTH_LONG);
    }

    public static void showLong(final CharSequence text) {
        show(text, Toast.LENGTH_LONG);
    }

    private static void show(@StringRes int resId, int duration) {
        CharSequence text = MainApplication.getContext().getText(resId);
        show(text, duration);
    }

    private static void show(final CharSequence text, final int duration) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(MainApplication.getContext(), "", duration);
                toast.setText(text);
                toast.show();
            }
        });
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

    public static void runOnUiThreadDelayed(Runnable runnable, long delayMillis) {
        new Handler(Looper.getMainLooper()).postDelayed(runnable, delayMillis);
    }

}
