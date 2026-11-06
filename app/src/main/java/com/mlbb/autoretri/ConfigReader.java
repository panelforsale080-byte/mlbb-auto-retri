package com.mlbb.autoretri;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/** Reads tap_x/tap_y from mlbb_retri_config.txt (saved by this app — use for gestures). */
public final class ConfigReader {
    private static final String TAG = "MlbbAutoRetriFile";

    private ConfigReader() {}

    public static final class Tap {
        public final float x;
        public final float y;

        public Tap(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    /** Returns null if file missing or parse fails. */
    public static Tap readTap(File dir) {
        File f = new File(dir, SharedPaths.CONFIG);
        if (!f.isFile()) {
            Log.w(TAG, "no config file: " + f.getAbsolutePath());
            return null;
        }
        float x = Float.NaN;
        float y = Float.NaN;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                if ("tap_x".equals(k)) x = Float.parseFloat(v);
                else if ("tap_y".equals(k)) y = Float.parseFloat(v);
            }
        } catch (Throwable e) {
            Log.e(TAG, "readTap parse failed: " + f.getAbsolutePath(), e);
            return null;
        }
        if (Float.isNaN(x) || Float.isNaN(y)) {
            Log.w(TAG, "readTap missing tap_x/tap_y in " + f.getAbsolutePath());
            return null;
        }
        return new Tap(x, y);
    }
}
