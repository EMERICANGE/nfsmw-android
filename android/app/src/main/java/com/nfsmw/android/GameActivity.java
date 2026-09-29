package com.nfsmw.android;

import android.os.Bundle;
import android.os.Environment;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.libsdl.app.SDLActivity;

import java.io.File;

public final class GameActivity extends SDLActivity {
    private String gameRoot;
    private String userRoot;
    private String cacheRoot;

    @Override
    protected void onCreate(Bundle state) {
        File appRoot = new File(getFilesDir(), "nfsmw");
        gameRoot = new File(Environment.getExternalStorageDirectory(), "nsfmw-androidevolved").getAbsolutePath();
        userRoot = new File(appRoot, "user").getAbsolutePath();
        cacheRoot = new File(appRoot, "cache").getAbsolutePath();
        new File(userRoot).mkdirs();
        new File(cacheRoot).mkdirs();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        super.onCreate(state);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.FILL);
        ViewGroup root = findViewById(android.R.id.content);
        root.addView(new TouchControls(this), overlayParams);
    }

    private static native void nativeSetTouchState(int buttons, int steering,
                                                   int brake, int throttle);

    private static final class TouchControls extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float steering;
        private boolean up, down, left, right;
        private boolean gas, brake, nitro, handbrake, start;
        private boolean camera, lookBack;
        private float width, height;

        TouchControls(GameActivity activity) {
            super(activity);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            width = getWidth();
            height = getHeight();
            float scale = Math.min(width / 900f, height / 440f);
            float radius = 35f * scale;

            drawButton(canvas, width * .105f, height * .76f, radius, "<\nIZQ", left, scale, false);
            drawButton(canvas, width * .255f, height * .76f, radius, ">\nDER", right, scale, false);
            drawButton(canvas, width * .18f, height * .56f, radius, "^\nARRIBA", up, scale, false);
            drawButton(canvas, width * .18f, height * .91f, radius, "v\nABAJO", down, scale, false);
            drawButton(canvas, width * .87f, height * .76f, radius * 1.75f, "GAS", gas, scale, true);
            drawButton(canvas, width * .73f, height * .84f, radius * 1.38f, "FRENO", brake, scale, true);
            drawButton(canvas, width * .84f, height * .49f, radius * 1.22f, "B\nATRAS / N2O", nitro, scale, true);
            drawButton(canvas, width * .75f, height * .49f, radius * 1.05f, "A\nACEPTAR / MANO", handbrake, scale, true);
            drawButton(canvas, width * .95f, height * .18f, radius * .88f, "START", start, scale, true);
            drawButton(canvas, width * .75f, height * .24f, radius * .86f, "X\nCAMARA", camera, scale, true);
            drawButton(canvas, width * .88f, height * .24f, radius * .86f, "Y\nATRAS", lookBack, scale, true);

            paint.setColor(0xD9FFFFFF);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(10f * scale);
            canvas.drawText("DIRECCION", width * .18f, height * .39f, paint);
            canvas.drawText("CONDUCCION", width * .80f, height * .13f, paint);
        }

        private void drawButton(Canvas canvas, float x, float y, float radius,
                                String label, boolean pressed, float scale, boolean action) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0x55000000);
            canvas.drawCircle(x, y + 3f * scale, radius + 2f * scale, paint);
            paint.setColor(pressed ? 0xE6FF9A32 : (action ? 0xA9202B38 : 0x9E18232E));
            canvas.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(Math.max(1.5f, 1.3f * scale));
            paint.setColor(pressed ? 0xFFFFD18A : 0xBFEAF2FA);
            canvas.drawCircle(x, y, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            String[] lines = label.split("\n");
            float lineHeight = 14f * scale;
            float y0 = y - (lines.length - 1) * lineHeight * .5f;
            for (int i = 0; i < lines.length; i++) {
                paint.setTextSize((i == 0 && lines.length > 1 ? 12f : 8f) * scale);
                canvas.drawText(lines[i], x, y0 + i * lineHeight + paint.getTextSize() * .34f, paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (width <= 0 || height <= 0) {
                width = getWidth();
                height = getHeight();
            }
            if (event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                clearState();
                return true;
            }
            steering = 0f;
            up = down = left = right = false;
            gas = brake = nitro = handbrake = start = camera = lookBack = false;
            for (int i = 0; i < event.getPointerCount(); i++) {
                if ((event.getActionMasked() == MotionEvent.ACTION_UP ||
                        event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) &&
                        i == event.getActionIndex()) continue;
                float x = event.getX(i) / width;
                float y = event.getY(i) / height;
                float radius = 37f * Math.min(width / 900f, height / 440f);
                if (inside(x, y, .105f, .76f, radius)) left = true;
                else if (inside(x, y, .255f, .76f, radius)) right = true;
                else if (inside(x, y, .18f, .56f, radius)) up = true;
                else if (inside(x, y, .18f, .91f, radius)) down = true;
                else if (x > .79f && y > .56f) gas = true;
                else if (x > .64f && x < .80f && y > .66f) brake = true;
                else if (x > .77f && x < .92f && y > .34f && y < .62f) nitro = true;
                else if (x > .67f && x < .82f && y > .36f && y < .62f) handbrake = true;
                else if (x > .68f && x < .82f && y > .13f && y < .35f) camera = true;
                else if (x > .82f && x < .94f && y > .13f && y < .35f) lookBack = true;
                else if (x > .90f && y < .34f) start = true;
            }
            steering = left ? -1f : (right ? 1f : 0f);
            updateNativeState();
            invalidate();
            return true;
        }

        private void clearState() {
            steering = 0f;
            up = down = left = right = false;
            gas = brake = nitro = handbrake = start = camera = lookBack = false;
            updateNativeState();
            invalidate();
        }

        private void updateNativeState() {
            int buttons = (up ? 0x0001 : 0) | (down ? 0x0002 : 0) |
                    (left ? 0x0004 : 0) | (right ? 0x0008 : 0) |
                    (start ? 0x0010 : 0) | (handbrake ? 0x1000 : 0) |
                    (nitro ? 0x2000 : 0) | (camera ? 0x4000 : 0) |
                    (lookBack ? 0x8000 : 0);
            nativeSetTouchState(buttons, Math.round(steering * 32767f),
                    brake ? 255 : 0, gas ? 255 : 0);
        }

        private boolean inside(float x, float y, float centerX, float centerY, float radius) {
            float dx = (x - centerX) * width;
            float dy = (y - centerY) * height;
            return dx * dx + dy * dy <= radius * radius;
        }
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
                "--game_data_root=" + gameRoot,
                "--user_data_root=" + userRoot,
                "--cache_root=" + cacheRoot
        };
    }
}
