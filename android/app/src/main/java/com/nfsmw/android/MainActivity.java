package com.nfsmw.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.TextView;

public final class MainActivity extends Activity implements SurfaceHolder.Callback {
    static {
        System.loadLibrary("nfsmw_android");
    }

    private static native String nativeInitialize(String filesDir);
    private static native String nativeSurfaceReady(Surface surface);

    private TextView status;
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FrameLayout content = new FrameLayout(this);
        SurfaceView surface = new SurfaceView(this);
        surface.getHolder().addCallback(this);
        content.addView(surface, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setTextSize(18);
        status.setPadding(32, 32, 32, 32);
        status.setText(nativeInitialize(getFilesDir().getAbsolutePath()));
        content.addView(status);
        setContentView(content);
    }

    @Override public void surfaceCreated(SurfaceHolder holder) {
        status.setText(nativeSurfaceReady(holder.getSurface()));
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) {}
}
