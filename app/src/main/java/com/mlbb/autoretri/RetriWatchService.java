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

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Polls ESP Pure's {@code mlbb_retri_state.txt}; when trigger=1 fires accessibility tap.
 * Uses tolerant parser for glued/concatenated key=value text.
 */
public final class RetriWatchService extends Service {
    private static final String TAG = "MlbbAutoRetriWatch";
    private static final long TAP_COOLDOWN_MS = 600;

    private static final Set<String> STATE_KEYS;

    static {
        HashSet<String> s = new HashSet<>();
        Collections.addAll(
                s,
                "ver",
                "seq",
                "ts",
                "ready",
                "trigger",
                "tap_x",
                "tap_y",
                "retri_dmg",
                "best_hp",
                "best_id",
                "best_dist",
                "my_level");
        STATE_KEYS = Collections.unmodifiableSet(s);
    }

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private long lastTapMono = 0;
    private long lastNoStateLog = 0;
    private boolean prevTriggerHigh = false;
    /** Last seq we successfully tapped (while trigger may stay 1 with new seq each tick). */
    private long lastTappedSeq = -1;

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
        return RetriPreferences.effectiveHandoffDir(this);
    }

    private boolean masterEnabled() {
        return RetriPreferences.prefs(this).getBoolean(RetriPreferences.PREF_MASTER, true);
    }

    private static long parseLong(Map<String, String> m, String k, long def) {
        String v = m.get(k);
        if (v == null) return def;
        v = v.trim();
        int end = 0;
        while (end < v.length() && (Character.isDigit(v.charAt(end)) || v.charAt(end) == '-')) {
            end++;
        }
        if (end == 0) return def;
        try {
            return Long.parseLong(v.substring(0, end));
        } catch (Throwable e) {
            return def;
        }
    }

    private void loop() {
        while (running.get()) {
            try {
                Thread.sleep(16);
                if (!masterEnabled()) continue;
                File dir = resolveDir();
                File state = new File(dir, SharedPaths.STATE);
                if (!state.isFile()) {
                    long nowWall = System.currentTimeMillis();
                    if (nowWall - lastNoStateLog > 5000) {
                        lastNoStateLog = nowWall;
                        Log.w(
                                TAG,
                                "no "
                                        + SharedPaths.STATE
                                        + " in "
                                        + dir.getAbsolutePath()
                                        + " — ESP Pure must use same folder (set nativeSetRetriSharedDir / path).");
                    }
                    continue;
                }

                Map<String, String> sm;
                try {
                    sm = HandoffParse.parse(state, STATE_KEYS, 131072);
                } catch (Throwable e) {
                    Log.e(TAG, "state parse failed " + state.getAbsolutePath(), e);
                    continue;
                }

                long seq = parseLong(sm, "seq", -1);
                long trig = parseLong(sm, "trigger", -1);
                long ts = parseLong(sm, "ts", -1);

                boolean trigHigh = trig == 1;
                if (!trigHigh) {
                    prevTriggerHigh = false;
                    lastTappedSeq = -1;
                    continue;
                }

                boolean risingEdge = trigHigh && !prevTriggerHigh;
                prevTriggerHigh = true;

                boolean needTap = risingEdge || (seq >= 0 && seq > lastTappedSeq);
                if (!needTap) continue;

                long now = android.os.SystemClock.uptimeMillis();
                if (now - lastTapMono < TAP_COOLDOWN_MS) {
                    Log.d(TAG, "skip tap: cooldown seq=" + seq);
                    continue;
                }

                ConfigReader.Tap cfgTap = ConfigReader.readTap(dir);
                if (cfgTap == null) {
                    Log.e(TAG, "missing/bad " + SharedPaths.CONFIG + " in " + dir.getAbsolutePath());
                    continue;
                }

                double stx = 0, sty = 0;
                try {
                    String xs = sm.get("tap_x");
                    String ys = sm.get("tap_y");
                    if (xs != null) stx = Double.parseDouble(xs.trim());
                    if (ys != null) sty = Double.parseDouble(ys.trim());
                } catch (Throwable ignored) {
                }
                float tpx = cfgTap.x;
                float tpy = cfgTap.y;

                if (!RetriAccessibilityService.ready()) {
                    Log.w(
                            TAG,
                            "trigger=1 seq="
                                    + seq
                                    + " but Accessibility not ready — enable 'MLBB Auto Retri (tap)'");
                    prevTriggerHigh = false;
                    continue;
                }

                Log.d(
                        TAG,
                        String.format(
                                Locale.US,
                                "tap attempt seq=%d rising=%b cfg=(%.1f,%.1f) state tap=(%.1f,%.1f) ts=%d",
                                seq,
                                risingEdge,
                                tpx,
                                tpy,
                                stx,
                                sty,
                                ts));

                if (RetriAccessibilityService.dispatchTap(tpx, tpy)) {
                    lastTapMono = now;
                    lastTappedSeq = seq;
                    Log.i(
                            TAG,
                            String.format(
                                    Locale.US,
                                    "tap ok seq=%d %.1f %.1f ts=%d",
                                    seq,
                                    tpx,
                                    tpy,
                                    ts));
                } else {
                    Log.w(TAG, "dispatchGesture failed seq=" + seq + " — retry");
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
