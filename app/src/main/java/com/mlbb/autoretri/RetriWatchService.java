package com.mlbb.autoretri;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls ESP Pure's {@code mlbb_retri_state.txt}; when trigger=1 fires accessibility tap at tap_x/tap_y.
 */
public final class RetriWatchService extends Service {
    private static final String TAG = "MlbbAutoRetriWatch";
    private static final long TAP_COOLDOWN_MS = 600;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private long lastTapMono = 0;
    /** Rising edge: only tap when trigger goes 0 -> 1 (ESP holds trigger=1 many ticks with new seq each time). */
    private boolean prevTriggerHigh = false;

    public static void start(Context ctx) {
        Intent i = new Intent(ctx, RetriWatchService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i);
        else ctx.startService(i);
    }

    public static void stop(Context ctx) {
        ctx.stopService(new Intent(ctx, RetriWatchService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        startFg();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (worker == null || !worker.isAlive()) {
            running.set(true);
            worker = new Thread(this::loop, "retri-watch");
            worker.start();
        }
        return START_STICKY;
    }

    private void startFg() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                    new NotificationChannel("ar", "Auto Retri", NotificationManager.IMPORTANCE_LOW));
            Notification n =
                    new Notification.Builder(this, "ar")
                            .setContentTitle("MLBB Auto Retri")
                            .setContentText("Watching state file")
                            .setSmallIcon(android.R.drawable.ic_media_play)
                            .build();
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    startForeground(9, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else startForeground(9, n);
            } catch (Throwable t) {
                startForeground(9, n);
            }
        }
    }

    private File resolveDir() {
        return RetriPreferences.resolveConfigDir(this);
    }

    private boolean masterEnabled() {
        return RetriPreferences.prefs(this).getBoolean(RetriPreferences.PREF_MASTER, true);
    }

    private void loop() {
        while (running.get()) {
            try {
                Thread.sleep(16);
                if (!masterEnabled()) continue;
                File dir = resolveDir();
                File state = new File(dir, SharedPaths.STATE);
                if (!state.isFile()) continue;
                long trig = -1;
                long seq = -1;
                long ts = -1;
                double stateTapX = 0, stateTapY = 0;
                try (BufferedReader br = new BufferedReader(new FileReader(state))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        int eq = line.indexOf('=');
                        if (eq <= 0) continue;
                        String k = line.substring(0, eq).trim();
                        String v = line.substring(eq + 1).trim();
                        switch (k) {
                            case "seq":
                                seq = Long.parseLong(v);
                                break;
                            case "trigger":
                                trig = Long.parseLong(v);
                                break;
                            case "ts":
                                ts = Long.parseLong(v);
                                break;
                            case "tap_x":
                                stateTapX = Double.parseDouble(v);
                                break;
                            case "tap_y":
                                stateTapY = Double.parseDouble(v);
                                break;
                            default:
                                break;
                        }
                    }
                }
                /* Gesture coords: prefer mlbb_retri_config.txt (set anchor / Save on this device).
                   State file tap_* can lag or differ if ESP Pure uses another folder. */
                ConfigReader.Tap cfgTap = ConfigReader.readTap(dir);
                float tpx = cfgTap != null ? cfgTap.x : (float) stateTapX;
                float tpy = cfgTap != null ? cfgTap.y : (float) stateTapY;
                boolean trigHigh = (trig == 1);
                boolean risingEdge = trigHigh && !prevTriggerHigh;
                prevTriggerHigh = trigHigh;

                if (!risingEdge) continue;

                long now = android.os.SystemClock.uptimeMillis();
                if (now - lastTapMono < TAP_COOLDOWN_MS) {
                    Log.d(TAG, "skip tap: cooldown");
                    continue;
                }
                if (!RetriAccessibilityService.ready()) {
                    Log.w(
                            TAG,
                            "trigger=1 but Accessibility not ready — enable 'MLBB Auto Retri (tap)' in Settings");
                    /* Do not consume edge; drop prev so next poll can retry after user enables a11y. */
                    prevTriggerHigh = false;
                    continue;
                }
                float x = tpx;
                float y = tpy;
                if (RetriAccessibilityService.dispatchTap(x, y)) {
                    lastTapMono = now;
                    Log.i(
                            TAG,
                            String.format(
                                    Locale.US,
                                    "tap ok seq=%d %.1f %.1f ts=%d (cfg=%b)",
                                    seq,
                                    x,
                                    y,
                                    ts,
                                    cfgTap != null));
                } else {
                    Log.w(TAG, "dispatchGesture failed — retry next trigger edge");
                    prevTriggerHigh = false;
                }
            } catch (InterruptedException e) {
                break;
            } catch (Throwable t) {
                Log.e(TAG, "loop", t);
            }
        }
    }

    @Override
    public void onDestroy() {
        running.set(false);
        if (worker != null) worker.interrupt();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
