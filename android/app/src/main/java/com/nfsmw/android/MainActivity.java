package com.nfsmw.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public final class MainActivity extends Activity {
    static {
        System.loadLibrary("nfsmw_android");
    }

    private static native String nativeInitialize(String filesDir);

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(18);
        status.setPadding(32, 32, 32, 32);
        status.setText(nativeInitialize(getFilesDir().getAbsolutePath()));
        setContentView(status);
    }
}
