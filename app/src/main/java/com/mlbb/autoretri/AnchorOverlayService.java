package com.mlbb.autoretri;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Floating draggable dot + SET ANCHOR — center of dot = spell tap. Small windows only (game stays touchable
 * elsewhere when {@link WindowManager.LayoutParams#FLAG_NOT_TOUCH_MODAL} is used; some OEMs still intercept).
 */
public final class AnchorOverlayService extends Service {
    private static final int NOTIF_ID = 11;

    private WindowManager wm;
    private View anchor;
    private TextView saveBtn;
    private WindowManager.LayoutParams anchorLp;
    private WindowManager.LayoutParams saveLp;
    private int dotPx;

    public static void start(android.content.Context ctx) {
        Intent i = new Intent(ctx, AnchorOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundNotif();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        dotPx = dp(52);
        int type =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE;
        int flags =
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;

        float sx = RetriPreferences.prefs(this).getFloat(RetriPreferences.PREF_TAP_X, 1575f);
        float sy = RetriPreferences.prefs(this).getFloat(RetriPreferences.PREF_TAP_Y, 661f);

        anchor = new View(this);
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(Color.parseColor("#F4511E"));
        anchor.setBackground(gd);

        anchorLp =
                new WindowManager.LayoutParams(
                        dotPx,
                        dotPx,
                        type,
                        flags,
                        PixelFormat.TRANSLUCENT);
        anchorLp.gravity = Gravity.TOP | Gravity.START;
        anchorLp.x = Math.round(sx - dotPx / 2f);
        anchorLp.y = Math.round(sy - dotPx / 2f);

        saveBtn = new TextView(this);
        saveBtn.setText("SET ANCHOR");
        saveBtn.setTextColor(Color.WHITE);
        saveBtn.setPadding(dp(20), dp(14), dp(20), dp(14));
        saveBtn.setBackgroundColor(0xDD1565C0);
        saveLp =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        flags,
                        PixelFormat.TRANSLUCENT);
        saveLp.gravity = Gravity.BOTTOM | Gravity.END;
        saveLp.x = dp(16);
        saveLp.y = dp(120);

        try {
            wm.addView(anchor, anchorLp);
            wm.addView(saveBtn, saveLp);
        } catch (Throwable t) {
            Toast.makeText(this, "Overlay failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        anchor.setOnTouchListener(
                new View.OnTouchListener() {
                    int basex, basey;
                    float downRawX, downRawY;

                    @Override
                    public boolean onTouch(View v, MotionEvent e) {
                        switch (e.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                basex = anchorLp.x;
                                basey = anchorLp.y;
                                downRawX = e.getRawX();
                                downRawY = e.getRawY();
                                return true;
                            case MotionEvent.ACTION_MOVE:
                                anchorLp.x = basex + Math.round(e.getRawX() - downRawX);
                                anchorLp.y = basey + Math.round(e.getRawY() - downRawY);
                                clampAnchorToScreen();
                                try {
                                    wm.updateViewLayout(anchor, anchorLp);
                                } catch (Throwable ignored) {
                                }
                                return true;
                            default:
                                return false;
                        }
                    }
                });

        saveBtn.setOnClickListener(
                v -> {
                    float cx = anchorLp.x + anchorLp.width / 2f;
                    float cy = anchorLp.y + anchorLp.height / 2f;
                    try {
                        RetriPreferences.saveTapAndSyncConfig(this, cx, cy);
                        Toast.makeText(
                                        this,
                                        String.format(java.util.Locale.US, "Saved tap_x=%.0f tap_y=%.0f", cx, cy),
                                        Toast.LENGTH_LONG)
                                .show();
                    } catch (Throwable t) {
                        Toast.makeText(this, "Save failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                    destroyViews();
                    stopSelf();
                });
    }

    private void clampAnchorToScreen() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxX = Math.max(0, dm.widthPixels - anchorLp.width);
        int maxY = Math.max(0, dm.heightPixels - anchorLp.height);
        if (anchorLp.x < 0) anchorLp.x = 0;
        if (anchorLp.y < 0) anchorLp.y = 0;
        if (anchorLp.x > maxX) anchorLp.x = maxX;
        if (anchorLp.y > maxY) anchorLp.y = maxY;
    }

    private int dp(int d) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(d * density);
    }

    private void destroyViews() {
        try {
            if (saveBtn != null) wm.removeView(saveBtn);
        } catch (Throwable ignored) {
        }
        try {
            if (anchor != null) wm.removeView(anchor);
        } catch (Throwable ignored) {
        }
        saveBtn = null;
        anchor = null;
    }

    private void startForegroundNotif() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel("ov", "Overlay", NotificationManager.IMPORTANCE_LOW));
        Notification n =
                new Notification.Builder(this, "ov")
                        .setContentTitle("Set Retri anchor")
                        .setContentText("Drag dot, tap SET ANCHOR")
                        .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                        .build();
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (Throwable t) {
            startForeground(NOTIF_ID, n);
        }
    }

    @Override
    public void onDestroy() {
        destroyViews();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
