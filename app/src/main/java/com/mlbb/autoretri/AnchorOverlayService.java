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
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.Locale;

/**
 * Overlay: draggable orange dot + compact menu (live coords, SAVE writes config, CLOSE exits).
 * Center uses dot size (LayoutParams.width is often 0 before layout).
 */
public final class AnchorOverlayService extends Service {
    private static final int NOTIF_ID = 11;

    private WindowManager wm;
    private View anchor;
    private LinearLayout menu;
    private TextView coordsTv;
    private WindowManager.LayoutParams anchorLp;
    private WindowManager.LayoutParams menuLp;
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

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setPadding(dp(12), dp(10), dp(12), dp(10));
        menu.setBackgroundColor(0xEE1E1E1E);

        TextView title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(14f);
        title.setText("Retri anchor");
        menu.addView(title);

        coordsTv = new TextView(this);
        coordsTv.setTextColor(0xFFB0BEC5);
        coordsTv.setTextSize(12f);
        menu.addView(coordsTv);

        Button btnSave = new Button(this);
        btnSave.setText("SAVE");
        btnSave.setOnClickListener(v -> doSaveAndExit());
        menu.addView(btnSave);

        Button btnClose = new Button(this);
        btnClose.setText("CLOSE");
        btnClose.setOnClickListener(v -> stopSelf());
        menu.addView(btnClose);

        menuLp =
                new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        type,
                        flags,
                        PixelFormat.TRANSLUCENT);
        menuLp.gravity = Gravity.TOP | Gravity.START;
        menuLp.x = dp(12);
        menuLp.y = dp(72);

        try {
            wm.addView(anchor, anchorLp);
            wm.addView(menu, menuLp);
        } catch (Throwable t) {
            Toast.makeText(this, "Overlay failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        refreshCoordsLabel();

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
                                refreshCoordsLabel();
                                return true;
                            default:
                                return false;
                        }
                    }
                });

    }

    private void refreshCoordsLabel() {
        float cx = anchorLp.x + dotPx / 2f;
        float cy = anchorLp.y + dotPx / 2f;
        coordsTv.setText(String.format(Locale.US, "Center: %.0f , %.0f", cx, cy));
    }

    private void doSaveAndExit() {
        float cx = anchorLp.x + dotPx / 2f;
        float cy = anchorLp.y + dotPx / 2f;
        RetriPreferences.SaveResult r = RetriPreferences.saveTapAndSyncConfig(this, cx, cy);
        if (r.ok) {
            File cfg = new File(r.directory, SharedPaths.CONFIG);
            File st = new File(r.directory, SharedPaths.STATE);
            String extra =
                    st.isFile()
                            ? ""
                            : "\n(No state file yet — point ESP Pure to:\n" + r.directory.getAbsolutePath() + ")";
            Toast.makeText(
                            this,
                            String.format(Locale.US, "Saved → %s%s", cfg.getAbsolutePath(), extra),
                            Toast.LENGTH_LONG)
                    .show();
        } else {
            Toast.makeText(
                            this,
                            "Save failed: "
                                    + (r.error != null ? r.error : "?")
                                    + "\nTried: "
                                    + r.directory
                                    + "\nLogcat: MlbbAutoRetriFile MlbbAutoRetriStorage MlbbAutoRetriPrefs",
                            Toast.LENGTH_LONG)
                    .show();
        }
        stopSelf();
    }

    private void clampAnchorToScreen() {
        android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
        int maxX = Math.max(0, dm.widthPixels - dotPx);
        int maxY = Math.max(0, dm.heightPixels - dotPx);
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
            if (menu != null) wm.removeView(menu);
        } catch (Throwable ignored) {
        }
        try {
            if (anchor != null) wm.removeView(anchor);
        } catch (Throwable ignored) {
        }
        menu = null;
        anchor = null;
    }

    private void startForegroundNotif() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(
                new NotificationChannel("ov", "Overlay", NotificationManager.IMPORTANCE_LOW));
        Notification n =
                new Notification.Builder(this, "ov")
                        .setContentTitle("Retri anchor overlay")
                        .setContentText("Drag dot, SAVE")
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
